package com.mhq.fynecast.auth.data.session

import androidx.datastore.preferences.core.stringPreferencesKey

internal object SessionKeys {
    val USER_ID = stringPreferencesKey("user_id")
    val USERNAME = stringPreferencesKey("username")
    val EMAIL = stringPreferencesKey("email")
    val PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")
}