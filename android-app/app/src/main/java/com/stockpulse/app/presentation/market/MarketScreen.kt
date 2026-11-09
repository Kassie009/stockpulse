package com.stockpulse.app.presentation.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockpulse.app.domain.repository.MarketQuote
import com.stockpulse.app.domain.repository.MarketSearchResult
import com.stockpulse.app.ui.theme.GreenGain
import com.stockpulse.app.ui.theme.RedLoss
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * FASE 5 - Pantalla Mercado con cotizaciones REALES de Finnhub via backend.
 * Cada precio se muestra con su fecha/hora (regla 12); si el proveedor no
 * devolvio dato, se marca claramente (regla 13).
 */
@Composable
fun MarketScreen(viewModel: MarketViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mercado bursátil",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Empresas populares · precios reales de Finnhub",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
            placeholder = { Text("Buscar por nombre o símbolo…") },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.errorMessage != null && state.stocks.isEmpty() -> {
                ErrorBlock(
                    message = state.errorMessage,
                    onRetry = viewModel::loadPopular
                )
            }

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.query.trim().length >= 2) {
                    // Resultados de busqueda real (Finnhub)
                    item {
                        Text(
                            text = if (state.isSearching) "Buscando…" 
                                else "Resultados de búsqueda (${state.searchResults.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (state.isSearching) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp)
                            }
                        }
                    } else if (state.searchResults.isEmpty()) {
                        item {
                            Text(
                                text = "Sin resultados para \"${state.query.trim()}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(state.searchResults) { result -> SearchResultRow(result) }
                    }
                    item {
                        Text(
                            text = "— Empresas populares —",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else if (state.errorMessage != null) {
                    item { ErrorInline(message = state.errorMessage) }
                }

                items(state.stocks) { quote -> StockRow(quote) }

                item {
                    Text(
                        text = state.recordedAt?.let { "Actualizado: ${formatRecordedAt(it)} (hora local)" }
                            ?: "Precios de Finnhub via StockPulse backend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StockRow(quote: MarketQuote) {
    if (!quote.available || quote.price == null) {
        // Regla 13: dato no disponible se declara, no se inventa
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(quote.symbol, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Sin dato en este momento",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    val isUp = (quote.changePercent ?: 0.0) >= 0.0
    val changeColor = if (isUp) GreenGain else RedLoss
    val sign = if (isUp) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(quote.symbol, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    quote.name ?: quote.symbol,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${"%,.2f".format(quote.price)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$sign${"%,.2f".format(quote.changePercent ?: 0.0)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = changeColor
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: MarketSearchResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(result.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    result.type ?: "Acción",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                result.symbol,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ErrorBlock(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message ?: "Error desconocido",
            color = RedLoss,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("Reintentar") }
    }
}

@Composable
private fun ErrorInline(message: String?) {
    Text(
        text = message ?: "",
        color = RedLoss,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

/** Convierte el ISO UTC del backend a hora local legible (regla 12). */
private fun formatRecordedAt(isoUtc: String): String = runCatching {
    val local = Instant.parse(isoUtc).atZone(ZoneId.systemDefault())
    local.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
}.getOrDefault(isoUtc)
