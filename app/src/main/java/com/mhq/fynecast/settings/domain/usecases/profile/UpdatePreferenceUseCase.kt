package com.mhq.fynecast.settings.domain.usecases.profile

import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository

class UpdatePreferenceUseCase(
    private val preferencesRepository: UserPreferencesRepository
) {
    suspend fun metricSystem(isMetric: Boolean) =
        preferencesRepository.updateMetricSystem(isMetric)

    suspend fun notifications(enabled: Boolean) =
        preferencesRepository.updateNotifications(enabled)

    suspend fun language(language: String) =
        preferencesRepository.updateLanguage(language)

    suspend fun darkMode(enabled: Boolean) =
        preferencesRepository.updateDarkMode(enabled)

    //suspend fun themeMode(mode: ThemeMode) =
        //preferencesRepository.updateThemeMode(mode)

    suspend fun lastSearchedCity(name: String, lat: Double, lon: Double) =
        preferencesRepository.updateLastSearchedCity(name, lat, lon)
}