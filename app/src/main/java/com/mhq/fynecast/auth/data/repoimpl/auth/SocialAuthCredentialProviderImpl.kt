package com.mhq.fynecast.auth.data.repoimpl.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.facebook.login.LoginManager
import com.mhq.fynecast.auth.data.repository.auth.SocialAuthCredentialProvider
import kotlinx.coroutines.CancellationException

class SocialAuthCredentialProviderImpl(
    private val appContext: Context
) : SocialAuthCredentialProvider {

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