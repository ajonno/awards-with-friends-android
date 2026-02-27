package com.aamsco.awardswithfriends.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aamsco.awardswithfriends.data.model.Category
import com.aamsco.awardswithfriends.data.model.Competition
import com.aamsco.awardswithfriends.data.model.Participant
import com.aamsco.awardswithfriends.data.model.Vote
import com.aamsco.awardswithfriends.data.repository.CeremonyRepository
import com.aamsco.awardswithfriends.data.repository.CompetitionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val competition: Competition? = null,
    val participants: List<Participant> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val totalCategories: Int
        get() = categories.count { !it.isHidden }

    val completedCategories: Int
        get() = categories.count { !it.isHidden && it.hasWinner }

    val canViewPicks: Boolean
        get() = competition?.status != "open"
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val competitionRepository: CompetitionRepository,
    private val ceremonyRepository: CeremonyRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private var competitionId: String = ""

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    fun initialize(competitionId: String) {
        if (this.competitionId == competitionId) return // Already initialized
        this.competitionId = competitionId
        loadCompetition()
        loadParticipants()
    }

    private fun loadCompetition() {
        viewModelScope.launch {
            competitionRepository.competitionFlow(competitionId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { competition ->
                    _uiState.update { it.copy(competition = competition) }
                    // Load categories once we have the competition
                    if (competition != null) {
                        loadCategories(competition.ceremonyYear, competition.event)
                    }
                }
        }
    }

    private fun loadParticipants() {
        viewModelScope.launch {
            competitionRepository.participantsFlow(competitionId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { participants ->
                    _uiState.update {
                        it.copy(
                            participants = participants.sortedByDescending { p -> p.score },
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun loadCategories(ceremonyYear: String, event: String?) {
        viewModelScope.launch {
            ceremonyRepository.categoriesFlow(ceremonyYear, event)
                .catch { /* Silently fail - category counts are supplementary */ }
                .collect { categories ->
                    _uiState.update { it.copy(categories = categories) }
                }
        }
    }

    suspend fun votesForUser(userId: String): List<Vote> {
        return competitionRepository.votesForUser(competitionId, userId)
    }

    fun isCurrentUser(participant: Participant): Boolean {
        return participant.id == currentUserId
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
