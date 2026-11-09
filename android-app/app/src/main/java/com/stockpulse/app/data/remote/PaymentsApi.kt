package com.stockpulse.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Endpoints de pagos (FASE 8). El precio y la verificacion siempre
 * ocurren en el backend; la app solo pide el checkout y verifica.
 */
interface PaymentsApi {

    @GET("api/payments/packages")
    suspend fun packages(): Response<PackagesResponse>

    @POST("api/payments/checkout")
    suspend fun checkout(@Body request: CheckoutRequest): Response<CheckoutResponse>

    @GET("api/payments/status")
    suspend fun status(@Query("session_id") sessionId: String): Response<PaymentStatusResponse>
}

data class PackagesResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val packages: List<PackageDto>? = null
)

data class PackageDto(
    val code: String,
    val name: String,
    val priceMxn: Double,
    val credits: Int
)

data class CheckoutRequest(val packageCode: String)

data class CheckoutResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val sessionId: String? = null,
    val checkoutUrl: String? = null,
    val amount: Double? = null,
    val credits: Int? = null
)

data class PaymentStatusResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val status: String? = null,        // paid | pending | not_found
    val message: String? = null,
    val creditsAdded: Int? = null,
    val newBalance: Double? = null
)
