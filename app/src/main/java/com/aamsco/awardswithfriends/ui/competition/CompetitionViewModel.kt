package com.aamsco.awardswithfriends.ui.competition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aamsco.awardswithfriends.data.model.Category
import com.aamsco.awardswithfriends.data.model.Competition
import com.aamsco.awardswithfriends.data.model.CompetitionStatus
import com.aamsco.awardswithfriends.data.model.Vote
import com.aamsco.awardswithfriends.data.repository.CeremonyRepository
import com.aamsco.awardswithfriends.data.repository.CompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompetitionDetailUiState(
    val competition: Competition? = null,
    val categories: List<Category> = emptyList(),
    val votes: Map<String, Vote> = emptyMap(), // categoryId -> Vote
    val isLoading: Boolean = true,
    val error: String? = null,
    val isLeaving: Boolean = false
)

@HiltViewModel
class CompetitionViewModel @Inject constructor(
    private val competitionRepository: CompetitionRepository,
    private val ceremonyRepository: CeremonyRepository
) : ViewModel() {

    private var competitionId: String = ""

    private val _uiState = MutableStateFlow(CompetitionDetailUiState())
    val uiState: StateFlow<CompetitionDetailUiState> = _uiState.asStateFlow()

    fun initialize(competitionId: String) {
        if (this.competitionId == competitionId) return // Already initialized
        this.competitionId = competitionId
        loadCompetition()
        loadVotes()
    }

    private fun loadCompetition() {
        viewModelScope.launch {
            competitionRepository.competitionFlow(competitionId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { competition ->
                    _uiState.update { it.copy(competition = competition) }
                    competition?.let {
                        // Use ceremonyYear and event if available, otherwise fall back to ceremonyId
                        if (it.ceremonyYear.isNotEmpty()) {
                            loadCategories(it.ceremonyYear, it.event)
                        } else {
                            loadCategoriesByCeremonyId(it.ceremonyId)
                        }
                    }
                }
        }
    }

    private fun loadCategories(ceremonyYear: String, event: String?) {
        viewModelScope.launch {
            ceremonyRepository.categoriesFlow(ceremonyYear, event)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { categories ->
                    // Filter out hidden categories
                    val visibleCategories = categories.filter { !it.isHidden }
                    _uiState.update {
                        it.copy(
                            categories = visibleCategories.sortedBy { c -> c.displayOrder },
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun loadCategoriesByCeremonyId(ceremonyId: String) {
        viewModelScope.launch {
            ceremonyRepository.categoriesFlowByCeremonyId(ceremonyId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { categories ->
                    _uiState.update {
                        it.copy(
                            categories = categories.sortedBy { c -> c.displayOrder },
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun loadVotes() {
        viewModelScope.launch {
            competitionRepository.myVotesFlow(competitionId)
                .catch { /* ignore vote loading errors */ }
                .collect { votes ->
                    _uiState.update {
                        it.copy(votes = votes.associateBy { v -> v.categoryId })
                    }
                }
        }
    }

    fun getVotedCount(): Int {
        val state = _uiState.value
        return state.categories.count { state.votes.containsKey(it.id) }
    }

    fun isOwner(): Boolean {
        return _uiState.value.competition?.let {
            competitionRepository.isOwner(it)
        } ?: false
    }

    fun getVotedNomineeName(category: Category): String? {
        val vote = _uiState.value.votes[category.id] ?: return null
        return category.nominees.find { it.id == vote.nomineeId }?.title
    }

    fun leaveCompetition(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLeaving = true) }
            try {
                competitionRepository.leaveCompetition(competitionId)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLeaving = false, error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun toggleInactive() {
        val competition = _uiState.value.competition ?: return
        val isCurrentlyInactive = competition.competitionStatus == CompetitionStatus.INACTIVE
        val newInactiveState = !isCurrentlyInactive

        viewModelScope.launch {
            try {
                competitionRepository.setCompetitionInactive(competitionId, newInactiveState)
                // The Firestore listener will update the UI automatically
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
