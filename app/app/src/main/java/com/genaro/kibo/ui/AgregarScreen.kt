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
import com.genaro.kibo.data.CategoriaDto
import com.genaro.kibo.data.ProductoDto
import com.genaro.kibo.ui.theme.*
import kotlinx.coroutines.delay
import java.text.Normalizer

// Para que "jamon" encuentre "Jamón"
internal fun sinAcentos(s: String) =
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
    var creando by remember { mutableStateOf(false) }

    // Espera 300 ms después de la última letra antes de preguntar a la API
    LaunchedEffect(busqueda) {
        if (busqueda.trim().length < 2) {
            vm.buscarProductos("")
        } else {
            delay(300)
            vm.buscarProductos(busqueda)
        }
    }

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
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                if (vm.errorAgregar != null) {
                    Text(vm.errorAgregar!!, color = AvisoRojo)
                } else {
                    CircularProgressIndicator(color = KiboGreen)
                }
            }
            return@Column
        }

        // Elegir un producto: selecciona y propone la unidad "pza" por defecto
        val elegir: (ProductoDto) -> Unit = { p ->
            producto = p
            errorLocal = null
            if (unidadId == null) {
                unidadId = catalogo.unidades.firstOrNull { it.abreviatura == "pza" }?.id_unidad
                    ?: catalogo.unidades.firstOrNull()?.id_unidad
            }
        }

        // ---------- Buscador ----------
        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            label = { Text("Buscar (ej. tomate, pollo, leche)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
        if (vm.buscandoProducto) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp), color = KiboGreen
            )
        }
        Spacer(Modifier.height(8.dp))

        val texto = busqueda.trim()
        val lista: List<ProductoDto> =
            if (texto.length < 2) {
                catalogo.productos.filter {
                    texto.isEmpty() || sinAcentos(it.nombre).contains(sinAcentos(texto))
                }
            } else {
                // La API entiende alias y errores de escritura; sin red, filtramos localmente
                vm.resultadosBusqueda ?: catalogo.productos.filter {
                    sinAcentos(it.nombre).contains(sinAcentos(texto))
                }
            }

        // ---------- Lista de productos ----------
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (lista.isEmpty() && !vm.buscandoProducto) {
                item {
                    Text(
                        if (texto.isEmpty()) "No hay productos"
                        else "No encontramos \"$texto\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
            items(lista, key = { it.id_producto }) { p ->
                val seleccionado = producto?.id_producto == p.id_producto
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (seleccionado) Color(0xFFE9F5D8) else Color(0xFFF3EEF8))
                        .clickable { elegir(p) }
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
                    if (seleccionado) Text("✓", fontSize = 22.sp, color = KiboGreen)
                }
            }
            if (texto.length >= 2 && !vm.buscandoProducto) {
                item {
                    OutlinedButton(
                        onClick = { creando = true },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) { Text("➕ Crear \"$texto\" como producto personalizado") }
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

    // ---------- Diálogo: crear producto personalizado ----------
    if (creando && catalogo != null) {
        DialogoNuevoProducto(
            nombreInicial = busqueda.trim(),
            categorias = catalogo.categorias,
            guardando = vm.creandoProducto,
            errorApi = vm.errorProducto,
            onCancelar = {
                creando = false
                vm.limpiarErrorProducto()
            },
            onCrear = { nombre, idCategoria, dias ->
                vm.crearProducto(nombre, idCategoria, dias) { nuevo ->
                    producto = nuevo
                    if (unidadId == null) {
                        unidadId = catalogo.unidades
                            .firstOrNull { it.abreviatura == "pza" }?.id_unidad
                            ?: catalogo.unidades.firstOrNull()?.id_unidad
                    }
                    busqueda = ""
                    creando = false
                }
            }
        )
    }
}

@Composable
private fun DialogoNuevoProducto(
    nombreInicial: String,
    categorias: List<CategoriaDto>,
    guardando: Boolean,
    errorApi: String?,
    onCancelar: () -> Unit,
    onCrear: (String, Int, Int) -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial.replaceFirstChar { it.uppercase() }) }
    var categoriaId by remember { mutableStateOf<Int?>(null) }
    var diasTxt by remember { mutableStateOf("7") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo producto") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Categoría", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categorias, key = { it.id_categoria }) { c ->
                        ChipCategoria(c.nombre, c.id_categoria == categoriaId) {
                            categoriaId = c.id_categoria
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = diasTxt,
                    onValueChange = { diasTxt = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Días que dura en el refri") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Referencia: carnes 2-4, verduras 5-7, lácteos 7-14.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                (error ?: errorApi)?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = AvisoRojo)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !guardando,
                onClick = {
                    val dias = diasTxt.toIntOrNull()
                    val cat = categoriaId
                    when {
                        nombre.isBlank() -> error = "Escribe un nombre"
                        cat == null -> error = "Elige una categoría"
                        dias == null || dias !in 1..3650 ->
                            error = "Los días deben estar entre 1 y 3650"
                        else -> {
                            error = null
                            onCrear(nombre.trim(), cat, dias)
                        }
                    }
                }
            ) { Text(if (guardando) "Guardando..." else "Crear") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}