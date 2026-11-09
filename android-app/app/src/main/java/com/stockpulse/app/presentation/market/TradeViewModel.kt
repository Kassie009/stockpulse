package com.stockpulse.app.presentation.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.domain.repository.TradeResult
import com.stockpulse.app.domain.repository.TradingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado del dialogo de compraventa (FASE 6).
 * La app NUNCA manda el precio: solo simbolo y cantidad (regla del prompt).
 */
data class TradeUiState(
    val isSubmitting: Boolean = false,
    val successMessage: String? = null,
    val newBalance: Double? = null,
    val profitLoss: Double? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class TradeViewModel @Inject constructor(
    private val tradingRepository: TradingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TradeUiState())
    val state: StateFlow<TradeUiState> = _state

    fun buy(symbol: String, quantity: Int) = submit(symbol, quantity, isBuy = true)

    fun sell(symbol: String, quantity: Int) = submit(symbol, quantity, isBuy = false)

    private fun submit(symbol: String, quantity: Int, isBuy: Boolean) {
        if (quantity < 1) {
            _state.update { it.copy(errorMessage = "La cantidad debe ser al menos 1 accion") }
            return
        }
        _state.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = if (isBuy) {
                tradingRepository.buy(symbol, quantity)
            } else {
                tradingRepository.sell(symbol, quantity)
            }
            when (result) {
                is TradeResult.Success -> _state.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = result.receipt.message,
                        newBalance = result.receipt.walletBalance,
                        profitLoss = result.receipt.profitLoss
                    )
                }
                is TradeResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, errorMessage = result.message)
                }
            }
        }
    }

    /** Reenvia el mismo pedido (util si el proveedor no tenia dato). */
    fun retryLast(symbol: String, action: String, quantity: Int) {
        clearOutcome()
        if (action == "buy") buy(symbol, quantity) else sell(symbol, quantity)
    }

    fun clearOutcome() {
        _state.update { TradeUiState() }
    }
}
