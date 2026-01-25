package com.aamsco.awardswithfriends.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aamsco.awardswithfriends.data.model.Competition
import com.aamsco.awardswithfriends.data.model.EventTypeData
import com.aamsco.awardswithfriends.data.repository.BillingRepository
import com.aamsco.awardswithfriends.data.repository.CeremonyRepository
import com.aamsco.awardswithfriends.data.repository.CompetitionRepository
import com.aamsco.awardswithfriends.data.repository.ConfigRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CompetitionFilter {
    ALL, MINE, JOINED
}

data class HomeUiState(
    val competitions: List<Competition> = emptyList(),
    val eventTypes: Map<String, EventTypeData> = emptyMap(),
    val isLoading: Boolean = true,
    val filter: CompetitionFilter = CompetitionFilter.ALL,
    val error: String? = null,
    val requiresPayment: Boolean = true,
    val hasCompetitionsAccess: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val competitionRepository: CompetitionRepository,
    private val ceremonyRepository: CeremonyRepository,
    private val configRepository: ConfigRepository,
    private val billingRepository: BillingRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        loadCompetitions()
        loadEventTypes()
        observeConfig()
        observeBilling()
    }

    private fun loadCompetitions() {
        viewModelScope.launch {
            competitionRepository.competitionsFlow()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { competitions ->
                    _uiState.update {
                        it.copy(
                            competitions = competitions.sortedWith(
                                compareBy<Competition> { comp ->
                                    // Inactive at bottom
                                    if (comp.status == "inactive") 1 else 0
                                }.thenByDescending { comp ->
                                    // Then by date
                                    comp.createdAt?.seconds ?: 0
                                }
                            ),
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun loadEventTypes() {
        viewModelScope.launch {
            ceremonyRepository.eventTypesFlow()
                .catch { /* ignore errors */ }
                .collect { eventTypes ->
                    _uiState.update {
                        // Store event types - we'll look up by slug or id
                        it.copy(eventTypes = eventTypes.flatMap { et ->
                            listOfNotNull(
                                et.slug.takeIf { s -> s.isNotEmpty() }?.let { s -> s to et },
                                et.id.takeIf { i -> i.isNotEmpty() }?.let { i -> i to et }
                            )
                        }.toMap())
                    }
                }
        }
    }

    fun getEventDisplayName(competition: Competition): String {
        val eventId = competition.event ?: return "Unknown Event"
        return _uiState.value.eventTypes[eventId]?.displayName ?: "Unknown Event"
    }

    private fun observeConfig() {
        viewModelScope.launch {
            configRepository.requiresPaymentFlow().collect { requiresPayment ->
                _uiState.update { it.copy(requiresPayment = requiresPayment) }
            }
        }
    }

    private fun observeBilling() {
        viewModelScope.launch {
            billingRepository.hasCompetitionsAccess.collect { hasAccess ->
                _uiState.update { it.copy(hasCompetitionsAccess = hasAccess) }
            }
        }
    }

    fun setFilter(filter: CompetitionFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun getFilteredCompetitions(): List<Competition> {
        val userId = currentUserId ?: return _uiState.value.competitions
        val competitions = _uiState.value.competitions
            // Filter out hidden competitions
            .filter { !it.hidden }
            // Filter out inactive competitions unless you're the owner
            .filter { it.status != "inactive" || it.createdBy == userId }

        return when (_uiState.value.filter) {
            CompetitionFilter.ALL -> competitions
            CompetitionFilter.MINE -> competitions.filter { it.createdBy == userId }
            CompetitionFilter.JOINED -> competitions.filter { it.createdBy != userId }
        }
    }

    fun canAccessCompetitions(): Boolean {
        val state = _uiState.value
        return !state.requiresPayment || state.hasCompetitionsAccess
    }

    fun isOwner(competition: Competition): Boolean {
        return competition.createdBy == currentUserId
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
