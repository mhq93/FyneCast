package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class SetLanguageUseCase(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke(language: String) =
        preferencesRepository.updateLanguage(language)
}