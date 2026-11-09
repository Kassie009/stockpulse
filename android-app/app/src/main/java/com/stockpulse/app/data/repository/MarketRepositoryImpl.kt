package com.stockpulse.app.data.repository

import com.google.gson.Gson
import com.stockpulse.app.data.remote.MarketApi
import com.stockpulse.app.data.remote.PopularResponse
import com.stockpulse.app.domain.repository.MarketQuote
import com.stockpulse.app.domain.repository.MarketRepository
import com.stockpulse.app.domain.repository.MarketResult
import com.stockpulse.app.domain.repository.MarketSearchResult
import com.stockpulse.app.domain.repository.SearchStocksResult
import retrofit2.Response
import javax.inject.Inject

private data class MarketErrorResponse(val ok: Boolean? = null, val error: String? = null)

/**
 * Repositorio del mercado (FASE 5). Consumo exclusivo del backend.
 */
class MarketRepositoryImpl @Inject constructor(
    private val marketApi: MarketApi
) : MarketRepository {

    override suspend fun popular(): MarketResult {
        val response = runCatching { marketApi.popular() }
            .getOrElse { return MarketResult.Failure("Sin conexion con el servidor. Verifica que el backend este corriendo.") }

        if (!response.isSuccessful) {
            return MarketResult.Failure(extractError(response) ?: "El servidor rechazo la peticion (HTTP ${response.code()})")
        }

        val body: PopularResponse = response.body()
            ?: return MarketResult.Failure("Respuesta invalida del servidor")

        if (body.ok != true || body.stocks == null) {
            return MarketResult.Failure(body.error ?: "Respuesta invalida del servidor")
        }

        return MarketResult.Success(
            stocks = body.stocks.map { dto ->
                MarketQuote(
                    symbol = dto.symbol,
                    name = dto.name,
                    price = dto.price,
                    changePercent = dto.changePercent,
                    recordedAt = dto.recordedAt,
                    available = dto.available
                )
            },
            recordedAt = body.recordedAt
        )
    }

    override suspend fun search(query: String): SearchStocksResult {
        val response = runCatching { marketApi.search(query) }
            .getOrElse { return SearchStocksResult.Failure("Sin conexion con el servidor") }

        if (!response.isSuccessful) {
            return SearchStocksResult.Failure(extractError(response) ?: "Busqueda fallida (HTTP ${response.code()})")
        }

        val body = response.body()
            ?: return SearchStocksResult.Failure("Respuesta invalida del servidor")

        if (body.ok != true) {
            return SearchStocksResult.Failure(body.error ?: "Busqueda fallida")
        }

        return SearchStocksResult.Success(
            body.results.orEmpty().map {
                MarketSearchResult(
                    symbol = it.symbol,
                    description = it.description ?: it.symbol,
                    type = it.type
                )
            }
        )
    }

    private fun extractError(response: Response<*>): String? {
        val raw = response.errorBody()?.string() ?: return null
        return runCatching { Gson().fromJson(raw, MarketErrorResponse::class.java).error }.getOrNull()
    }
}
