package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationResult

class ValidateLoginFormUseCase {
    private val emailRegex = Regex("[a-zA-Z\\d._-]+@[a-z]+\\.+[a-z]+")

    fun validateEmailFormat(email: String): Boolean {
        return email.isEmpty() || email.matches(emailRegex)
    }

    operator fun invoke(email: String, password: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Failure("Email field cannot be empty.")
            !email.matches(emailRegex) -> ValidationResult.Failure("Please, enter a valid email address.")
            password.isBlank() -> ValidationResult.Failure("Password field cannot be empty.")
            else -> ValidationResult.Success
        }
    }
}