package com.aamsco.awardswithfriends.ui.competition

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aamsco.awardswithfriends.data.model.Nominee
import com.aamsco.awardswithfriends.ui.components.TrailerPlayerActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    competitionId: String,
    categoryId: String,
    onNavigateBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    // Initialize ViewModel with the IDs
    LaunchedEffect(competitionId, categoryId) {
        viewModel.initialize(competitionId, categoryId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var trailerConfirmNominee by remember { mutableStateOf<Nominee?>(null) }

    // Trailer confirmation dialog
    trailerConfirmNominee?.let { nominee ->
        AlertDialog(
            onDismissRequest = { trailerConfirmNominee = null },
            title = { Text("Play trailer?") },
            text = { Text("Watch the ${nominee.title} trailer?") },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(context, TrailerPlayerActivity::class.java).apply {
                        putExtra(TrailerPlayerActivity.EXTRA_YOUTUBE_ID, nominee.trailerYouTubeId)
                    }
                    context.startActivity(intent)
                    trailerConfirmNominee = null
                }) {
                    Text("Play")
                }
            },
            dismissButton = {
                TextButton(onClick = { trailerConfirmNominee = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Derive computed values from the observed state so they update in real-time
    val canVote = remember(uiState.selectedNomineeId, uiState.isVoting, uiState.currentVote, uiState.category) {
        uiState.selectedNomineeId != null &&
            !uiState.isVoting &&
            uiState.selectedNomineeId != uiState.currentVote?.nomineeId &&
            uiState.category?.votingLocked != true
    }

    val hasExistingVote = remember(uiState.currentVote) {
        uiState.currentVote != null
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.category?.name ?: "Category") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.category?.votingLocked != true) {
                        TextButton(
                            onClick = { viewModel.castVote(onSuccess = onNavigateBack) },
                            enabled = canVote
                        ) {
                            if (uiState.isVoting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (hasExistingVote) "Update" else "Vote",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.category == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Category not found")
                }
            }
            else -> {
                val category = uiState.category!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status message if locked or has winner
                    if (category.isVotingLocked || category.hasWinner) {
                        item {
                            StatusMessage(
                                hasWinner = category.hasWinner,
                                isLocked = category.isVotingLocked
                            )
                        }
                    }

                    // Section header
                    item {
                        Text(
                            text = "Select your prediction",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Nominees list
                    items(
                        items = category.nominees,
                        key = { it.id }
                    ) { nominee ->
                        NomineeCard(
                            nominee = nominee,
                            categoryName = category.name,
                            isSelected = uiState.selectedNomineeId == nominee.id,
                            isWinner = category.winnerId == nominee.id,
                            isLocked = category.isVotingLocked,
                            onClick = { viewModel.selectNominee(nominee.id) },
                            onPlayTrailer = if (nominee.trailerYouTubeId != null) {
                                { trailerConfirmNominee = nominee }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(
    hasWinner: Boolean,
    isLocked: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (hasWinner) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (hasWinner) Icons.Default.EmojiEvents else Icons.Default.Lock,
                contentDescription = null,
                tint = if (hasWinner) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
            Text(
                text = if (hasWinner) "Winner announced" else "Voting is locked",
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasWinner) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
    }
}

@Composable
private fun NomineeCard(
    nominee: Nominee,
    categoryName: String,
    isSelected: Boolean,
    isWinner: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    onPlayTrailer: (() -> Unit)? = null
) {
    val isPeopleCategory = categoryName.lowercase().let {
        it.contains("actor") ||
        it.contains("actress") ||
        it.contains("director") ||
        it.contains("performer") ||
        it.contains("supporting")
    }

    val placeholderUrl = if (isPeopleCategory) {
        "https://awardswithfriends-25718.web.app/placeholders/person.svg"
    } else {
        "https://awardswithfriends-25718.web.app/placeholders/movie.svg"
    }

    val imageUrl = nominee.imageUrl.ifEmpty { placeholderUrl }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio button + image — taps to vote
            Row(
                modifier = Modifier.clickable(enabled = !isLocked, onClick = onClick),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSelected) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.RadioButtonUnchecked
                    },
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(28.dp)
                )

                AsyncImage(
                    model = imageUrl,
                    contentDescription = nominee.title,
                    modifier = Modifier
                        .size(50.dp, 70.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Text area — taps to play trailer (or vote if no trailer)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        enabled = !isLocked || onPlayTrailer != null,
                        onClick = onPlayTrailer ?: onClick
                    )
            ) {
                Text(
                    text = nominee.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isLocked && !isSelected && !isWinner) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                nominee.subtitle?.let { subtitle ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onPlayTrailer != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Trailer Available",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Winner badge
            if (isWinner) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFF3CD)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFB8860B)
                        )
                        Text(
                            text = "Winner",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB8860B)
                        )
                    }
                }
            }
        }
    }
}
