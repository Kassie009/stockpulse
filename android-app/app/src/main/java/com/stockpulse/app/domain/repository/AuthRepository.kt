package com.stockpulse.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Sesion de usuario activa.
 */
data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String
)

sealed interface RegisterResult {
    data object Success : RegisterResult
    data class NeedsEmailConfirmation(val message: String) : RegisterResult
    data class Failure(val message: String) : RegisterResult
}

sealed interface LoginResult {
    data object Success : LoginResult
    data class Failure(val message: String) : LoginResult
}

interface AuthRepository {
    /** Emite la sesion actual (o null si no hay nadie autenticado). */
    val sessionFlow: Flow<UserSession?>

    suspend fun register(fullName: String, email: String, password: String): RegisterResult
    suspend fun login(email: String, password: String): LoginResult
    suspend fun logout()
}
