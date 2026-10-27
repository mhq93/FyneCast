package com.mhq.fynecast.auth.data.datasource

/** The phone-side session store. Repositories use this instead of touching DataStore. */
interface SessionLocalDataSource {
    suspend fun read(): StoredSession

    /** Starts a session; brings back this account's own saved picture, never another account's. */
    suspend fun saveSession(userId: String, username: String, email: String)

    /** Saves edited profile values; the picture is only replaced when [profileImageUri] is not null. */
    suspend fun saveProfile(username: String, email: String, profileImageUri: String?)

    /** Ends the session; the picture is parked under the account's id so it returns on next login. */
    suspend fun clear()
}

/** What is stored on the phone about the signed-in account. Every field can be missing. */
data class StoredSession(
    val userId: String?,
    val username: String?,
    val email: String?,
    val profileImageUri: String?
)
