package com.example.network

import com.example.data.WeatherResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Interface declarativa Retrofit para comunicação com a API de clima Open-Meteo.
 */
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true
    ): WeatherResponse
}

/**
 * Cliente WeatherService com tratamento premium, parsing de dados climáticos e tolerância a falhas.
 */
class WeatherService {
    private val api: OpenMeteoApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(OpenMeteoApi::class.java)
    }

    /**
     * Consome a API Open-Meteo para obter a temperatura atual e o weathercode.
     */
    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherResponse {
        return api.getCurrentWeather(latitude, longitude)
    }
}
