package com.genaro.kibo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.genaro.kibo.AppViewModel
import com.genaro.kibo.ui.theme.AvisoRojo

@Composable
fun RegistroScreen(vm: AppViewModel) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var errorLocal by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = { vm.cerrarRegistro() }, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al inicio de sesión")
        }

        Text("Crear cuenta", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Únete a K.I.B.O.", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it.uppercase() },
            label = { Text("Código de invitación (opcional)") },
            supportingText = { Text("Déjalo vacío para crear tu propio hogar") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        (errorLocal ?: vm.errorRegistro)?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = AvisoRojo)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val n = nombre.trim()
                val e = email.trim()
                when {
                    n.isEmpty() -> errorLocal = "Escribe tu nombre"
                    e.isEmpty() -> errorLocal = "Escribe tu correo"
                    password.length < 6 -> errorLocal = "La contraseña debe tener al menos 6 caracteres"
                    else -> {
                        errorLocal = null
                        vm.registrar(n, e, password, codigo.trim())
                    }
                }
            },
            enabled = !vm.cargandoRegistro,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.cargandoRegistro) "Creando cuenta..." else "Crear cuenta")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { vm.cerrarRegistro() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}
