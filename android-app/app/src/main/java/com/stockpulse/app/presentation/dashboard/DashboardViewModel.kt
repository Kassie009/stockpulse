package com.stockpulse.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.domain.repository.HealthRepository
import com.stockpulse.app.domain.repository.PortfolioResult
import com.stockpulse.app.domain.repository.SystemStatus
import com.stockpulse.app.domain.repository.TradingRepository
import com.stockpulse.app.domain.repository.TransactionView
import com.stockpulse.app.domain.repository.TransactionsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del Dashboard (MVVM, seccion 3 del prompt).
 * Estado del sistema + portafolio REAL del usuario.
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val systemStatus: SystemStatus? = null,
    val errorMessage: String? = null,
    // Datos reales del usuario
    val balance: Double? = null,
    val portfolioValue: Double? = null,
    val investedCost: Double? = null,
    val recentTransactions: List<TransactionView> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val tradingRepository: TradingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadSystemStatus()
        loadPortfolio()
    }

    fun loadSystemStatus() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            healthRepository.getSystemStatus()
                .onSuccess { status ->
                    _uiState.update { it.copy(isLoading = false, systemStatus = status) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Sin conexion al backend: ${error.message ?: "error desconocido"}"
                        )
                    }
                }
        }
    }

    fun loadPortfolio() {
        viewModelScope.launch {
            when (val r = tradingRepository.summary()) {
                is PortfolioResult.Success -> _uiState.update {
                    it.copy(
                        balance = r.summary.balance,
                        portfolioValue = r.summary.portfolioValue,
                        investedCost = r.summary.investedCost
                    )
                }
                is PortfolioResult.Failure -> Unit // sin sesion o backend caido: se parte NULL
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
