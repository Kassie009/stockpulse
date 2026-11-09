package com.stockpulse.app.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.domain.repository.AlertsReadResult
import com.stockpulse.app.domain.repository.AlertsRepository
import com.stockpulse.app.domain.repository.AlertsWriteResult
import com.stockpulse.app.domain.repository.UserAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tipos de alerta (mismos del backend/schema). */
data class AlertTypeOption(val value: String, val label: String, val isPercent: Boolean)

val ALERT_TYPES = listOf(
    AlertTypeOption("price_above", "Si el precio SUBE a (USD) o más", false),
    AlertTypeOption("price_below", "Si el precio BAJA a (USD) o menos", false),
    AlertTypeOption("change_pos", "Si el precio sube MÁS DE (%) en el día", true),
    AlertTypeOption("change_neg", "Si el precio cae MÁS DE (%) en el día", true),
    AlertTypeOption("volatility_high", "Si hay volatilidad alta (> % en el día)", true)
)

data class AlertsUiState(
    val isLoading: Boolean = true,
    val alerts: List<UserAlert> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isSubmitting: Boolean = false,
    // Formulario
    val symbol: String = "",
    val type: String = "price_above",
    val threshold: String = "",
    val thresholdLabelIndex: Int = 0
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertsRepository: AlertsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AlertsUiState())
    val state: StateFlow<AlertsUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(isLoading = it.alerts.isEmpty()) }
        viewModelScope.launch {
            when (val result = alertsRepository.list()) {
                is AlertsReadResult.Success -> _state.update {
                    it.copy(isLoading = false, alerts = result.alerts, errorMessage = null)
                }
                is AlertsReadResult.Failure -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun onSymbolChange(value: String) {
        _state.update { it.copy(symbol = value.take(12)) }
    }

    fun onTypeChange(value: String) {
        _state.update { it.copy(type = value) }
    }

    fun onThresholdChange(value: String) {
        _state.update { it.copy(threshold = value.filter { ch -> ch.isDigit() || ch == '.' }.take(10)) }
    }

    fun createAlert() {
        val s = _state.value
        val symbol = s.symbol.trim().uppercase()
        val threshold = s.threshold.toDoubleOrNull()

        when {
            symbol.isBlank() -> _state.update { it.copy(errorMessage = "Escribe el simbolo (ejemplo: AAPL)") }
            threshold == null || threshold <= 0 ->
                _state.update { it.copy(errorMessage = "El umbral debe ser un numero mayor a 0") }
            else -> {
                _state.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
                viewModelScope.launch {
                    when (val result = alertsRepository.create(symbol, s.type, threshold)) {
                        is AlertsWriteResult.Success -> _state.update {
                            it.copy(
                                isSubmitting = false,
                                infoMessage = result.message,
                                symbol = "",
                                threshold = ""
                            )
                        }
                        is AlertsWriteResult.Failure -> _state.update {
                            it.copy(isSubmitting = false, errorMessage = result.message)
                        }
                    }
                    // Recarga la lista para mostrar la nueva alerta
                    when (val r = alertsRepository.list()) {
                        is AlertsReadResult.Success -> _state.update {
                            it.copy(alerts = r.alerts)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    fun deleteAlert(id: String) {
        viewModelScope.launch {
            when (val result = alertsRepository.delete(id)) {
                is AlertsWriteResult.Success -> _state.update {
                    it.copy(infoMessage = result.message, alerts = it.alerts.filter { a -> a.id != id })
                }
                is AlertsWriteResult.Failure -> _state.update {
                    it.copy(errorMessage = result.message)
                }
            }
        }
    }
}
