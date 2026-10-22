package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator

class ValidateLoginFormUseCase {
    fun validateEmailFormat(email: String): Boolean {
        return EmailFormatValidator.isValidOrEmpty(email)
    }

    operator fun invoke(email: String, password: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Failure(ValidationError.EmailEmpty)
            !EmailFormatValidator.isValid(email) -> ValidationResult.Failure(ValidationError.EmailInvalid)
            password.isBlank() -> ValidationResult.Failure(ValidationError.PasswordEmpty)
            else -> ValidationResult.Success
        }
    }
}