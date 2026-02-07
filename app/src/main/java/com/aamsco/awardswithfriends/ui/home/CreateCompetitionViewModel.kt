package com.aamsco.awardswithfriends.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aamsco.awardswithfriends.data.model.Ceremony
import com.aamsco.awardswithfriends.data.repository.CeremonyRepository
import com.aamsco.awardswithfriends.data.repository.CompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateCompetitionUiState(
    val name: String = "",
    val ceremonies: List<Ceremony> = emptyList(),
    val selectedCeremonyId: String? = null,
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val error: String? = null,
    val createdInviteCode: String? = null
)

@HiltViewModel
class CreateCompetitionViewModel @Inject constructor(
    private val ceremonyRepository: CeremonyRepository,
    private val competitionRepository: CompetitionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateCompetitionUiState())
    val uiState: StateFlow<CreateCompetitionUiState> = _uiState.asStateFlow()

    init {
        loadCeremonies()
    }

    private fun loadCeremonies() {
        viewModelScope.launch {
            ceremonyRepository.ceremoniesFlow()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { ceremonies ->
                    val activeCeremonies = ceremonies.filter { it.status != "completed" }
                    _uiState.update { state ->
                        state.copy(
                            ceremonies = activeCeremonies,
                            selectedCeremonyId = state.selectedCeremonyId ?: activeCeremonies.firstOrNull()?.id,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun selectCeremony(ceremonyId: String) {
        _uiState.update { it.copy(selectedCeremonyId = ceremonyId) }
    }

    fun createCompetition() {
        val state = _uiState.value
        val selectedCeremony = state.ceremonies.find { it.id == state.selectedCeremonyId }
            ?: return

        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a competition name") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }

            try {
                val result = competitionRepository.createCompetition(
                    name = state.name.trim(),
                    ceremonyYear = selectedCeremony.year,
                    event = selectedCeremony.event
                )
                val inviteCode = result["inviteCode"] as? String
                _uiState.update { it.copy(isCreating = false, createdInviteCode = inviteCode) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreating = false, error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getSelectedCeremony(): Ceremony? {
        val state = _uiState.value
        return state.ceremonies.find { it.id == state.selectedCeremonyId }
    }

    fun isFormValid(): Boolean {
        val state = _uiState.value
        return state.name.isNotBlank() && state.selectedCeremonyId != null
    }
}
