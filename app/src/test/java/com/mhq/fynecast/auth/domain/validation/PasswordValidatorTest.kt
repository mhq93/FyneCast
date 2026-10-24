package com.mhq.fynecast.auth.domain.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordValidatorTest {

    @Test
    fun `accepts a password with digit, lower, upper and special character`() {
        assertTrue(PasswordValidator.isValid("Abcd1@"))
        assertTrue(PasswordValidator.isValid("Str0ng#Password"))
    }

    @Test
    fun `rejects a password shorter than six characters`() {
        assertFalse(PasswordValidator.isValid("Ab1@x"))
    }

    @Test
    fun `rejects a password missing one required kind of character`() {
        assertFalse(PasswordValidator.isValid("abcd1@"))  // no uppercase
        assertFalse(PasswordValidator.isValid("ABCD1@"))  // no lowercase
        assertFalse(PasswordValidator.isValid("Abcdef@")) // no digit
        assertFalse(PasswordValidator.isValid("Abcd12"))  // no special character
    }

    @Test
    fun `rejects a password that contains whitespace`() {
        assertFalse(PasswordValidator.isValid("Abc d1@"))
    }

    @Test
    fun `isValidOrEmpty does not flag an empty field while typing`() {
        assertTrue(PasswordValidator.isValidOrEmpty(""))
        assertFalse(PasswordValidator.isValidOrEmpty("weak"))
    }

    @Test
    fun `isValid is strict about an empty password`() {
        assertFalse(PasswordValidator.isValid(""))
    }
}
