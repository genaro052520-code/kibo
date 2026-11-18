package com.genaro.kibo.data


import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
interface ApiService {
    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("api/hogares/{id}/inventario")
    suspend fun inventario(@Path("id") idHogar: Int): InventarioResponse
}

object ApiClient {
    // 10.0.2.2 = tu PC visto desde el emulador de Android.
    // Con celular físico, cámbialo por http://IP_DE_TU_PC:8000/
    const val BASE_URL = "http://192.168.0.31:8000/"

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

    val api: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}