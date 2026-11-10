package com.stockpulse.app.presentation.devices

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockpulse.app.domain.repository.LinkedDevice
import com.stockpulse.app.ui.theme.GreenGain
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * FASE 9.5 - Dispositivos que recibirán tus alertas (tokens FCM reales
 * registrados en el backend): telefonos y relojes de tu cuenta.
 */
@Composable
fun DevicesScreen(viewModel: DevicesViewModel = hiltViewModel(), onBack: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Dispositivos vinculados", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Aquí llegan tus notificaciones y alertas de precio",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        state.infoMessage?.let {
            Text(it, color = GreenGain, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
        }

        when {
            state.isLoading -> Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Cargando dispositivos…", style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                if (state.devices.isEmpty()) {
                    Text(
                        "Aún no hay dispositivos registrados. Abre esta cuenta en tu teléfono o en el reloj para vincularlos.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                state.devices.forEach { device -> DeviceRow(device) ; Spacer(Modifier.height(8.dp)) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = viewModel::reRegisterPhone, modifier = Modifier.fillMaxWidth()) {
            Text("Re-registrar este teléfono")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::load, modifier = Modifier.fillMaxWidth()) {
            Text("Actualizar lista")
        }

        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("¿Cómo vinculo mi reloj?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "1. Instala StockPulse en tu reloj Wear OS\n" +
                        "2. Ábrelo y accede con la MISMA cuenta\n" +
                        "3. Aparecerá aquí automáticamente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← Volver")
        }
    }
}

@Composable
private fun DeviceRow(device: LinkedDevice) {
    val isWatch = device.platform.equals("wearos", ignoreCase = true)
    val seen = device.lastSeenAt?.let { raw ->
        runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(raw.substringBefore("+").substringBefore("Z"))
                ?: return@let "visto: fecha no reconocida"
            val out = SimpleDateFormat("dd MMM HH:mm", Locale("es"))
            out.timeZone = TimeZone.getDefault()
            "visto por última vez: " + out.format(date)
        }.getOrNull()
    } ?: "visto: sin registro"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isWatch) Icons.Filled.Watch else Icons.Filled.Phone,
                contentDescription = if (isWatch) "Reloj" else "Teléfono",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (isWatch) "Reloj · " + (device.deviceName ?: "Wear OS")
                    else "Teléfono · " + (device.deviceName ?: "Android"),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    seen,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
