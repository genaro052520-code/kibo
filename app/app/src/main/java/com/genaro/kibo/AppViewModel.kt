package com.genaro.kibo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.genaro.kibo.data.ActualizarItemRequest
import com.genaro.kibo.data.ApiClient
import com.genaro.kibo.data.CatalogoResponse
import com.genaro.kibo.data.EditarItemRequest
import com.genaro.kibo.data.EstadisticasResponse
import com.genaro.kibo.data.HogarDto
import com.genaro.kibo.data.InvitacionResponse
import com.genaro.kibo.data.ItemDto
import com.genaro.kibo.data.LoginRequest
import com.genaro.kibo.data.LoginResponse
import com.genaro.kibo.data.NuevoItemRequest
import com.genaro.kibo.data.PlanResponse
import com.genaro.kibo.data.RegistroRequest
import com.genaro.kibo.data.SessionStore
import com.genaro.kibo.data.UnirseRequest
import com.genaro.kibo.data.UsuarioDto
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val sessionStore = SessionStore(app)

    // ===== Sesión e inventario =====
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

    // ===== Hogar activo =====
    var hogarActivoId by mutableStateOf<Int?>(null)
        private set

    /** El hogar que se está viendo. Si no hay uno elegido (o ya no existe), el primero. */
    val hogarActivo: HogarDto?
        get() {
            val lista = sesion?.hogares ?: return null
            return lista.firstOrNull { it.id_hogar == hogarActivoId } ?: lista.firstOrNull()
        }

    // ===== Registro =====
    var cargandoRegistro by mutableStateOf(false)
        private set
    var errorRegistro by mutableStateOf<String?>(null)
        private set
    var mostrandoRegistro by mutableStateOf(false)
        private set

    // ===== Navegación =====
    var pantallaActual by mutableStateOf("inicio")
        private set

    // ===== Agregar alimento =====
    var catalogo by mutableStateOf<CatalogoResponse?>(null)
        private set
    var mostrandoAgregar by mutableStateOf(false)
        private set
    var guardando by mutableStateOf(false)
        private set
    var errorAgregar by mutableStateOf<String?>(null)
        private set

    // ===== Estadísticas (Inicio) =====
    var estadisticas by mutableStateOf<EstadisticasResponse?>(null)
        private set
    var cargandoEstadisticas by mutableStateOf(false)
        private set
    var errorEstadisticas by mutableStateOf<String?>(null)
        private set

    // ===== Ajustes: plan, invitaciones, unirse =====
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

    init {
        restaurarSesion()
    }

    fun cambiarPantalla(nuevaPantalla: String) {
        pantallaActual = nuevaPantalla
    }

    // ---------------------------------------------------------------
    // Sesión
    // ---------------------------------------------------------------
    private fun restaurarSesion() {
        viewModelScope.launch {
            val guardada = sessionStore.leerSesion()
            if (guardada != null) {
                ApiClient.token = guardada.access_token
                sesion = guardada
                hogarActivoId = sessionStore.leerHogarActivo()
                hogarActivo?.let { hogar ->
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

    private fun reiniciarEstado() {
        ApiClient.token = null
        sesion = null
        hogarActivoId = null
        inventario = emptyList()
        inventarioCargado = false
        sinConexion = false
        estadisticas = null
        errorEstadisticas = null
        plan = null
        invitacion = null
        errorAjustes = null
        avisoAjustes = null
        catalogo = null
        mostrandoAgregar = false
        error = null
        errorAgregar = null
        mostrandoRegistro = false
        errorRegistro = null
    }

    private suspend fun limpiarSesionLocal() {
        val hogarId = hogarActivo?.id_hogar
        reiniciarEstado()
        sessionStore.limpiarSesion()
        sessionStore.limpiarHogarActivo()
        if (hogarId != null) sessionStore.limpiarInventario(hogarId)
    }

    fun salir() {
        val hogarId = hogarActivo?.id_hogar
        reiniciarEstado()
        viewModelScope.launch {
            sessionStore.limpiarSesion()
            sessionStore.limpiarHogarActivo()
            if (hogarId != null) sessionStore.limpiarInventario(hogarId)
        }
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
                hogarActivoId = r.hogares.firstOrNull()?.id_hogar
                sessionStore.guardarSesion(r)
                hogarActivoId?.let { sessionStore.guardarHogarActivo(it) }
                traerInventario()
            } catch (e: Exception) {
                error = mensaje(e)
            }
            cargando = false
        }
    }

    // ---------------------------------------------------------------
    // Registro
    // ---------------------------------------------------------------
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
                hogarActivoId = r.id_hogar
                sessionStore.guardarSesion(nueva)
                sessionStore.guardarHogarActivo(r.id_hogar)
                mostrandoRegistro = false
                traerInventario()
            } catch (e: Exception) {
                errorRegistro = mensaje(e)
            }
            cargandoRegistro = false
        }
    }

    // ---------------------------------------------------------------
    // Cambiar de hogar
    // ---------------------------------------------------------------
    fun cambiarHogar(idHogar: Int) {
        val existe = sesion?.hogares?.any { it.id_hogar == idHogar } == true
        if (!existe || idHogar == hogarActivo?.id_hogar) return

        hogarActivoId = idHogar
        // Limpia lo del hogar anterior para no mostrar datos de otro hogar
        inventario = emptyList()
        inventarioCargado = false
        sinConexion = false
        estadisticas = null
        errorEstadisticas = null
        plan = null
        invitacion = null
        catalogo = null
        error = null
        errorAjustes = null
        avisoAjustes = null

        viewModelScope.launch {
            sessionStore.guardarHogarActivo(idHogar)
            sessionStore.leerInventario(idHogar)?.let { cache ->
                if (hogarActivo?.id_hogar == idHogar) {
                    inventario = cache
                    inventarioCargado = true
                }
            }
            recargar()
        }
    }

    // ---------------------------------------------------------------
    // Inventario
    // ---------------------------------------------------------------
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

    private suspend fun traerInventario() {
        val hogar = hogarActivo
            ?: throw IllegalStateException("Tu usuario no pertenece a ningún hogar")
        try {
            val items = ApiClient.api.inventario(hogar.id_hogar).inventario
            sessionStore.guardarInventario(hogar.id_hogar, items)
            // Si mientras tanto cambió de hogar, esta respuesta ya no aplica
            if (hogarActivo?.id_hogar == hogar.id_hogar) {
                inventario = items
                inventarioCargado = true
                sinConexion = false
            }
        } catch (e: IOException) {
            if (hogarActivo?.id_hogar == hogar.id_hogar) {
                val cache = sessionStore.leerInventario(hogar.id_hogar)
                if (cache != null) {
                    inventario = cache
                    inventarioCargado = true
                }
                sinConexion = true
            }
            throw e
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
                val hogar = hogarActivo ?: return@launch
                val resultado = ApiClient.api.catalogo(hogar.id_hogar)
                if (hogarActivo?.id_hogar == hogar.id_hogar) catalogo = resultado
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
                val hogar = hogarActivo
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

    // ----- Consumir / desechar -----
    fun cambiarEstado(idItem: Int, estado: String) {
        viewModelScope.launch {
            error = null
            try {
                val hogar = hogarActivo
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

    // ----- Editar / eliminar -----
    fun editarItem(idItem: Int, cantidad: Double, fechaCaducidad: String) {
        viewModelScope.launch {
            error = null
            try {
                val hogar = hogarActivo
                    ?: throw IllegalStateException("Tu usuario no pertenece a ningún hogar")
                ApiClient.api.editarItem(
                    hogar.id_hogar, idItem, EditarItemRequest(cantidad, fechaCaducidad)
                )
                recargar()
            } catch (e: Exception) {
                error = mensaje(e)
            }
        }
    }

    fun eliminarItem(idItem: Int) {
        viewModelScope.launch {
            error = null
            try {
                val hogar = hogarActivo
                    ?: throw IllegalStateException("Tu usuario no pertenece a ningún hogar")
                ApiClient.api.eliminarItem(hogar.id_hogar, idItem)
                inventario = inventario.filter { it.id_item != idItem }
                sessionStore.guardarInventario(hogar.id_hogar, inventario)
            } catch (e: Exception) {
                error = mensaje(e)
            }
        }
    }

    // ---------------------------------------------------------------
    // Estadísticas
    // ---------------------------------------------------------------
    fun cargarEstadisticas() {
        viewModelScope.launch {
            val hogar = hogarActivo ?: return@launch
            cargandoEstadisticas = true
            errorEstadisticas = null
            try {
                val resultado = ApiClient.cuentaApi.estadisticas(hogar.id_hogar)
                if (hogarActivo?.id_hogar == hogar.id_hogar) estadisticas = resultado
            } catch (e: Exception) {
                errorEstadisticas = if (e is IOException)
                    "Sin conexión: no se pudieron actualizar las estadísticas"
                else mensaje(e)
            }
            cargandoEstadisticas = false
        }
    }

    // ---------------------------------------------------------------
    // Ajustes
    // ---------------------------------------------------------------
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
                val hogar = hogarActivo ?: return@launch
                val resultado = ApiClient.cuentaApi.crearInvitacion(hogar.id_hogar)
                if (hogarActivo?.id_hogar == hogar.id_hogar) invitacion = resultado
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
                    val nueva = actual.copy(
                        hogares = actual.hogares + HogarDto(r.id_hogar, "miembro")
                    )
                    sesion = nueva
                    sessionStore.guardarSesion(nueva)
                }
                // Pasa a ver el hogar al que se acaba de unir
                cambiarHogar(r.id_hogar)
                avisoAjustes = "Te uniste al hogar #${r.id_hogar} y ahora lo estás viendo. " +
                        "Puedes cambiar de hogar con el selector 🏠."
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

    // ---------------------------------------------------------------
    // Mensajes de error
    // ---------------------------------------------------------------
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