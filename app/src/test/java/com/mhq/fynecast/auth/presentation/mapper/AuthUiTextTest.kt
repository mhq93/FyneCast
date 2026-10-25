package com.mhq.fynecast.auth.presentation.mapper

import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationException
import com.mhq.fynecast.core.presentation.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUiTextTest {

    @Test
    fun `every validation error has a string resource`() {
        // Building the text for each error also proves the mapper handles all of them.
        ValidationError.entries.forEach { error ->
            assertTrue(error.toUiText() is UiText.Resource)
        }
    }

    @Test
    fun `validation exception becomes its localized message`() {
        val text = ValidationException(ValidationError.EmailEmpty)
            .toUiText(R.string.error_login_failed)

        assertEquals(UiText.res(R.string.error_email_empty), text)
    }

    @Test
    fun `provider message is kept as dynamic text`() {
        val text = IllegalStateException("Network down").toUiText(R.string.error_login_failed)

        assertEquals(UiText.Dynamic("Network down"), text)
    }

    @Test
    fun `blank message falls back to the given resource`() {
        val text = IllegalStateException().toUiText(R.string.error_login_failed)

        assertEquals(UiText.res(R.string.error_login_failed), text)
    }
}
