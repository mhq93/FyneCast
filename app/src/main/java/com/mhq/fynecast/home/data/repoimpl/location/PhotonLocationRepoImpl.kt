package com.mhq.fynecast.home.data.repoimpl.location

import android.util.Log
import com.mhq.fynecast.home.data.api.location.LocationApiService
import com.mhq.fynecast.home.domain.models.CitySuggestionModel
import com.mhq.fynecast.home.domain.repository.LocationRepository

class PhotonLocationRepoImpl(
    private val locationApiService: LocationApiService
) : LocationRepository {

    override suspend fun getCitySuggestions(query: String, lang: String): List<CitySuggestionModel> {
        return try {
            val response = locationApiService.getSuggestions(query = query, lang = lang)
            response.features.map { feature ->
                val props = feature.properties
                val nameParts = listOfNotNull(props.name, props.country).distinct().filter { it.isNotBlank() }
                val coordinates = feature.geometry.coordinates
                CitySuggestionModel(
                    fullName = nameParts.joinToString(", "),
                    latitude = coordinates.getOrNull(1) ?: 0.0,
                    longitude = coordinates.getOrNull(0) ?: 0.0
                )
            }.distinctBy { it.fullName }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getCityNameOffMap(lat: Double, lon: Double, lang: String): CitySuggestionModel? {
        return try {
            val response = locationApiService.reverseGeocode(lat = lat, lon = lon, lang = lang)

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
                !props.state.isNullOrBlank() -> props.state
                !props.city.isNullOrBlank() -> props.city
                !props.municipality.isNullOrBlank() -> props.municipality
                !props.town.isNullOrBlank() -> props.town
                !props.village.isNullOrBlank() -> props.village
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
            null
        }
    }
}