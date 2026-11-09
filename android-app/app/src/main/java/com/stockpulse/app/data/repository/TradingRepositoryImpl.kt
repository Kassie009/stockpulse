package com.stockpulse.app.data.repository

import com.google.gson.Gson
import com.stockpulse.app.data.remote.TradeRequest
import com.stockpulse.app.data.remote.TradeResponse
import com.stockpulse.app.data.remote.TradingApi
import com.stockpulse.app.data.remote.TradingSummaryResponse
import com.stockpulse.app.domain.repository.HoldingView
import com.stockpulse.app.domain.repository.PortfolioResult
import com.stockpulse.app.domain.repository.PortfolioSummary
import com.stockpulse.app.domain.repository.TradeReceipt
import com.stockpulse.app.domain.repository.TradeResult
import com.stockpulse.app.domain.repository.TradingRepository
import com.stockpulse.app.domain.repository.TransactionView
import com.stockpulse.app.domain.repository.TransactionsResult
import retrofit2.Response
import javax.inject.Inject

private data class ApiErrorEnvelope(val ok: Boolean? = null, val error: String? = null)

/**
 * Repositorio de trading (FASE 6). El backend es la unica fuente de verdad
 * del saldo y de los precios; aqui solo traducimos HTTP a dominio.
 */
class TradingRepositoryImpl @Inject constructor(
    private val tradingApi: TradingApi
) : TradingRepository {

    override suspend fun summary(): PortfolioResult {
        val response = runCatching { tradingApi.summary() }
            .getOrElse { return PortfolioResult.Failure("Sin conexion con el servidor") }
        if (!response.isSuccessful) {
            return PortfolioResult.Failure(extractError(response) ?: "No se pudo cargar tu portafolio (HTTP ${response.code()})")
        }
        val body = response.body()
            ?: return PortfolioResult.Failure("Respuesta invalida del servidor")
        val dto = body.summary
            ?: return PortfolioResult.Failure(body.error ?: "Respuesta invalida del servidor")
        return PortfolioResult.Success(
            PortfolioSummary(
                balance = dto.balance ?: 0.0,
                holdings = dto.holdingsEnVivo.orEmpty().map { h ->
                    HoldingView(
                        symbol = h.symbol,
                        quantity = h.quantity ?: 0,
                        avgPrice = h.avgPrice ?: 0.0,
                        price = h.price,
                        changePercent = h.changePercent,
                        marketValue = h.marketValue,
                        unrealizedPnl = h.unrealizedPnl,
                        available = h.available
                    )
                },
                portfolioValue = dto.portfolioValue ?: 0.0,
                investedCost = dto.investedCost ?: 0.0
            )
        )
    }

    override suspend fun buy(symbol: String, quantity: Int): TradeResult =
        execute { tradingApi.buy(TradeRequest(symbol, quantity)) }

    override suspend fun sell(symbol: String, quantity: Int): TradeResult =
        execute { tradingApi.sell(TradeRequest(symbol, quantity)) }

    override suspend fun transactions(): TransactionsResult {
        val response = runCatching { tradingApi.transactions() }
            .getOrElse { return TransactionsResult.Failure("Sin conexion con el servidor") }
        if (!response.isSuccessful) {
            return TransactionsResult.Failure(extractError(response) ?: "No se pudo cargar el historial")
        }
        val body = response.body()
            ?: return TransactionsResult.Failure("Respuesta invalida del servidor")
        return TransactionsResult.Success(
            body.transactions.orEmpty().map { t ->
                TransactionView(
                    id = t.id,
                    symbol = t.symbol,
                    type = t.type ?: "",
                    quantity = t.quantity ?: 0,
                    unitPrice = t.unit_price,
                    total = t.total,
                    profitLoss = t.profit_loss,
                    createdAt = t.created_at
                )
            }
        )
    }

    private suspend fun execute(call: suspend () -> Response<TradeResponse>): TradeResult {
        val response = runCatching { call() }
            .getOrElse { return TradeResult.Failure("Sin conexion con el servidor") }
        val body = response.body()
        if (!response.isSuccessful) {
            return TradeResult.Failure(extractError(response) ?: body?.error ?: "Operacion rechazada (HTTP ${response.code()})")
        }
        val trade = body?.trade
            ?: return TradeResult.Failure(body?.error ?: "El servidor no confirmo la operacion")
        return TradeResult.Success(
            TradeReceipt(
                symbol = trade.symbol ?: "",
                action = trade.action ?: "",
                quantity = trade.quantity ?: 0,
                unitPrice = trade.unitPrice ?: 0.0,
                total = trade.total ?: 0.0,
                profitLoss = trade.profitLoss,
                walletBalance = trade.walletBalance ?: 0.0,
                message = body.message ?: "Operacion realizada"
            )
        )
    }

    private fun extractError(response: Response<*>): String? {
        val raw = response.errorBody()?.string() ?: return null
        return runCatching { Gson().fromJson(raw, ApiErrorEnvelope::class.java).error }.getOrNull()
    }
}
