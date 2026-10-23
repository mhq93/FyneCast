package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class SetNotificationsEnabledUseCase(private val preferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean) =
        preferencesRepository.updateNotifications(enabled)
}