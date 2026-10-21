package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.home.domain.repository.LocationRepository

class LocalizePlaceNameUseCase(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(
        name: String,
        latitude: Double,
        longitude: Double,
        lang: String
    ): String? = locationRepository.getLocalizedPlaceName(
        name,
        latitude,
        longitude,
        lang
    )
}