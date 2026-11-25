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

data class RegistroRequest(
    val nombre: String,
    val email: String,
    val password: String,
    val codigo_invitacion: String? = null
)

data class RegistroResponse(
    val mensaje: String,
    val id_usuario: Int,
    val id_hogar: Int,
    val rol: String,
    val plan: String,
    val access_token: String
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
data class CategoriaDto(val id_categoria: Int, val nombre: String)

data class UnidadDto(val id_unidad: Int, val nombre: String, val abreviatura: String)

data class ProductoDto(
    val id_producto: Int,
    val nombre: String,
    val id_categoria: Int,
    val categoria: String,
    val dias_vida_util: Int,
    val personalizado: Boolean,
    val id_hogar: Int?
)

data class CatalogoResponse(
    val categorias: List<CategoriaDto>,
    val unidades: List<UnidadDto>,
    val productos: List<ProductoDto>
)

data class NuevoItemRequest(val id_producto: Int, val id_unidad: Int, val cantidad: Double)

data class NuevoItemResponse(
    val mensaje: String,
    val id_item: Int,
    val producto: String,
    val fecha_caducidad: String
)
data class ActualizarItemRequest(val estado: String)

data class ActualizarItemResponse(
    val mensaje: String,
    val id_item: Int,
    val estado: String,
    val cantidad: Double,
    val fecha_caducidad: String
)
data class EditarItemRequest(val cantidad: Double, val fecha_caducidad: String)

data class EliminarItemResponse(val mensaje: String, val id_item: Int)