package com.stockpulse.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Colors as WearColors
import com.google.firebase.messaging.FirebaseMessaging
import com.stockpulse.wear.data.AlertDto
import com.stockpulse.wear.data.WearRepository
import com.stockpulse.wear.data.WearSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
// Paleta: identica al tema claro de la app movil (FASE 3)
// ============================================================
private val BgLight = Color(0xFFF7F9FC)
private val TextDark = Color(0xFF10182A)
private val TextMuted = Color(0xFF5B6B85)
private val BrandNavy = Color(0xFF0E2A55)
private val AccentGain = Color(0xFF15B86A)
private val AccentLoss = Color(0xFFE04545)

/** Hora arriba con el color de texto secundario app (fondo claro). */
private val WearTimeText: @Composable () -> Unit = {
    Text(
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
        fontSize = 11.sp,
        color = TextMuted,
        fontWeight = FontWeight.SemiBold
    )
}

private fun wearColors(): WearColors = WearColors(
    primary = BrandNavy,
    onPrimary = Color.White,
    background = BgLight,
    onBackground = TextDark,
    surface = Color.White,
    onSurface = TextDark,
    secondary = AccentGain,
    onSecondary = Color.White,
    error = AccentLoss
)

/**
 * FASE 9 - StockPulse en la muneca.
 * - Login una vez (token persistido en DataStore)
 * - Resumen real: saldo, valor del portafolio, P&L, posiciones y alertas
 * - Registro FCM con platform "wearos" -> las alertas llegan al reloj
 */
class MainActivity : ComponentActivity() {

    private lateinit var repository: WearRepository

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = WearRepository(this)

        // Permiso de notificaciones (API 33+) para las alertas en la muneca
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent { StockPulseWearApp() }
    }

    @Composable
    private fun StockPulseWearApp() {
        val scope = rememberCoroutineScope()
        var estado by remember { mutableStateOf<EstadoApp>(EstadoApp.Iniciando) }

        // Al abrir: si hay token guardado, directo al resumen
        LaunchedEffect(Unit) {
            val token = WearSession.tokenOrNull(applicationContext)
            estado = if (token == null) EstadoApp.Login else EstadoApp.CargandoDatos
        }

        // Carga de datos SOLO si ya hay sesion (tras login o token persistido)
        if (estado is EstadoApp.CargandoDatos) {
            LaunchedEffect(Unit) {
                val resumen = repository.summary()
                if (resumen.isSuccess) {
                    val summary = resumen.getOrThrow()
                    val alerts: List<AlertDto> = repository
                        .alerts()
                        .getOrDefault(emptyList())
                    estado = EstadoApp.Resumen(summary, alerts)
                } else {
                    val message =
                        resumen.exceptionOrNull()?.message ?: "Sin conexion con el servidor"
                    estado = EstadoApp.Error(message)
                }
            }
        }

        // Registro del reloj para recibir FCM (idempotente del lado backend)
        LaunchedEffect(estado) {
            if (estado is EstadoApp.Resumen) {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                    scope.launch {
                        try {
                            repository.registerDevice(fcmToken)
                        } catch (_: Exception) {
                            // No bloquea la app: el motor de alertas ya la empuja igual
                        }
                    }
                }
            }
        }

        MaterialTheme(colors = wearColors()) {
            androidx.compose.material3.MaterialTheme(
                colorScheme = androidx.compose.material3.lightColorScheme(
                    primary = BrandNavy,
                    onPrimary = Color.White,
                    secondary = AccentGain,
                    onSecondary = Color.White,
                    error = AccentLoss,
                    onError = Color.White,
                    background = BgLight,
                    onBackground = TextDark,
                    surface = Color.White,
                    onSurface = TextDark
                )
            ) {
            when (val actual: EstadoApp = estado) {
                is EstadoApp.Iniciando, is EstadoApp.CargandoDatos ->
                    PantallaCargando("Cargando tu portafolio…")
                is EstadoApp.Login -> PantallaLogin(
                    onLoggedIn = { estado = EstadoApp.CargandoDatos }
                )
                is EstadoApp.Resumen -> PantallaResumen(
                    estado = actual,
                    onLogout = {
                        scope.launch {
                            WearSession.clear(applicationContext)
                            estado = EstadoApp.Login
                        }
                    }
                )
                is EstadoApp.Error -> PantallaError(
                    mensaje = actual.mensaje,
                    onReintentar = { estado = EstadoApp.CargandoDatos },
                    onLogout = {
                        scope.launch {
                            WearSession.clear(applicationContext)
                            estado = EstadoApp.Login
                        }
                    }
                )
            }
        }
        }
    }
}

private sealed interface EstadoApp {
    data object Iniciando : EstadoApp
    data object Login : EstadoApp
    data object CargandoDatos : EstadoApp
    data class Resumen(
        val summary: com.stockpulse.wear.data.SummaryDto,
        val alerts: List<AlertDto>
    ) : EstadoApp
    data class Error(val mensaje: String) : EstadoApp
}

@Composable
private fun PantallaCargando(texto: String) {
    Scaffold() {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text(texto, Modifier.padding(top = 40.dp), fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PantallaLogin(onLoggedIn: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val focusPassword = remember { androidx.compose.ui.focus.FocusRequester() }

    val listaEstado = rememberScalingLazyListState(initialCenterItemIndex = 0)

    // Entrar: compartido entre el circulo azul (IME) y el boton
    val entrar: () -> Unit = {
        if (email.isBlank() || !email.contains("@")) {
            error = "Escribe un email valido"
        } else if (password.length < 6) {
            error = "Contrasena de minimo 6 caracteres"
        } else {
            enviando = true
            error = null
            scope.launch {
                val repo = WearRepository(context)
                repo.login(email, password).fold(
                    onSuccess = { session ->
                        val token = session.accessToken
                        if (token.isNullOrBlank()) {
                            error = "El servidor devolvio una sesion invalida"
                            enviando = false
                        } else {
                            WearSession.save(context, token)
                            enviando = false
                            onLoggedIn()
                        }
                    },
                    onFailure = { e ->
                        error = e.message ?: "No se pudo iniciar sesion"
                        enviando = false
                    }
                )
            }
        }
    }

    Scaffold(timeText = WearTimeText) {
        ScalingLazyColumn(
            state = listaEstado,
            modifier = Modifier.fillMaxSize(),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "STOCKPULSE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Inicia sesion para ver tu portafolio",
                        fontSize = 10.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = {
                        androidx.compose.material3.Text("Email", fontSize = 10.sp, color = TextMuted)
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextDark),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { focusPassword.requestFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = {
                        androidx.compose.material3.Text("Contrasena", fontSize = 10.sp, color = TextMuted)
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextDark),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { entrar() }
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusPassword)
                )
            }
            item {
                Button(
                    onClick = entrar,
                    enabled = !enviando,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = BrandNavy,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (enviando) "Entrando…" else "Entrar")
                }
            }
            error?.let { msg ->
                item {
                    Text(
                        msg,
                        color = AccentLoss,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PantallaResumen(
    estado: EstadoApp.Resumen,
    onLogout: () -> Unit
) {
    val summary = estado.summary
    val saldo = summary.balance ?: 0.0
    val valor = summary.portfolioValue ?: 0.0
    val cost = summary.investedCost ?: 0.0
    val pnl = valor - cost
    val pnlPct = if (cost > 0.0) (pnl / cost) * 100.0 else 0.0
    val pnlColor = if (pnl >= 0.0) AccentGain else AccentLoss
    val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    val listaEstado = rememberScalingLazyListState(initialCenterItemIndex = 1)

    Scaffold(
        timeText = WearTimeText,
        positionIndicator = { PositionIndicator(scalingLazyListState = listaEstado) }
    ) {
        ScalingLazyColumn(state = listaEstado, modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    "STOCKPULSE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "Portafolio",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "$${"%,.2f".format(saldo)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "saldo disponible",
                        fontSize = 9.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "$${"%,.2f".format(valor)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${if (pnl >= 0.0) "+" else ""}${"%,.2f".format(pnl)} (${if (pnlPct >= 0.0) "+" else ""}${"%.2f".format(pnlPct)}%)",
                        fontSize = 10.sp,
                        color = pnlColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Text(
                    "Posiciones",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
            }
            (summary.holdingsEnVivo.orEmpty()).forEach { h ->
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "${h.symbol} x${h.quantity ?: 0}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            buildString {
                                h.marketValue.let { append("$${"%,.2f".format(it)}") }
                                h.unrealizedPnl?.let {
                                    append(" · ${if (it >= 0.0) "+" else ""}${"%,.2f".format(it)}")
                                }
                            },
                            fontSize = 9.sp,
                            color = TextMuted,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            if ((summary.holdingsEnVivo.orEmpty()).isEmpty()) {
                item {
                    Text(
                        "Sin posiciones abiertas",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
            item {
                Text(
                    "Tus alertas",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
            }
            estado.alerts.forEach { alerta ->
                item {
                    Text(
                        buildString {
                            append(alerta.symbol)
                            when (alerta.type) {
                                "price_above" -> append(" ≥")
                                "price_below" -> append(" ≤")
                                "change_pos", "change_neg" -> append("%")
                                else -> append(" ")
                            }
                            alerta.threshold.let { append(" $${"%,.2f".format(it)}") }
                        },
                        fontSize = 10.sp,
                        color = TextDark,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
            if (estado.alerts.isEmpty()) {
                item {
                    Text(
                        "Sin alertas (crealas en el telefono)",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }
            }
            item {
                Text(
                    "Actualizado $hora",
                    fontSize = 8.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AccentLoss,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar sesion")
                }
            }
        }
    }
}

@Composable
private fun PantallaError(mensaje: String, onReintentar: () -> Unit, onLogout: () -> Unit) {
    val listaEstado = rememberScalingLazyListState(initialCenterItemIndex = 0)
    Scaffold(timeText = WearTimeText) {
        ScalingLazyColumn(state = listaEstado, modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    "Ups",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentLoss,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    mensaje,
                    fontSize = 10.sp,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
            item {
                Button(
                    onClick = onReintentar,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AccentGain,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reintentar")
                }
            }
            item {
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = BrandNavy,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar sesion")
                }
            }
        }
    }
}
