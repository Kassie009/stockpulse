package com.stockpulse.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.domain.repository.AuthRepository
import com.stockpulse.app.domain.repository.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sesion global observable (para la puerta de entrada de la app).
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val alertsRepository: com.stockpulse.app.domain.repository.AlertsRepository
) : ViewModel() {

    val session: StateFlow<UserSession?> = authRepository.sessionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** true cuando DataStore ya respondio (tenga o no sesion). */
    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady

    /** Registro del token FCM en cuanto hay sesion (una sola vez por login). */
    private var registeredForUserId: String? = null

    init {
        viewModelScope.launch {
            session.collect { session ->
                _sessionReady.value = true
                if (session != null && session.userId != registeredForUserId) {
                    registeredForUserId = session.userId
                    val token = com.stockpulse.app.data.remote.currentFcmToken()
                    if (token != null) {
                        val result = alertsRepository.registerDevice(token, "android-phone")
                        if (result is com.stockpulse.app.domain.repository.AlertsWriteResult.Failure) {
                            // No bloquea la app; el motor funciona de todos modos
                            android.util.Log.w(
                                "SessionViewModel",
                                "No se pudo registrar el token FCM: ${result.message}"
                            )
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
