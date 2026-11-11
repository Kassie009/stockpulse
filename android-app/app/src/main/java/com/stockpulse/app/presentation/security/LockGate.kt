package com.stockpulse.app.presentation.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Puerta de seguridad de la app del telefono (FASE 9.5): al abrir con
 * sesion guardada, NADA sensible se muestra hasta autenticar con la
 * huella (o el PIN del telefono). Igual que el candado del reloj.
 */
@Composable
fun LockGate(
    onUnlocked: () -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // El prompt necesita FragmentActivity (ComponentActivity no alcanza)
        (context as? FragmentActivity) ?: run { onUnlocked() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Text("🔒", fontSize = 30.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Saldo protegido",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Verifica tu identidad para entrar a StockPulse",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            androidx.compose.material3.Button(
                onClick = {
                    autenticar(context, onUnlocked)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Desbloquear con huella / PIN", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Cerrar sesión",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onLogout)
                    .padding(6.dp)
            )
        }
    }
}

private fun autenticar(context: android.content.Context, onUnlocked: () -> Unit) {
    val activity = context as? FragmentActivity ?: run {
        onUnlocked()
        return
    }
    val manager = BiometricManager.from(context)
    val puede = manager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    )

    val executor = ContextCompat.getMainExecutor(context)
    val prompt = BiometricPrompt(
        activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onUnlocked()
            }
        }
    )

    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("StockPulse protegido")
        .setSubtitle("Verifica tu identidad para ver tu portafolio")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    when (puede) {
        BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
            // En emulador: registra una huella en Extended Controls -> Fingerprint
            try {
                prompt.authenticate(info)
            } catch (_: Exception) {
                // En API 29 la combinacion huella+PIN no esta permitida:
                // pedir solo huella; si no hay, liberar con PIN del sistema
                if (manager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
                    BiometricManager.BIOMETRIC_SUCCESS
                ) {
                    val soloCredencial = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("StockPulse protegido")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        .build()
                    prompt.authenticate(soloCredencial)
                } else if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                    BiometricManager.BIOMETRIC_SUCCESS
                ) {
                    val soloHuella = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("StockPulse protegido")
                        .setNegativeButtonText("Cancelar")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                        .build()
                    prompt.authenticate(soloHuella)
                } else {
                    onUnlocked() // sin biometria disponible en el dispositivo
                }
            }
        }
        else -> onUnlocked() // dispositivo sin bloqueo configurado: no pedimos nada
    }
}
