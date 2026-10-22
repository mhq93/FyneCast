package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator

class ValidateSignupFormUseCase {
    private val usernameRegex =
        Regex("^[a-zA-Z0-9_]{3,15}$")

    private val passwordRegex =
        Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$")

    fun validateUsername(username: String): Boolean =
        username.isEmpty() || username.matches(usernameRegex)

    fun validateEmail(email: String): Boolean =
        EmailFormatValidator.isValidOrEmpty(email)

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
            username.isBlank() -> ValidationResult.Failure(ValidationError.NameEmpty)
            !username.matches(usernameRegex) -> ValidationResult.Failure(ValidationError.NameInvalidForSignup)

            email.isBlank() -> ValidationResult.Failure(ValidationError.EmailEmpty)
            !EmailFormatValidator.isValid(email) -> ValidationResult.Failure(ValidationError.EmailInvalid)

            password.isBlank() -> ValidationResult.Failure(ValidationError.PasswordEmpty)
            !password.matches(passwordRegex) -> ValidationResult.Failure(ValidationError.PasswordWeak)

            confirmPassword.isBlank() -> ValidationResult.Failure(ValidationError.ConfirmPasswordEmpty)
            password != confirmPassword -> ValidationResult.Failure(ValidationError.PasswordsMismatch)

            else -> ValidationResult.Success
        }
    }
}