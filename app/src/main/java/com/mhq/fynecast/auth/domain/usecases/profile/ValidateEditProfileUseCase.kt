package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator

class ValidateEditProfileUseCase {
    private val usernameRegex = Regex("^[a-zA-Z0-9_ -]{3,30}$")

    fun validateUsernameFormat(name: String): Boolean {
        return name.isEmpty() || name.matches(usernameRegex)
    }

    fun validateEmailFormat(email: String): Boolean {
        return EmailFormatValidator.isValidOrEmpty(email)
    }

    operator fun invoke(username: String, email: String): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Failure(ValidationError.NameEmpty)
            !username.matches(usernameRegex) -> ValidationResult.Failure(ValidationError.NameInvalidForProfile)
            email.isBlank() -> ValidationResult.Failure(ValidationError.EmailEmpty)
            !EmailFormatValidator.isValid(email) -> ValidationResult.Failure(ValidationError.EmailInvalid)
            else -> ValidationResult.Success
        }
    }
}