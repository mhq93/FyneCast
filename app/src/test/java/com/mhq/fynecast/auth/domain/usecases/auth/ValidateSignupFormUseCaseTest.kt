package com.mhq.fynecast.auth.domain.usecases.auth

import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateSignupFormUseCaseTest {

    private val validate = ValidateSignupFormUseCase()

    private fun signup(
        username: String = "mohamed_1",
        email: String = "user@example.com",
        password: String = "Abcd1@",
        confirm: String = "Abcd1@"
    ) = validate(username, email, password, confirm)

    private fun failure(error: ValidationError) = ValidationResult.Failure(error)

    @Test
    fun `valid form succeeds`() {
        assertEquals(ValidationResult.Success, signup())
    }

    @Test
    fun `blank username fails with NameEmpty`() {
        assertEquals(failure(ValidationError.NameEmpty), signup(username = " "))
    }

    @Test
    fun `username that is too short, too long or has spaces fails with NameInvalidForSignup`() {
        assertEquals(failure(ValidationError.NameInvalidForSignup), signup(username = "ab"))
        assertEquals(failure(ValidationError.NameInvalidForSignup), signup(username = "a".repeat(16)))
        assertEquals(failure(ValidationError.NameInvalidForSignup), signup(username = "has space"))
    }

    @Test
    fun `username of 3 to 15 letters, digits or underscores is accepted`() {
        assertEquals(ValidationResult.Success, signup(username = "abc"))
        assertEquals(ValidationResult.Success, signup(username = "a".repeat(15)))
    }

    @Test
    fun `blank and malformed emails fail`() {
        assertEquals(failure(ValidationError.EmailEmpty), signup(email = ""))
        assertEquals(failure(ValidationError.EmailInvalid), signup(email = "user@host"))
    }

    @Test
    fun `blank password fails with PasswordEmpty and weak password with PasswordWeak`() {
        assertEquals(failure(ValidationError.PasswordEmpty), signup(password = "", confirm = ""))
        assertEquals(failure(ValidationError.PasswordWeak), signup(password = "weak", confirm = "weak"))
    }

    @Test
    fun `missing confirmation fails with ConfirmPasswordEmpty`() {
        assertEquals(failure(ValidationError.ConfirmPasswordEmpty), signup(confirm = ""))
    }

    @Test
    fun `different confirmation fails with PasswordsMismatch`() {
        assertEquals(failure(ValidationError.PasswordsMismatch), signup(confirm = "Abcd1@x"))
    }

    @Test
    fun `live field checks do not flag empty fields`() {
        assertTrue(validate.validateUsername(""))
        assertTrue(validate.validateEmail(""))
        assertTrue(validate.validatePassword(""))
        assertTrue(validate.validatePasswordMatch("Abcd1@", ""))
    }

    @Test
    fun `live field checks flag wrong input`() {
        assertFalse(validate.validateUsername("a"))
        assertFalse(validate.validateEmail("nope"))
        assertFalse(validate.validatePassword("weak"))
        assertFalse(validate.validatePasswordMatch("Abcd1@", "Abcd1"))
    }
}
