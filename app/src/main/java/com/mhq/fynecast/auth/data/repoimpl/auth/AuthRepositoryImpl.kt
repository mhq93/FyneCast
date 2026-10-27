package com.mhq.fynecast.auth.data.repoimpl.auth

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.mhq.fynecast.auth.data.repository.auth.SocialAuthCredentialProvider
import com.mhq.fynecast.auth.data.datasource.SessionLocalDataSource
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.core.domain.util.suspendRunCatching
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val socialAuthCredentialProvider: SocialAuthCredentialProvider
) : AuthRepository {

    override suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): Result<UserDomainModel> = suspendRunCatching {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Auth registration failed.")

        /** Account already exists at this point; displayName is cosmetic,
         * so a failure here shouldn't fail the whole registration.
         * Retry once so a transient failure doesn't leave the name unpersisted server-side,
         * and surface it if it still doesn't stick so it isn't silently lost. */
        val profileSynced = suspendRunCatching {
            user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        }.isSuccess
        if (!profileSynced) {
            val retrySynced = suspendRunCatching {
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
        suspendRunCatching {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Auth login failed.")

            /** The local cache lookup is a best-effort fallback only;
             * a DataStore read failure here must not fail an otherwise-successful login. */
            val snapshot = suspendRunCatching { sessionLocalDataSource.read() }.getOrNull()
            val cachedUsername = snapshot?.username
                ?.takeIf { snapshot.userId == user.uid }
            val resolvedName = user.displayName ?: cachedUsername ?: "Username"
            UserDomainModel(id = user.uid, name = resolvedName, email = user.email ?: email)
        }

    override suspend fun loginWithGoogle(idToken: String): Result<UserDomainModel> =
        suspendRunCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google token confirmation failed.")
            UserDomainModel(user.uid, user.displayName ?: "", user.email ?: "")
        }

    override suspend fun loginWithFacebook(accessToken: String): Result<UserDomainModel> =
        suspendRunCatching {
            val credential = FacebookAuthProvider.getCredential(accessToken)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Facebook session parsing failed.")
            UserDomainModel(user.uid, user.displayName ?: "", user.email ?: "")
        }

    override suspend fun addEmailPasswordLogin(email: String, password: String): Result<Unit> =
        suspendRunCatching {
            val user = firebaseAuth.currentUser
                ?: throw Exception("No authenticated user active session found")
            user.linkWithCredential(EmailAuthProvider.getCredential(email, password)).await()
            user.reload().await()
            val current = firebaseAuth.currentUser ?: user
            if (current.email.equals(email, ignoreCase = true)) {
                // Linking already made this the account's email; it only needs verifying.
                current.sendEmailVerification().await()
            } else {
                current.verifyBeforeUpdateEmail(email).await()
            }
        }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        suspendRunCatching {
            firebaseAuth.sendPasswordResetEmail(email).await()
        }

    override fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null

    override suspend fun saveLocalSession(user: UserDomainModel) {
        sessionLocalDataSource.saveSession(user.id, user.name, user.email)
    }

    override suspend fun detachOutdatedSocialProviders() {
        try {
            val user = firebaseAuth.currentUser ?: return
            val accountEmail = user.email ?: return
            if (!user.isEmailVerified) return
            val passwordWorksForAccountEmail = user.providerData.any {
                it.providerId == EmailAuthProvider.PROVIDER_ID &&
                        it.email.equals(accountEmail, ignoreCase = true)
            }
            if (!passwordWorksForAccountEmail) return
            val outdated = user.providerData.filter {
                (it.providerId == GoogleAuthProvider.PROVIDER_ID ||
                        it.providerId == FacebookAuthProvider.PROVIDER_ID) &&
                        it.email != null &&
                        !it.email.equals(accountEmail, ignoreCase = true)
            }
            outdated.forEach { user.unlink(it.providerId).await() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("AuthRepository", "Could not detach outdated social providers", e)
        }
    }

    override suspend fun clearSession() {
        firebaseAuth.signOut()
        sessionLocalDataSource.clear()
        socialAuthCredentialProvider.clearCredentials()
    }
}