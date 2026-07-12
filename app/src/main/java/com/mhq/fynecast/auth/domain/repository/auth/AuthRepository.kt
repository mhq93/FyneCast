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


    // Password Reset
    suspend fun sendPasswordResetEmail(
        email: String
    ): Result<Unit>


    // Session Management persistence
    fun isUserLoggedIn(): Boolean
    suspend fun saveLocalSession(
        user: UserDomainModel
    )
    suspend fun clearSession()
}