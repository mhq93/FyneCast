package com.mhq.fynecast.settings.data.infra

import com.mhq.fynecast.core.domain.infra.AppLanguageProvider
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PreferencesAppLanguageProvider(
    private val preferencesRepository: UserPreferencesRepository
) : AppLanguageProvider {

    override fun observeLanguageCode(): Flow<String> =
        preferencesRepository.userPreferencesFlow
            .map { it.currentLanguage.code }
            .distinctUntilChanged()
}
