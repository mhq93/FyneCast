package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationResult

class ValidateSignupFormUseCase {
    private val usernameRegex =
        Regex("^[a-zA-Z0-9_]{3,15}$")

    private val emailRegex =
        Regex("[a-zA-Z\\d._-]+@[a-z]+\\.+[a-z]+")

    private val passwordRegex =
        Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$")

    fun validateUsername(username: String): Boolean =
        username.isEmpty() || username.matches(usernameRegex)

    fun validateEmail(email: String): Boolean =
        email.isEmpty() || email.matches(emailRegex)

    fun validatePassword(password: String): Boolean =
        password.isEmpty() || password.matches(passwordRegex)

    fun validatePasswordMatch(password: String, confirmPassword: String): Boolean =
        confirmPassword.isEmpty() || password == confirmPassword

    operator fun invoke(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Failure("Name field cannot be empty.")
            !username.matches(usernameRegex) -> ValidationResult.Failure("Name must be 3-15 non-special characters.")

            email.isBlank() -> ValidationResult.Failure("Email field cannot be empty.")
            !email.matches(emailRegex) -> ValidationResult.Failure("Please, enter a valid email address.")

            password.isBlank() -> ValidationResult.Failure("Password field cannot be empty.")
            !password.matches(passwordRegex) -> ValidationResult.Failure("Password must be +6 chars with upper, lower, numeric and special character.")

            confirmPassword.isBlank() -> ValidationResult.Failure("Please, confirm your password.")
            password != confirmPassword -> ValidationResult.Failure("Passwords do not match.")

            else -> ValidationResult.Success
        }
    }
}