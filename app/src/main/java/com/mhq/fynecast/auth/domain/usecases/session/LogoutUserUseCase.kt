package com.mhq.fynecast.auth.domain.usecases.session

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import com.mhq.fynecast.settings.domain.usecases.profile.ClearLastSearchedCityUseCase

class LogoutUserUseCase(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase,
    private val clearLastSearchedCityUseCase: ClearLastSearchedCityUseCase
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        authRepository.clearSession()
        userProfileRepository.refreshActiveUserSession()

        runCatching { cancelAllAlertTrackingUseCase() }
        runCatching { clearLastSearchedCityUseCase() }
    }
}