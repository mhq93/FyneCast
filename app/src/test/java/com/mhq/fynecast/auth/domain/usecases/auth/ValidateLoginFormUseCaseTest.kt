package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateLoginFormUseCaseTest {

    private val validate = ValidateLoginFormUseCase()

    @Test
    fun `blank email fails with EmailEmpty`() {
        assertEquals(
            ValidationResult.Failure(ValidationError.EmailEmpty),
            validate("   ", "password")
        )
    }

    @Test
    fun `badly formatted email fails with EmailInvalid`() {
        assertEquals(
            ValidationResult.Failure(ValidationError.EmailInvalid),
            validate("not-an-email", "password")
        )
    }

    @Test
    fun `blank password fails with PasswordEmpty`() {
        assertEquals(
            ValidationResult.Failure(ValidationError.PasswordEmpty),
            validate("user@example.com", " ")
        )
    }

    @Test
    fun `email problems are reported before password problems`() {
        assertEquals(
            ValidationResult.Failure(ValidationError.EmailEmpty),
            validate("", "")
        )
    }

    @Test
    fun `any non-blank password is accepted at login`() {
        // Strength is only enforced at signup; login must accept older, weaker passwords.
        assertEquals(ValidationResult.Success, validate("user@example.com", "abc"))
    }
}
