package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationException
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator

class SendPasswordResetUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(ValidationException(ValidationError.EmailEmpty))
        }
        if (!EmailFormatValidator.isValid(email)) {
            return Result.failure(ValidationException(ValidationError.EmailInvalid))
        }
        return authRepository.sendPasswordResetEmail(email)
    }
}