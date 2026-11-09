package com.stockpulse.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.domain.repository.AuthRepository
import com.stockpulse.app.domain.repository.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sesion global observable (para la puerta de entrada de la app).
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val session: StateFlow<UserSession?> = authRepository.sessionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
