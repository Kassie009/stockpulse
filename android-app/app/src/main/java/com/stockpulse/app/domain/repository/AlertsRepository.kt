package com.stockpulse.app.domain.repository

data class UserAlert(
    val id: String,
    val symbol: String,
    val type: String,
    val threshold: Double,
    val active: Boolean,
    val lastTriggeredAt: String?
)

sealed interface AlertsReadResult {
    data class Success(val alerts: List<UserAlert>) : AlertsReadResult
    data class Failure(val message: String) : AlertsReadResult
}

sealed interface AlertsWriteResult {
    data class Success(val message: String) : AlertsWriteResult
    data class Failure(val message: String) : AlertsWriteResult
}

interface AlertsRepository {
    suspend fun create(symbol: String, type: String, threshold: Double): AlertsWriteResult
    suspend fun list(): AlertsReadResult
    suspend fun delete(id: String): AlertsWriteResult
    suspend fun registerDevice(token: String, deviceName: String?): AlertsWriteResult
}
