package com.aamsco.awardswithfriends.ui.home

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aamsco.awardswithfriends.data.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaywallUiState(
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val isLoading: Boolean = true,
    val priceText: String = "--",
    val error: String? = null,
    val purchaseSuccess: Boolean = false
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        observeBillingState()
    }

    private fun observeBillingState() {
        viewModelScope.launch {
            billingRepository.isLoading.collect { isLoading ->
                _uiState.update { it.copy(isLoading = isLoading) }
            }
        }

        viewModelScope.launch {
            billingRepository.productPrice.collect { price ->
                if (price != null) {
                    _uiState.update { it.copy(priceText = price) }
                }
            }
        }

        viewModelScope.launch {
            billingRepository.hasCompetitionsAccess.collect { hasAccess ->
                if (hasAccess) {
                    _uiState.update { it.copy(purchaseSuccess = true) }
                }
            }
        }
    }

    fun purchase(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, error = null) }
            try {
                val success = billingRepository.purchaseCompetitions(activity)
                if (!success) {
                    _uiState.update { it.copy(isPurchasing = false) }
                }
                // If success, the hasCompetitionsAccess flow will update
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isPurchasing = false, error = e.message ?: "Purchase failed")
                }
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, error = null) }
            try {
                val restored = billingRepository.restorePurchases()
                if (!restored) {
                    _uiState.update {
                        it.copy(isRestoring = false, error = "No previous purchases found")
                    }
                }
                // If restored, the hasCompetitionsAccess flow will update
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isRestoring = false, error = e.message ?: "Restore failed")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
