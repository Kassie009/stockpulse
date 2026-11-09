package com.stockpulse.app.presentation.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockpulse.app.ui.theme.GreenGain
import com.stockpulse.app.ui.theme.RedLoss

/**
 * FASE 3 - Pantalla Mercado con los 7 nombres del prompt (seccion 4.1).
 * Precios de EJEMPLO; en FASE 5 se conectan al backend/Finnhub.
 */

private data class DemoStock(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePct: Double
)

private val demoStocks = listOf(
    DemoStock("AAPL", "Apple Inc.", 232.90, 0.64),
    DemoStock("MSFT", "Microsoft Corp.", 402.11, -0.38),
    DemoStock("NVDA", "NVIDIA Corp.", 138.27, 2.15),
    DemoStock("TSLA", "Tesla Inc.", 250.75, -1.32),
    DemoStock("AMZN", "Amazon.com Inc.", 201.22, 0.91),
    DemoStock("GOOGL", "Alphabet Inc.", 171.84, 0.27),
    DemoStock("META", "Meta Platforms Inc.", 590.13, 1.44)
)

@Composable
fun MarketScreen() {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Mercado bursátil",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Empresas populares · precios de EJEMPLO",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        // Buscador (funciona sobre la lista de ejemplo)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
            placeholder = { Text("Buscar por nombre o símbolo…") },
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        val filtered = demoStocks.filter {
            it.symbol.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            Text(
                text = "Sin resultados para \"$query\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            filtered.forEach { stock ->
                StockRow(stock)
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Precios reales con fecha/hora desde FASE 5 (Finnhub vía backend).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StockRow(stock: DemoStock) {
    val isUp = stock.changePct >= 0.0
    val changeColor = if (isUp) GreenGain else RedLoss
    val sign = if (isUp) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = stock.symbol,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stock.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${"%,.2f".format(stock.price)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$sign${"%,.2f".format(stock.changePct)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = changeColor
                )
            }
        }
    }
}
