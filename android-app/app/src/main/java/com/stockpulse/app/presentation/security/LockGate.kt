package com.stockpulse.app.presentation.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Puerta de seguridad del telefono (FASE 9.5): al abrir con sesion
 * guardada, NADA sensible se muestra hasta ingresar el PIN de bloqueo
 * del dispositivo (KeyguardManager, la via oficial para credencial-only).
 * El reloj tiene su propia version con PIN local.
 */
@Composable
fun LockGate(
    onUnlocked: () -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var sinPin by remember { mutableStateOf(false) }

    // Resultado de la pantalla de confirmacion de credenciales del sistema
    val confirmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) onUnlocked()
    }

    fun pedirPin() {
        val manager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (manager.isDeviceSecure) {
            confirmLauncher.launch(
                manager.createConfirmDeviceCredentialIntent(
                    "StockPulse protegido",
                    "Ingresa tu PIN para ver tu portafolio"
                )
            )
        } else {
            sinPin = true
        }
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
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔒", fontSize = 30.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Saldo protegido",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Verifica con el PIN de bloqueo de tu teléfono",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { pedirPin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Desbloquear con PIN", fontWeight = FontWeight.SemiBold)
            }

            if (sinPin) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Este teléfono no tiene PIN de bloqueo. Configúralo para poder desbloquear StockPulse.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        (context as? Activity)?.startActivity(
                            Intent(Settings.ACTION_SECURITY_SETTINGS)
                        )
                    },
                    modifier = Modifier.width(240.dp)
                ) {
                    Text("Abrir ajustes de seguridad")
                }
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
