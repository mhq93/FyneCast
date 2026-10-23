package com.mhq.fynecast.settings.domain.repository

import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.domain.models.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>

    suspend fun getPreferences(): UserPreferences
    suspend fun updateMetricSystem(isMetric: Boolean)
    suspend fun updateNotifications(enabled: Boolean)
    suspend fun updateLanguage(language: AppLanguage)
    suspend fun updateDarkMode(enabled: Boolean)
    suspend fun updateLastSearchedCity(name: String, lat: Double, lon: Double)
    suspend fun clearLastSearchedCity()
    suspend fun consumeNotificationPrompt(): Boolean
}