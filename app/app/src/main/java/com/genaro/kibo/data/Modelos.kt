package com.genaro.kibo.data

data class LoginRequest(val email: String, val password: String)

data class UsuarioDto(
    val id_usuario: Int,
    val nombre: String,
    val email: String,
    val plan: String
)

data class HogarDto(val id_hogar: Int, val rol: String)

data class LoginResponse(
    val access_token: String,
    val usuario: UsuarioDto,
    val hogares: List<HogarDto>
)

data class ItemDto(
    val id_item: Int,
    val producto: String,
    val categoria: String,
    val cantidad: Double,
    val unidad: String,
    val fecha_ingreso: String,
    val fecha_caducidad: String,
    val dias_restantes: Int
)

data class InventarioResponse(
    val id_hogar: Int,
    val total: Int,
    val inventario: List<ItemDto>
)