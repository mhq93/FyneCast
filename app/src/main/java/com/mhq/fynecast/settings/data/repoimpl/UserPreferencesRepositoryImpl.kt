package com.mhq.fynecast.settings.data.repoimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mhq.fynecast.settings.domain.models.UserPreferences
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class UserPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val IS_METRIC = booleanPreferencesKey("is_metric")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CURRENT_LANGUAGE = stringPreferencesKey("current_language")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LAST_CITY_NAME = stringPreferencesKey("last_searched_city_name")
        val LAST_CITY_LAT = doublePreferencesKey("last_searched_city_lat")
        val LAST_CITY_LON = doublePreferencesKey("last_searched_city_lon")
    }

    override val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .map { preferences -> mapPreferences(preferences) }

    override suspend fun getPreferences(): UserPreferences {
        return mapPreferences(dataStore.data.first())
    }

    override suspend fun updateMetricSystem(isMetric: Boolean) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.IS_METRIC] = isMetric }
    }

    override suspend fun updateNotifications(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun updateLanguage(language: String) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.CURRENT_LANGUAGE] = language }
    }

    override suspend fun updateDarkMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.IS_DARK_MODE] = enabled }
    }

    override suspend fun updateLastSearchedCity(name: String, lat: Double, lon: Double) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_CITY_NAME] = name
            prefs[PreferencesKeys.LAST_CITY_LAT] = lat
            prefs[PreferencesKeys.LAST_CITY_LON] = lon
        }
    }

    private fun mapPreferences(preferences: Preferences): UserPreferences {
        return UserPreferences(
            isMetric = preferences[PreferencesKeys.IS_METRIC] ?: true,
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            currentLanguage = preferences[PreferencesKeys.CURRENT_LANGUAGE] ?: "English",
            isDarkMode = preferences[PreferencesKeys.IS_DARK_MODE] ?: true,
            lastSearchedCityName = preferences[PreferencesKeys.LAST_CITY_NAME],
            lastSearchedCityLat = preferences[PreferencesKeys.LAST_CITY_LAT],
            lastSearchedCityLon = preferences[PreferencesKeys.LAST_CITY_LON]
        )
    }
}