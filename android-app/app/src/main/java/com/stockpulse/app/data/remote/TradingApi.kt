package com.stockpulse.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Endpoints del simulador de trading (FASE 6).
 * El precio SIEMPRE lo define el backend (Finnhub); la app solo manda
 * simbolo y cantidad.
 */
interface TradingApi {

    @GET("api/trading/summary")
    suspend fun summary(): Response<TradingSummaryResponse>

    @POST("api/trading/buy")
    suspend fun buy(@Body request: TradeRequest): Response<TradeResponse>

    @POST("api/trading/sell")
    suspend fun sell(@Body request: TradeRequest): Response<TradeResponse>

    @GET("api/trading/transactions")
    suspend fun transactions(): Response<TransactionsResponse>
}

data class TradeRequest(
    val symbol: String,
    val quantity: Int
)

data class TradingSummaryResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val summary: TradingSummaryDto? = null,
    val recordedAt: String? = null
)

data class TradingSummaryDto(
    val balance: Double? = null,
    val holdingsEnVivo: List<HoldingDto>? = null,
    val portfolioValue: Double? = null,
    val investedCost: Double? = null
)

data class HoldingDto(
    val symbol: String,
    val quantity: Int? = null,
    val avgPrice: Double? = null,
    val price: Double? = null,
    val changePercent: Double? = null,
    val marketValue: Double? = null,
    val unrealizedPnl: Double? = null,
    val available: Boolean = false
)

data class TradeResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val message: String? = null,
    val trade: TradeResultDto? = null
)

data class TradeResultDto(
    val transactionId: String? = null,
    val symbol: String? = null,
    val action: String? = null,
    val quantity: Int? = null,
    val unitPrice: Double? = null,
    val total: Double? = null,
    val profitLoss: Double? = null,
    val walletBalance: Double? = null
)

data class TransactionsResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val transactions: List<TransactionDto>? = null
)

data class TransactionDto(
    val id: String,
    val symbol: String,
    val type: String? = null,
    val quantity: Int? = null,
    val unit_price: Double? = null,
    val total: Double? = null,
    val profit_loss: Double? = null,
    val created_at: String? = null
)
