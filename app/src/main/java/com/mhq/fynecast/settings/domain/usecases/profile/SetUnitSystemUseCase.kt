package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class SetUnitSystemUseCase(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke(isMetric: Boolean) =
        preferencesRepository.updateMetricSystem(isMetric)
}