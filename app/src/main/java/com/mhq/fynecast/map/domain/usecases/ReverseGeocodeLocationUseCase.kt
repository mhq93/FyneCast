package com.mhq.fynecast.map.domain.usecases

import com.mhq.fynecast.home.domain.models.CitySuggestionModel
import com.mhq.fynecast.home.domain.repository.LocationRepository

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