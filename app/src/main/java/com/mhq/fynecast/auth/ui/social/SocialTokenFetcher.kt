package com.mhq.fynecast.auth.ui.social

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mhq.fynecast.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Gets provider tokens. Needs an Activity/Context,
 * so it lives in the UI layer. */
object SocialTokenFetcher {

    suspend fun getGoogleIdToken(context: Context): String {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        return GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    }

    suspend fun getFacebookAccessToken(activity: ComponentActivity): String =
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
}