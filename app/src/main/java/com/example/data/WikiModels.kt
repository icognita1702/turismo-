package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Resposta raiz da busca geográfica na Wikipédia.
 */
@JsonClass(generateAdapter = true)
data class WikipediaGeoSearchResponse(
    @Json(name = "query") val query: GeoSearchQuery?
)

/**
 * Query contendo a lista resultante de monumentos próximos.
 */
@JsonClass(generateAdapter = true)
data class GeoSearchQuery(
    @Json(name = "geosearch") val geosearch: List<GeoSearchResult>?
)

/**
 * Detalhes reduzidos de geolocalização do monumento de retorno rápido.
 */
@JsonClass(generateAdapter = true)
data class GeoSearchResult(
    @Json(name = "pageid") val pageid: Long,
    @Json(name = "title") val title: String,
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double,
    @Json(name = "distance") val distance: Double
)

/**
 * Resposta raiz do detalhamento dos monumentos identificados de forma granular.
 */
@JsonClass(generateAdapter = true)
data class WikipediaPageDetailsResponse(
    @Json(name = "query") val query: PageDetailsQuery?
)

/**
 * Query agrupando páginas detalhadas solicitadas por seus ID exclusivos.
 */
@JsonClass(generateAdapter = true)
data class PageDetailsQuery(
    @Json(name = "pages") val pages: List<WikipediaPage>?
)

/**
 * Dados biográficos e visuais enriquecidos do monumento cultural.
 */
@JsonClass(generateAdapter = true)
data class WikipediaPage(
    @Json(name = "pageid") val pageid: Long,
    @Json(name = "title") val title: String,
    @Json(name = "extract") val extract: String?,
    @Json(name = "thumbnail") val thumbnail: WikipediaThumbnail?
)

/**
 * Referência de imagem adaptável do monumento guardada no Commons Wikipédia.
 */
@JsonClass(generateAdapter = true)
data class WikipediaThumbnail(
    @Json(name = "source") val source: String?,
    @Json(name = "width") val width: Int?,
    @Json(name = "height") val height: Int?
)

/**
 * Modelo Premium unificado e desacoplado exposto diretamente na interface do usuário.
 */
data class WikipediaMonument(
    val id: Long,
    val title: String,
    val summary: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String?
)
