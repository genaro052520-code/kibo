package com.genaro.kibo.data

data class EstadisticasResponse(
    val id_hogar: Int,
    val disponibles: Int,
    val consumidos: Int,
    val desechados: Int,
    val por_vencer_3_dias: Int,
    val caducados_sin_desechar: Int,
    val porcentaje_aprovechado: Double?   // null si aún no hay historial
)

data class CupoDto(val usados: Int, val maximo: Int?)   // maximo null = ilimitado

data class HogarPlanDto(
    val id_hogar: Int,
    val rol: String,
    val plan_del_hogar: String,
    val miembros: CupoDto,
    val items: CupoDto
)

data class PlanResponse(
    val plan: String,
    val precio_mensual: Double,
    val hogares: List<HogarPlanDto>
)

data class MensajePlanResponse(val mensaje: String, val plan: String)

data class InvitacionResponse(val codigo: String, val expira: String, val mensaje: String)

data class UnirseRequest(val codigo: String)

data class UnirseResponse(val mensaje: String, val id_hogar: Int)