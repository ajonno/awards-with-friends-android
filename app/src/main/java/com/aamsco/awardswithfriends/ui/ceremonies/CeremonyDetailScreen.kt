package com.aamsco.awardswithfriends.ui.ceremonies

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aamsco.awardswithfriends.data.model.Category
import com.aamsco.awardswithfriends.data.model.Nominee
import com.aamsco.awardswithfriends.data.model.Vote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CeremonyDetailScreen(
    ceremonyId: String,
    ceremonyYear: String,
    event: String?,
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToCompetitions: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CeremonyDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(ceremonyId, ceremonyYear, event) {
        viewModel.initialize(ceremonyId, ceremonyYear, event)
    }

    // Bottom sheet for category details
    selectedCategory?.let { category ->
        CategoryBottomSheet(
            category = category,
            currentVote = uiState.votes[category.id],
            canVote = uiState.openCompetitionCount > 0,
            isVoting = uiState.isVoting,
            sheetState = sheetState,
            onDismiss = { selectedCategory = null },
            onNavigateToCompetitions = {
                selectedCategory = null
                onNavigateToCompetitions()
            },
            onSubmitVote = { nomineeId ->
                viewModel.castCeremonyVote(
                    categoryId = category.id,
                    nomineeId = nomineeId,
                    onSuccess = { selectedCategory = null }
                )
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.ceremony?.name ?: "Loading...",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.categories.isNotEmpty()) {
                            Text(
                                text = "${uiState.categories.size} categories",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
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
            uiState.categories.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Categories",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Categories haven't been added yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Competition prompt section
                    item {
                        CompetitionPromptCard(
                            openCompetitionCount = uiState.openCompetitionCount,
                            onClick = onNavigateToCompetitions
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Categories list
                    items(
                        items = uiState.categories,
                        key = { it.id }
                    ) { category ->
                        CategoryRow(
                            category = category,
                            vote = uiState.votes[category.id],
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompetitionPromptCard(
    openCompetitionCount: Int,
    onClick: () -> Unit
) {
    val hasActiveCompetitions = openCompetitionCount > 0
    val iconTint = if (hasActiveCompetitions) Color(0xFF2196F3) else Color(0xFFFF9800) // Blue or Orange
    val textColor = if (hasActiveCompetitions) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFFFF9800) // Orange for "join" prompt
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = iconTint
            )
            Text(
                text = if (hasActiveCompetitions) {
                    "You can vote in $openCompetitionCount active ${if (openCompetitionCount == 1) "competition" else "competitions"}"
                } else {
                    "Join a competition to vote on awards!"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    vote: Vote?,
    onClick: () -> Unit
) {
    // Find the nominee for the vote
    val votedNominee = vote?.let { v ->
        category.nominees.find { it.id == v.nomineeId }
    }
    val votedNomineeName = votedNominee?.title
    val votedNomineeSubtitle = votedNominee?.subtitle

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Show vote status or nominee count
                if (votedNomineeName != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Your pick: $votedNomineeName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Show movie name if available
                    if (votedNomineeSubtitle != null) {
                        Text(
                            text = votedNomineeSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp) // Align with text above
                        )
                    }
                } else {
                    Text(
                        text = "${category.nominees.size} nominees",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Winner info
                category.winner?.let { winner ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFD700) // Gold
                        )
                        Text(
                            text = "Winner: ${winner.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50) // Green
                        )
                    }
                }
            }

            // Status icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (category.isVotingLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Voting locked",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFF9800) // Orange
                    )
                }

                if (category.hasWinner) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Winner announced",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFD700) // Gold
                    )
                }

                // Vote status indicator
                if (vote != null) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Voted",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF4CAF50) // Green
                    )
                } else if (category.isVotingLocked) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Not voted",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Not voted yet",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBottomSheet(
    category: Category,
    currentVote: Vote?,
    canVote: Boolean,
    isVoting: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onNavigateToCompetitions: () -> Unit,
    onSubmitVote: (nomineeId: String) -> Unit
) {
    // Initialize with current vote's nominee if exists
    var selectedNomineeId by remember(currentVote) {
        mutableStateOf(currentVote?.nomineeId)
    }

    // Voting is disabled if: locked, has winner, OR user can't vote (no open competitions)
    val isVotingDisabled = category.isVotingLocked || category.hasWinner || !canVote

    // Check if user has changed their selection
    val hasChanges = selectedNomineeId != null && selectedNomineeId != currentVote?.nomineeId

    val showSubmitButton = !category.isVotingLocked && !category.hasWinner

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable content area
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                // Status section
                if (category.isVotingLocked && !category.hasWinner) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF9800)
                            )
                            Text(
                                text = "Voting is locked for this category",
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }

                // Winner section
                category.winner?.let { winner ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFD700).copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Winner -", fontWeight = FontWeight.SemiBold)
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700)
                                    )
                                    Text(
                                        text = winner.title,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                winner.subtitle?.let { subtitle ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (winner.imageUrl.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    AsyncImage(
                                        model = winner.imageUrl,
                                        contentDescription = winner.title,
                                        modifier = Modifier
                                            .heightIn(max = 200.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }

                // Join competition prompt (show when user hasn't paid and voting is still open)
                if (!canVote && !category.isVotingLocked && !category.hasWinner) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Join a competition to vote on awards!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Create or join a competition with friends to start voting!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onNavigateToCompetitions
                                ) {
                                    Text("Go to Competitions")
                                }
                            }
                        }
                    }
                }

                // Nominees section header
                item {
                    Text(
                        text = "Nominees",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Nominees list
                if (category.nominees.isEmpty()) {
                    item {
                        Text(
                            text = "No nominees available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(
                        items = category.nominees,
                        key = { it.id }
                    ) { nominee ->
                        NomineeRow(
                            nominee = nominee,
                            isSelected = selectedNomineeId == nominee.id,
                            isWinner = category.winnerId == nominee.id,
                            isLocked = isVotingDisabled,
                            onClick = {
                                if (!isVotingDisabled) {
                                    selectedNomineeId = nominee.id
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // Fixed Submit Prediction button at bottom (only show if user can vote)
            if (showSubmitButton && canVote) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                if (selectedNomineeId != null) {
                                    onSubmitVote(selectedNomineeId!!)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = hasChanges && !isVoting
                        ) {
                            if (isVoting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (currentVote != null) "Update Prediction" else "Submit Prediction",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NomineeRow(
    nominee: Nominee,
    isSelected: Boolean,
    isWinner: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isWinner -> Color(0xFFFFD700).copy(alpha = 0.1f)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
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
            // Selection indicator
            if (!isLocked) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Nominee image
            if (nominee.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = nominee.imageUrl,
                    contentDescription = nominee.title,
                    modifier = Modifier
                        .size(50.dp, 70.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Nominee info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nominee.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                nominee.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Winner badge
            if (isWinner) {
                Surface(
                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
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
                            tint = Color(0xFFFFD700)
                        )
                        Text(
                            text = "Winner",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }
        }
    }
}
