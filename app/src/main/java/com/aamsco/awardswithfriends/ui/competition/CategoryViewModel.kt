package com.aamsco.awardswithfriends.ui.competition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aamsco.awardswithfriends.data.model.Category
import com.aamsco.awardswithfriends.data.model.Vote
import com.aamsco.awardswithfriends.data.repository.CeremonyRepository
import com.aamsco.awardswithfriends.data.repository.CompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryDetailUiState(
    val category: Category? = null,
    val currentVote: Vote? = null,
    val selectedNomineeId: String? = null,
    val isLoading: Boolean = true,
    val isVoting: Boolean = false,
    val error: String? = null,
    val voteSuccess: Boolean = false
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val competitionRepository: CompetitionRepository,
    private val ceremonyRepository: CeremonyRepository
) : ViewModel() {

    private var competitionId: String = ""
    private var categoryId: String = ""

    private val _uiState = MutableStateFlow(CategoryDetailUiState())
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    fun initialize(competitionId: String, categoryId: String) {
        if (this.competitionId == competitionId && this.categoryId == categoryId) return // Already initialized
        this.competitionId = competitionId
        this.categoryId = categoryId
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
                    competition?.let {
                        // Use ceremonyYear and event if available
                        if (it.ceremonyYear.isNotEmpty()) {
                            loadCategory(it.ceremonyYear, it.event)
                        } else {
                            // Fallback - try to load from categories collection directly by ID
                            loadCategoryById()
                        }
                    }
                }
        }
    }

    private fun loadCategory(ceremonyYear: String, event: String?) {
        viewModelScope.launch {
            ceremonyRepository.categoryFlow(ceremonyYear, event, categoryId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { category ->
                    _uiState.update {
                        it.copy(
                            category = category,
                            isLoading = false,
                            // Initialize selectedNomineeId from current vote if not already set
                            selectedNomineeId = it.selectedNomineeId ?: it.currentVote?.nomineeId
                        )
                    }
                }
        }
    }

    private fun loadCategoryById() {
        // This is a fallback for older data - just set loading to false
        // The category detail screen will show an error state
        _uiState.update { it.copy(isLoading = false, error = "Category not found") }
    }

    private fun loadVotes() {
        viewModelScope.launch {
            competitionRepository.myVotesFlow(competitionId)
                .catch { /* ignore */ }
                .collect { votes ->
                    val vote = votes.find { it.categoryId == categoryId }
                    _uiState.update { state ->
                        state.copy(
                            currentVote = vote,
                            // Set selectedNomineeId from vote if not already selected by user
                            selectedNomineeId = state.selectedNomineeId ?: vote?.nomineeId
                        )
                    }
                }
        }
    }

    fun selectNominee(nomineeId: String) {
        val category = _uiState.value.category ?: return
        if (!category.isVotingLocked) {
            _uiState.update { it.copy(selectedNomineeId = nomineeId) }
        }
    }

    fun castVote(onSuccess: () -> Unit) {
        val nomineeId = _uiState.value.selectedNomineeId ?: return
        val category = _uiState.value.category ?: return

        if (category.isVotingLocked) return
        if (nomineeId == _uiState.value.currentVote?.nomineeId) return

        viewModelScope.launch {
            _uiState.update { it.copy(isVoting = true, error = null) }

            try {
                competitionRepository.castVote(competitionId, categoryId, nomineeId)
                _uiState.update { it.copy(isVoting = false, voteSuccess = true) }
                // Brief delay then navigate back
                kotlinx.coroutines.delay(300)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isVoting = false, error = e.message) }
            }
        }
    }

    fun canVote(): Boolean {
        val state = _uiState.value
        return state.selectedNomineeId != null &&
               !state.isVoting &&
               state.selectedNomineeId != state.currentVote?.nomineeId &&
               state.category?.votingLocked != true
    }

    fun hasExistingVote(): Boolean {
        return _uiState.value.currentVote != null
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
