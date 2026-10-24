package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.validation.EmailFormatValidator
import com.mhq.fynecast.auth.domain.validation.PasswordValidator

class ValidateEditProfileUseCase {
    private val usernameRegex = Regex("^[a-zA-Z0-9_ -]{3,30}$")

    fun validateUsernameFormat(name: String): Boolean {
        return name.isEmpty() || name.matches(usernameRegex)
    }

    fun validateEmailFormat(email: String): Boolean {
        return EmailFormatValidator.isValidOrEmpty(email)
    }

    fun validatePassword(password: String): Boolean = PasswordValidator.isValidOrEmpty(password)

    fun validatePasswordMatch(password: String, confirmPassword: String): Boolean =
        confirmPassword.isEmpty() || password == confirmPassword

    /** [newPassword] is non-null only when the email switch needs a password. */
    operator fun invoke(
        username: String,
        email: String,
        newPassword: String? = null,
        confirmPassword: String? = null
    ): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Failure(ValidationError.NameEmpty)
            !username.matches(usernameRegex) -> ValidationResult.Failure(ValidationError.NameInvalidForProfile)
            email.isBlank() -> ValidationResult.Failure(ValidationError.EmailEmpty)
            !EmailFormatValidator.isValid(email) -> ValidationResult.Failure(ValidationError.EmailInvalid)
            newPassword != null && newPassword.isBlank() ->
                ValidationResult.Failure(ValidationError.PasswordEmpty)
            newPassword != null && !PasswordValidator.isValid(newPassword) ->
                ValidationResult.Failure(ValidationError.PasswordWeak)
            newPassword != null && confirmPassword.isNullOrBlank() ->
                ValidationResult.Failure(ValidationError.ConfirmPasswordEmpty)
            newPassword != null && newPassword != confirmPassword ->
                ValidationResult.Failure(ValidationError.PasswordsMismatch)
            else -> ValidationResult.Success
        }
    }
}