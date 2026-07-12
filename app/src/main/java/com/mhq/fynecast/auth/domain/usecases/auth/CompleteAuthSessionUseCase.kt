package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository

class CompleteAuthSessionUseCase(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(result: Result<UserDomainModel>): Result<UserDomainModel> =
        result.onSuccess { user ->
            authRepository.saveLocalSession(user)
            userProfileRepository.refreshActiveUserSession()
        }
}