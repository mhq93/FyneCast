package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class SetDarkModeUseCase(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean) =
        preferencesRepository.updateDarkMode(enabled)
}