package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository

class RegisterWithEmailUseCase(
    private val authRepository: AuthRepository,
    private val completeAuthSessionUseCase: CompleteAuthSessionUseCase
) {
    suspend operator fun invoke(
        username: String,
        email: String,
        password: String
    ): Result<UserDomainModel> {
        val result = authRepository.registerWithEmail(username, email, password)
        return completeAuthSessionUseCase(result)
    }
}