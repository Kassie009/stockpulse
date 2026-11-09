package com.stockpulse.app.presentation.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pantalla de bienvenida (Modulo 1: Autenticacion).
 */
@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text(
                text = "STOCKPULSE",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Aprende inversiones con créditos virtuales.\n100% simulación, 0% dinero real.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Button(
                onClick = onCreateAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear cuenta")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar sesión")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Al crearte damos 10,000 créditos virtuales para practicar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
