package com.aamsco.awardswithfriends.ui.ceremonies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aamsco.awardswithfriends.data.model.Ceremony
import com.aamsco.awardswithfriends.data.model.EventTypeData
import com.aamsco.awardswithfriends.data.repository.CeremonyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CeremoniesViewModel"

data class CeremoniesUiState(
    val ceremonies: List<Ceremony> = emptyList(),
    val eventTypes: Map<String, EventTypeData> = emptyMap(), // eventId -> EventTypeData
    val fetchedCategoryCounts: Map<String, Int> = emptyMap(), // ceremonyId -> count (for ceremonies with null categoryCount)
    val selectedEvent: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CeremoniesViewModel @Inject constructor(
    private val ceremonyRepository: CeremonyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CeremoniesUiState())
    val uiState: StateFlow<CeremoniesUiState> = _uiState.asStateFlow()

    init {
        loadEventTypes()
        loadCeremonies()
    }

    private fun loadEventTypes() {
        viewModelScope.launch {
            ceremonyRepository.eventTypesFlow()
                .catch { /* ignore event type loading errors */ }
                .collect { eventTypes ->
                    _uiState.update {
                        it.copy(eventTypes = eventTypes.associateBy { et -> et.slug })
                    }
                }
        }
    }

    private fun loadCeremonies() {
        viewModelScope.launch {
            ceremonyRepository.ceremoniesFlow()
                .catch { e ->
                    Log.e(TAG, "Error loading ceremonies", e)
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { ceremonies ->
                    val sortedCeremonies = ceremonies.sortedByDescending { c -> c.date?.seconds ?: 0 }

                    // Debug logging
                    sortedCeremonies.forEach { ceremony ->
                        Log.d(TAG, "Ceremony: ${ceremony.name}, id=${ceremony.id}, year=${ceremony.year}, event=${ceremony.event}, categoryCount=${ceremony.categoryCount}")
                    }

                    _uiState.update {
                        it.copy(
                            ceremonies = sortedCeremonies,
                            isLoading = false
                        )
                    }
                    // Fetch category counts for ceremonies that don't have them stored
                    val ceremoniesWithoutCount = sortedCeremonies.filter { it.categoryCount == null }
                    Log.d(TAG, "Ceremonies without categoryCount: ${ceremoniesWithoutCount.size}")
                    ceremoniesWithoutCount.forEach { ceremony ->
                        Log.d(TAG, "Fetching category count for: ${ceremony.name} (year=${ceremony.year}, event=${ceremony.event})")
                        fetchCategoryCount(ceremony.id, ceremony.year, ceremony.event)
                    }
                }
        }
    }

    private fun fetchCategoryCount(ceremonyId: String, year: String, event: String?) {
        viewModelScope.launch {
            ceremonyRepository.categoriesFlow(year, event)
                .catch { e ->
                    Log.e(TAG, "Error fetching categories for $ceremonyId (year=$year, event=$event)", e)
                }
                .collect { categories ->
                    // Filter out hidden categories like iOS does
                    val visibleCategories = categories.filter { !it.isHidden }
                    Log.d(TAG, "Fetched ${visibleCategories.size} categories for ceremony $ceremonyId")
                    _uiState.update { state ->
                        state.copy(
                            fetchedCategoryCounts = state.fetchedCategoryCounts + (ceremonyId to visibleCategories.size)
                        )
                    }
                }
        }
    }

    fun setEventFilter(event: String?) {
        _uiState.update { it.copy(selectedEvent = event) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
