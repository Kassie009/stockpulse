package com.stockpulse.app.domain.repository

/**
 * Modelos de dominio del simulador (FASE 6).
 */
data class HoldingView(
    val symbol: String,
    val quantity: Int,
    val avgPrice: Double,
    val price: Double?,
    val changePercent: Double?,
    val marketValue: Double?,
    val unrealizedPnl: Double?,
    val available: Boolean
)

data class PortfolioSummary(
    val balance: Double,
    val holdings: List<HoldingView>,
    val portfolioValue: Double,
    val investedCost: Double
)

data class TradeReceipt(
    val symbol: String,
    val action: String,
    val quantity: Int,
    val unitPrice: Double,
    val total: Double,
    val profitLoss: Double?,
    val walletBalance: Double,
    val message: String
)

data class TransactionView(
    val id: String,
    val symbol: String,
    val type: String,
    val quantity: Int,
    val unitPrice: Double?,
    val total: Double?,
    val profitLoss: Double?,
    val createdAt: String?
)

sealed interface PortfolioResult {
    data class Success(val summary: PortfolioSummary) : PortfolioResult
    data class Failure(val message: String) : PortfolioResult
}

sealed interface TradeResult {
    data class Success(val receipt: TradeReceipt) : TradeResult
    data class Failure(val message: String) : TradeResult
}

sealed interface TransactionsResult {
    data class Success(val transactions: List<TransactionView>) : TransactionsResult
    data class Failure(val message: String) : TransactionsResult
}

interface TradingRepository {
    suspend fun summary(): PortfolioResult
    suspend fun buy(symbol: String, quantity: Int): TradeResult
    suspend fun sell(symbol: String, quantity: Int): TradeResult
    suspend fun transactions(): TransactionsResult
}
