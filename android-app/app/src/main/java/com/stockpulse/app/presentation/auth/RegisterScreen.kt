package com.stockpulse.app.presentation.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockpulse.app.ui.theme.GreenGain
import com.stockpulse.app.ui.theme.RedLoss

/**
 * Pantalla de registro (Modulo 1). Al terminar, Supabase entrega los
 * 10,000 creditos automaticamente (trigger de base de datos).
 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onAuthed: () -> Unit
) {
    val viewModel: AuthViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    LaunchedEffect(state.authed) {
        if (state.authed) onAuthed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Recibiras 10,000 creditos virtuales de bienvenida",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Nombre completo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electronico") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contrasena (minimo 8 caracteres)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("Confirmar contrasena") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        state.errorMessage?.let {
            Text(
                text = it,
                color = RedLoss,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
        }
        state.infoMessage?.let {
            Text(
                text = it,
                color = GreenGain,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = { viewModel.register(fullName, email, password, confirm) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.width(18.dp).height(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(10.dp))
            }
            Text("Crear mi cuenta")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onBack,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}
