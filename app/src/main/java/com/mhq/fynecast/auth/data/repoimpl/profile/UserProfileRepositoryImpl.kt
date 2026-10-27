package com.mhq.fynecast.auth.data.repoimpl.profile

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.userProfileChangeRequest
import com.mhq.fynecast.auth.data.datasource.SessionLocalDataSource
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import com.mhq.fynecast.core.domain.util.suspendRunCatching
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserProfileRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val sessionLocalDataSource: SessionLocalDataSource,
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
        val snapshot = sessionLocalDataSource.read()
        val name = snapshot.username ?: user?.displayName ?: "Username"
        val email = user?.email?.takeIf { it.isNotBlank() }
            ?: snapshot.email
            ?: "username@gmail.com"
        val img = snapshot.profileImageUri
        UserDomainModel(
            id = user?.uid ?: snapshot.userId ?: "",
            name = name,
            email = email,
            profileImageUri = img,
            hasPasswordLogin = user?.providerData
                ?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } ?: true
        )
    }

    override suspend fun saveProfile(profile: UserDomainModel) {
        withContext(Dispatchers.IO) {
            sessionLocalDataSource.saveProfile(profile.name, profile.email, profile.profileImageUri)
        }
    }

    override suspend fun updateRemoteProfile(name: String, email: String): Result<Boolean> =
        suspendRunCatching {
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

    override suspend fun refreshActiveUserSession(): Boolean {
        var isSessionValid = true
        try {
            firebaseAuth.currentUser?.reload()?.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            // Email/password changed elsewhere, or the account was disabled or deleted.
            isSessionValid = false
        } catch (e: Exception) {
            Log.w("UserProfileRepository", "Session refresh failed", e)
        }
        _profileState.value = getProfile()
        return isSessionValid
    }

}