package com.stockpulse.app.data.repository

import com.stockpulse.app.data.remote.StockPulseApi
import com.stockpulse.app.domain.repository.HealthRepository
import com.stockpulse.app.domain.repository.SystemStatus
import javax.inject.Inject

/**
 * Implementacion del repositorio: traduce DTOs del backend a modelo de dominio.
 */
class HealthRepositoryImpl @Inject constructor(
    private val api: StockPulseApi
) : HealthRepository {

    override suspend fun getSystemStatus(): Result<SystemStatus> = runCatching {
        val response = api.getIntegrationStatus()
        SystemStatus(
            backendOk = response.ok == true,
            finnhubOk = response.results?.finnhub?.ok == true,
            aaplPrice = response.results?.finnhub?.aaplPrice,
            aaplChangePercent = response.results?.finnhub?.changePercent,
            supabaseOk = response.results?.supabase?.ok,
            rawTime = response.time
        )
    }
}
