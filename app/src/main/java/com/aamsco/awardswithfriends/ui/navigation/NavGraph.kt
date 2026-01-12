package com.aamsco.awardswithfriends.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.aamsco.awardswithfriends.ui.auth.EmailSignInScreen
import com.aamsco.awardswithfriends.ui.auth.LoginScreen
import com.aamsco.awardswithfriends.ui.ceremonies.CeremonyDetailScreen
import com.aamsco.awardswithfriends.ui.competition.CategoryDetailScreen
import com.aamsco.awardswithfriends.ui.competition.CompetitionDetailScreen
import com.aamsco.awardswithfriends.ui.home.CreateCompetitionScreen
import com.aamsco.awardswithfriends.ui.home.JoinCompetitionScreen
import com.aamsco.awardswithfriends.ui.home.PaywallScreen
import com.aamsco.awardswithfriends.ui.leaderboard.LeaderboardScreen

@Composable
fun AppNavGraph(
    isAuthenticated: Boolean
) {
    val startDestination: Any = if (isAuthenticated) HomeDestination else LoginDestination
    val backStack = remember { mutableStateListOf<Any>(startDestination) }

    // State to request tab switch when navigating back to main screen
    var requestedTab by remember { mutableStateOf<String?>(null) }

    // Track current tab so it persists across navigation and process death
    var currentTab by rememberSaveable { mutableStateOf("ceremonies_tab") }

    // Handle auth state changes
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated && backStack.lastOrNull() !is HomeDestination) {
            backStack.clear()
            backStack.add(HomeDestination)
        } else if (!isAuthenticated && backStack.lastOrNull() !is LoginDestination) {
            backStack.clear()
            backStack.add(LoginDestination)
        }
    }

    // Consume back press at root to prevent app from closing
    // When at root (MainScreen), back button does nothing
    BackHandler(enabled = backStack.size == 1) {
        // At root - consume back press but don't close app
        // User can switch tabs manually or use home button to exit
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            // Only pop if not at root destination
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            // Auth screens
            entry<LoginDestination> {
                LoginScreen(
                    onNavigateToEmailSignIn = {
                        backStack.add(EmailSignInDestination)
                    }
                )
            }

            entry<EmailSignInDestination> {
                EmailSignInScreen(
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }

            // Main screen (with bottom navigation)
            entry<HomeDestination> {
                MainScreen(
                    backStack = backStack,
                    currentTab = currentTab,
                    onTabChanged = { newTab -> currentTab = newTab },
                    requestedTab = requestedTab,
                    onTabSwitched = { requestedTab = null }
                )
            }

            // Competition screens
            entry<CreateCompetitionDestination> {
                CreateCompetitionScreen(
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }

            entry<JoinCompetitionDestination> {
                JoinCompetitionScreen(
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }

            entry<CompetitionDetailDestination> { destination ->
                CompetitionDetailScreen(
                    competitionId = destination.competitionId,
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    },
                    onNavigateToCategory = { categoryId ->
                        backStack.add(
                            CategoryDetailDestination(
                                competitionId = destination.competitionId,
                                categoryId = categoryId
                            )
                        )
                    },
                    onNavigateToLeaderboard = {
                        backStack.add(LeaderboardDestination(destination.competitionId))
                    }
                )
            }

            entry<CategoryDetailDestination> { destination ->
                CategoryDetailScreen(
                    competitionId = destination.competitionId,
                    categoryId = destination.categoryId,
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }

            entry<LeaderboardDestination> { destination ->
                LeaderboardScreen(
                    competitionId = destination.competitionId,
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }

            entry<PaywallDestination> {
                PaywallScreen(
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }

            // Ceremony screens
            entry<CeremonyDetailDestination> { destination ->
                CeremonyDetailScreen(
                    ceremonyId = destination.ceremonyId,
                    ceremonyYear = destination.ceremonyYear,
                    event = destination.event,
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    },
                    onNavigateToCategory = { categoryId ->
                        // TODO: Navigate to browse category detail
                    },
                    onNavigateToCompetitions = {
                        // Pop back to main screen and switch to competitions tab
                        backStack.removeLastOrNull()
                        requestedTab = "competitions_tab"
                    }
                )
            }
        }
    )
}
