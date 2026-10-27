package com.mhq.fynecast.auth.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

class SessionDataStoreDataSource(
    private val dataStore: DataStore<Preferences>
) : SessionLocalDataSource {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")

        /** Per-account copy of the profile picture; survives logout so it returns on the next login. */
        fun profileImageFor(userId: String) = stringPreferencesKey("profile_image_uri_$userId")
    }

    override suspend fun read(): StoredSession {
        val prefs = dataStore.data.first()
        return StoredSession(
            userId = prefs[Keys.USER_ID],
            username = prefs[Keys.USERNAME],
            email = prefs[Keys.EMAIL],
            profileImageUri = prefs[Keys.PROFILE_IMAGE_URI]
        )
    }

    override suspend fun saveSession(userId: String, username: String, email: String) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USERNAME] = username
            prefs[Keys.EMAIL] = email

            /** Bring back this account's own picture (if it saved one); never another account's. */
            val savedImage = prefs[Keys.profileImageFor(userId)]
            if (savedImage != null) {
                prefs[Keys.PROFILE_IMAGE_URI] = savedImage
            } else {
                prefs.remove(Keys.PROFILE_IMAGE_URI)
            }
        }
    }

    override suspend fun saveProfile(username: String, email: String, profileImageUri: String?) {
        dataStore.edit { prefs ->
            prefs[Keys.USERNAME] = username
            prefs[Keys.EMAIL] = email
            if (profileImageUri != null) {
                prefs[Keys.PROFILE_IMAGE_URI] = profileImageUri
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            /** Park the picture under the account's id before wiping the active session. */
            val uid = prefs[Keys.USER_ID]
            val image = prefs[Keys.PROFILE_IMAGE_URI]
            if (!uid.isNullOrBlank() && image != null) {
                prefs[Keys.profileImageFor(uid)] = image
            }
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USERNAME)
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.PROFILE_IMAGE_URI)
        }
    }
}