package com.peterlaurence.trekme.core.geocoding.backend

import com.peterlaurence.trekme.core.geocoding.GeoPlace
import com.peterlaurence.trekme.core.geocoding.POI
import com.peterlaurence.trekme.util.performRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import com.peterlaurence.trekme.core.geocoding.City as GeoCity
import com.peterlaurence.trekme.core.geocoding.Street as GeoStreet

/**
 * A [GeocodingBackend] which uses Komoot's Photon.
 *
 * @author P.Laurence on 01/01/21
 */
class Photon(private val client: OkHttpClient) : GeocodingBackend {
    private val requestBuilder = Request.Builder()

    override suspend fun search(query: String): List<GeoPlace>? {
        val req = makeRequest(query)
        val resp = client.performRequest<PhotonMainResponse>(req)

        return resp?.let { convert(it) }
    }

    private fun makeRequest(query: String): Request {
        return requestBuilder.url("$photonApi?q=$query&limit=5").build()
    }

    private fun convert(response: PhotonMainResponse): List<GeoPlace>? {
        if (response.features.isEmpty()) return null
        return response.features.mapNotNull {
            if (it.geometry.coordinates.size == 2) {
                val lon = it.geometry.coordinates[0]
                val lat = it.geometry.coordinates[1]
                val geoType = when (it.properties.type) {
                    PhotonType.Street -> GeoStreet
                    PhotonType.City -> GeoCity
                    PhotonType.House -> POI
                }
                val name = it.properties.name
                val locality = it.properties.city
                GeoPlace(geoType, name, locality, lat, lon)
            } else null
        }
    }
}

private const val photonApi = "https://photon.komoot.io/api/"

@Serializable
private data class PhotonMainResponse(val features: List<PhotonFeature>)

@Serializable
private data class PhotonFeature(val geometry: PhotonGeometry, val properties: PhotonProperties)

@Serializable
private data class PhotonGeometry(val coordinates: List<Double>)

@Serializable
private data class PhotonProperties(val name: String, val city: String, val country: String, val state: String, val type: PhotonType)

@Serializable
private enum class PhotonType {
    @SerialName("street")
    Street,
    @SerialName("city")
    City,
    @SerialName("house")
    House
}
