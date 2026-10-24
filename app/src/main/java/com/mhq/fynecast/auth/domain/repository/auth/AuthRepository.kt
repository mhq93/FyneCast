package com.mhq.fynecast.auth.domain.repository.auth

import com.mhq.fynecast.auth.domain.models.UserDomainModel

interface AuthRepository {

    // Standard Email Operations
    suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): Result<UserDomainModel>

    suspend fun loginWithEmail(
        email: String,
        password: String
    ): Result<UserDomainModel>


    // Social Media Token Operations
    suspend fun loginWithGoogle(
        idToken: String
    ): Result<UserDomainModel>

    suspend fun loginWithFacebook(
        accessToken: String
    ): Result<UserDomainModel>

    /** Adds email/password sign-in for [email] to a social account and sends that email a verification link. */
    suspend fun addEmailPasswordLogin(email: String, password: String): Result<Unit>

    // Password Reset
    suspend fun sendPasswordResetEmail(
        email: String
    ): Result<Unit>


    // Session Management persistence
    fun isUserLoggedIn(): Boolean

    suspend fun saveLocalSession(
        user: UserDomainModel
    )

    /**
     * After an email switch: detaches Google/Facebook links whose email is no longer the account's email.
     * Only runs when email/password login already works for the account's current email.
     */
    suspend fun detachOutdatedSocialProviders()

    suspend fun clearSession()
}