package com.stockpulse.app.presentation.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.domain.repository.PortfolioResult
import com.stockpulse.app.domain.repository.PortfolioSummary
import com.stockpulse.app.domain.repository.TradingRepository
import com.stockpulse.app.domain.repository.TransactionView
import com.stockpulse.app.domain.repository.TransactionsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PortfolioUiState(
    val isLoading: Boolean = true,
    val summary: PortfolioSummary? = null,
    val transactions: List<TransactionView> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val tradingRepository: TradingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PortfolioUiState())
    val state: StateFlow<PortfolioUiState> = _state

    init {
        refresh()
        viewModelScope.launch {
            com.stockpulse.app.data.sync.SyncBus.signal.collect { refresh() }
        }
        // Auto-refresco cada 30s mientras Portafolio est visible
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000)
                refresh()
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(isLoading = it.summary == null) }
        viewModelScope.launch {
            // Portafolio + historial en paralelo (una sola pasada de lectura)
            val summaryResult = tradingRepository.summary()
            val txResult = tradingRepository.transactions()

            if (summaryResult is PortfolioResult.Failure) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = summaryResult.message,
                        transactions = if (txResult is TransactionsResult.Success) {
                            txResult.transactions
                        } else it.transactions
                    )
                }
                return@launch
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = null,
                    summary = (summaryResult as PortfolioResult.Success).summary,
                    transactions = if (txResult is TransactionsResult.Success) {
                        txResult.transactions
                    } else it.transactions
                )
            }
        }
    }
}
