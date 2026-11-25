package com.genaro.kibo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.genaro.kibo.data.ApiClient
import com.genaro.kibo.data.CatalogoResponse
import com.genaro.kibo.data.HogarDto
import com.genaro.kibo.data.ItemDto
import com.genaro.kibo.data.LoginRequest
import com.genaro.kibo.data.LoginResponse
import com.genaro.kibo.data.NuevoItemRequest
import com.genaro.kibo.data.ActualizarItemRequest
import com.genaro.kibo.data.RegistroRequest
import com.genaro.kibo.data.SessionStore
import com.genaro.kibo.data.UsuarioDto
import com.genaro.kibo.data.EstadisticasResponse
import com.genaro.kibo.data.InvitacionResponse
import com.genaro.kibo.data.PlanResponse
import com.genaro.kibo.data.UnirseRequest
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val sessionStore = SessionStore(app)

    init {
        restaurarSesion()
    }

    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var sesion by mutableStateOf<LoginResponse?>(null)
        private set
    var inventario by mutableStateOf<List<ItemDto>>(emptyList())
        private set
    var inventarioCargado by mutableStateOf(false)
        private set
    var sinConexion by mutableStateOf(false)
        private set
    var restaurandoSesion by mutableStateOf(true)
        private set

    // ----- Pantalla de registro -----
    var cargandoRegistro by mutableStateOf(false)
        private set
    var errorRegistro by mutableStateOf<String?>(null)
        private set
    var mostrandoRegistro by mutableStateOf(false)
        private set
//agrego esto jaz
    var pantallaActual by mutableStateOf("inicio")
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
    //agrego esto jaz
    fun cambiarPantalla(nuevaPantalla: String) {
        pantallaActual = nuevaPantalla
    }
    private fun restaurarSesion() {
        viewModelScope.launch {
            val guardada = sessionStore.leerSesion()
            if (guardada != null) {
                ApiClient.token = guardada.access_token
                sesion = guardada
                guardada.hogares.firstOrNull()?.let { hogar ->
                    sessionStore.leerInventario(hogar.id_hogar)?.let { cache ->
                        inventario = cache
                        inventarioCargado = true
                    }
                }
                try {
                    traerInventario()
                } catch (e: Exception) {
                    if (esNoAutorizado(e)) {
                        limpiarSesionLocal()
                    } else if (!esSinRed(e)) {
                        error = mensaje(e)
                    }
                }
            }
            restaurandoSesion = false
        }
    }

    private suspend fun limpiarSesionLocal() {
        val hogarId = sesion?.hogares?.firstOrNull()?.id_hogar
        sessionStore.limpiarSesion()
        if (hogarId != null) sessionStore.limpiarInventario(hogarId)
        ApiClient.token = null
        sesion = null
        inventario = emptyList()
        estadisticas = null
        plan = null
        invitacion = null
        errorAjustes = null
        avisoAjustes = null
        inventarioCargado = false
        sinConexion = false
        error = null
    }

    private fun esSinRed(e: Exception) = e is IOException
    private fun esNoAutorizado(e: Exception) = e is HttpException && e.code() == 401

    fun login(email: String, password: String) {
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                val r = ApiClient.api.login(LoginRequest(email, password))
                ApiClient.token = r.access_token
                sesion = r
                sessionStore.guardarSesion(r)
                traerInventario()
            } catch (e: Exception) {
                error = mensaje(e)
            }
            cargando = false
        }
    }

    // ----- Registro -----
    fun abrirRegistro() {
        errorRegistro = null
        mostrandoRegistro = true
    }

    fun cerrarRegistro() {
        mostrandoRegistro = false
        errorRegistro = null
    }

    fun registrar(
        nombre: String,
        email: String,
        password: String,
        codigoInvitacion: String?
    ) {
        viewModelScope.launch {
            cargandoRegistro = true
            errorRegistro = null
            try {
                val r = ApiClient.api.registrar(
                    RegistroRequest(
                        nombre = nombre,
                        email = email,
                        password = password,
                        codigo_invitacion = codigoInvitacion?.takeIf { it.isNotBlank() }
                    )
                )
                ApiClient.token = r.access_token
                val nueva = LoginResponse(
                    access_token = r.access_token,
                    usuario = UsuarioDto(r.id_usuario, nombre, email, r.plan),
                    hogares = listOf(HogarDto(r.id_hogar, r.rol))
                )
                sesion = nueva
                sessionStore.guardarSesion(nueva)
                mostrandoRegistro = false
                traerInventario()
            } catch (e: Exception) {
                errorRegistro = mensaje(e)
            }
            cargandoRegistro = false
        }
    }

    fun recargar() {
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                traerInventario()
            } catch (e: Exception) {
                if (!esSinRed(e)) error = mensaje(e)
            }
            cargando = false
        }
    }

    fun salir() {
        val hogarId = sesion?.hogares?.firstOrNull()?.id_hogar
        ApiClient.token = null
        sesion = null
        inventario = emptyList()
        estadisticas = null
        plan = null
        invitacion = null
        errorAjustes = null
        avisoAjustes = null
        inventarioCargado = false
        sinConexion = false
        catalogo = null
        mostrandoAgregar = false
        error = null
        errorAgregar = null
        mostrandoRegistro = false
        errorRegistro = null
        viewModelScope.launch {
            sessionStore.limpiarSesion()
            if (hogarId != null) sessionStore.limpiarInventario(hogarId)
        }
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
    // ----- Estadísticas (pantalla Inicio) -----
    var estadisticas by mutableStateOf<EstadisticasResponse?>(null)
        private set
    var cargandoEstadisticas by mutableStateOf(false)
        private set
    var errorEstadisticas by mutableStateOf<String?>(null)
        private set

    fun cargarEstadisticas() {
        viewModelScope.launch {
            val hogar = sesion?.hogares?.firstOrNull() ?: return@launch
            cargandoEstadisticas = true
            errorEstadisticas = null
            try {
                estadisticas = ApiClient.cuentaApi.estadisticas(hogar.id_hogar)
            } catch (e: Exception) {
                errorEstadisticas = if (e is IOException)
                    "Sin conexión: no se pudieron actualizar las estadísticas"
                else mensaje(e)
            }
            cargandoEstadisticas = false
        }
    }
    // ----- Ajustes: plan, invitaciones, unirse -----
    var plan by mutableStateOf<PlanResponse?>(null)
        private set
    var cargandoPlan by mutableStateOf(false)
        private set
    var errorAjustes by mutableStateOf<String?>(null)
        private set
    var avisoAjustes by mutableStateOf<String?>(null)
        private set
    var invitacion by mutableStateOf<InvitacionResponse?>(null)
        private set

    fun cargarPlan() {
        viewModelScope.launch {
            cargandoPlan = true
            errorAjustes = null
            try {
                plan = ApiClient.cuentaApi.plan()
            } catch (e: Exception) {
                errorAjustes = mensaje(e)
            }
            cargandoPlan = false
        }
    }

    fun generarInvitacion() {
        viewModelScope.launch {
            errorAjustes = null
            avisoAjustes = null
            try {
                val hogar = sesion?.hogares?.firstOrNull() ?: return@launch
                invitacion = ApiClient.cuentaApi.crearInvitacion(hogar.id_hogar)
            } catch (e: Exception) {
                errorAjustes = mensaje(e)
            }
        }
    }

    fun unirseAHogar(codigo: String) {
        viewModelScope.launch {
            errorAjustes = null
            avisoAjustes = null
            try {
                val r = ApiClient.cuentaApi.unirse(UnirseRequest(codigo.trim().uppercase()))
                val actual = sesion
                if (actual != null && actual.hogares.none { it.id_hogar == r.id_hogar }) {
                    val nueva = actual.copy(hogares = actual.hogares + HogarDto(r.id_hogar, "miembro"))
                    sesion = nueva
                    sessionStore.guardarSesion(nueva)
                }
                avisoAjustes = "Te uniste al hogar #${r.id_hogar}. " +
                        "La app seguirá mostrando tu primer hogar hasta que agreguemos el selector de hogar."
                cargarPlan()
            } catch (e: Exception) {
                errorAjustes = mensaje(e)
            }
        }
    }

    fun mejorarAPremium() {
        viewModelScope.launch {
            errorAjustes = null
            avisoAjustes = null
            try {
                val r = ApiClient.cuentaApi.mejorarAPremium()
                val actual = sesion
                if (actual != null) {
                    val nueva = actual.copy(usuario = actual.usuario.copy(plan = r.plan))
                    sesion = nueva
                    sessionStore.guardarSesion(nueva)
                }
                avisoAjustes = r.mensaje
                cargarPlan()
            } catch (e: Exception) {
                errorAjustes = mensaje(e)
            }
        }
    }

    fun limpiarAvisosAjustes() {
        errorAjustes = null
        avisoAjustes = null
    }
    // ----- Consumir / desechar -----
    fun cambiarEstado(idItem: Int, estado: String) {
        viewModelScope.launch {
            error = null
            try {
                val hogar = sesion?.hogares?.firstOrNull()
                    ?: throw IllegalStateException("Tu usuario no pertenece a ningún hogar")
                ApiClient.api.actualizarItem(
                    hogar.id_hogar, idItem, ActualizarItemRequest(estado)
                )
                inventario = inventario.filter { it.id_item != idItem }
                sessionStore.guardarInventario(hogar.id_hogar, inventario)
            } catch (e: Exception) {
                error = mensaje(e)
            }
        }
    }

    private suspend fun traerInventario() {
        val hogar = sesion?.hogares?.firstOrNull()
            ?: throw IllegalStateException("Tu usuario no pertenece a ningún hogar")
        try {
            val items = ApiClient.api.inventario(hogar.id_hogar).inventario
            inventario = items
            inventarioCargado = true
            sinConexion = false
            sessionStore.guardarInventario(hogar.id_hogar, items)
        } catch (e: IOException) {
            val cache = sessionStore.leerInventario(hogar.id_hogar)
            if (cache != null) {
                inventario = cache
                inventarioCargado = true
            }
            sinConexion = true
            throw e
        }
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