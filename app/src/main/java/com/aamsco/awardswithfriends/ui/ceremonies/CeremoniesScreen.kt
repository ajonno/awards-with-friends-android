package com.aamsco.awardswithfriends.ui.ceremonies

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aamsco.awardswithfriends.data.model.Ceremony
import com.aamsco.awardswithfriends.data.model.CeremonyStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CeremoniesScreen(
    onNavigateToCeremony: (ceremonyId: String, ceremonyYear: String, event: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CeremoniesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Helper to get event display name from the cached event types
    fun getEventDisplayName(eventId: String?): String {
        if (eventId == null) return "Unknown Event"
        return uiState.eventTypes[eventId]?.displayName ?: "Unknown Event"
    }

    // Derive filtered ceremonies from the observed state so it updates in real-time
    val filteredCeremonies = remember(uiState.ceremonies, uiState.selectedEvent, uiState.eventTypes) {
        val visibleCeremonies = uiState.ceremonies.filter { !it.hidden }
        if (uiState.selectedEvent == null) {
            visibleCeremonies
        } else {
            visibleCeremonies.filter { getEventDisplayName(it.event) == uiState.selectedEvent }
        }
    }

    val eventNames = remember(uiState.ceremonies, uiState.eventTypes) {
        uiState.ceremonies
            .filter { !it.hidden }
            .map { getEventDisplayName(it.event) }
            .distinct()
            .sorted()
    }

    Scaffold(
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Text(
                text = "Award Ceremonies",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Event filter chips
            if (uiState.ceremonies.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedEvent == null,
                        onClick = { viewModel.setEventFilter(null) },
                        label = { Text("All") }
                    )

                    eventNames.forEach { eventName ->
                        FilterChip(
                            selected = uiState.selectedEvent == eventName,
                            onClick = { viewModel.setEventFilter(eventName) },
                            label = { Text(eventName) }
                        )
                    }
                }
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
                uiState.ceremonies.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Ceremonies",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No ceremonies available yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                filteredCeremonies.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No ceremonies for this event",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredCeremonies,
                            key = { it.id }
                        ) { ceremony ->
                            CeremonyRow(
                                ceremony = ceremony,
                                eventDisplayName = getEventDisplayName(ceremony.event),
                                fetchedCategoryCount = uiState.fetchedCategoryCounts[ceremony.id],
                                onClick = { onNavigateToCeremony(ceremony.id, ceremony.year, ceremony.event) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CeremonyRow(
    ceremony: Ceremony,
    eventDisplayName: String,
    fetchedCategoryCount: Int?,
    onClick: () -> Unit
) {
    // Use stored categoryCount or fetched count
    val categoryCount = ceremony.categoryCount ?: fetchedCategoryCount

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trophy icon - darker gold in light mode for better visibility
            val trophyColor = if (isSystemInDarkTheme()) {
                Color(0xFFFFD700) // Bright gold for dark mode
            } else {
                Color(0xFFD4A800) // Darker gold for light mode
            }
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = trophyColor
            )

            // Ceremony info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ceremony.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = eventDisplayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ceremony.date?.let { timestamp ->
                    Text(
                        text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                            .format(Date(timestamp.seconds * 1000)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (categoryCount != null && categoryCount > 0) {
                        Text(
                            text = "$categoryCount categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    CeremonyStatusBadge(status = ceremony.ceremonyStatus)
                }
            }
        }
    }
}

@Composable
private fun CeremonyStatusBadge(status: CeremonyStatus) {
    val (text, backgroundColor, textColor) = when (status) {
        CeremonyStatus.UPCOMING -> Triple(
            "Upcoming",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        CeremonyStatus.LIVE -> Triple(
            "Live",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        CeremonyStatus.COMPLETED -> Triple(
            "Completed",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
