package com.genaro.kibo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.genaro.kibo.AppViewModel

@Composable
fun LoginScreen(vm: AppViewModel) {
    // Valores de prueba para desarrollo; bórralos después
    var email by remember { mutableStateOf("carlos@correo.com") }
    var password by remember { mutableStateOf("123456") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("K.I.B.O.", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Inventario", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        vm.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { vm.login(email.trim(), password) },
            enabled = !vm.cargando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.cargando) "Entrando..." else "Entrar")
        }
    }
}