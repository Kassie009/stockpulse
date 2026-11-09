package com.stockpulse.app.presentation.alerts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockpulse.app.ui.theme.GreenGain
import com.stockpulse.app.ui.theme.RedLoss
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * FASE 7 - Pantalla de alertas de precio.
 * Las alertas se evaluan en el backend cada 20 s y llegan como
 * notificaciones push reales (FCM) al dispositivo.
 */
@Composable
fun AlertsScreen(viewModel: AlertsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Mis alertas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Te avisamos con una notificacion cuando se cumpla",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        // ---------- Crear nueva ----------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Nueva alerta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.symbol,
                    onValueChange = viewModel::onSymbolChange,
                    label = { Text("Simbolo (ej. AAPL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Text("Condicion", style = MaterialTheme.typography.bodySmall)
                ALERT_TYPES.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(
                            selected = state.type == option.value,
                            onClick = { viewModel.onTypeChange(option.value) }
                        )
                        Text(option.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.threshold,
                    onValueChange = viewModel::onThresholdChange,
                    label = {
                        val isPercent = ALERT_TYPES.first { it.value == state.type }.isPercent
                        Text(if (isPercent) "Umbral en %" else "Umbral en USD")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                state.infoMessage?.let {
                    Text(it, color = GreenGain, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                state.errorMessage?.let {
                    Text(it, color = RedLoss, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = viewModel::createAlert,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Crear alerta")
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---------- Lista ----------
        Text("Activas y pasadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.alerts.isEmpty() -> Text(
                "Aun no tienes alertas. Crea la primera arriba.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> state.alerts.forEach { alert -> AlertRow(alert, viewModel::deleteAlert) }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
            Text("Actualizar lista")
        }
    }
}

@Composable
private fun AlertRow(alert: com.stockpulse.app.domain.repository.UserAlert, onDelete: (String) -> Unit) {
    val label = ALERT_TYPES.first { it.value == alert.type }
    val triggered = alert.lastTriggeredAt?.let { es ->
        runCatching {
            Instant.parse(es).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM HH:mm"))
        }.getOrNull()
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${alert.symbol} · ${if (alert.active) "ACTIVA" else "pausada"}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (alert.active) GreenGain else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = label.label + " " + (if (label.isPercent) "${alert.threshold}%" else "$${alert.threshold}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (triggered != null) {
                    Text(
                        "Ultimo disparo: $triggered",
                        style = MaterialTheme.typography.bodySmall,
                        color = RedLoss
                    )
                }
            }
            TextButton(onClick = { onDelete(alert.id) }) { Text("Borrar") }
        }
    }
}
