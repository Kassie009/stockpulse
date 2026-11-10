package com.stockpulse.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.domain.repository.PortfolioResult
import com.stockpulse.app.domain.repository.TradingRepository
import com.stockpulse.app.domain.repository.TransactionView
import com.stockpulse.app.domain.repository.TransactionsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel del Dashboard (MVVM, seccion 3 del prompt).
 * Muestra el portafolio REAL del usuario (saldo, valor y movimientos).
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    // Datos reales del usuario
    val balance: Double? = null,
    val portfolioValue: Double? = null,
    val investedCost: Double? = null,
    val recentTransactions: List<TransactionView> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val tradingRepository: TradingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadPortfolio()
        viewModelScope.launch {
            com.stockpulse.app.data.sync.SyncBus.signal.collect { loadPortfolio() }
        }
        // Auto-refresco cada 30s mientras el Dashboard est visible
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                loadPortfolio()
            }
        }
    }

    fun loadPortfolio() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val r = tradingRepository.summary()) {
                is PortfolioResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        balance = r.summary.balance,
                        portfolioValue = r.summary.portfolioValue,
                        investedCost = r.summary.investedCost
                    )
                }
                is PortfolioResult.Failure -> _uiState.update {
                    it.copy(isLoading = false)
                }
            }
            when (val t = tradingRepository.transactions()) {
                is TransactionsResult.Success -> _uiState.update {
                    it.copy(recentTransactions = t.transactions.take(3))
                }
                is TransactionsResult.Failure -> Unit
            }
        }
    }
}
