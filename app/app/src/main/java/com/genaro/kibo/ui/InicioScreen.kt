package com.genaro.kibo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genaro.kibo.AppViewModel
import com.genaro.kibo.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun InicioScreen(vm: AppViewModel) {
    // Cada vez que se entra a Inicio, se piden datos frescos
    LaunchedEffect(Unit) { vm.cargarEstadisticas() }
    val est = vm.estadisticas

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text("K.I.B.O.", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = KiboPurple)
        Text("Inicio", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KiboPurple)
        Spacer(Modifier.height(16.dp))

        if (vm.cargandoEstadisticas) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = KiboGreen)
            Spacer(Modifier.height(8.dp))
        }
        vm.errorEstadisticas?.let {
            Text(it, color = AvisoNaranja)
            Spacer(Modifier.height(8.dp))
        }

        if (est == null) {
            if (!vm.cargandoEstadisticas) {
                Text("Cargando estadísticas...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        // ---------- Porcentaje aprovechado ----------
        val pct = est.porcentaje_aprovechado
        val colorPct = when {
            pct == null -> KiboPurple
            pct >= 70 -> AvisoVerde
            pct >= 40 -> AvisoNaranja
            else -> AvisoRojo
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EEF8))
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Comida aprovechada", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(
                    if (pct != null) "${pct.roundToInt()} %" else "—",
                    fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = colorPct
                )
                Text(
                    if (pct != null) "de lo que sacaste del refri se consumió"
                    else "Marca alimentos como consumidos o desechados para ver tu avance",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------- Consumidos y desechados ----------
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Contador(Modifier.weight(1f), est.consumidos, "✅ Consumidos", AvisoVerde)
            Contador(Modifier.weight(1f), est.desechados, "🗑️ Desechados", AvisoRojo)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Tienes ${est.disponibles} alimentos en el refri",
            fontSize = 18.sp, fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(12.dp))

        // ---------- Alertas (llevan a Inventario) ----------
        if (est.caducados_sin_desechar > 0) {
            Alerta("⛔ ${est.caducados_sin_desechar} caducados sin desechar", AvisoRojo) {
                vm.cambiarPantalla("inventario")
            }
            Spacer(Modifier.height(10.dp))
        }
        if (est.por_vencer_3_dias > 0) {
            Alerta("⚠️ ${est.por_vencer_3_dias} vencen en 3 días o menos", AvisoNaranja) {
                vm.cambiarPantalla("inventario")
            }
        }
        if (est.caducados_sin_desechar == 0 && est.por_vencer_3_dias == 0 && est.disponibles > 0) {
            Text("Todo en orden 👍", color = AvisoVerde, fontSize = 18.sp)
        }
    }
}

@Composable
private fun Contador(modifier: Modifier, valor: Int, etiqueta: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$valor", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(etiqueta, fontSize = 15.sp)
        }
    }
}

@Composable
private fun Alerta(texto: String, color: Color, onClick: () -> Unit) {
    Surface(
        color = color,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Text(
            "$texto  ›",
            color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(16.dp)
        )
    }
}