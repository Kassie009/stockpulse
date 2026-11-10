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

/** Dispositivo vinculado (telefono/reloj) con su token FCM (FASE 9.5). */
data class LinkedDevice(
    val id: String,
    val platform: String,
    val deviceName: String?,
    val lastSeenAt: String?
)

sealed interface DevicesResult {
    data class Success(val devices: List<LinkedDevice>) : DevicesResult
    data class Failure(val message: String) : DevicesResult
}

interface AlertsRepository {
    suspend fun create(symbol: String, type: String, threshold: Double): AlertsWriteResult
    suspend fun list(): AlertsReadResult
    suspend fun delete(id: String): AlertsWriteResult
    suspend fun registerDevice(token: String, deviceName: String?): AlertsWriteResult
    suspend fun devices(): DevicesResult
}
