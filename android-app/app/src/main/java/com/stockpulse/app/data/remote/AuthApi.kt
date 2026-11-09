package com.stockpulse.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Endpoints de autenticacion del backend (Supabase Auth via api REST).
 */
interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthLoginRequest): Response<AuthResponse>
}

data class AuthRegisterRequest(
    val fullName: String,
    val email: String,
    val password: String
)

data class AuthLoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val message: String? = null,
    val requiresEmailConfirmation: Boolean? = null,
    val session: SessionDto? = null
)

data class SessionDto(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: UserDto? = null
)

data class UserDto(
    val id: String? = null,
    val email: String? = null
)
