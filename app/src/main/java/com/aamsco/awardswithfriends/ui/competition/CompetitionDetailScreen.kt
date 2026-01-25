package com.aamsco.awardswithfriends.ui.competition

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aamsco.awardswithfriends.data.model.CompetitionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionDetailScreen(
    competitionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    viewModel: CompetitionViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Initialize ViewModel with the competitionId
    LaunchedEffect(competitionId) {
        viewModel.initialize(competitionId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val competition = uiState.competition

    val isOwner = remember(uiState.competition) {
        viewModel.isOwner()
    }

    var showLeaveDialog by remember { mutableStateOf(false) }
    var showInactiveDialog by remember { mutableStateOf(false) }
    var showCopiedSnackbar by remember { mutableStateOf(false) }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Show copied snackbar
    LaunchedEffect(showCopiedSnackbar) {
        if (showCopiedSnackbar) {
            snackbarHostState.showSnackbar("Invite code copied!")
            showCopiedSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Competition") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isOwner) {
                        IconButton(onClick = {
                            // Show invite friends sheet
                            competition?.let { comp ->
                                val inviteMessage = "Join my ${comp.eventDisplayName} competition!\n\nUse invite code: ${comp.inviteCode}\n\nDownload Awards With Friends:\nhttps://play.google.com/store/apps/details?id=com.aamsco.awardswithfriends"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, inviteMessage)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Invite Friends"))
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Invite Friends",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading || competition == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Competition Info Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = competition.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isOwner) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Owner",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    StatusBadge(status = competition.competitionStatus)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${competition.ceremonyYear} ${competition.eventDisplayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${competition.participantCount} participants",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Invite Code Section (only visible to owner)
                    if (isOwner) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Invite Code",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = competition.inviteCode,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Copy button
                                        OutlinedIconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Invite Code", competition.inviteCode)
                                                clipboard.setPrimaryClip(clip)
                                                showCopiedSnackbar = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy"
                                            )
                                        }

                                        // Share button
                                        OutlinedIconButton(
                                            onClick = {
                                                val inviteMessage = "Join my ${competition.eventDisplayName} competition!\n\nUse invite code: ${competition.inviteCode}\n\nDownload Awards With Friends:\nhttps://play.google.com/store/apps/details?id=com.aamsco.awardswithfriends"
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, inviteMessage)
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Invite Friends"))
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share"
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Share this code with friends to invite them",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions Section - Leaderboard
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = onNavigateToLeaderboard
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Leaderboard",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Owner Actions Section
                    if (isOwner) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            onClick = { showInactiveDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isInactive = competition.competitionStatus == CompetitionStatus.INACTIVE
                                Icon(
                                    imageVector = if (isInactive) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = if (isInactive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isInactive) "Reactivate Competition" else "Set as Inactive",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isInactive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = if (isInactive) {
                                            "Make accessible to participants again"
                                        } else {
                                            "Hide from participants"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Leave Section (only for non-owners)
                    if (!isOwner) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            onClick = { showLeaveDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Leave Competition",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Leave confirmation dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Competition") },
            text = {
                Text("Are you sure you want to leave \"${competition?.name}\"? Your votes will be deleted.")ye
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        viewModel.leaveCompetition(onSuccess = onNavigateBack)
                    },
                    enabled = !uiState.isLeaving
                ) {
                    if (uiState.isLeaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Leave", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Set inactive confirmation dialog
    if (showInactiveDialog) {
        val isInactive = competition?.competitionStatus == CompetitionStatus.INACTIVE
        AlertDialog(
            onDismissRequest = { showInactiveDialog = false },
            title = { Text(if (isInactive) "Reactivate Competition" else "Set Competition as Inactive") },
            text = {
                Text(
                    if (isInactive) {
                        "This will make the competition accessible to all participants again."
                    } else {
                        "Participants will no longer be able to access this competition. You can reactivate it later."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInactiveDialog = false
                        viewModel.toggleInactive()
                    }
                ) {
                    Text(
                        text = if (isInactive) "Reactivate" else "Set Inactive",
                        color = if (isInactive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showInactiveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusBadge(status: CompetitionStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        CompetitionStatus.OPEN -> Triple(
            Color(0xFF4CAF50).copy(alpha = 0.1f),
            Color(0xFF4CAF50),
            "Open"
        )
        CompetitionStatus.LOCKED -> Triple(
            Color(0xFFFF9800).copy(alpha = 0.1f),
            Color(0xFFFF9800),
            "Voting Closed"
        )
        CompetitionStatus.COMPLETED -> Triple(
            Color(0xFF2196F3).copy(alpha = 0.1f),
            Color(0xFF2196F3),
            "Completed"
        )
        CompetitionStatus.INACTIVE -> Triple(
            Color(0xFF9E9E9E).copy(alpha = 0.1f),
            Color(0xFF9E9E9E),
            "Inactive"
        )
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
