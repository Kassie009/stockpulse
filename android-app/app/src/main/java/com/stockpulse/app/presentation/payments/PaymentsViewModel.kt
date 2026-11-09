package com.stockpulse.app.presentation.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.domain.repository.CheckoutResult
import com.stockpulse.app.domain.repository.CreditPurchaseResult
import com.stockpulse.app.domain.repository.CreditPackage
import com.stockpulse.app.domain.repository.PackagesResult
import com.stockpulse.app.domain.repository.PaymentsRepository
import com.stockpulse.app.domain.repository.PurchaseStatusResult
import com.stockpulse.app.domain.repository.TradingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentsUiState(
    val isLoading: Boolean = true,
    val packages: List<CreditPackage> = emptyList(),
    val balance: Double? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    // Checkout activo
    val checkoutUrl: String? = null,
    val sessionId: String? = null,
    val isVerifying: Boolean = false,
    val purchaseMessage: String? = null,
    val purchasePaid: Boolean? = null,
    // Para saber si un error de checkout es del intento actual
    val purchasingPackage: String? = null
)

@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val paymentsRepository: PaymentsRepository,
    private val tradingRepository: com.stockpulse.app.domain.repository.TradingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentsUiState())
    val state: StateFlow<PaymentsUiState> = _state

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(isLoading = it.packages.isEmpty()) }
        viewModelScope.launch {
            val pkgsRes = paymentsRepository.packages()
            val balance = (tradingRepository.summary() as? com.stockpulse.app.domain.repository.PortfolioResult.Success)
                ?.summary?.balance

            when (pkgsRes) {
                is PackagesResult.Success -> _state.update {
                    it.copy(isLoading = false, packages = pkgsRes.packages, balance = balance, errorMessage = null)
                }
                is PackagesResult.Failure -> _state.update {
                    it.copy(isLoading = false, errorMessage = pkgsRes.message, balance = balance)
                }
            }
        }
    }

    fun startCheckout(packageCode: String) {
        _state.update { it.copy(purchasingPackage = packageCode, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = paymentsRepository.checkout(packageCode)) {
                is CheckoutResult.Success -> _state.update {
                    it.copy(
                        purchasingPackage = null,
                        checkoutUrl = result.launch.checkoutUrl,
                        sessionId = result.launch.sessionId
                    )
                }
                is CheckoutResult.Failure -> _state.update {
                    it.copy(purchasingPackage = null, errorMessage = result.message)
                }
            }
        }
    }

    /** Se llama cuando el WebView termina (retorno de Stripe). */
    fun onCheckoutClosed() {
        val sessionId = _state.value.sessionId ?: return
        _state.update { it.copy(isVerifying = true, checkoutUrl = null) }
        viewModelScope.launch {
            when (val result = paymentsRepository.verify(sessionId)) {
                is PurchaseStatusResult.Success -> {
                    val r: CreditPurchaseResult = result.result
                    // Nueva lectura del saldo tras acreditacion (si procede)
                    var balance = _state.value.balance
                    if (r.newBalance != null) balance = r.newBalance
                    else {
                        val s = (tradingRepository.summary() as? com.stockpulse.app.domain.repository.PortfolioResult.Success)?.summary
                        if (s != null) balance = s.balance
                    }
                    _state.update {
                        it.copy(
                            isVerifying = false,
                            sessionId = null,
                            purchaseMessage = r.message,
                            purchasePaid = r.paid,
                            balance = balance
                        )
                    }
                }
                is PurchaseStatusResult.Failure -> _state.update {
                    it.copy(isVerifying = false, sessionId = null, errorMessage = result.message)
                }
            }
        }
    }

    fun dismissCheckoutAndResult() {
        _state.update { it.copy(checkoutUrl = null, purchaseMessage = null, purchasePaid = null, sessionId = null) }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, infoMessage = null, purchaseMessage = null, purchasePaid = null) }
    }
}
