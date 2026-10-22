package com.mhq.fynecast.auth.data.repository.auth

interface SocialAuthCredentialProvider {
    /** Forgets the Google and Facebook sessions so the next sign-in starts clean. */
    suspend fun clearCredentials()
}