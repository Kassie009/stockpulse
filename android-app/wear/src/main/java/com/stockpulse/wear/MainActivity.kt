package com.stockpulse.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
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
 * - Seguridad: al abrir la app NADA se muestra sin confirmar (pantalla
 *   de bloqueo) y FLAG_SECURE la oculta del multitarea
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

        // Seguridad bancaria: el contenido NUNCA aparece en el multitarea
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

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

        // Re-bloqueo automatico: al salir de primer plano (boton home,
        // regresar al reloj, multitarea) la app vuelve al candado. Asi
        // 'abrir la app' SIEMPRE pasa por el desbloqueo.
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observador = androidx.lifecycle.LifecycleEventObserver { _, evento ->
                if (evento == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                    if (estado is EstadoApp.Resumen || estado is EstadoApp.Error) {
                        estado = EstadoApp.Bloqueado
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observador)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
        }

        // Al abrir: si hay token guardado, va a la pantalla de BLOQUEO
        // (nada de datos sensibles sin confirmar - regla del maestro)
        LaunchedEffect(Unit) {
            val token = WearSession.tokenOrNull(applicationContext)
            estado = if (token == null) EstadoApp.Login else EstadoApp.Bloqueado
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

        // Sincronizacion INSTANTANEA: el backend avisa por FCM (mensaje
        // silencioso "sync") cuando algo cambia en el telefono
        LaunchedEffect(Unit) {
            com.stockpulse.wear.data.WearSyncBus.signal.collect {
                if (estado is EstadoApp.Resumen) {
                    val resumen = repository.summary()
                    if (resumen.isSuccess) {
                        val summary = resumen.getOrThrow()
                        val alerts: List<AlertDto> = repository
                            .alerts()
                            .getOrDefault(emptyList())
                        estado = EstadoApp.Resumen(summary, alerts)
                    }
                }
            }
        }

        // Auto-sincronizacion cada 45s (red de seguridad si el push fallo)
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(45_000)
                if (estado is EstadoApp.Resumen) {
                    val resumen = repository.summary()
                    if (resumen.isSuccess) {
                        val summary = resumen.getOrThrow()
                        val alerts: List<AlertDto> = repository
                            .alerts()
                            .getOrDefault(emptyList())
                        estado = EstadoApp.Resumen(summary, alerts)
                    }
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
                is EstadoApp.Bloqueado -> PantallaBloqueo(
                    context = applicationContext,
                    onUnlock = { estado = EstadoApp.CargandoDatos },
                    onLogout = {
                        scope.launch {
                            WearSession.clear(applicationContext)
                            com.stockpulse.wear.data.WearPin.clear(applicationContext)
                            estado = EstadoApp.Login
                        }
                    }
                )
                is EstadoApp.Resumen -> PantallaResumen(
                    estado = actual,
                    onLock = { estado = EstadoApp.Bloqueado }
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
    data object Bloqueado : EstadoApp
    data object CargandoDatos : EstadoApp
    data class Resumen(
        val summary: com.stockpulse.wear.data.SummaryDto,
        val alerts: List<AlertDto>
    ) : EstadoApp
    data class Error(val mensaje: String) : EstadoApp
}

/** Pantalla de bloqueo con PIN: nada sensible visible sin el codigo. */
@Composable
private fun PantallaBloqueo(
    context: android.content.Context,
    onUnlock: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var crear by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var listo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        crear = !com.stockpulse.wear.data.WearPin.hasPin(context)
        listo = true
    }

    val accion: () -> Unit = {
        if (pin.length != 4) {
            error = "El PIN debe tener 4 digitos"
        } else {
            error = null
            scope.launch {
                if (crear) {
                    com.stockpulse.wear.data.WearPin.setPin(context, pin)
                    onUnlock()
                } else if (com.stockpulse.wear.data.WearPin.checkPin(context, pin)) {
                    onUnlock()
                } else {
                    error = "PIN incorrecto"
                    pin = ""
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔒", fontSize = 15.sp)
            Text(
                "Saldo protegido",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (listo) {
                Text(
                    if (crear) "Crea tu PIN de 4 digitos" else "Escribe tu PIN para desbloquear",
                    fontSize = 8.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                CampoRedondo(
                    value = pin,
                    onValueChange = { nuevo ->
                        if (nuevo.length <= 4 && nuevo.all { c -> c.isDigit() }) pin = nuevo
                    },
                    placeholder = if (crear) "Nuevo PIN" else "PIN",
                    onEnter = accion,
                    secret = true,
                    numeric = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { msg ->
                    Text(
                        msg,
                        color = AccentLoss,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp)
                    )
                }
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(BrandNavy, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .clickable(enabled = true) { accion() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (crear) "Guardar PIN" else "Desbloquear",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (!crear) {
                    androidx.compose.material3.Text(
                        "Olvidaste tu PIN? Cerrar sesion",
                        fontSize = 8.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onLogout)
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
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

    // Columna compacta y centrada: SIEMPRE cabe en el reloj redondo
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "STOCKPULSE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BrandNavy,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Inicia sesion",
                fontSize = 10.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            CampoRedondo(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                onEnter = { focusPassword.requestFocus() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            CampoRedondo(
                value = password,
                onValueChange = { password = it },
                placeholder = "Contrasena",
                onEnter = { entrar() },
                secret = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusPassword)
            )
            Spacer(Modifier.height(8.dp))
            // Boton compacto del mismo tamano que los campos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(BrandNavy, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .clickable(enabled = !enviando) { entrar() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (enviando) "Iniciando sesión…" else "Iniciar sesión",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            error?.let { msg ->
                Text(
                    msg,
                    color = AccentLoss,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

/** Campo compacto (34dp) con caja redondeada, pensado para la muneca. */
@Composable
private fun CampoRedondo(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onEnter: () -> Unit,
    secret: Boolean = false,
    numeric: Boolean = false,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextDark),
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                secret -> KeyboardType.Password
                numeric -> KeyboardType.NumberPassword
                else -> KeyboardType.Email
            },
            imeAction = if (secret) {
                androidx.compose.ui.text.input.ImeAction.Done
            } else {
                androidx.compose.ui.text.input.ImeAction.Next
            }
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onNext = { onEnter() },
            onDone = { onEnter() }
        ),
        visualTransformation = if (secret) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        modifier = modifier.onPreviewKeyEvent { evento ->
            // El teclado de Wear manda "Enter" como tecla, no como accion IME
            if (evento.type == androidx.compose.ui.input.key.KeyEventType.KeyUp &&
                (evento.key == androidx.compose.ui.input.key.Key.Enter ||
                    evento.key == androidx.compose.ui.input.key.Key.NumPadEnter)
            ) {
                onEnter()
                true   // consumido: no inserta salto de linea
            } else {
                false
            }
        },
        decorationBox = { inner ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .height(34.dp)
                    .background(Color(0xFFE8EDF5), androidx.compose.foundation.shape.RoundedCornerShape(9.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 10.sp, color = TextMuted)
                }
                inner()
            }
        }
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PantallaResumen(
    estado: EstadoApp.Resumen,
    onLock: () -> Unit
) {
    val summary = estado.summary
    val saldo = summary.balance ?: 0.0
    val valor = summary.portfolioValue ?: 0.0
    val cost = summary.investedCost ?: 0.0
    val pnl = valor - cost
    val pnlPct = if (cost > 0.0) (pnl / cost) * 100.0 else 0.0
    val pnlColor = if (pnl >= 0.0) AccentGain else AccentLoss
    val posiciones = summary.holdingsEnVivo.orEmpty()

    // 3 pantallas deslizantes: 0=Portafolio, 1=Alertas, 2=Cerrar sesion
    val pagerEstado = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = 0,
        pageCount = { 3 }
    )

    Scaffold(timeText = WearTimeText) {
        Box(Modifier.fillMaxSize()) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerEstado,
                modifier = Modifier.fillMaxSize()
            ) { pagina ->
                when (pagina) {
                    0 -> {
                        // PAGINA 1 · Portafolio (un vistazo)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                        ) {
                            Text(
                                "STOCKPULSE",
                                fontSize = 9.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Saldo",
                                fontSize = 9.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "$${"%,.2f".format(saldo)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Portafolio $${"%,.2f".format(valor)} · ${
                                    if (pnl >= 0.0) "+" else ""
                                }${"%,.2f".format(pnl)} (${
                                    if (pnlPct >= 0.0) "+" else ""
                                }${"%.2f".format(pnlPct)}%)",
                                fontSize = 11.sp,
                                color = pnlColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            if (posiciones.isEmpty()) {
                                Text(
                                    "Sin posiciones",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            posiciones.take(3).forEach { h ->
                                Text(
                                    buildString {
                                        append("${h.symbol} x${h.quantity ?: 0}")
                                        h.unrealizedPnl?.let {
                                            append("  ${if (it >= 0.0) "+" else ""}${"%,.2f".format(it)}")
                                        }
                                    },
                                    fontSize = 10.sp,
                                    color = TextDark,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Text(
                                "Desliza para ver mas  →",
                                fontSize = 8.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    1 -> {
                        // PAGINA 2 · Tus alertas
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                        ) {
                            Text(
                                "Tus alertas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            if (estado.alerts.isEmpty()) {
                                Text(
                                    "Sin alertas activas",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            estado.alerts.forEach { alerta ->
                                Text(
                                    buildString {
                                        append(alerta.symbol)
                                        when (alerta.type) {
                                            "price_above" -> append(" ≥ $${"%,.2f".format(alerta.threshold)}")
                                            "price_below" -> append(" ≤ $${"%,.2f".format(alerta.threshold)}")
                                            "change_pos" -> append(" ↗ +${"%,.1f".format(alerta.threshold)}%")
                                            "change_neg" -> append(" ↘ −${"%,.1f".format(alerta.threshold)}%")
                                            else -> append(" ${"%,.1f".format(alerta.threshold)}%")
                                        }
                                    },
                                    fontSize = 11.sp,
                                    color = TextDark,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Text(
                                "Se crean y editan en el telefono",
                                fontSize = 8.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    else -> {
                        // PAGINA 3 · Cuenta
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                        ) {
                            androidx.compose.material3.Text(
                                "Bloquear",
                                fontSize = 13.sp,
                                color = AccentLoss,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onLock)
                                    .padding(vertical = 12.dp)
                            )
                            Text(
                                "StockPulse Wear",
                                fontSize = 8.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            // Puntos de paginacion (ayudan a entender que se puede deslizar)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) { indice ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (pagerEstado.currentPage == indice) BrandNavy
                                else Color(0xFF5B6B85).copy(alpha = 0.30f),
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
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
