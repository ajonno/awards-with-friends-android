package com.aamsco.awardswithfriends.ui.profile

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aamsco.awardswithfriends.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: FirebaseUser? = null,
    val isSigningOut: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                user = auth.currentUser,
                notificationsEnabled = FirebaseMessaging.getInstance().isAutoInitEnabled
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOut = true) }
            try {
                authRepository.signOut()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSigningOut = false, error = e.message) }
            }
        }
    }

    fun getProviderDisplayName(): String {
        // Skip "firebase" provider and find the actual sign-in provider
        val providerId = auth.currentUser?.providerData
            ?.map { it.providerId }
            ?.firstOrNull { it != "firebase" }
        return when (providerId) {
            "google.com" -> "Google"
            "apple.com" -> "Apple"
            "password" -> "Email"
            else -> providerId ?: "Unknown"
        }
    }

    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    fun getBuildNumber(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        } catch (e: Exception) {
            "1"
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        FirebaseMessaging.getInstance().isAutoInitEnabled = enabled
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun checkNotificationPermission(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
