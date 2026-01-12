package com.aamsco.awardswithfriends.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations for Navigation 3.
 * Keys are simple serializable data objects/classes.
 */

// Auth destinations
@Serializable
data object LoginDestination

@Serializable
data object EmailSignInDestination

// Main destinations
@Serializable
data object HomeDestination

// Competition destinations
@Serializable
data object CreateCompetitionDestination

@Serializable
data object JoinCompetitionDestination

@Serializable
data class CompetitionDetailDestination(val competitionId: String)

@Serializable
data class CategoryDetailDestination(
    val competitionId: String,
    val categoryId: String
)

@Serializable
data class LeaderboardDestination(val competitionId: String)

// Ceremony destinations
@Serializable
data class CeremonyDetailDestination(
    val ceremonyId: String,
    val ceremonyYear: String,
    val event: String?
)

// Paywall
@Serializable
data object PaywallDestination
