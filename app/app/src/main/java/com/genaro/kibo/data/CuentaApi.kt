package com.genaro.kibo.data
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CuentaApi {
    @GET("api/hogares/{id}/estadisticas")
    suspend fun estadisticas(@Path("id") idHogar: Int): EstadisticasResponse

    @GET("api/plan")
    suspend fun plan(): PlanResponse

    @POST("api/plan/premium")
    suspend fun mejorarAPremium(): MensajePlanResponse

    @POST("api/hogares/{id}/invitaciones")
    suspend fun crearInvitacion(@Path("id") idHogar: Int): InvitacionResponse

    @POST("api/hogares/unirse")
    suspend fun unirse(@Body body: UnirseRequest): UnirseResponse
}