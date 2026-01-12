package com.aamsco.awardswithfriends.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aamsco.awardswithfriends.data.model.Competition
import com.aamsco.awardswithfriends.ui.components.CompetitionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCompetition: (String) -> Unit,
    onNavigateToLeaderboard: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToJoin: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onShowInvite: (Competition) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Derive filtered competitions from the observed state so it updates in real-time
    val filteredCompetitions = remember(uiState.competitions, uiState.filter) {
        val userId = viewModel.currentUserId
        when (uiState.filter) {
            CompetitionFilter.ALL -> uiState.competitions
            CompetitionFilter.MINE -> uiState.competitions.filter { it.createdBy == userId }
            CompetitionFilter.JOINED -> uiState.competitions.filter { it.createdBy != userId }
        }
    }

    var showMenu by remember { mutableStateOf(false) }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Competitions",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Create Competition") },
                            onClick = {
                                showMenu = false
                                if (viewModel.canAccessCompetitions()) {
                                    onNavigateToCreate()
                                } else {
                                    onNavigateToPaywall()
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Join Competition") },
                            onClick = {
                                showMenu = false
                                if (viewModel.canAccessCompetitions()) {
                                    onNavigateToJoin()
                                } else {
                                    onNavigateToPaywall()
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                            }
                        )
                    }
                }
            }

            // Filter tabs
            if (uiState.competitions.isNotEmpty()) {
                FilterTabs(
                    selectedFilter = uiState.filter,
                    onFilterSelected = { viewModel.setFilter(it) }
                )
            }

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.competitions.isEmpty() -> {
                    EmptyState(
                        onCreateClick = {
                            if (viewModel.canAccessCompetitions()) {
                                onNavigateToCreate()
                            } else {
                                onNavigateToPaywall()
                            }
                        },
                        onJoinClick = {
                            if (viewModel.canAccessCompetitions()) {
                                onNavigateToJoin()
                            } else {
                                onNavigateToPaywall()
                            }
                        }
                    )
                }
                filteredCompetitions.isEmpty() -> {
                    FilteredEmptyState(
                        filter = uiState.filter,
                        onActionClick = {
                            if (viewModel.canAccessCompetitions()) {
                                if (uiState.filter == CompetitionFilter.MINE) {
                                    onNavigateToCreate()
                                } else {
                                    onNavigateToJoin()
                                }
                            } else {
                                onNavigateToPaywall()
                            }
                        }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredCompetitions,
                            key = { it.id }
                        ) { competition ->
                            CompetitionCard(
                                competition = competition,
                                isOwner = viewModel.isOwner(competition),
                                eventDisplayName = viewModel.getEventDisplayName(competition),
                                onClick = { onNavigateToCompetition(competition.id) },
                                onLeaderboardClick = { onNavigateToLeaderboard(competition.id) },
                                onShareClick = if (viewModel.isOwner(competition)) {
                                    { onShowInvite(competition) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabs(
    selectedFilter: CompetitionFilter,
    onFilterSelected: (CompetitionFilter) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedFilter.ordinal,
        modifier = Modifier.padding(horizontal = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        divider = {}
    ) {
        CompetitionFilter.entries.forEach { filter ->
            Tab(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                text = {
                    Text(
                        text = when (filter) {
                            CompetitionFilter.ALL -> "All"
                            CompetitionFilter.MINE -> "Mine"
                            CompetitionFilter.JOINED -> "Joined"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyState(
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏆",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Competitions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create a new competition or join one with an invite code",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Create Competition", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onJoinClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Join Competition", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FilteredEmptyState(
    filter: CompetitionFilter,
    onActionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏆",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (filter == CompetitionFilter.MINE) {
                "No Competitions Created"
            } else {
                "No Competitions Joined"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (filter == CompetitionFilter.MINE) {
                "You haven't created any competitions yet"
            } else {
                "You haven't joined any competitions from others"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onActionClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (filter == CompetitionFilter.MINE) {
                    "Create Competition"
                } else {
                    "Join Competition"
                }
            )
        }
    }
}
