package com.mhq.fynecast.auth.domain.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailFormatValidatorTest {

    @Test
    fun `accepts common valid addresses`() {
        assertTrue(EmailFormatValidator.isValid("user@example.com"))
        assertTrue(EmailFormatValidator.isValid("first.last+tag@sub.domain.org"))
        assertTrue(EmailFormatValidator.isValid("a_b-c@my-site.co"))
    }

    @Test
    fun `rejects addresses without a proper domain`() {
        assertFalse(EmailFormatValidator.isValid("plain"))
        assertFalse(EmailFormatValidator.isValid("user@"))
        assertFalse(EmailFormatValidator.isValid("user@host"))
        assertFalse(EmailFormatValidator.isValid("user@host.c"))
        assertFalse(EmailFormatValidator.isValid("user@-host.com"))
    }

    @Test
    fun `rejects addresses with a missing local part or spaces`() {
        assertFalse(EmailFormatValidator.isValid("@example.com"))
        assertFalse(EmailFormatValidator.isValid("us er@example.com"))
    }

    @Test
    fun `strict check treats an empty string as invalid`() {
        assertFalse(EmailFormatValidator.isValid(""))
    }

    @Test
    fun `isValidOrEmpty does not flag an empty field while typing`() {
        assertTrue(EmailFormatValidator.isValidOrEmpty(""))
        assertFalse(EmailFormatValidator.isValidOrEmpty("not-an-email"))
        assertTrue(EmailFormatValidator.isValidOrEmpty("user@example.com"))
    }
}
