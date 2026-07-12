package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository

class LoginWithEmailUseCase(
    private val authRepository: AuthRepository,
    private val completeAuthSessionUseCase: CompleteAuthSessionUseCase
) {
    suspend operator fun invoke(email: String, pass: String): Result<UserDomainModel> {
        val result = authRepository.loginWithEmail(email, pass)
        return completeAuthSessionUseCase(result)
    }
}