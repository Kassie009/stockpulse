package com.stockpulse.app.data.remote

import retrofit2.http.GET

/**
 * Cliente Retrofit del backend STOCKPULSE (seccion 4.4 del prompt:
 * la app solo habla con NUESTRO backend; Finnhub nunca se consulta
 * directo desde la app).
 */
interface StockPulseApi {

    /** Diagnostico de integraciones (ruta real del backend, FASE 2). */
    @GET("api/health/integrations")
    suspend fun getIntegrationStatus(): IntegrationsResponse
}

// ── DTOs espejo de la respuesta del backend ──

data class IntegrationsResponse(
    val ok: Boolean? = null,
    val time: String? = null,
    val results: ResultsDto? = null
)

data class ResultsDto(
    val finnhub: FinnhubDto? = null,
    val supabase: StatusDto? = null,
    val firebase: FirebaseDto? = null
)

data class FinnhubDto(
    val ok: Boolean? = null,
    val aaplPrice: Double? = null,
    val changePercent: Double? = null,
    val timestamp: Long? = null
)

data class StatusDto(val ok: Boolean? = null, val error: String? = null)

data class FirebaseDto(val ready: Boolean? = null, val error: String? = null)
