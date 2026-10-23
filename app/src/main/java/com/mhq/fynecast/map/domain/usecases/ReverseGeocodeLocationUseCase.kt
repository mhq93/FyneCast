package com.mhq.fynecast.map.domain.usecases

import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import com.mhq.fynecast.core.domain.location.LocationRepository

class ReverseGeocodeLocationUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double,
        lang: String
    ): CitySuggestionModel? {
        return locationRepository.getCityNameOffMap(latitude, longitude, lang)
    }
}