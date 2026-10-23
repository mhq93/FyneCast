package com.mhq.fynecast.auth.data.repoimpl.profile

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.mhq.fynecast.auth.data.session.SessionKeys
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import com.mhq.fynecast.core.domain.util.suspendRunCatching
import kotlinx.coroutines.CancellationException
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
    private val sessionDataStore: DataStore<Preferences>,
    externalScope: CoroutineScope
) : UserProfileRepository {

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
        externalScope.launch(Dispatchers.IO) {
            try {
                refreshActiveUserSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("UserProfileRepository", "Initial profile refresh failed", e)
            }
        }
    }

    override suspend fun getProfile(): UserDomainModel = withContext(Dispatchers.IO) {
        val user = firebaseAuth.currentUser
        val snapshot = sessionDataStore.data.first()
        val name = snapshot[SessionKeys.USERNAME] ?: user?.displayName ?: "Username"
        val email = user?.email?.takeIf { it.isNotBlank() }
            ?: snapshot[SessionKeys.EMAIL]
            ?: "username@gmail.com"
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
        suspendRunCatching  {
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
        /** Picks up an email change verified outside the app. (Best effort), offline is fine. */
        try {
            firebaseAuth.currentUser?.reload()?.await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        _profileState.value = getProfile()
    }
}