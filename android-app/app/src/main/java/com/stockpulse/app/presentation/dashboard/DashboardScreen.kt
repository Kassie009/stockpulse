package com.stockpulse.app.presentation.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockpulse.app.domain.repository.TransactionView
import com.stockpulse.app.ui.theme.GreenGain
import com.stockpulse.app.ui.theme.RedLoss

/**
 * Dashboard con datos REALES del usuario (portafolio y movimientos
 * del motor de trading) + estado del sistema en vivo.
 */
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

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
            text = "Simulación bursátil",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        // Estado del sistema en vivo (backend real)
        SystemStatusCard(state = state, onRetry = viewModel::loadSystemStatus)
        Spacer(Modifier.height(16.dp))

        // Saldo real del wallet
        SummaryCard(
            label = "Saldo disponible",
            amount = state.balance?.let { "%,.2f".format(it) }
        )
        Spacer(Modifier.height(12.dp))

        // Valor real del portafolio + P&L no realizado vs costo de inversion
        val value = state.portfolioValue
        val cost = state.investedCost
        var deltaText: String? = null
        var deltaColor: Color = GreenGain
        if (value != null && cost != null) {
            val pnl = value - cost
            val pct = if (cost > 0.0) (pnl / cost) * 100.0 else 0.0
            deltaText = "${if (pnl >= 0.0) "+" else ""}${"%,.2f".format(pnl)} (${if (pct >= 0.0) "+" else ""}${"%.2f".format(pct)}%)"
            deltaColor = if (pnl >= 0.0) GreenGain else RedLoss
        }
        SummaryCard(
            label = "Valor del portafolio",
            amount = value?.let { "%,.2f".format(it) },
            extraLine = deltaText != null,
            deltaText = deltaText ?: "",
            deltaColor = deltaColor
        )
        Spacer(Modifier.height(20.dp))

        Text(
            text = "Movimientos recientes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        if (state.recentTransactions.isEmpty()) {
            Text(
                text = "Aún no tienes movimientos. Ve a Portafolio y haz tu primera operación.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.recentTransactions.forEach { tx ->
                MovementRow(tx)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SystemStatusCard(
    state: DashboardUiState,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            when {
                state.isLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Conectando con el backend…",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                state.systemStatus != null && state.systemStatus.backendOk -> {
                    val status = state.systemStatus
                    Text(
                        text = "Backend: CONECTADO ✓",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenGain
                    )
                    if (status.finnhubOk && status.aaplPrice != null) {
                        Text(
                            text = "AAPL (Finnhub): $${"%,.2f".format(status.aaplPrice)} " +
                                "${
                                    if ((status.aaplChangePercent ?: 0.0) >= 0.0) "+" else ""
                                }${"%,.2f".format(status.aaplChangePercent ?: 0.0)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if ((status.aaplChangePercent ?: 0.0) >= 0.0) GreenGain else RedLoss
                        )
                    }
                    if (status.supabaseOk == true) {
                        Text(
                            text = "Base de datos (Supabase): OK ✓",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    Text(
                        text = state.errorMessage ?: "Backend no disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RedLoss
                    )
                    Text(
                        text = "Inicia el backend en tu PC y reintenta para ver datos en vivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: String?,
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
                text = if (amount != null) "$$amount" else "Cargando…",
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
private fun MovementRow(tx: TransactionView) {
    val isBuy = tx.type.equals("buy", ignoreCase = true)
    val pnlColor: Color? = when {
        tx.profitLoss == null -> null
        tx.profitLoss >= 0.0 -> GreenGain
        else -> RedLoss
    }
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
            // Marcador circular: verde venta / rojo compra (con flujo de dinero)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(if (isBuy) RedLoss else GreenGain, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.symbol,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = buildString {
                        append(if (isBuy) "Compra" else "Venta")
                        if (tx.quantity > 0) append(" · ${tx.quantity} título${if (tx.quantity == 1) "" else "s"}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (tx.total != null) {
                    Text(
                        text = "${if (isBuy) "-" else "+"}$${"%,.2f".format(kotlin.math.abs(tx.total))}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isBuy) RedLoss else GreenGain
                    )
                }
                if (pnlColor != null) {
                    Text(
                        text = "P&L ${if ((tx.profitLoss ?: 0.0) >= 0.0) "+" else ""}${"%,.2f".format(tx.profitLoss)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = pnlColor
                    )
                }
            }
        }
    }
}
