package com.mhq.fynecast.settings.data.datasource

import kotlinx.coroutines.flow.Flow

/** The phone-side preferences store. The repository uses this instead of touching DataStore. */
interface PreferencesLocalDataSource {
    val data: Flow<StoredPreferences>

    suspend fun read(): StoredPreferences
    suspend fun setMetric(isMetric: Boolean)
    suspend fun setNotifications(enabled: Boolean)
    suspend fun setLanguage(storedName: String)
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setLastCity(name: String, lat: Double, lon: Double)
    suspend fun clearLastCity()

    /** True exactly once, ever. */
    suspend fun consumeNotificationPrompt(): Boolean
}

/** Raw stored values; null means "never saved". Defaults are decided by the repository. */
data class StoredPreferences(
    val isMetric: Boolean?,
    val notificationsEnabled: Boolean?,
    val language: String?,
    val isDarkMode: Boolean?,
    val lastCityName: String?,
    val lastCityLat: Double?,
    val lastCityLon: Double?
)