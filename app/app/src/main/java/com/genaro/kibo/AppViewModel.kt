package com.genaro.kibo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genaro.kibo.data.ApiClient
import com.genaro.kibo.data.ItemDto
import com.genaro.kibo.data.LoginRequest
import com.genaro.kibo.data.LoginResponse
import kotlinx.coroutines.launch
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
        error = null
    }

    private suspend fun traerInventario() {
        val hogar = sesion?.hogares?.firstOrNull()
            ?: throw IllegalStateException("Tu usuario no pertenece a ningún hogar")
        inventario = ApiClient.api.inventario(hogar.id_hogar).inventario
    }

    private fun mensaje(e: Exception): String = when (e) {
        is HttpException ->
            if (e.code() == 401) "Credenciales incorrectas o sesión expirada"
            else "Error del servidor (${e.code()})"
        is  IOException -> "No se pudo conectar: ${e.javaClass.simpleName} - ${e.message}"
        else -> e.message ?: "Error desconocido"
    }
}