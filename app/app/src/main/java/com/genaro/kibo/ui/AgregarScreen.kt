package com.genaro.kibo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genaro.kibo.AppViewModel
import com.genaro.kibo.data.ProductoDto
import com.genaro.kibo.ui.theme.*
import java.text.Normalizer

// Para que "jamon" encuentre "Jamón"
private fun sinAcentos(s: String) =
    Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()

@Composable
fun AgregarScreen(vm: AppViewModel) {
    val catalogo = vm.catalogo
    var busqueda by remember { mutableStateOf("") }
    var producto by remember { mutableStateOf<ProductoDto?>(null) }
    var unidadId by remember { mutableStateOf<Int?>(null) }
    var cantidadTxt by remember { mutableStateOf("1") }
    var errorLocal by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
            .imePadding()
    ) {
        // ---------- Encabezado ----------
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.cerrarAgregar() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver",
                    tint = KiboPurple)
            }
            Text("Agregar alimento", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = KiboPurple)
        }

        if (catalogo == null) {
            // Cargando el catálogo (o error al cargarlo)
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                if (vm.errorAgregar != null) {
                    Text(vm.errorAgregar!!, color = AvisoRojo)
                } else {
                    CircularProgressIndicator(color = KiboGreen)
                }
            }
            return@Column
        }

        // ---------- Buscador ----------
        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            label = { Text("Buscar producto") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))

        val filtrados = catalogo.productos.filter {
            sinAcentos(it.nombre).contains(sinAcentos(busqueda.trim()))
        }

        // ---------- Lista de productos ----------
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filtrados.isEmpty()) {
                item {
                    Text("No hay productos con ese nombre",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp))
                }
            }
            items(filtrados, key = { it.id_producto }) { p ->
                val elegido = producto?.id_producto == p.id_producto
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (elegido) Color(0xFFE9F5D8) else Color(0xFFF3EEF8))
                        .clickable {
                            producto = p
                            errorLocal = null
                            if (unidadId == null) {
                                unidadId = catalogo.unidades
                                    .firstOrNull { it.abreviatura == "pza" }?.id_unidad
                                    ?: catalogo.unidades.firstOrNull()?.id_unidad
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(p.nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            p.categoria + if (p.personalizado) " • Personalizado" else "",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (elegido) Text("✓", fontSize = 22.sp, color = KiboGreen)
                }
            }
        }

        // ---------- Panel inferior: unidad, cantidad, guardar ----------
        val elegido = producto
        if (elegido != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAF8F3))
                    .padding(20.dp)
            ) {
                Text(elegido.nombre, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Caducará en unos ${elegido.dias_vida_util} días (se calcula solo)",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(catalogo.unidades, key = { it.id_unidad }) { u ->
                        ChipCategoria(u.nombre, u.id_unidad == unidadId) {
                            unidadId = u.id_unidad
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = cantidadTxt,
                    onValueChange = { cantidadTxt = it },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                val mensajeError = errorLocal ?: vm.errorAgregar
                mensajeError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = AvisoRojo)
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val c = cantidadTxt.replace(',', '.').toDoubleOrNull()
                        val u = unidadId
                        if (c == null || c <= 0) {
                            errorLocal = "Escribe una cantidad mayor que 0"
                        } else if (u == null) {
                            errorLocal = "Elige una unidad"
                        } else {
                            errorLocal = null
                            vm.agregarAlimento(elegido.id_producto, u, c)
                        }
                    },
                    enabled = !vm.guardando,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(if (vm.guardando) "Guardando..." else "Guardar", fontSize = 18.sp)
                }
            }
        }
    }
}