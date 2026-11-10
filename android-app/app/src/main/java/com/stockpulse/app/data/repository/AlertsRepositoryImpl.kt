package com.stockpulse.app.data.repository

import com.google.gson.Gson
import com.stockpulse.app.data.remote.AlertDto
import com.stockpulse.app.data.remote.AlertsApi
import com.stockpulse.app.data.remote.CreateAlertRequest
import com.stockpulse.app.data.remote.DeviceRequest
import com.stockpulse.app.domain.repository.AlertsReadResult
import com.stockpulse.app.domain.repository.AlertsRepository
import com.stockpulse.app.domain.repository.AlertsWriteResult
import com.stockpulse.app.domain.repository.DevicesResult
import com.stockpulse.app.domain.repository.LinkedDevice
import com.stockpulse.app.domain.repository.UserAlert
import retrofit2.Response
import javax.inject.Inject

private data class AlertsErrorEnvelope(val ok: Boolean? = null, val error: String? = null)

class AlertsRepositoryImpl @Inject constructor(
    private val alertsApi: AlertsApi
) : AlertsRepository {

    override suspend fun create(symbol: String, type: String, threshold: Double): AlertsWriteResult {
        val response = runCatching { alertsApi.create(CreateAlertRequest(symbol.uppercase(), type, threshold)) }
            .getOrElse { return AlertsWriteResult.Failure("Sin conexion con el servidor") }
        if (!response.isSuccessful) {
            return AlertsWriteResult.Failure(extractError(response) ?: "No se pudo crear la alerta (HTTP ${response.code()})")
        }
        return AlertsWriteResult.Success("Alerta creada")
    }

    override suspend fun list(): AlertsReadResult {
        val response = runCatching { alertsApi.list() }
            .getOrElse { return AlertsReadResult.Failure("Sin conexion con el servidor") }
        if (!response.isSuccessful) {
            return AlertsReadResult.Failure(extractError(response) ?: "No se pudo leer tus alertas")
        }
        val body = response.body()
            ?: return AlertsReadResult.Failure("Respuesta invalida del servidor")
        return AlertsReadResult.Success(
            body.alerts.orEmpty().map { it.toDomain() }
        )
    }

    override suspend fun delete(id: String): AlertsWriteResult {
        val response = runCatching { alertsApi.delete(id) }
            .getOrElse { return AlertsWriteResult.Failure("Sin conexion con el servidor") }
        if (!response.isSuccessful) {
            return AlertsWriteResult.Failure(extractError(response) ?: "No se pudo eliminar la alerta")
        }
        return AlertsWriteResult.Success("Alerta eliminada")
    }

    override suspend fun registerDevice(token: String, deviceName: String?): AlertsWriteResult {
        val response = runCatching { alertsApi.registerDevice(DeviceRequest(token = token, deviceName = deviceName)) }
            .getOrElse { return AlertsWriteResult.Failure("Sin conexion con el servidor") }
        if (!response.isSuccessful) {
            return AlertsWriteResult.Failure(extractError(response) ?: "No se pudo registrar el dispositivo")
        }
        return AlertsWriteResult.Success("Dispositivo registrado")
    }

    override suspend fun devices(): DevicesResult {
        val response = runCatching { alertsApi.devices() }
            .getOrElse { return DevicesResult.Failure("Sin conexion con el servidor") }
        if (!response.isSuccessful) {
            return DevicesResult.Failure(extractError(response) ?: "No se pudo leer tus dispositivos")
        }
        val body = response.body()
            ?: return DevicesResult.Failure("Respuesta invalida del servidor")
        return DevicesResult.Success(
            body.devices.orEmpty().map {
                LinkedDevice(
                    id = it.id,
                    platform = it.platform ?: "android",
                    deviceName = it.device_name,
                    lastSeenAt = it.last_seen_at
                )
            }
        )
    }

    private fun AlertDto.toDomain() = UserAlert(
        id = id,
        symbol = symbol,
        type = type,
        threshold = threshold ?: 0.0,
        active = active ?: false,
        lastTriggeredAt = last_triggered_at
    )

    private fun extractError(response: Response<*>): String? {
        val raw = response.errorBody()?.string() ?: return null
        return runCatching { Gson().fromJson(raw, AlertsErrorEnvelope::class.java).error }.getOrNull()
    }
}
