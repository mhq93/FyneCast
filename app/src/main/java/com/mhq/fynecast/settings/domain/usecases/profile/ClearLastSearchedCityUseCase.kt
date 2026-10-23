package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class ClearLastSearchedCityUseCase(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke() = preferencesRepository.clearLastSearchedCity()
}