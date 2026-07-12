package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository

class AuthenticateWithSocialTokenUseCase(
    private val authRepository: AuthRepository,
    private val completeAuthSessionUseCase: CompleteAuthSessionUseCase
) {
    suspend operator fun invoke(token: String, isGoogle: Boolean): Result<UserDomainModel> {
        val result =
            if (isGoogle)
                authRepository.loginWithGoogle(token)
            else
                authRepository.loginWithFacebook(token)
        return completeAuthSessionUseCase(result)
    }
}