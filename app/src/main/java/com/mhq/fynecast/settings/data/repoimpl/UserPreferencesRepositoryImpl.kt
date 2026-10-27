package com.mhq.fynecast.settings.data.repoimpl

import com.mhq.fynecast.settings.data.datasource.PreferencesLocalDataSource
import com.mhq.fynecast.settings.data.datasource.StoredPreferences
import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.domain.models.UserPreferences
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class UserPreferencesRepositoryImpl(
    private val localDataSource: PreferencesLocalDataSource
) : UserPreferencesRepository {

    override val userPreferencesFlow: Flow<UserPreferences> = localDataSource.data
        .map { stored -> mapPreferences(stored) }

    override suspend fun getPreferences(): UserPreferences {
        return mapPreferences(localDataSource.read())
    }

    override suspend fun updateMetricSystem(isMetric: Boolean) {
        localDataSource.setMetric(isMetric)
    }

    override suspend fun updateNotifications(enabled: Boolean) {
        localDataSource.setNotifications(enabled)
    }

    override suspend fun updateLanguage(language: AppLanguage) {
        localDataSource.setLanguage(language.storedName)
    }

    override suspend fun updateDarkMode(enabled: Boolean) {
        localDataSource.setDarkMode(enabled)
    }

    override suspend fun updateLastSearchedCity(name: String, lat: Double, lon: Double) {
        localDataSource.setLastCity(name, lat, lon)
    }

    override suspend fun clearLastSearchedCity() {
        localDataSource.clearLastCity()
    }

    /** True exactly once, ever. */
    override suspend fun consumeNotificationPrompt(): Boolean {
        return localDataSource.consumeNotificationPrompt()
    }

    private fun mapPreferences(stored: StoredPreferences): UserPreferences {
        return UserPreferences(
            isMetric = stored.isMetric ?: true,
            notificationsEnabled = stored.notificationsEnabled ?: true,
            currentLanguage = stored.language
                ?.let { AppLanguage.fromStoredName(it) }
                ?: phoneLanguage(),
            isDarkMode = stored.isDarkMode,
            lastSearchedCityName = stored.lastCityName,
            lastSearchedCityLat = stored.lastCityLat,
            lastSearchedCityLon = stored.lastCityLon
        )
    }

    /** First run: follow the phone's language. Anything that isn't Arabic gets English. */
    private fun phoneLanguage(): AppLanguage =
        if (Locale.getDefault().language == "ar") AppLanguage.Arabic else AppLanguage.English
}