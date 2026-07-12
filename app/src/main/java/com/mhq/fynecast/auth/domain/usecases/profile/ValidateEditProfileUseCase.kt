package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.models.ValidationResult

class ValidateEditProfileUseCase {
    private val usernameRegex = Regex("^[a-zA-Z0-9_\\s-]{3,30}$")
    private val emailRegex = Regex("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")

    fun validateUsernameFormat(name: String): Boolean {
        return name.isEmpty() || name.matches(usernameRegex)
    }

    fun validateEmailFormat(email: String): Boolean {
        return email.isEmpty() || email.matches(emailRegex)
    }

    operator fun invoke(username: String, email: String): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Failure("Name field cannot be left empty.")
            !username.matches(usernameRegex) -> ValidationResult.Failure("Username must be 3-30 characters (spaces/letters/numbers allowed).")
            email.isBlank() -> ValidationResult.Failure("Email field cannot be left empty.")
            !email.matches(emailRegex) -> ValidationResult.Failure("Please enter a valid email address.")
            else -> ValidationResult.Success
        }
    }
}