package com.mhq.fynecast.auth.domain.usecases.session

import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository

class LogoutUserUseCase(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return runCatching {
            authRepository.clearSession()
            userProfileRepository.refreshActiveUserSession()
        }
    }
}