package com.mhq.fynecast.settings.presentation.mapper

import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.presentation.model.LanguageOption
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageOptionMapperTest {

    @Test
    fun `every domain language maps to the presentation option and back`() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, language.toPresentation().toDomain())
        }
    }

    @Test
    fun `presentation codes match the domain codes`() {
        // The two enums each store the language code; this fails if they ever drift apart.
        AppLanguage.entries.forEach { language ->
            assertEquals(language.code, language.toPresentation().code)
        }
    }

    @Test
    fun `option maps to the expected domain language`() {
        assertEquals(AppLanguage.Arabic, LanguageOption.Arabic.toDomain())
        assertEquals(AppLanguage.English, LanguageOption.English.toDomain())
    }
}
