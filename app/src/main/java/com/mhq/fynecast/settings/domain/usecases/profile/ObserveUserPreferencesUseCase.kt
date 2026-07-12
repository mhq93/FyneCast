package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.models.UserPreferences
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserPreferencesUseCase(private val preferencesRepository: UserPreferencesRepository) {
    operator fun invoke(): Flow<UserPreferences> = preferencesRepository.userPreferencesFlow
}