package com.mhq.fynecast.settings.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferencesDataStoreDataSource(
    private val dataStore: DataStore<Preferences>
) : PreferencesLocalDataSource {

    private object Keys {
        val IS_METRIC = booleanPreferencesKey("is_metric")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CURRENT_LANGUAGE = stringPreferencesKey("current_language")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LAST_CITY_NAME = stringPreferencesKey("last_searched_city_name")
        val LAST_CITY_LAT = doublePreferencesKey("last_searched_city_lat")
        val LAST_CITY_LON = doublePreferencesKey("last_searched_city_lon")
        val NOTIFICATION_PROMPT_SHOWN = booleanPreferencesKey("notification_prompt_shown")
    }

    override val data: Flow<StoredPreferences> = dataStore.data.map { it.toStored() }

    override suspend fun read(): StoredPreferences = dataStore.data.first().toStored()

    override suspend fun setMetric(isMetric: Boolean) {
        dataStore.edit { it[Keys.IS_METRIC] = isMetric }
    }

    override suspend fun setNotifications(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun setLanguage(storedName: String) {
        dataStore.edit { it[Keys.CURRENT_LANGUAGE] = storedName }
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
    }

    override suspend fun setLastCity(name: String, lat: Double, lon: Double) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_CITY_NAME] = name
            prefs[Keys.LAST_CITY_LAT] = lat
            prefs[Keys.LAST_CITY_LON] = lon
        }
    }

    override suspend fun clearLastCity() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.LAST_CITY_NAME)
            prefs.remove(Keys.LAST_CITY_LAT)
            prefs.remove(Keys.LAST_CITY_LON)
        }
    }

    /** The read and write happen inside one DataStore transaction. */
    override suspend fun consumeNotificationPrompt(): Boolean {
        var firstTime = false
        dataStore.edit { prefs ->
            if (prefs[Keys.NOTIFICATION_PROMPT_SHOWN] != true) {
                prefs[Keys.NOTIFICATION_PROMPT_SHOWN] = true
                firstTime = true
            }
        }
        return firstTime
    }

    private fun Preferences.toStored() = StoredPreferences(
        isMetric = this[Keys.IS_METRIC],
        notificationsEnabled = this[Keys.NOTIFICATIONS_ENABLED],
        language = this[Keys.CURRENT_LANGUAGE],
        isDarkMode = this[Keys.IS_DARK_MODE],
        lastCityName = this[Keys.LAST_CITY_NAME],
        lastCityLat = this[Keys.LAST_CITY_LAT],
        lastCityLon = this[Keys.LAST_CITY_LON]
    )
}