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
import com.aamsco.awardswithfriends.ui.navigation.AppNavGraph
import com.aamsco.awardswithfriends.ui.theme.AwardsWithFriendsTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var hasRequestedPermission = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result - FCM works regardless, just controls display */ }

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
            }
        }

        enableEdgeToEdge()
        setContent {
            val authenticated by isAuthenticated.collectAsState()

            AwardsWithFriendsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Only render nav graph once auth state is known
                    authenticated?.let { isAuth ->
                        AppNavGraph(isAuthenticated = isAuth)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Request notification permission once activity is fully visible and user is authenticated
        if (isAuthenticated.value == true && !hasRequestedPermission) {
            hasRequestedPermission = true
            requestNotificationPermission()
        }
    }

    private fun registerFcmToken() {
        activityScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                cloudFunctionsDataSource.updateFcmToken(token)
                Log.d(TAG, "FCM token registered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM token", e)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
