package com.genaro.kibo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genaro.kibo.data.ApiClient
import com.genaro.kibo.data.CatalogoResponse
import com.genaro.kibo.data.ItemDto
import com.genaro.kibo.data.LoginRequest
import com.genaro.kibo.data.LoginResponse
import com.genaro.kibo.data.NuevoItemRequest
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class AppViewModel : ViewModel() {

    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var sesion by mutableStateOf<LoginResponse?>(null)
        private set
    var inventario by mutableStateOf<List<ItemDto>>(emptyList())
        private set

    // ----- Pantalla "Agregar alimento" -----
    var catalogo by mutableStateOf<CatalogoResponse?>(null)
        private set
    var mostrandoAgregar by mutableStateOf(false)
        private set
    var guardando by mutableStateOf(false)
        private set
    var errorAgregar by mutableStateOf<String?>(null)
        private set

    fun login(email: String, password: String) {
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                val r = ApiClient.api.login(LoginRequest(email, password))
                ApiClient.token = r.access_token
                sesion = r
                traerInventario()
            } catch (e: Exception) {
                error = mensaje(e)
            }
            cargando = false
        }
    }

    fun recargar() {
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                traerInventario()
            } catch (e: Exception) {
                error = mensaje(e)
            }
            cargando = false
        }
    }

    fun salir() {
        ApiClient.token = null
        sesion = null
        inventario = emptyList()
        catalogo = null
        mostrandoAgregar = false
        error = null
        errorAgregar = null
    }

    // ----- Agregar alimento -----
    fun abrirAgregar() {
        errorAgregar = null
        mostrandoAgregar = true
        if (catalogo == null) cargarCatalogo()
    }

    fun cerrarAgregar() {
        mostrandoAgregar = false
    }

    private fun cargarCatalogo() {
        viewModelScope.launch {
            try {
                val hogar = sesion?.hogares?.firstOrNull() ?: return@launch
                catalogo = ApiClient.api.catalogo(hogar.id_hogar)
            } catch (e: Exception) {
                errorAgregar = mensaje(e)
            }
        }
    }

    fun agregarAlimento(idProducto: Int, idUnidad: Int, cantidad: Double) {
        viewModelScope.launch {
            guardando = true
            errorAgregar = null
            try {
                val hogar = sesion?.hogares?.firstOrNull()
                    ?: throw IllegalStateException("Tu usuario no pertenece a ningún hogar")
                ApiClient.api.agregarItem(
                    hogar.id_hogar,
                    NuevoItemRequest(idProducto, idUnidad, cantidad)
                )
                mostrandoAgregar = false
                recargar()
            } catch (e: Exception) {
                errorAgregar = mensaje(e)
            }
            guardando = false
        }
    }

    private suspend fun traerInventario() {
        val hogar = sesion?.hogares?.firstOrNull()
            ?: throw IllegalStateException("Tu usuario no pertenece a ningún hogar")
        inventario = ApiClient.api.inventario(hogar.id_hogar).inventario
    }

    private fun mensaje(e: Exception): String = when (e) {
        is HttpException -> {
            // La API manda {"error": "..."}: se lo mostramos al usuario
            val detalle = try {
                JSONObject(e.response()?.errorBody()?.string() ?: "").optString("error")
            } catch (_: Exception) {
                ""
            }
            when {
                detalle.isNotBlank() -> detalle
                e.code() == 401 -> "Credenciales incorrectas o sesión expirada"
                else -> "Error del servidor (${e.code()})"
            }
        }
        is IOException -> "No se pudo conectar: ${e.javaClass.simpleName} - ${e.message}"
        else -> e.message ?: "Error desconocido"
    }
}