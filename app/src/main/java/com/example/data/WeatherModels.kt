package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Estados reativos para guiar a busca de dados climáticos da Open-Meteo API.
 */
sealed interface WeatherState {
    object Idle : WeatherState
    object Loading : WeatherState
    data class Success(val temperature: Double, val weatherCode: Int) : WeatherState
    data class Error(val message: String) : WeatherState
}

/**
 * Resposta raiz da Open-Meteo API para a consulta de clima atual.
 */
@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "current_weather") val currentWeather: CurrentWeather?
)

/**
 * Detalhes do clima atual retornados pela Open-Meteo API.
 */
@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "temperature") val temperature: Double,
    @Json(name = "weathercode") val weatherCode: Int,
    @Json(name = "is_day") val isDay: Int?,
    @Json(name = "windspeed") val windSpeed: Double?,
    @Json(name = "winddirection") val windDirection: Double?
)
