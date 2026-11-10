package com.stockpulse.app.presentation.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.data.remote.currentFcmToken
import com.stockpulse.app.domain.repository.AlertsRepository
import com.stockpulse.app.domain.repository.DevicesResult
import com.stockpulse.app.domain.repository.LinkedDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DevicesUiState(
    val isLoading: Boolean = true,
    val devices: List<LinkedDevice> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

/**
 * FASE 9.5 - Dispositivos vinculados de verdad: muestra telefono(s)
 * y reloj(es) registrados con su token FCM en el backend.
 */
@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val alertsRepository: AlertsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DevicesUiState())
    val state: StateFlow<DevicesUiState> = _state

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            when (val r = alertsRepository.devices()) {
                is DevicesResult.Success -> _state.update {
                    it.copy(isLoading = false, devices = r.devices)
                }
                is DevicesResult.Failure -> _state.update {
                    it.copy(isLoading = false, errorMessage = r.message)
                }
            }
        }
    }

    /** Re-registra el token de ESTE telefono (por si cambio). */
    fun reRegisterPhone() {
        viewModelScope.launch {
            val token = currentFcmToken()
            if (token == null) {
                _state.update { it.copy(infoMessage = "El telefono aun no tiene token FCM disponible") }
                return@launch
            }
            when (val r = alertsRepository.registerDevice(token, "android-phone")) {
                is com.stockpulse.app.domain.repository.AlertsWriteResult.Success ->
                    _state.update { it.copy(infoMessage = "Telefono registrado de nuevo", isLoading = true) }
                is com.stockpulse.app.domain.repository.AlertsWriteResult.Failure ->
                    _state.update { it.copy(infoMessage = r.message) }
            }
            load()
        }
    }
}
