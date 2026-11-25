package com.genaro.kibo.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genaro.kibo.AppViewModel
import com.genaro.kibo.data.LineaDictada
import com.genaro.kibo.data.UnidadDto
import com.genaro.kibo.ui.theme.*

private fun formatoCantidad(d: Double) =
    if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

@Composable
fun VozScreen(vm: AppViewModel) {
    var frase by remember { mutableStateOf("") }
    var alternativas by remember { mutableStateOf<List<String>>(emptyList()) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var textoManual by remember { mutableStateOf("") }

    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val lista = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
            if (lista.isNotEmpty()) {
                aviso = null
                frase = lista.first()
                alternativas = lista.drop(1)
                vm.procesarFrase(frase)
            }
        }
    }

    fun hablar() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di qué compraste")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        try {
            lanzador.launch(intent)
        } catch (e: ActivityNotFoundException) {
            aviso = "Tu celular no tiene reconocimiento de voz disponible. " +
                    "Instala o actualiza la app Google, o escribe la frase abajo."
        }
    }

    val unidades = vm.catalogo?.unidades.orEmpty()
    val porGuardar = vm.lineasDictado.count { it.incluir && it.elegidoId != null }

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
            IconButton(onClick = { vm.cerrarDictado() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver",
                    tint = KiboPurple)
            }
            Text("Agregar por voz", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = KiboPurple)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------- Micrófono y campo de texto ----------
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { hablar() },
                        enabled = !vm.procesandoDictado && !vm.guardandoDictado,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Toca y habla", fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Ejemplo: \"dos litros de leche, una docena de huevos y tres jitomates\"",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    aviso?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = AvisoNaranja, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = textoManual,
                        onValueChange = { textoManual = it },
                        label = { Text("O escribe la frase") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            frase = textoManual.trim()
                            alternativas = emptyList()
                            vm.procesarFrase(frase)
                        },
                        enabled = textoManual.isNotBlank() && !vm.procesandoDictado,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) { Text("Interpretar") }
                }
            }

            // ---------- Lo que se escuchó ----------
            if (frase.isNotBlank()) {
                item {
                    Column {
                        Text("Escuché: «$frase»", fontWeight = FontWeight.Medium)
                        if (alternativas.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("¿Quisiste decir…?", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            alternativas.take(3).forEach { alt ->
                                TextButton(onClick = {
                                    frase = alt
                                    vm.procesarFrase(alt)
                                }) { Text(alt) }
                            }
                        }
                    }
                }
            }

            if (vm.procesandoDictado) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = KiboGreen) }
            }
            vm.errorDictado?.let { msg ->
                item { Text(msg, color = AvisoRojo) }
            }

            // ---------- Líneas para confirmar ----------
            items(vm.lineasDictado, key = { it.id }) { l ->
                TarjetaDictado(vm, l, unidades)
            }
        }

        // ---------- Guardar ----------
        if (vm.lineasDictado.isNotEmpty()) {
            Button(
                onClick = { vm.guardarDictado() },
                enabled = porGuardar > 0 && !vm.guardandoDictado && !vm.procesandoDictado,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(52.dp)
            ) {
                Text(
                    when {
                        vm.guardandoDictado -> "Guardando..."
                        porGuardar == 1 -> "Agregar 1 alimento"
                        else -> "Agregar $porGuardar alimentos"
                    },
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun TarjetaDictado(vm: AppViewModel, l: LineaDictada, unidades: List<UnidadDto>) {
    var cantTxt by remember(l.id) { mutableStateOf(formatoCantidad(l.cantidad)) }
    val elegido = l.elegido

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EEF8))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = l.incluir && elegido != null,
                    enabled = elegido != null,
                    onCheckedChange = { marcado ->
                        vm.actualizarLineaDictado(l.id) { it.copy(incluir = marcado) }
                    }
                )
                Column(Modifier.weight(1f)) {
                    if (elegido != null) {
                        Text(elegido.nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Entendí: «${l.consulta}»", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (elegido.confianza != null && elegido.confianza != "alta") {
                            Text("⚠️ Revisa que sea el correcto", fontSize = 13.sp,
                                color = AvisoNaranja)
                        }
                    } else {
                        Text("No encontré «${l.consulta}»", fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, color = AvisoRojo)
                        Text(
                            "Agrégalo con el botón + (puedes crearlo como producto personalizado)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ¿Era otro producto?
            if (l.candidatos.size > 1) {
                Spacer(Modifier.height(6.dp))
                Text("¿Era otro?", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(l.candidatos, key = { it.id_producto }) { c ->
                        ChipCategoria(c.nombre, c.id_producto == l.elegidoId) {
                            vm.actualizarLineaDictado(l.id) {
                                it.copy(elegidoId = c.id_producto, incluir = true)
                            }
                        }
                    }
                }
            }

            // Cantidad y unidad
            if (elegido != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = cantTxt,
                        onValueChange = { texto ->
                            cantTxt = texto
                            val v = texto.replace(',', '.').toDoubleOrNull()
                            if (v != null && v > 0) {
                                vm.actualizarLineaDictado(l.id) { it.copy(cantidad = v) }
                            }
                        },
                        label = { Text("Cantidad") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(110.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(unidades, key = { it.id_unidad }) { u ->
                            ChipCategoria(u.abreviatura, u.abreviatura == l.unidad) {
                                vm.actualizarLineaDictado(l.id) { it.copy(unidad = u.abreviatura) }
                            }
                        }
                    }
                }
            }
        }
    }
}