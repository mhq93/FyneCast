package com.mhq.fynecast.auth.data.repoimpl.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserProfileRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val sessionDataStore: DataStore<Preferences>
) : UserProfileRepository {

    private object SessionKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")
    }

    private val _profileState =
        MutableStateFlow(
            UserDomainModel(
                "",
                "Username",
                "username@gmail.com",
                null
            )
        )

    override val profileState: StateFlow<UserDomainModel> = _profileState.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch { refreshActiveUserSession() }
    }

    override suspend fun getProfile(): UserDomainModel = withContext(Dispatchers.IO) {
        val user = firebaseAuth.currentUser
        val snapshot = sessionDataStore.data.first()
        val name = snapshot[SessionKeys.USERNAME] ?: user?.displayName ?: "Username"
        val email = snapshot[SessionKeys.EMAIL] ?: user?.email ?: "username@gmail.com"
        val img = snapshot[SessionKeys.PROFILE_IMAGE_URI]
        UserDomainModel(user?.uid ?: snapshot[SessionKeys.USER_ID] ?: "", name, email, img)
    }

    override suspend fun saveProfile(profile: UserDomainModel) {
        withContext(Dispatchers.IO) {
            sessionDataStore.edit { prefs ->
                prefs[SessionKeys.USERNAME] = profile.name
                prefs[SessionKeys.EMAIL] = profile.email
                if (profile.profileImageUri != null) {
                    prefs[SessionKeys.PROFILE_IMAGE_URI] = profile.profileImageUri
                }
            }
        }
    }

    override suspend fun updateRemoteProfile(name: String, email: String): Result<Boolean> =
        runCatching {
            withContext(Dispatchers.IO) {
                val user = firebaseAuth.currentUser
                    ?: throw Exception("No authenticated user active session found")
                var verificationSent = false
                if (user.displayName != name) {
                    user.updateProfile(userProfileChangeRequest { displayName = name }).await()
                }
                if (user.email != email) {
                    user.verifyBeforeUpdateEmail(email).await()
                    verificationSent = true
                }
                verificationSent
            }
        }

    override suspend fun refreshActiveUserSession() {
        val current = getProfile()
        _profileState.value = current
    }
}