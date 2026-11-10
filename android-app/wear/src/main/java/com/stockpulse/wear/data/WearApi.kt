package com.stockpulse.wear.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * API que consume el reloj directamente del backend StockPulse (FASE 9).
 * Mismas reglas que el telefono: precios reales en el servidor.
 */
interface WearApi {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/trading/summary")
    suspend fun summary(): Response<SummaryResponse>

    @GET("api/alerts")
    suspend fun alerts(): Response<AlertsResponse>

    @POST("api/alerts/devices")
    suspend fun registerDevice(@Body request: DeviceRequest): Response<SimpleResponse>
}

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val session: SessionDto? = null
)

data class SessionDto(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val email: String? = null
)

data class SummaryResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val summary: SummaryDto? = null,
    val recordedAt: String? = null
)

data class SummaryDto(
    val balance: Double? = null,
    val holdingsEnVivo: List<HoldingDto>? = null,
    val portfolioValue: Double? = null,
    val investedCost: Double? = null
)

data class HoldingDto(
    val symbol: String,
    val quantity: Int? = null,
    val price: Double? = null,
    val marketValue: Double? = null,
    val unrealizedPnl: Double? = null
)

data class AlertsResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val alerts: List<AlertDto>? = null
)

data class AlertDto(
    val id: String,
    val symbol: String,
    val type: String? = null,
    val threshold: Double? = null,
    val active: Boolean? = null
)

data class DeviceRequest(
    val token: String,
    val platform: String = "wearos",
    val deviceName: String? = "StockPulse Wear"
)

data class SimpleResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val message: String? = null
)
