package com.example.network

import com.example.data.WikipediaGeoSearchResponse
import com.example.data.WikipediaPageDetailsResponse
import com.example.data.WikipediaMonument
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
 * Interface declarativa Retrofit para comunicação com a API REST oficial da Wikipédia Global.
 */
interface WikipediaApi {
    @GET("w/api.php")
    suspend fun getNearbyPlaces(
        @Query("action") action: String = "query",
        @Query("list") list: String = "geosearch",
        @Query("gscoord") coordinates: String, // format: "lat|lon"
        @Query("gsradius") radius: Int = 1000,
        @Query("gslimit") limit: Int = 10,
        @Query("format") format: String = "json",
        @Query("formatversion") formatversion: Int = 2
    ): WikipediaGeoSearchResponse

    @GET("w/api.php")
    suspend fun getPageDetails(
        @Query("action") action: String = "query",
        @Query("prop") prop: String = "extracts|pageimages",
        @Query("exintro") exintro: Int = 1,
        @Query("explaintext") explaintext: Int = 1,
        @Query("piprop") piprop: String = "thumbnail",
        @Query("pithumbsize") thumbnailSize: Int = 400,
        @Query("pageids") pageIds: String,
        @Query("format") format: String = "json",
        @Query("formatversion") formatversion: Int = 2
    ): WikipediaPageDetailsResponse
}

/**
 * Cliente WikipediaService com tratamento imperativo e de alta tolerância a falhas na rede.
 */
class WikipediaService {
    private val api: WikipediaApi

    init {
        // Interceptador para logs operacionais detalhados no logcat de desenvolvimento
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // OkHttpClient com timeouts configurados para operação móvel instável
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://pt.wikipedia.org/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(WikipediaApi::class.java)
    }

    /**
     * Busca monumentos históricos e geográficos num raio delimitado ao redor das coordenadas.
     * Realiza a mesclagem das informações de listagem e detalhes de forma integrada e segura.
     */
    suspend fun fetchNearbyCulturalGuide(latitude: Double, longitude: Double, radius: Int = 1000): List<WikipediaMonument> {
        val coordString = "$latitude|$longitude"
        
        try {
            // Passo 1: Descobrir monumentos na proximidade através de GeoSearch
            val geoResponse = api.getNearbyPlaces(coordinates = coordString, radius = radius)
            val searchResults = geoResponse.query?.geosearch ?: emptyList()
            
            if (searchResults.isEmpty()) {
                return emptyList()
            }

            // Passo 2: Buscar detalhes enriquecidos (resumos de texto e imagens) para os IDs localizados
            val pageIdsString = searchResults.map { it.pageid }.joinToString("|")
            val detailsResponse = api.getPageDetails(pageIds = pageIdsString)
            val pages = detailsResponse.query?.pages ?: emptyList()

            // Passo 3: Mesclar os dados estruturando na nossa classe combinada de alta fidelidade
            return searchResults.mapNotNull { result ->
                val matchingPage = pages.find { it.pageid == result.pageid }
                if (matchingPage != null) {
                    WikipediaMonument(
                        id = result.pageid,
                        title = result.title,
                        summary = matchingPage.extract ?: "Pontos culturais empolgantes aguardam por você neste local histórico da Wikipédia.",
                        latitude = result.lat,
                        longitude = result.lon,
                        imageUrl = matchingPage.thumbnail?.source
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            // Emissão segura e propagação tratada para controle do estado do ViewModel
            throw WikipediaNetworkException("Falha na busca ou mapeamento da API Wikipédia: ${e.localizedMessage}", e)
        }
    }
}

/**
 * Exceção personalizada para capturas de rede isoladas e resilientes nos blocos de UI.
 */
class WikipediaNetworkException(message: String, cause: Throwable) : Exception(message, cause)
