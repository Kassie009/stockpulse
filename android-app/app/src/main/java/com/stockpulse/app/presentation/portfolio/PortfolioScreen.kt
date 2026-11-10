package com.stockpulse.app.presentation.portfolio

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
import com.stockpulse.app.domain.repository.HoldingView
import com.stockpulse.app.domain.repository.TransactionView
import com.stockpulse.app.ui.theme.GreenGain
import com.stockpulse.app.ui.theme.RedLoss
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * FASE 6 - Portafolio real: saldo virtual, holdings con valor en vivo,
 * P&L no realizado e historial de operaciones. Todo desde el backend.
 */
@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Portafolio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Tus inversiones virtuales · créditos de simulación",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        val summary = state.summary

        when {
            state.isLoading -> Box(
                Modifier.fillMaxWidth().height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }

            state.errorMessage != null -> {
                val errorText: String = state.errorMessage ?: ""
                Column {
                    Text(
                        text = errorText,
                        color = RedLoss,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::refresh) { Text("Reintentar") }
                }
            }

            summary != null -> {
                // ---------- Tarjeta principal: saldo + total ----------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Créditos disponibles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$${"%,.2f".format(summary.balance)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Valor portafolio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "$${"%,.2f".format(summary.portfolioValue)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                val pnl = summary.portfolioValue - summary.investedCost
                                Text(
                                    "P&L no realizado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = (if (pnl >= 0) "+$" else "-$") + "%,.2f".format(kotlin.math.abs(pnl)),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (pnl >= 0) GreenGain else RedLoss
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ---------- Holdings ----------
                Text("Tus acciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                if (summary.holdings.isEmpty()) {
                    Text(
                        "Aún no tienes acciones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    summary.holdings.forEach { holding -> HoldingRow(holding) }
                }
                Spacer(Modifier.height(16.dp))

                // ---------- Historial ----------
                Text("Historial de operaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                if (state.transactions.isEmpty()) {
                    Text(
                        "Sin operaciones registradas todavía.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.transactions.take(20).forEach { tx -> TransactionRow(tx) }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::refresh) { Text("Actualizar") }
            }
        }
    }
}

@Composable
private fun HoldingRow(holding: HoldingView) {
    val pnl = holding.unrealizedPnl
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(holding.symbol, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${holding.quantity} acciones · promedio $${"%,.2f".format(holding.avgPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (holding.available && holding.marketValue != null) {
                        "$${"%,.2f".format(holding.marketValue)}"
                    } else {
                        "sin dato ahora"   // regla 13: no inventar
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (pnl != null) {
                    Text(
                        text = (if (pnl >= 0) "+$" else "-$") + "%,.2f".format(kotlin.math.abs(pnl)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (pnl >= 0) GreenGain else RedLoss
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionView) {
    val isBuy = tx.type == "buy"
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
                    text = "${if (isBuy) "COMPRA" else "VENTA"} · ${tx.symbol}",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isBuy) GreenGain else RedLoss
                )
                Text(
                    text = tx.unitPrice?.let { price ->
                        "${tx.quantity} a $${"%.2f".format(price)} · ${formatIso(tx.createdAt)}"
                    } ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${"%,.2f".format(tx.total ?: 0.0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                tx.profitLoss?.let { pnl ->
                    Text(
                        text = if (pnl >= 0) "+$${"%,.2f".format(pnl)}" else "-$${"%,.2f".format(kotlin.math.abs(pnl))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (pnl >= 0) GreenGain else RedLoss
                    )
                }
            }
        }
    }
}

private fun formatIso(iso: String?): String {
    if (iso == null) return ""
    return runCatching {
        Instant.parse(iso)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM HH:mm"))
    }.getOrDefault(iso.take(10))
}
