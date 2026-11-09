package com.stockpulse.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.domain.repository.HealthRepository
import com.stockpulse.app.domain.repository.SystemStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del Dashboard (MVVM, seccion 3 del prompt).
 * Carga el estado real del sistema desde el backend.
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val systemStatus: SystemStatus? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadSystemStatus()
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
}
