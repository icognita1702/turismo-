package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Representa os tipos de Pontos de Interesse (POIs) suportados pela nossa aplicação de turismo.
 */
enum class PoiType {
    RESTAURANT,
    BAR,
    VIEWPOINT
}

/**
 * Resposta raiz da Overpass API para a consulta de interpreter.
 */
@JsonClass(generateAdapter = true)
data class OverpassResponse(
    @Json(name = "elements") val elements: List<OverpassElement>?
)

/**
 * Centro geográfico opcional para geometries complexas (como caminhos/Ways em OSM).
 */
@JsonClass(generateAdapter = true)
data class OverpassCenter(
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double
)

/**
 * No individual retornado pela Overpass API, podendo ser um ponto (node) ou caminho (way).
 */
@JsonClass(generateAdapter = true)
data class OverpassElement(
    @Json(name = "type") val type: String,
    @Json(name = "id") val id: Long,
    @Json(name = "lat") val lat: Double?,
    @Json(name = "lon") val lon: Double?,
    @Json(name = "center") val center: OverpassCenter?,
    @Json(name = "tags") val tags: Map<String, String>?
) {
    /**
     * Resolve a latitude de forma flexível priorizando o ponto exato ou o centro geométrico.
     */
    val resolvedLatitude: Double?
        get() = lat ?: center?.lat

    /**
     * Resolve a longitude de forma flexível priorizando o ponto exato ou o centro geométrico.
     */
    val resolvedLongitude: Double?
        get() = lon ?: center?.lon

    /**
     * Extrai o nome amigável do monumento/estabelecimento com fallbacks adequados.
     */
    val resolvedName: String
        get() = tags?.get("name") ?: tags?.get("brand") ?: when (resolvedType) {
            PoiType.RESTAURANT -> "Restaurante Sem Nome"
            PoiType.BAR -> "Bar Local"
            PoiType.VIEWPOINT -> "Ponto Turístico Observatório"
        }

    /**
     * Descobre o tipo do elemento de interesse baseado em suas tags OSM estruturadas.
     */
    val resolvedType: PoiType
        get() {
            val amenity = tags?.get("amenity")
            val tourism = tags?.get("tourism")
            return when {
                amenity == "restaurant" -> PoiType.RESTAURANT
                amenity == "bar" || amenity == "pub" -> PoiType.BAR
                tourism == "viewpoint" -> PoiType.VIEWPOINT
                else -> PoiType.RESTAURANT
            }
        }
}

/**
 * Modelo representativo premium unificado e desacoplado exposto diretamente à UI.
 */
data class PointOfInterest(
    val id: Long,
    val name: String,
    val type: PoiType,
    val latitude: Double,
    val longitude: Double,
    val distance: Double
)
