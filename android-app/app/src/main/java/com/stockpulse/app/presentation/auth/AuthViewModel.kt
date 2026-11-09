package com.stockpulse.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpulse.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val authed: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun register(fullName: String, email: String, password: String, confirm: String) {
        clearMessages()
        when {
            fullName.trim().length < 2 -> _state.update { it.copy(errorMessage = "Escribe tu nombre") }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                _state.update { it.copy(errorMessage = "Correo invalido") }
            password.length < 8 ->
                _state.update { it.copy(errorMessage = "La contrasena debe tener al menos 8 caracteres") }
            password != confirm ->
                _state.update { it.copy(errorMessage = "Las contrasenas no coinciden") }
            else -> {
                _state.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    authRepository.register(fullName.trim(), email.trim(), password).let { result ->
                        when (result) {
                            is com.stockpulse.app.domain.repository.RegisterResult.Success ->
                                _state.update { it.copy(isLoading = false, authed = true) }

                            is com.stockpulse.app.domain.repository.RegisterResult.NeedsEmailConfirmation ->
                                _state.update {
                                    it.copy(isLoading = false, infoMessage = result.message)
                                }

                            is com.stockpulse.app.domain.repository.RegisterResult.Failure ->
                                _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                        }
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        clearMessages()
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(errorMessage = "Completa correo y contrasena") }
            return
        }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            authRepository.login(email.trim(), password).let { result ->
                when (result) {
                    is com.stockpulse.app.domain.repository.LoginResult.Success ->
                        _state.update { it.copy(isLoading = false, authed = true) }

                    is com.stockpulse.app.domain.repository.LoginResult.Failure ->
                        _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}
