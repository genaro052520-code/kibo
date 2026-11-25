package com.genaro.kibo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genaro.kibo.AppViewModel
import com.genaro.kibo.ui.theme.KiboPurple

/** Menú para cambiar de hogar. Solo aparece si el usuario pertenece a 2 o más. */
@Composable
fun SelectorHogar(vm: AppViewModel, modifier: Modifier = Modifier) {
    var abierto by remember { mutableStateOf(false) }
    val hogares = vm.sesion?.hogares ?: return
    if (hogares.size < 2) return
    val activo = vm.hogarActivo

    Box(modifier) {
        Surface(
            color = Color(0xFFF3EEF8),
            shape = RoundedCornerShape(50),
            modifier = Modifier.clickable { abierto = true }
        ) {
            Text(
                "🏠 Hogar #${activo?.id_hogar}  ▾",
                color = KiboPurple,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            hogares.forEach { h ->
                val rol = if (h.rol == "admin") "admin" else "miembro"
                val marca = if (h.id_hogar == activo?.id_hogar) "  ✓" else ""
                DropdownMenuItem(
                    text = { Text("Hogar #${h.id_hogar} ($rol)$marca") },
                    onClick = {
                        abierto = false
                        vm.cambiarHogar(h.id_hogar)
                    }
                )
            }
        }
    }
}