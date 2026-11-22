package com.genaro.kibo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genaro.kibo.AppViewModel
import com.genaro.kibo.data.ItemDto
import com.genaro.kibo.ui.theme.*

@Composable
fun InventarioScreen(vm: AppViewModel) {
    var filtro by remember { mutableStateOf("Todos") }
    var menuAbierto by remember { mutableStateOf(false) }

    val categorias = listOf("Todos") + vm.inventario.map { it.categoria }.distinct()
    val filtroActivo = if (filtro in categorias) filtro else "Todos"
    val visibles = if (filtroActivo == "Todos") vm.inventario
    else vm.inventario.filter { it.categoria == filtroActivo }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            // ---------- Encabezado ----------
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("K.I.B.O.", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                        color = KiboPurple)
                    Text("Inventario", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = KiboPurple)
                }
                IconButton(onClick = { /* búsqueda: más adelante */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar",
                        tint = KiboDarkGreen, modifier = Modifier.size(30.dp))
                }
                Box {
                    IconButton(onClick = { menuAbierto = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Opciones",
                            tint = KiboDarkGreen, modifier = Modifier.size(30.dp))
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        DropdownMenuItem(
                            text = { Text("Recargar") },
                            onClick = { menuAbierto = false; vm.recargar() }
                        )
                        DropdownMenuItem(
                            text = { Text("Salir") },
                            onClick = { menuAbierto = false; vm.salir() }
                        )
                    }
                }
            }

            // ---------- Chips de categoría ----------
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categorias) { cat ->
                    ChipCategoria(cat, cat == filtroActivo) { filtro = cat }
                }
            }

            if (vm.cargando) LinearProgressIndicator(Modifier.fillMaxWidth(), color = KiboGreen)
            vm.error?.let {
                Text(it, color = AvisoRojo,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            if (vm.sinConexion && vm.inventarioCargado) {
                Text("Sin conexión: mostrando datos guardados", color = AvisoNaranja,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }

            // ---------- Lista ----------
            if (visibles.isEmpty() && !vm.cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        when {
                            !vm.inventarioCargado && vm.sinConexion ->
                                "Sin conexión. Conéctate para cargar tu inventario"
                            !vm.inventarioCargado -> "Cargando inventario..."
                            else -> "No hay alimentos en esta categoría"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(visibles, key = { it.id_item }) { TarjetaAlimento(it) }
                }
            }
        }

        // ---------- Botón + ----------
        FloatingActionButton(
            onClick = { vm.abrirAgregar() },
            containerColor = KiboGreen,
            contentColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Agregar alimento",
                modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun ChipCategoria(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(forma)
            .background(if (seleccionado) KiboGreen else Color.Transparent)
            .border(2.dp, if (seleccionado) KiboGreen else KiboPurple, forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            texto,
            fontSize = 17.sp,
            color = if (seleccionado) Color.White else KiboPurple
        )
    }
}

private fun emojiDe(categoria: String) = when (categoria) {
    "Lácteos" -> "🥛"
    "Carnes" -> "🥩"
    "Frutas" -> "🍎"
    "Verduras" -> "🥬"
    "Sobras cocinadas" -> "🍲"
    else -> "🍽️"
}

@Composable
fun TarjetaAlimento(item: ItemDto) {
    val (texto, color) = when {
        item.dias_restantes < 0 -> "⛔ Caducado" to AvisoRojo
        item.dias_restantes == 0 -> "🚨 Caduca hoy" to AvisoRojo
        item.dias_restantes == 1 -> "⚠️ Caduca mañana" to AvisoNaranja
        item.dias_restantes <= 3 -> "⚠️ Expira en ${item.dias_restantes} días" to AvisoNaranja
        else -> "Expira en ${item.dias_restantes} días" to AvisoVerde
    }
    val cantidad = if (item.cantidad % 1.0 == 0.0) item.cantidad.toInt().toString()
    else item.cantidad.toString()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF3EEF8)),
                contentAlignment = Alignment.Center
            ) {
                Text(emojiDe(item.categoria), fontSize = 44.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.producto, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F))
                Text(
                    "${item.categoria} • $cantidad ${item.unidad}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Surface(color = color, shape = RoundedCornerShape(50)) {
                    Text(
                        texto,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}