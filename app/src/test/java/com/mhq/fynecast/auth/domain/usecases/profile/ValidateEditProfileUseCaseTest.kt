package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateEditProfileUseCaseTest {

    private val validate = ValidateEditProfileUseCase()

    private fun failure(error: ValidationError) = ValidationResult.Failure(error)

    @Test
    fun `valid name and email succeed without a password`() {
        assertEquals(ValidationResult.Success, validate("Mohamed Ali", "user@example.com"))
    }

    @Test
    fun `names may contain spaces and hyphens but not other symbols`() {
        assertEquals(ValidationResult.Success, validate("Anne-Marie Smith", "user@example.com"))
        assertEquals(failure(ValidationError.NameInvalidForProfile), validate("Bad@Name", "user@example.com"))
        assertEquals(failure(ValidationError.NameInvalidForProfile), validate("ab", "user@example.com"))
    }

    @Test
    fun `blank name and blank email fail`() {
        assertEquals(failure(ValidationError.NameEmpty), validate(" ", "user@example.com"))
        assertEquals(failure(ValidationError.EmailEmpty), validate("Mohamed", ""))
        assertEquals(failure(ValidationError.EmailInvalid), validate("Mohamed", "user@host"))
    }

    @Test
    fun `no password rules apply when no new password is given`() {
        assertEquals(ValidationResult.Success, validate("Mohamed", "user@example.com", null, null))
    }

    @Test
    fun `when a new password is required it must be present, strong and confirmed`() {
        val email = "new@example.com"
        assertEquals(failure(ValidationError.PasswordEmpty), validate("Mohamed", email, "", ""))
        assertEquals(failure(ValidationError.PasswordWeak), validate("Mohamed", email, "weak", "weak"))
        assertEquals(failure(ValidationError.ConfirmPasswordEmpty), validate("Mohamed", email, "Abcd1@", ""))
        assertEquals(failure(ValidationError.ConfirmPasswordEmpty), validate("Mohamed", email, "Abcd1@", null))
        assertEquals(failure(ValidationError.PasswordsMismatch), validate("Mohamed", email, "Abcd1@", "Abcd1@x"))
        assertEquals(ValidationResult.Success, validate("Mohamed", email, "Abcd1@", "Abcd1@"))
    }

    @Test
    fun `name and email problems are reported before password problems`() {
        assertEquals(failure(ValidationError.NameEmpty), validate("", "user@example.com", "weak", "x"))
    }

    @Test
    fun `live field checks do not flag empty fields`() {
        assertTrue(validate.validateUsernameFormat(""))
        assertTrue(validate.validateEmailFormat(""))
        assertTrue(validate.validatePassword(""))
        assertTrue(validate.validatePasswordMatch("Abcd1@", ""))
    }

    @Test
    fun `live field checks flag wrong input`() {
        assertFalse(validate.validateUsernameFormat("a"))
        assertFalse(validate.validateEmailFormat("nope"))
        assertFalse(validate.validatePassword("weak"))
        assertFalse(validate.validatePasswordMatch("Abcd1@", "other"))
    }
}
