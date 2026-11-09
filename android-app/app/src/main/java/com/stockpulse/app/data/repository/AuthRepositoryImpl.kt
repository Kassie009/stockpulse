package com.stockpulse.app.data.repository

import com.stockpulse.app.data.local.SessionStore
import com.stockpulse.app.data.remote.AuthApi
import com.stockpulse.app.data.remote.AuthLoginRequest
import com.stockpulse.app.data.remote.AuthRegisterRequest
import com.stockpulse.app.data.remote.AuthResponse
import com.stockpulse.app.domain.repository.AuthRepository
import com.stockpulse.app.domain.repository.LoginResult
import com.stockpulse.app.domain.repository.RegisterResult
import com.stockpulse.app.domain.repository.UserSession
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import javax.inject.Inject

/**
 * Repositorio de autenticacion contra el backend.
 * El backend valida y entrega los tokens de Supabase Auth (seccion 4.4/12).
 */
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore
) : AuthRepository {

    override val sessionFlow: Flow<UserSession?> = sessionStore.sessionFlow

    override suspend fun register(
        fullName: String,
        email: String,
        password: String
    ): RegisterResult {
        val response = runCatching {
            authApi.register(AuthRegisterRequest(fullName, email, password))
        }.getOrElse { return RegisterResult.Failure("Sin conexion con el servidor") }

        if (!response.isSuccessful) {
            return RegisterResult.Failure(
                extractError(response) ?: "No se pudo crear la cuenta"
            )
        }

        val body = response.body() ?: return RegisterResult.Failure("Respuesta invalida")
        if (body.requiresEmailConfirmation == true) {
            return RegisterResult.NeedsEmailConfirmation(
                body.message ?: "Cuenta creada. Revisa tu correo para confirmar."
            )
        }
        val session = body.session ?: return RegisterResult.Failure("El servidor no entregó sesion")
        sessionStore.save(session.toDomain())
        return RegisterResult.Success
    }

    override suspend fun login(email: String, password: String): LoginResult {
        val response = runCatching {
            authApi.login(AuthLoginRequest(email, password))
        }.getOrElse { return LoginResult.Failure("Sin conexion con el servidor") }

        if (!response.isSuccessful) {
            return LoginResult.Failure(
                extractError(response) ?: "Correo o contraana incorrectos"
            )
        }

        val body = response.body() ?: return LoginResult.Failure("Respuesta invalida")
        val session = body.session
            ?: return LoginResult.Failure(body.error ?: "Login invalido")
        sessionStore.save(session.toDomain())
        return LoginResult.Success
    }

    override suspend fun logout() {
        sessionStore.clear()
    }

    private fun extractError(response: Response<AuthResponse>): String? =
        response.body()?.error
}

private fun com.stockpulse.app.data.remote.SessionDto.toDomain(): UserSession = UserSession(
    accessToken = accessToken ?: "",
    refreshToken = refreshToken ?: "",
    userId = user?.id ?: "",
    email = user?.email ?: ""
)
