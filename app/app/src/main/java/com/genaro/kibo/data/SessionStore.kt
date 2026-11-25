package com.genaro.kibo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.intPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kibo_sesion")

class SessionStore(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val claveSesion = stringPreferencesKey("sesion_json")
    private val claveHogarActivo = intPreferencesKey("hogar_activo")

    private fun claveInventario(idHogar: Int) =
        stringPreferencesKey("inventario_${idHogar}_json")

    suspend fun leerSesion(): LoginResponse? = appContext.dataStore.data
        .map { it[claveSesion] }
        .first()
        ?.let { runCatching { gson.fromJson(it, LoginResponse::class.java) }.getOrNull() }

    suspend fun guardarSesion(sesion: LoginResponse) {
        appContext.dataStore.edit { it[claveSesion] = gson.toJson(sesion) }
    }

    suspend fun limpiarSesion() {
        appContext.dataStore.edit { it.remove(claveSesion) }
    }

    suspend fun leerInventario(idHogar: Int): List<ItemDto>? {
        val json = appContext.dataStore.data
            .map { it[claveInventario(idHogar)] }
            .first() ?: return null
        val tipo = object : TypeToken<List<ItemDto>>() {}.type
        return runCatching { gson.fromJson<List<ItemDto>>(json, tipo) }.getOrNull()
    }

    suspend fun guardarInventario(idHogar: Int, items: List<ItemDto>) {
        appContext.dataStore.edit { it[claveInventario(idHogar)] = gson.toJson(items) }
    }

    suspend fun limpiarInventario(idHogar: Int) {
        appContext.dataStore.edit { it.remove(claveInventario(idHogar)) }
    }
    suspend fun leerHogarActivo(): Int? = appContext.dataStore.data
        .map { it[claveHogarActivo] }
        .first()

    suspend fun guardarHogarActivo(idHogar: Int) {
        appContext.dataStore.edit { it[claveHogarActivo] = idHogar }
    }

    suspend fun limpiarHogarActivo() {
        appContext.dataStore.edit { it.remove(claveHogarActivo) }
    }
}
