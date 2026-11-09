package com.stockpulse.app.presentation.market

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockpulse.app.ui.theme.GreenGain
import com.stockpulse.app.ui.theme.RedLoss

/**
 * Dialogo de compraventa (FASE 6). Muestra el precio real del momento,
 * el total estimado y ejecuta contra el backend. Mensajes de error reales.
 */
@Composable
fun TradeDialog(
    quoteSymbol: String,
    quoteName: String?,
    quotePrice: Double?,
    maxSellable: Int?,
    onDismiss: () -> Unit
) {
    val tradeViewModel: TradeViewModel = hiltViewModel()
    val state by tradeViewModel.state.collectAsState()

    var quantityText by remember { mutableStateOf("1") }
    val quantity = quantityText.toIntOrNull() ?: 0
    val estimatedTotal = (quantity.coerceAtLeast(0)) * (quotePrice ?: 0.0)

    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${quoteSymbol}${if (quoteName != null) " · $quoteName" else ""}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = quotePrice?.let { "Precio real ahora: $${"%,.2f".format(it)}" }
                    ?: "El precio no esta disponible en este momento",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (state.successMessage != null) {
                val success: String = state.successMessage ?: ""
                Text(
                    text = success,
                    color = GreenGain,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                state.newBalance?.let {
                    Text(
                        "Saldo nuevo: $${"%,.2f".format(it)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Listo")
                }
            } else {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() }.take(6) },
                    label = { Text("Cantidad de acciones") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (quotePrice != null && quantity > 0)
                        "Total estimado: $${"%,.2f".format(estimatedTotal)}"
                    else "El precio se confirmara al ejecutar",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                maxSellable?.let {
                    Text(
                        text = "Tienes $it accion${if (it == 1) "" else "es"} de $quoteSymbol",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))

                state.errorMessage?.let {
                    Text(it, color = RedLoss, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { tradeViewModel.buy(quoteSymbol, quantity) },
                        enabled = !state.isSubmitting && quotePrice != null && quantity >= 1,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Comprar")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { tradeViewModel.sell(quoteSymbol, quantity) },
                        enabled = !state.isSubmitting && quotePrice != null && quantity >= 1,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Vender", color = if (maxSellable != null && maxSellable > 0) RedLoss else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar")
                }
            }
        }
    }
}
