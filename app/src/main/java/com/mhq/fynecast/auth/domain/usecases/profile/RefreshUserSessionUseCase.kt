package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase

class RefreshUserSessionUseCase(
    private val profileRepository: UserProfileRepository,
    private val authRepository: AuthRepository,
    private val logoutUserUseCase: LogoutUserUseCase
) {
    /** @return false when the session had expired and the user was signed out. */
    suspend operator fun invoke(): Boolean {
        if (!profileRepository.refreshActiveUserSession()) {
            logoutUserUseCase()
            return false
        }
        authRepository.detachOutdatedSocialProviders()
        return true
    }
}