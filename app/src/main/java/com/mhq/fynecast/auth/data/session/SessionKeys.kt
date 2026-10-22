package com.mhq.fynecast.auth.data.session

import androidx.datastore.preferences.core.stringPreferencesKey

internal object SessionKeys {
    val USER_ID = stringPreferencesKey("user_id")
    val USERNAME = stringPreferencesKey("username")
    val EMAIL = stringPreferencesKey("email")
    val PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")
    /** Per-account copy of the profile picture; survives logout so it returns on the next login. */
    fun profileImageFor(userId: String) = stringPreferencesKey("profile_image_uri_$userId")
}