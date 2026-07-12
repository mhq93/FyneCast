package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository

class SendPasswordResetUseCase(
    private val authRepository: AuthRepository,
    private val validateLoginFormUseCase: ValidateLoginFormUseCase
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be empty."))
        }

        if (!validateLoginFormUseCase.validateEmailFormat(email)) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }

        return authRepository.sendPasswordResetEmail(email)
    }
}