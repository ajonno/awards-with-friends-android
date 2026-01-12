package com.aamsco.awardswithfriends.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aamsco.awardswithfriends.data.repository.CompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinCompetitionUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val joinedCompetitionName: String? = null
)

@HiltViewModel
class JoinCompetitionViewModel @Inject constructor(
    private val competitionRepository: CompetitionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinCompetitionUiState())
    val uiState: StateFlow<JoinCompetitionUiState> = _uiState.asStateFlow()

    fun updateCode(code: String) {
        // Limit to 6 characters, uppercase only
        val sanitized = code.uppercase().filter { it.isLetterOrDigit() }.take(6)
        _uiState.update { it.copy(code = sanitized) }
    }

    fun joinCompetition() {
        val code = _uiState.value.code

        if (code.length != 6) {
            _uiState.update { it.copy(error = "Please enter a 6-character invite code") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val result = competitionRepository.joinCompetition(code)
                val competitionName = result["competitionName"] as? String ?: "Competition"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        joinedCompetitionName = competitionName
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to join competition"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun isCodeValid(): Boolean {
        return _uiState.value.code.length == 6
    }
}
