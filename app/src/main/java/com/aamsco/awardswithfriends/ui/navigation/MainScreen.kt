package com.aamsco.awardswithfriends.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.aamsco.awardswithfriends.data.model.Competition
import com.aamsco.awardswithfriends.ui.ceremonies.CeremoniesScreen
import com.aamsco.awardswithfriends.ui.home.HomeScreen
import com.aamsco.awardswithfriends.ui.home.InviteBottomSheet
import com.aamsco.awardswithfriends.ui.profile.ProfileScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Ceremonies : BottomNavItem(
        route = "ceremonies_tab",
        title = "Ceremonies",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )

    data object Competitions : BottomNavItem(
        route = "competitions_tab",
        title = "Competitions",
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents
    )

    data object Profile : BottomNavItem(
        route = "profile_tab",
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

@Composable
fun MainScreen(
    backStack: SnapshotStateList<Any>,
    currentTab: String = "ceremonies_tab",
    onTabChanged: (String) -> Unit = {},
    requestedTab: String? = null,
    onTabSwitched: () -> Unit = {}
) {
    var showInviteSheet by remember { mutableStateOf<Competition?>(null) }

    // Tab order matches iOS: Ceremonies, Competitions, Profile
    val tabs = listOf(BottomNavItem.Ceremonies, BottomNavItem.Competitions, BottomNavItem.Profile)

    // Derive selectedTab from currentTab route
    val selectedTab = tabs.find { it.route == currentTab } ?: BottomNavItem.Ceremonies

    // Handle requested tab switch
    LaunchedEffect(requestedTab) {
        requestedTab?.let { tabRoute ->
            tabs.find { it.route == tabRoute }?.let { tab ->
                onTabChanged(tab.route)
                onTabSwitched()
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == item) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = selectedTab == item,
                        onClick = { onTabChanged(item.route) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            BottomNavItem.Ceremonies -> {
                CeremoniesScreen(
                    modifier = Modifier.padding(padding),
                    onNavigateToCeremony = { ceremonyId, ceremonyYear, event ->
                        backStack.add(CeremonyDetailDestination(ceremonyId, ceremonyYear, event))
                    }
                )
            }
            BottomNavItem.Competitions -> {
                HomeScreen(
                    modifier = Modifier.padding(padding),
                    onNavigateToCompetition = { competitionId ->
                        backStack.add(CompetitionDetailDestination(competitionId))
                    },
                    onNavigateToLeaderboard = { competitionId ->
                        backStack.add(LeaderboardDestination(competitionId))
                    },
                    onNavigateToCreate = {
                        backStack.add(CreateCompetitionDestination)
                    },
                    onNavigateToJoin = {
                        backStack.add(JoinCompetitionDestination)
                    },
                    onNavigateToPaywall = {
                        backStack.add(PaywallDestination)
                    },
                    onShowInvite = { competition ->
                        showInviteSheet = competition
                    }
                )
            }
            BottomNavItem.Profile -> {
                ProfileScreen(
                    modifier = Modifier.padding(padding)
                )
            }
        }

        // Invite bottom sheet
        showInviteSheet?.let { competition ->
            InviteBottomSheet(
                competition = competition,
                onDismiss = { showInviteSheet = null }
            )
        }
    }
}
