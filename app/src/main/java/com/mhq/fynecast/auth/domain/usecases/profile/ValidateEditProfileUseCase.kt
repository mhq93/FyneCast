package com.mhq.fynecast.auth.domain.usecases.profile

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
            username.isBlank() -> ValidationResult.Failure("Name field cannot be left empty.")
            !username.matches(usernameRegex) -> ValidationResult.Failure("Username must be 3-30 characters (spaces/letters/numbers allowed).")
            email.isBlank() -> ValidationResult.Failure("Email field cannot be left empty.")
            !EmailFormatValidator.isValid(email) -> ValidationResult.Failure("Please enter a valid email address.")
            else -> ValidationResult.Success
        }
    }
}