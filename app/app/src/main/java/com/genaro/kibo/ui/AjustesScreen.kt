package com.genaro.kibo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genaro.kibo.AppViewModel
import com.genaro.kibo.data.CupoDto
import com.genaro.kibo.ui.theme.*

@Composable
fun AjustesScreen(vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.limpiarAvisosAjustes() }
    LaunchedEffect(vm.hogarActivoId) { vm.cargarPlan() }

    val usuario = vm.sesion?.usuario
    val hogarId = vm.hogarActivo?.id_hogar
    val hogarPlan = vm.plan?.hogares?.firstOrNull { it.id_hogar == hogarId }
    val esAdmin = hogarPlan?.rol == "admin"
    var codigo by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text("K.I.B.O.", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = KiboPurple)
        Text("Ajustes", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KiboPurple)
        SelectorHogar(vm)
        Spacer(Modifier.height(16.dp))

        if (vm.cargandoPlan) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = KiboGreen)
            Spacer(Modifier.height(8.dp))
        }
        vm.errorAjustes?.let {
            Text(it, color = AvisoRojo)
            Spacer(Modifier.height(8.dp))
        }
        vm.avisoAjustes?.let {
            Text(it, color = AvisoVerde)
            Spacer(Modifier.height(8.dp))
        }

        // ---------- Cuenta ----------
        Tarjeta {
            Text(usuario?.nombre ?: "", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(usuario?.email ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Surface(
                color = if (usuario?.plan == "Premium") KiboGreen else KiboPurple,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "Plan ${usuario?.plan ?: ""}",
                    color = Color.White, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------- Uso del hogar ----------
        if (hogarPlan != null) {
            Tarjeta {
                Text("Tu hogar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Plan del hogar: ${hogarPlan.plan_del_hogar} (el del administrador)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                Cupo("Miembros", hogarPlan.miembros)
                Spacer(Modifier.height(10.dp))
                Cupo("Alimentos", hogarPlan.items)
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- Premium (simulado) ----------
        if (usuario?.plan != "Premium") {
            Tarjeta {
                Text("Pasa a Premium", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Hasta 10 miembros, alimentos y productos personalizados ilimitados.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.mejorarAPremium() },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Activar Premium (simulado)") }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- Invitar (solo admin) ----------
        if (esAdmin) {
            Tarjeta {
                Text("Invitar a tu hogar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Genera un código y compártelo. Sirve 7 días.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.generarInvitacion() },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Generar código") }

                vm.invitacion?.let { inv ->
                    Spacer(Modifier.height(12.dp))
                    SelectionContainer {
                        Text(
                            inv.codigo,
                            fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
                            color = KiboPurple,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    Text(
                        "Expira el ${inv.expira.take(10)}. Mantén presionado el código para copiarlo.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- Unirse a un hogar ----------
        Tarjeta {
            Text("Unirme a otro hogar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = codigo,
                onValueChange = { codigo = it.uppercase().take(12) },
                label = { Text("Código de invitación") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { vm.unirseAHogar(codigo); codigo = "" },
                enabled = codigo.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Unirme") }
        }

        Spacer(Modifier.height(24.dp))

        // ---------- Salir ----------
        OutlinedButton(
            onClick = { vm.salir() },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Cerrar sesión", color = AvisoRojo) }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Tarjeta(contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), content = contenido)
    }
}

@Composable
private fun Cupo(titulo: String, cupo: CupoDto) {
    val maximo = cupo.maximo   // null = ilimitado
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(titulo)
        Text(
            if (maximo == null) "${cupo.usados} (ilimitado)" else "${cupo.usados} de $maximo",
            fontWeight = FontWeight.Medium
        )
    }
    if (maximo != null && maximo > 0) {
        val avance = (cupo.usados.toFloat() / maximo).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { avance },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            color = if (avance >= 1f) AvisoRojo else KiboGreen
        )
    }
}