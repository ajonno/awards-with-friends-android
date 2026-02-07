package com.aamsco.awardswithfriends

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.aamsco.awardswithfriends.ui.navigation.AppNavGraph
import com.aamsco.awardswithfriends.ui.theme.AwardsWithFriendsTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private val isAuthenticated = MutableStateFlow<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate
        // Keep splash visible until auth state is determined
        installSplashScreen().setKeepOnScreenCondition { isAuthenticated.value == null }

        super.onCreate(savedInstanceState)

        // Listen to auth state changes
        firebaseAuth.addAuthStateListener { auth ->
            isAuthenticated.value = auth.currentUser != null
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
}
