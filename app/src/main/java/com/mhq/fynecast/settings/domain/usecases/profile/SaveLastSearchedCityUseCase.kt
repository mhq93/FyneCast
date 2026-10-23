package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class SaveLastSearchedCityUseCase(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke(name: String, lat: Double, lon: Double) =
        preferencesRepository.updateLastSearchedCity(name, lat, lon)
}