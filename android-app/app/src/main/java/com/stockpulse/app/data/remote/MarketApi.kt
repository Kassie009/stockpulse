package com.stockpulse.app.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Endpoints del mercado (FASE 5) servidos por el backend (Finnhub + Supabase).
 */
interface MarketApi {

    @GET("api/stocks/popular")
    suspend fun popular(): Response<PopularResponse>

    @GET("api/stocks/quote")
    suspend fun quote(@Query("symbol") symbol: String): Response<QuoteResponse>

    @GET("api/stocks/search")
    suspend fun search(@Query("q") query: String): Response<SearchResponse>
}

data class PopularResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val stocks: List<StockQuoteDto>? = null,
    val recordedAt: String? = null
)

data class QuoteResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val quote: StockQuoteDto? = null
)

data class StockQuoteDto(
    val symbol: String,
    val name: String? = null,
    val price: Double? = null,
    val changePercent: Double? = null,
    val dayHigh: Double? = null,
    val dayLow: Double? = null,
    val recordedAt: String? = null,
    val provider: String? = null,
    val available: Boolean = false
)

data class SearchResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val count: Int? = null,
    val results: List<SearchResultDto>? = null
)

data class SearchResultDto(
    val symbol: String,
    val description: String? = null,
    val displaySymbol: String? = null,
    val type: String? = null
)
