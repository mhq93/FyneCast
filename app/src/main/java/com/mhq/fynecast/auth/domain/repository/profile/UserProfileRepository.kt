package com.mhq.fynecast.auth.domain.repository.profile

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import kotlinx.coroutines.flow.StateFlow

interface UserProfileRepository {
    val profileState: StateFlow<UserDomainModel>
    suspend fun getProfile(): UserDomainModel
    suspend fun saveProfile(profile: UserDomainModel)
    suspend fun updateRemoteProfile(name: String, email: String): Result<Boolean>
    suspend fun refreshActiveUserSession()
}