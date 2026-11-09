package com.stockpulse.app.data.repository

import com.google.gson.Gson
import com.stockpulse.app.data.remote.CheckoutRequest
import com.stockpulse.app.data.remote.PaymentsApi
import com.stockpulse.app.domain.repository.CheckoutLaunch
import com.stockpulse.app.domain.repository.CheckoutResult
import com.stockpulse.app.domain.repository.CreditPackage
import com.stockpulse.app.domain.repository.CreditPurchaseResult
import com.stockpulse.app.domain.repository.PackagesResult
import com.stockpulse.app.domain.repository.PaymentsRepository
import com.stockpulse.app.domain.repository.PurchaseStatusResult
import retrofit2.Response
import javax.inject.Inject

private data class PaymentsErrorEnvelope(val ok: Boolean? = null, val error: String? = null)

class PaymentsRepositoryImpl @Inject constructor(
    private val paymentsApi: PaymentsApi
) : PaymentsRepository {

    override suspend fun packages(): PackagesResult {
        val response = runCatching { paymentsApi.packages() }
            .getOrElse { return PackagesResult.Failure("Sin conexion con el servidor") }
        if (!response.isSuccessful) {
            return PackagesResult.Failure(extractError(response) ?: "No se pudo leer los paquetes")
        }
        val body = response.body()
            ?: return PackagesResult.Failure("Respuesta invalida del servidor")
        return PackagesResult.Success(
            body.packages.orEmpty().map {
                CreditPackage(it.code, it.name, it.priceMxn, it.credits)
            }
        )
    }

    override suspend fun checkout(packageCode: String): CheckoutResult {
        val response = runCatching { paymentsApi.checkout(CheckoutRequest(packageCode)) }
            .getOrElse { return CheckoutResult.Failure("Sin conexion con el servidor") }
        if (!response.isSuccessful) {
            return CheckoutResult.Failure(extractError(response) ?: "No se pudo iniciar el pago")
        }
        val body = response.body()
            ?: return CheckoutResult.Failure("Respuesta invalida del servidor")
        val url = body.checkoutUrl
        val sessionId = body.sessionId
        if (body.ok != true || url == null || sessionId == null) {
            return CheckoutResult.Failure(body.error ?: "Stripe no inicio el pago")
        }
        return CheckoutResult.Success(CheckoutLaunch(sessionId, url))
    }

    override suspend fun verify(sessionId: String): PurchaseStatusResult {
        val response = runCatching { paymentsApi.status(sessionId) }
            .getOrElse { return PurchaseStatusResult.Failure("Sin conexion con el servidor") }
        if (!response.isSuccessful) {
            return PurchaseStatusResult.Failure(extractError(response) ?: "No se pudo verificar el pago")
        }
        val body = response.body()
            ?: return PurchaseStatusResult.Failure("Respuesta invalida del servidor")
        return PurchaseStatusResult.Success(
            CreditPurchaseResult(
                paid = body.status == "paid",
                message = body.message ?: "Estado del pago: ${body.status}",
                creditsAdded = body.creditsAdded,
                newBalance = body.newBalance
            )
        )
    }

    private fun extractError(response: Response<*>): String? {
        val raw = response.errorBody()?.string() ?: return null
        return runCatching { Gson().fromJson(raw, PaymentsErrorEnvelope::class.java).error }.getOrNull()
    }
}
