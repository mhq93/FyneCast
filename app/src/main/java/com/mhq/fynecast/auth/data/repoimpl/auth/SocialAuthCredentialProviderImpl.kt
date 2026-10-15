package com.mhq.fynecast.auth.data.repoimpl.auth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mhq.fynecast.auth.data.repository.auth.SocialAuthCredentialProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SocialAuthCredentialProviderImpl(
    private val appContext: Context,
    private val serverClientId: String
) : SocialAuthCredentialProvider {

    override suspend fun getGoogleIdToken(context: Context): String {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        return GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    }

    override suspend fun getFacebookAccessToken(activity: ComponentActivity): String =
        suspendCancellableCoroutine { continuation ->
            val loginManager = LoginManager.getInstance()
            val callbackManager = CallbackManager.Factory.create()

            loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    loginManager.unregisterCallback(callbackManager)
                    continuation.resume(result.accessToken.token)
                }

                override fun onCancel() {
                    loginManager.unregisterCallback(callbackManager)
                    continuation.cancel()
                }

                override fun onError(error: FacebookException) {
                    loginManager.unregisterCallback(callbackManager)
                    continuation.resumeWithException(error)
                }
            })

            continuation.invokeOnCancellation {
                loginManager.unregisterCallback(callbackManager)
            }

            loginManager.logInWithReadPermissions(
                activity,
                callbackManager,
                listOf("public_profile", "email")
            )
        }

    override suspend fun clearCredentials() {
        LoginManager.getInstance().logOut()
        try {
            CredentialManager
                .create(appContext)
                .clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort: signing out must not fail because the credential provider did.
        }
    }
}