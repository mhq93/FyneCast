package com.mhq.fynecast.home.data.repoimpl.location

import com.mhq.fynecast.home.data.api.location.LocationApiService
import com.mhq.fynecast.home.domain.models.CitySuggestionModel
import com.mhq.fynecast.home.domain.repository.LocationRepository
import kotlinx.coroutines.CancellationException

class PhotonLocationRepoImpl(
    private val locationApiService: LocationApiService,
    private val placeNameSource: PlaceNameSource
) : LocationRepository {

    override suspend fun getCitySuggestions(
        query: String,
        lang: String
    ): List<CitySuggestionModel> {
        return try {
            val response = locationApiService.getSuggestions(query = query, lang = photonLang(lang))
            response.features.map { feature ->
                val coordinates = feature.geometry.coordinates
                val props = feature.properties
                val nameParts = listOfNotNull(
                    props.name,
                    props.country
                ).distinct().filter { it.isNotBlank() }

                CitySuggestionModel(
                    fullName = nameParts.joinToString(", "),
                    latitude = coordinates.getOrNull(1) ?: 0.0,
                    longitude = coordinates.getOrNull(0) ?: 0.0
                )
            }.distinctBy { it.fullName }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    override suspend fun getCityNameOffMap(
        lat: Double,
        lon: Double,
        lang: String
    ): CitySuggestionModel? {
        placeNameSource.nameAt(lat, lon, lang)?.let { name ->
            return CitySuggestionModel(fullName = name, latitude = lat, longitude = lon)
        }
        return try {
            val response =
                locationApiService.reverseGeocode(lat = lat, lon = lon, lang = photonLang(lang))
            val bestFeature = response.features.firstOrNull { feature ->
                val props = feature.properties
                !props.state.isNullOrBlank() ||
                        !props.city.isNullOrBlank() ||
                        !props.town.isNullOrBlank() ||
                        !props.municipality.isNullOrBlank() ||
                        !props.village.isNullOrBlank()
            } ?: response.features.firstOrNull()
            val props = bestFeature?.properties ?: return null
            val cityName = when {
                !props.city.isNullOrBlank() -> props.city
                !props.town.isNullOrBlank() -> props.town
                !props.municipality.isNullOrBlank() -> props.municipality
                !props.village.isNullOrBlank() -> props.village
                !props.state.isNullOrBlank() -> props.state
                !props.county.isNullOrBlank() -> props.county
                else -> props.name ?: return null
            }
            val fullName = if (!props.country.isNullOrBlank() && cityName != props.country) {
                "$cityName, ${props.country}"
            } else {
                cityName
            }
            CitySuggestionModel(fullName = fullName, latitude = lat, longitude = lon)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    override suspend fun getLocalizedPlaceName(
        name: String,
        lat: Double,
        lon: Double,
        lang: String
    ): String? = placeNameSource.localize(name, lat, lon, lang)

    /** Photon's public server only knows English, German and French;
     * anything else falls back to English. */
    private fun photonLang(lang: String): String =
        if (lang in PHOTON_LANGUAGES)
            lang
        else
            "en"

    private companion object {
        val PHOTON_LANGUAGES = setOf("en", "de", "fr")
    }
}