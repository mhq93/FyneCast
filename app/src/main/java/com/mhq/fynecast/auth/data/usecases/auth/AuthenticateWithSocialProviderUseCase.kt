package com.mhq.fynecast.auth.data.usecases.auth

import android.content.Context
import androidx.activity.ComponentActivity
import com.mhq.fynecast.auth.data.repository.auth.SocialAuthCredentialProvider
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialTokenUseCase
import kotlinx.coroutines.CancellationException

class AuthenticateWithSocialProviderUseCase(
    private val socialAuthCredentialProvider: SocialAuthCredentialProvider,
    private val authenticateWithSocialTokenUseCase: AuthenticateWithSocialTokenUseCase
) {
    suspend fun withGoogle(context: Context): Result<UserDomainModel> = runCatching {
        socialAuthCredentialProvider.getGoogleIdToken(context)
    }.fold(
        onSuccess = { token -> authenticateWithSocialTokenUseCase(token, isGoogle = true) },
        onFailure = { e ->
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    )

    suspend fun withFacebook(activity: ComponentActivity): Result<UserDomainModel> = runCatching {
        socialAuthCredentialProvider.getFacebookAccessToken(activity)
    }.fold(
        onSuccess = { token -> authenticateWithSocialTokenUseCase(token, isGoogle = false) },
        onFailure = { e ->
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    )
}