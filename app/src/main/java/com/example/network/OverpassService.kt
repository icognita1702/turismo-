package com.example.network

import android.location.Location
import com.example.data.OverpassResponse
import com.example.data.PointOfInterest
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
 * Interface declarativa Retrofit para comunicação com a API Overpass do OpenStreetMap.
 */
interface OverpassApi {
    @GET("interpreter")
    suspend fun getNearbyPoi(
        @Query("data") query: String
    ): OverpassResponse
}

/**
 * Cliente OverpassService com tratamento premium, parsing de nós/caminhos e tolerância a falhas.
 */
class OverpassService {
    private val api: OverpassApi

    init {
        // Interceptador para logs operacionais detalhados
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // OkHttpClient resiliente com timeouts estendidos para a Overpass API pública
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://overpass-api.de/api/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(OverpassApi::class.java)
    }

    /**
     * Constrói a consulta estruturada Overpass QL usando as coordenadas e raio especificados.
     */
    private fun buildQuery(latitude: Double, longitude: Double, radius: Int): String {
        return """
            [out:json][timeout:25];
            (
              node["amenity"="restaurant"](around:$radius,$latitude,$longitude);
              node["amenity"="bar"](around:$radius,$latitude,$longitude);
              node["amenity"="pub"](around:$radius,$latitude,$longitude);
              node["tourism"="viewpoint"](around:$radius,$latitude,$longitude);
              way["amenity"="restaurant"](around:$radius,$latitude,$longitude);
              way["amenity"="bar"](around:$radius,$latitude,$longitude);
              way["amenity"="pub"](around:$radius,$latitude,$longitude);
              way["tourism"="viewpoint"](around:$radius,$latitude,$longitude);
            );
            out center;
        """.trimIndent()
    }

    /**
     * Consome a API Overpass, decodifica os elementos e os estrutura calculando a distância.
     */
    suspend fun fetchNearbyPoi(
        latitude: Double,
        longitude: Double,
        radius: Int = 1000
    ): List<PointOfInterest> {
        val qlExpression = buildQuery(latitude, longitude, radius)
        
        try {
            val response = api.getNearbyPoi(qlExpression)
            val elements = response.elements ?: emptyList()
            
            return elements.mapNotNull { element ->
                val poiLat = element.resolvedLatitude
                val poiLon = element.resolvedLongitude
                
                if (poiLat != null && poiLon != null) {
                    // Calcula a distância exata em metros entre o usuário e o POI
                    val distanceResults = FloatArray(1)
                    try {
                        Location.distanceBetween(latitude, longitude, poiLat, poiLon, distanceResults)
                    } catch (e: Exception) {
                        distanceResults[0] = 0f
                    }
                    
                    PointOfInterest(
                        id = element.id,
                        name = element.resolvedName,
                        type = element.resolvedType,
                        latitude = poiLat,
                        longitude = poiLon,
                        distance = distanceResults[0].toDouble()
                    )
                } else {
                    null
                }
            }.sortedBy { it.distance } // Ordena pelo local mais próximo
        } catch (e: Exception) {
            throw OverpassNetworkException("Falha ao comunicar com a Overpass API do OpenStreetMap: ${e.localizedMessage}", e)
        }
    }
}

/**
 * Exceção customizada e resiliente para falhas na conexão OpenStreetMap.
 */
class OverpassNetworkException(message: String, cause: Throwable) : Exception(message, cause)
