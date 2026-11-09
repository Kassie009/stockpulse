package com.stockpulse.app.domain.repository

/**
 * Modelo de dominio del estado del sistema, listo para el dashboard.
 */
data class SystemStatus(
    val backendOk: Boolean,
    val finnhubOk: Boolean,
    val aaplPrice: Double?,
    val aaplChangePercent: Double?,
    val supabaseOk: Boolean?,
    val rawTime: String?
)

interface HealthRepository {
    suspend fun getSystemStatus(): Result<SystemStatus>
}
