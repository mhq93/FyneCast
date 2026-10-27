package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.ImageStorageRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeUserProfileRepository(
    var profile: UserDomainModel,
    /** What Firebase answers: success(true) means "a verification email was sent". */
    var remoteResult: Result<Boolean> = Result.success(false)
) : UserProfileRepository {
    val remoteCalls = mutableListOf<Pair<String, String>>()
    val savedProfiles = mutableListOf<UserDomainModel>()
    var refreshCalls = 0
    var refreshThrows = false

    override val profileState: StateFlow<UserDomainModel> get() = MutableStateFlow(profile)

    override suspend fun getProfile(): UserDomainModel = profile

    override suspend fun saveProfile(profile: UserDomainModel) {
        savedProfiles += profile
        this.profile = profile
    }

    override suspend fun updateRemoteProfile(name: String, email: String): Result<Boolean> {
        remoteCalls += name to email
        return remoteResult
    }

    override suspend fun refreshActiveUserSession(): Boolean {
        refreshCalls++
        if (refreshThrows) throw IllegalStateException("refresh failed")
        return true
    }
}

internal class FakeImageStorageRepository(
    /** Pictures that already live inside the app's own storage. */
    private val sandboxedUris: Set<String> = emptySet(),
    /** What copying a new picture into app storage returns (null = it failed). */
    var sandboxResult: String? = null
) : ImageStorageRepository {
    val sandboxRequests = mutableListOf<String>()
    val deletedUris = mutableListOf<String>()

    override fun isAlreadySandboxed(uriString: String?): Boolean = uriString in sandboxedUris

    override suspend fun sandboxAndCompressImage(uriString: String): String? {
        sandboxRequests += uriString
        return sandboxResult
    }

    override fun deleteProfileImage(uriString: String) {
        deletedUris += uriString
    }
}

/** Only addEmailPasswordLogin matters for these tests; the rest should never be called. */
internal class FakeAuthRepository(
    var addEmailResult: Result<Unit> = Result.success(Unit)
) : AuthRepository {
    val addEmailCalls = mutableListOf<Pair<String, String>>()

    override suspend fun addEmailPasswordLogin(email: String, password: String): Result<Unit> {
        addEmailCalls += email to password
        return addEmailResult
    }

    override suspend fun registerWithEmail(name: String, email: String, password: String): Result<UserDomainModel> = unused()
    override suspend fun loginWithEmail(email: String, password: String): Result<UserDomainModel> = unused()
    override suspend fun loginWithGoogle(idToken: String): Result<UserDomainModel> = unused()
    override suspend fun loginWithFacebook(accessToken: String): Result<UserDomainModel> = unused()
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = unused()
    override fun isUserLoggedIn(): Boolean = unused()
    override suspend fun saveLocalSession(user: UserDomainModel) { unused() }
    override suspend fun detachOutdatedSocialProviders() { unused() }
    override suspend fun clearSession() { unused() }

    private fun unused(): Nothing = error("not expected in this test")
}
