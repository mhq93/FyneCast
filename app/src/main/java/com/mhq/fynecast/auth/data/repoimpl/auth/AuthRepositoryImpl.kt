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

        try {
            user.updateProfile(
                userProfileChangeRequest { displayName = name }
            ).await()
        } catch (e: Exception) {
            // Account already exists at this point; displayName is cosmetic,
            // so don't fail the whole registration over it.
        }

        UserDomainModel(user.uid, name, email)
    }

    override suspend fun loginWithEmail(email: String, password: String): Result<UserDomainModel> =
        runCatching {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Auth login failed.")
            val snapshot = sessionDataStore.data.first()
            val resolvedName = user.displayName ?: snapshot[SessionKeys.USERNAME] ?: "Username"
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