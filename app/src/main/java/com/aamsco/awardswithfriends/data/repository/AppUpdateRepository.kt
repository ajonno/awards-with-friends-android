package com.aamsco.awardswithfriends.data.repository

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("app_update", Context.MODE_PRIVATE)
    }

    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable

    private val checkInterval = 24 * 60 * 60 * 1000L // 24 hours
    private val dismissCooldown = 3 * 24 * 60 * 60 * 1000L // 3 days
    private var latestVersion: String? = null

    suspend fun checkIfNeeded() {
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong("lastCheckDate", 0)
        if (now - lastCheck < checkInterval) return
        check()
    }

    private suspend fun check() {
        try {
            val packageName = context.packageName
            val currentVersion = context.packageManager
                .getPackageInfo(packageName, 0).versionName ?: return

            val storeVersion = withContext(Dispatchers.IO) {
                fetchPlayStoreVersion(packageName)
            } ?: return

            latestVersion = storeVersion

            val dismissedVersion = prefs.getString("dismissedVersion", null)
            val dismissedDate = prefs.getLong("dismissedDate", 0)
            val isNewer = compareVersions(storeVersion, currentVersion) > 0
            val wasDismissed = dismissedVersion == storeVersion &&
                    System.currentTimeMillis() - dismissedDate < dismissCooldown

            _updateAvailable.value = isNewer && !wasDismissed
            prefs.edit().putLong("lastCheckDate", System.currentTimeMillis()).apply()
        } catch (e: Exception) {
            // Silently fail
        }
    }

    fun dismiss() {
        _updateAvailable.value = false
        latestVersion?.let { version ->
            prefs.edit()
                .putString("dismissedVersion", version)
                .putLong("dismissedDate", System.currentTimeMillis())
                .apply()
        }
    }

    private fun fetchPlayStoreVersion(packageName: String): String? {
        return try {
            val url = "https://play.google.com/store/apps/details?id=$packageName&hl=en"
            val html = URL(url).readText()
            val regex = Regex("""\[\[\["(\d+\.\d+\.\d+)"\]\]""")
            regex.find(html)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }
}
