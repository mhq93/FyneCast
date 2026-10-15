package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator

class ValidateLoginFormUseCase {
    fun validateEmailFormat(email: String): Boolean {
        return EmailFormatValidator.isValid(email)
    }

    operator fun invoke(email: String, password: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Failure("Email field cannot be empty.")
            !EmailFormatValidator.isValid(email) -> ValidationResult.Failure("Please, enter a valid email address.")
            password.isBlank() -> ValidationResult.Failure("Password field cannot be empty.")
            else -> ValidationResult.Success
        }
    }
}