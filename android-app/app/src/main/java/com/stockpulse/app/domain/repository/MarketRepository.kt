package com.stockpulse.app.domain.repository

/**
 * Modelo de dominio de una cotizacion (regla 12: siempre con fecha/hora;
 * regla 13: `available=false` indica dato no real no disponible).
 */
data class MarketQuote(
    val symbol: String,
    val name: String?,
    val price: Double?,
    val changePercent: Double?,
    val recordedAt: String?,
    val available: Boolean
)

data class MarketSearchResult(
    val symbol: String,
    val description: String,
    val type: String?
)

sealed interface MarketResult {
    data class Success(val stocks: List<MarketQuote>, val recordedAt: String?) : MarketResult
    data class Failure(val message: String) : MarketResult
}

sealed interface SearchStocksResult {
    data class Success(val results: List<MarketSearchResult>) : SearchStocksResult
    data class Failure(val message: String) : SearchStocksResult
}

interface MarketRepository {
    suspend fun popular(): MarketResult
    suspend fun search(query: String): SearchStocksResult
}
