package com.genaro.kibo.data

import com.genaro.kibo.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PATCH
import java.util.concurrent.TimeUnit
import retrofit2.http.Query
interface ApiService {
    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/usuarios")
    suspend fun registrar(@Body body: RegistroRequest): RegistroResponse

    @GET("api/hogares/{id}/inventario")
    suspend fun inventario(@Path("id") idHogar: Int): InventarioResponse

    @GET("api/catalogo")
    suspend fun catalogo(@Query("hogar") idHogar: Int): CatalogoResponse

    @POST("api/hogares/{id}/inventario")
    suspend fun agregarItem(
        @Path("id") idHogar: Int,
        @Body body: NuevoItemRequest
    ): NuevoItemResponse

    @PATCH("api/hogares/{id}/inventario/{idItem}")
    suspend fun actualizarItem(
        @Path("id") idHogar: Int,
        @Path("idItem") idItem: Int,
        @Body body: ActualizarItemRequest
    ): ActualizarItemResponse
}

object ApiClient {
    // 10.0.2.2 = tu PC visto desde el emulador de Android.
    // Con celular físico, cámbialo por http://IP_DE_TU_PC:8000/
    const val BASE_URL = BuildConfig.BASE_URL

    var token: String? = null

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val peticion = chain.request().newBuilder().apply {
                token?.let { addHeader("Authorization", "Bearer $it") }
            }.build()
            chain.proceed(peticion)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
    val cuentaApi: CuentaApi = retrofit.create(CuentaApi::class.java)
}