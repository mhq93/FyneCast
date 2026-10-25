package com.mhq.fynecast.settings.presentation.mapper

import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.presentation.model.LanguageOption

// Both `when` blocks are exhaustive on purpose: adding a language to one enum
// breaks the build here until the other enum and these mappers are updated.
fun AppLanguage.toPresentation(): LanguageOption = when (this) {
    AppLanguage.English -> LanguageOption.English
    AppLanguage.Arabic -> LanguageOption.Arabic
}

fun LanguageOption.toDomain(): AppLanguage = when (this) {
    LanguageOption.English -> AppLanguage.English
    LanguageOption.Arabic -> AppLanguage.Arabic
}
