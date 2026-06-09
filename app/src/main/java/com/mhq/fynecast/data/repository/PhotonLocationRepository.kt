package com.mhq.fynecast.data.repository

import com.mhq.fynecast.domain.models.CitySuggestionModel
import com.mhq.fynecast.data.network.location.LocationApiService

class PhotonLocationRepository(
    private val locationApiService: LocationApiService
) : LocationRepository {

    override suspend fun getCitySuggestions(query: String): List<CitySuggestionModel> {
        return try {
            val response = locationApiService.getSuggestions(query)
            response.features.map { feature ->
                val cityProperties = feature.properties
                val nameParts = listOfNotNull(
                    cityProperties.name,
                    //cityProperties.state,
                    cityProperties.country
                ).distinct().filter { it.isNotBlank() }

                val fullName = nameParts.joinToString(", ")

                CitySuggestionModel(
                    fullName = fullName,
                    latitude = feature.geometry.coordinates[1],
                    longitude = feature.geometry.coordinates[0]
                )
            }.distinctBy { it.fullName }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCityNameOffMap(lat: Double, lon: Double): CitySuggestionModel? {
        return try {
            val response = locationApiService.reverseGeocode(lat, lon)
            val bestFeature =
                response.features.find { it.properties.osmValue == "city" || it.properties.osmValue == "town" }
                    ?: response.features.firstOrNull()

            val properties = bestFeature?.properties ?: return null

            CitySuggestionModel(
                fullName = listOfNotNull(properties.name, properties.country).distinct()
                    .joinToString(", "),
                latitude = lat,
                longitude = lon
            )
        } catch (e: Exception) {
            null
        }
    }

//    suspend fun getCityNameOffMap(lat: Double, lon: Double): CitySuggestion? {
//        return try {
//            val response = photonApiService.reverseGeocode(lat, lon)
//            val feature = response.features.firstOrNull() ?: return null
//            val properties = feature.properties
//            val nameParts = listOfNotNull(properties.name, properties.country).distinct()
//
//            CitySuggestion(
//                fullName = nameParts.joinToString(", "),
//                latitude = lat,
//                longitude = lon
//            )
//
//        } catch (e: Exception) {
//            null
//        }
//    }
}

//class PhotonLocationRepository(
//    private val photonApiService: PhotonApiService
//) : LocationRepository {
//
//    suspend fun getCityNameOffMap(lat: Double, lon: Double): CitySuggestion? {
//        return try {
//            val response = photonApiService.reverseGeocode(lat, lon)
//            val feature = response.features.firstOrNull() ?: return null
//            val props = feature.properties
//
//            // The "Catch-All" for City names
//            val cityName = props.city
//                ?: props.town
//                ?: props.village
//                ?: props.district
//                // If it's a 'place' (because of our filter), the name is the city name
//                ?: props.name
//
//            if (cityName.isNullOrBlank()) return null
//
//            CitySuggestion(
//                fullName = listOfNotNull(cityName, props.country).joinToString(", "),
//                latitude = lat,
//                longitude = lon
//            )
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//
//    override suspend fun getCitySuggestions(query: String): List<CitySuggestion> {
//        return try {
//            val response = photonApiService.getSuggestions(query)
//            response.features.mapNotNull { feature ->
//                val props = feature.properties
//
//                // If the search result is a street/POI, 'city' will be filled.
//                // If the search result is the city itself, 'name' will be the city.
//                val isActuallyACity = props.type == "city" || props.type == "town" || props.type == "village"
//
//                val finalCityName = if (isActuallyACity) {
//                    props.name
//                } else {
//                    props.city ?: props.town // Grab the parent city if user searched a street
//                }
//
//                if (finalCityName.isNullOrBlank()) return@mapNotNull null
//
//                CitySuggestion(
//                    fullName = listOfNotNull(finalCityName, props.country).joinToString(", "),
//                    latitude = feature.geometry.coordinates[1],
//                    longitude = feature.geometry.coordinates[0]
//                )
//            }.distinctBy { it.fullName } // Remove duplicates (e.g., searching "Cairo" might return multiple points in Cairo)
//        } catch (e: Exception) {
//            emptyList()
//        }
//    }
//}