package com.mhq.fynecast.auth.data.repoimpl.auth

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val sessionDataStore: DataStore<Preferences>
) : AuthRepository {

    private object SessionKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
    }

    override suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): Result<UserDomainModel> = runCatching {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Auth registration failed.")

        // Account already exists at this point; displayName is cosmetic, so a
        // failure here shouldn't fail the whole registration. Retry once so a
        // transient failure doesn't leave the name unpersisted server-side,
        // and surface it if it still doesn't stick so it isn't silently lost.
        val profileSynced = runCatching {
            user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        }.isSuccess
        if (!profileSynced) {
            val retrySynced = runCatching {
                user.updateProfile(userProfileChangeRequest { displayName = name }).await()
            }.isSuccess
            if (!retrySynced) {
                Log.w(
                    "AuthRepositoryImpl",
                    "Display name for ${user.uid} was not persisted to Firebase after registration."
                )
            }
        }

        UserDomainModel(user.uid, name, email)
    }

    override suspend fun loginWithEmail(email: String, password: String): Result<UserDomainModel> =
        runCatching {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Auth login failed.")
            // The local cache lookup is a best-effort fallback only; a DataStore
            // read failure here must not fail an otherwise-successful login.
            val snapshot = runCatching { sessionDataStore.data.first() }.getOrNull()
            val cachedUsername = snapshot?.get(SessionKeys.USERNAME)
                ?.takeIf { snapshot[SessionKeys.USER_ID] == user.uid }
            val resolvedName = user.displayName ?: cachedUsername ?: "Username"
            UserDomainModel(id = user.uid, name = resolvedName, email = user.email ?: email)
        }

    override suspend fun loginWithGoogle(idToken: String): Result<UserDomainModel> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        val user = authResult.user ?: throw Exception("Google token confirmation failed.")
        UserDomainModel(user.uid, user.displayName ?: "", user.email ?: "")
    }

    override suspend fun loginWithFacebook(accessToken: String): Result<UserDomainModel> =
        runCatching {
            val credential = FacebookAuthProvider.getCredential(accessToken)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Facebook session parsing failed.")
            UserDomainModel(user.uid, user.displayName ?: "", user.email ?: "")
        }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    override fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null

    override suspend fun saveLocalSession(user: UserDomainModel) {
        sessionDataStore.edit { prefs ->
            prefs[SessionKeys.USER_ID] = user.id
            prefs[SessionKeys.USERNAME] = user.name
            prefs[SessionKeys.EMAIL] = user.email
        }
    }

    override suspend fun clearSession() {
        firebaseAuth.signOut()
        sessionDataStore.edit { prefs -> prefs.clear() }
    }
}