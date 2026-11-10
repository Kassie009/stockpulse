package com.stockpulse.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Endpoints de alertas + registro del dispositivo (FASE 7).
 */
interface AlertsApi {

    @POST("api/alerts")
    suspend fun create(@Body request: CreateAlertRequest): Response<CreateAlertResponse>

    @GET("api/alerts")
    suspend fun list(): Response<AlertListResponse>

    @DELETE("api/alerts")
    suspend fun delete(@retrofit2.http.Query("id") id: String): Response<SimpleResult>

    @POST("api/alerts/devices")
    suspend fun registerDevice(@Body request: DeviceRequest): Response<SimpleResult>

    @GET("api/alerts/devices")
    suspend fun devices(): Response<DeviceListResponse>
}

data class CreateAlertRequest(
    val symbol: String,
    val type: String,
    val threshold: Double
)

data class CreateAlertResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val alert: AlertDto? = null
)

data class AlertDto(
    val id: String,
    val symbol: String,
    val type: String,
    val threshold: Double? = null,
    val active: Boolean? = null,
    val last_triggered_at: String? = null,
    val created_at: String? = null
)

data class AlertListResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val alerts: List<AlertDto>? = null
)

data class DeviceRequest(
    val token: String,
    val platform: String = "android",
    val deviceName: String? = null
)

data class SimpleResult(
    val ok: Boolean? = null,
    val error: String? = null,
    val message: String? = null
)

data class DeviceListResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val devices: List<LinkedDeviceDto>? = null
)

data class LinkedDeviceDto(
    val id: String,
    val platform: String? = null,
    val device_name: String? = null,
    val last_seen_at: String? = null
)
