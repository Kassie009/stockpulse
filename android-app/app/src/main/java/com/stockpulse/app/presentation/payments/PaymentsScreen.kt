package com.stockpulse.app.presentation.payments

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockpulse.app.ui.theme.GreenGain
import com.stockpulse.app.ui.theme.RedLoss

/**
 * FASE 8 - Compra de paquetes de creditos con Stripe Checkout REAL
 * (modo test). Se abre la pagina hospedada de Stripe en WebView; al
 * volver, el backend verifica contra Stripe y acredita.
 *
 * Tarjeta de prueba: 4242 4242 4242 4242 · fecha futura · CVC 3 digitos.
 */
@Composable
fun PaymentsScreen(viewModel: PaymentsViewModel = hiltViewModel(), onBack: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("←  Volver") }
        Spacer(Modifier.height(8.dp))

        Text("Creditos virtuales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        state.balance?.let {
            Text(
                text = "Tu saldo actual: $${"%,.2f".format(it)} creditos",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(12.dp))

        state.infoMessage?.let { Text(it, color = GreenGain, style = MaterialTheme.typography.bodyMedium) }
        state.errorMessage?.let { Text(it, color = RedLoss, style = MaterialTheme.typography.bodyMedium) }
        state.purchaseMessage?.let {
            Text(
                it,
                color = if (state.purchasePaid == true) GreenGain else RedLoss,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (state.isVerifying) {
            Text("Verificando tu pago con Stripe…", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> state.packages.forEach { pkg ->
                PackageCard(
                    name = pkg.name,
                    price = pkg.priceMxn,
                    credits = pkg.credits,
                    isBusy = state.purchasingPackage == pkg.code,
                    onBuy = { viewModel.startCheckout(pkg.code) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    // ---------- Checkout real en WebView ----------
    state.checkoutUrl?.let { url ->
        StripeCheckoutDialog(url = url, onClose = viewModel::onCheckoutClosed)
    }

    // Dialogo de resultado (por si el WebView no termino de mostrarlo)
    if (state.purchaseMessage != null && state.checkoutUrl == null && !state.isVerifying) {
        val purchaseMessage: String = state.purchaseMessage ?: ""
        AlertDialog(
            onDismissRequest = viewModel::dismissCheckoutAndResult,
            confirmButton = {
                TextButton(onClick = viewModel::dismissCheckoutAndResult) { Text("Entendido") }
            },
            title = { Text(if (state.purchasePaid == true) "Pago confirmado" else "Resultado del pago") },
            text = { Text(purchaseMessage) }
        )
    }
}

@Composable
private fun PackageCard(name: String, price: Double, credits: Int, isBusy: Boolean, onBuy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "+${java.text.NumberFormat.getInstance().format(credits)} creditos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${"%,.2f".format(price)} MXN",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = onBuy, enabled = !isBusy) {
                    if (isBusy) CircularProgressIndicator(Modifier.height(14.dp), strokeWidth = 2.dp) else Text("Comprar")
                }
            }
        }
    }
}

/** WebView del Stripe Checkout hospedado. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun StripeCheckoutDialog(url: String, onClose: () -> Unit) {
    var closePending by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = { closePending = true }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Pago seguro · Stripe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { closePending = true }) { Text("Regresar") }
            }
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            // Adapta la pagina al ancho de la pantalla
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): Boolean {
                                    val target = request?.url?.toString()
                                    // Fin del checkout: retorno al backend
                                    if (target != null &&
                                        (target.contains("/api/payments/success") || target.contains("/api/payments/cancel"))
                                    ) {
                                        view?.post { closePending = true }
                                        return false
                                    }
                                    return false
                                }
                            }
                            loadUrl(url)
                        }
                    }
                )
            }
        }
    }

    if (closePending) {
        // Al primer ciclo de recomposicion cierra y verifica el pago
        androidx.compose.runtime.LaunchedEffect(closePending) {
            onClose()
        }
    }
}
