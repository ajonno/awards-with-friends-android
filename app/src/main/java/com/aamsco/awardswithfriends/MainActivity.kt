package com.aamsco.awardswithfriends

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aamsco.awardswithfriends.data.source.CloudFunctionsDataSource
import com.aamsco.awardswithfriends.ui.components.NotificationPermissionOverlay
import com.aamsco.awardswithfriends.ui.navigation.AppNavGraph
import com.aamsco.awardswithfriends.ui.theme.AwardsWithFriendsTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var cloudFunctionsDataSource: CloudFunctionsDataSource

    private val isAuthenticated = MutableStateFlow<Boolean?>(null)
    private val showNotificationOverlay = MutableStateFlow(false)
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { showNotificationOverlay.value = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate
        // Keep splash visible until auth state is determined
        installSplashScreen().setKeepOnScreenCondition { isAuthenticated.value == null }

        super.onCreate(savedInstanceState)

        // Listen to auth state changes
        firebaseAuth.addAuthStateListener { auth ->
            val wasAuthenticated = isAuthenticated.value
            val nowAuthenticated = auth.currentUser != null
            isAuthenticated.value = nowAuthenticated

            if (nowAuthenticated && wasAuthenticated != true) {
                registerFcmToken()
                if (needsNotificationPermission()) {
                    activityScope.launch {
                        delay(1500)
                        showNotificationOverlay.value = true
                    }
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            val authenticated by isAuthenticated.collectAsState()
            val showOverlay by showNotificationOverlay.collectAsState()

            AwardsWithFriendsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Only render nav graph once auth state is known
                    authenticated?.let { isAuth ->
                        AppNavGraph(isAuthenticated = isAuth)

                        if (showOverlay) {
                            NotificationPermissionOverlay(
                                onEnable = {
                                    launchSystemPermissionRequest()
                                },
                                onDismiss = {
                                    showNotificationOverlay.value = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Show overlay on resume if permission still needed (e.g. returning from app restart while logged in)
        if (isAuthenticated.value == true && needsNotificationPermission()) {
            showNotificationOverlay.value = true
        }
    }

    private fun needsNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
    }

    private fun launchSystemPermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showNotificationOverlay.value = false
        }
    }

    private fun registerFcmToken() {
        activityScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                cloudFunctionsDataSource.updateFcmToken(token)
                Log.d(TAG, "FCM token registered: $token")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM token", e)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
