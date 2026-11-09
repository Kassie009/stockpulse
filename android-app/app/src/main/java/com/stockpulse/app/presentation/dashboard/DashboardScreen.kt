package com.stockpulse.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockpulse.app.ui.theme.GreenGain
import com.stockpulse.app.ui.theme.RedLoss

/**
 * FASE 3 - Pantalla Dashboard (datos de EJEMPLO claramente marcados;
 * regla #13: datos ficticios identificados mientras no haya datos reales).
 */

private data class DemoMovement(val symbol: String, val isBuy: Boolean, val amount: Double)

private val demoMovements = listOf(
    DemoMovement("AAPL", true, -1_162.00),
    DemoMovement("NVDA", true, -491.50),
    DemoMovement("TSLA", false, 1_187.00)
)

@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Encabezado con identidad
        Text(
            text = "STOCKPULSE",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Simulacion bursatil · datos de EJEMPLO",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        // Tarjeta: saldo virtual
        SummaryCard(
            label = "Saldo virtual disponible",
            amount = "10,000.00"
        )
        Spacer(Modifier.height(12.dp))

        // Tarjeta: portafolio con ganancia simulada
        SummaryCard(
            label = "Valor total del portafolio",
            amount = "12,487.30",
            extraLine = true,
            deltaText = "+2,487.30 (+24.9%)",
            deltaColor = GreenGain
        )
        Spacer(Modifier.height(20.dp))

        Text(
            text = "Movimientos recientes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        demoMovements.forEach { movement ->
            MovementRow(movement)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Alertas importantes aparecen aqui cuando las configures (FASE 7).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: String,
    extraLine: Boolean = false,
    deltaText: String = "",
    deltaColor: Color = GreenGain
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$$amount",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (extraLine) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = deltaColor
                )
            }
        }
    }
}

@Composable
private fun MovementRow(movement: DemoMovement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Marcador circular verde (venta) o rojo (compra)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(if (movement.isBuy) RedLoss else GreenGain, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movement.symbol,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (movement.isBuy) "Compra" else "Venta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$${"%,.2f".format(movement.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (movement.isBuy) RedLoss else GreenGain
            )
        }
    }
}
