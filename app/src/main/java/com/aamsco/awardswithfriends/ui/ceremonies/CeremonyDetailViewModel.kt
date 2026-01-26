package com.aamsco.awardswithfriends.ui.ceremonies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aamsco.awardswithfriends.data.model.Category
import com.aamsco.awardswithfriends.data.model.Ceremony
import com.aamsco.awardswithfriends.data.model.CompetitionStatus
import com.aamsco.awardswithfriends.data.model.Vote
import com.aamsco.awardswithfriends.data.repository.BillingRepository
import com.aamsco.awardswithfriends.data.repository.CeremonyRepository
import com.aamsco.awardswithfriends.data.repository.CompetitionRepository
import com.aamsco.awardswithfriends.data.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class CeremonyDetailUiState(
    val ceremony: Ceremony? = null,
    val categories: List<Category> = emptyList(),
    val votes: Map<String, Vote> = emptyMap(),  // categoryId -> Vote
    val isLoading: Boolean = true,
    val error: String? = null,
    val canVote: Boolean = false,  // True if user has paid or payment not required
    val openCompetitionCount: Int = 0,  // Number of open competitions for this ceremony
    val isVoting: Boolean = false,
    val voteSuccess: Boolean = false
)

@HiltViewModel
class CeremonyDetailViewModel @Inject constructor(
    private val ceremonyRepository: CeremonyRepository,
    private val competitionRepository: CompetitionRepository,
    private val billingRepository: BillingRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private var ceremonyId: String = ""
    private var ceremonyYear: String = ""
    private var event: String? = null

    private val _uiState = MutableStateFlow(CeremonyDetailUiState())
    val uiState: StateFlow<CeremonyDetailUiState> = _uiState.asStateFlow()

    init {
        observeBillingState()
    }

    fun initialize(ceremonyId: String, ceremonyYear: String, event: String?) {
        if (this.ceremonyId == ceremonyId) return
        this.ceremonyId = ceremonyId
        this.ceremonyYear = ceremonyYear
        this.event = event
        loadCeremony()
        loadCategories()
        loadCompetitions()
        loadVotes()
    }

    private fun observeBillingState() {
        viewModelScope.launch {
            combine(
                configRepository.requiresPaymentFlow(),
                billingRepository.hasCompetitionsAccess
            ) { requiresPayment, hasAccess ->
                !requiresPayment || hasAccess
            }.collect { canVote ->
                _uiState.update { it.copy(canVote = canVote) }
            }
        }
    }

    private fun loadCompetitions() {
        viewModelScope.launch {
            competitionRepository.competitionsFlow()
                .catch { /* ignore errors */ }
                .collect { competitions ->
                    // Filter competitions for this ceremony that are open
                    val openForCeremony = competitions.filter { comp ->
                        comp.ceremonyYear == ceremonyYear &&
                        comp.competitionStatus == CompetitionStatus.OPEN &&
                        (event == null || comp.event == null || comp.event == event)
                    }
                    _uiState.update { it.copy(openCompetitionCount = openForCeremony.size) }
                }
        }
    }

    private fun loadVotes() {
        viewModelScope.launch {
            ceremonyRepository.ceremonyVotesFlow(ceremonyYear, event)
                .catch { /* ignore errors */ }
                .collect { votes ->
                    _uiState.update { it.copy(votes = votes) }
                }
        }
    }

    private fun loadCeremony() {
        viewModelScope.launch {
            ceremonyRepository.ceremonyFlow(ceremonyId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { ceremony ->
                    _uiState.update { it.copy(ceremony = ceremony) }
                }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            ceremonyRepository.categoriesFlow(ceremonyYear, event)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { categories ->
                    val visibleCategories = categories
                        .filter { !it.isHidden }
                        .sortedBy { it.displayOrder }
                    _uiState.update {
                        it.copy(
                            categories = visibleCategories,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun castCeremonyVote(categoryId: String, nomineeId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isVoting = true, error = null) }
            try {
                ceremonyRepository.castCeremonyVote(ceremonyYear, categoryId, nomineeId)

                // Wait for vote confirmation from Firestore (with 5 second timeout)
                val confirmedVote = withTimeoutOrNull(5000L) {
                    ceremonyRepository.categoryVoteFlow(ceremonyYear, event, categoryId)
                        .first { vote -> vote?.nomineeId == nomineeId }
                }

                // Update local votes map immediately for instant UI feedback
                if (confirmedVote != null) {
                    _uiState.update { state ->
                        val updatedVotes = state.votes.toMutableMap()
                        updatedVotes[categoryId] = confirmedVote
                        state.copy(votes = updatedVotes, isVoting = false, voteSuccess = true)
                    }
                } else {
                    // Timeout - still dismiss since cloud function succeeded
                    _uiState.update { it.copy(isVoting = false, voteSuccess = true) }
                }

                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isVoting = false, error = e.message) }
            }
        }
    }

    fun clearVoteSuccess() {
        _uiState.update { it.copy(voteSuccess = false) }
    }
}
