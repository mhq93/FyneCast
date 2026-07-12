package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.profile.ImageStorageRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository

class UpdateUserProfileUseCase(
    private val profileRepository: UserProfileRepository,
    private val imageRepository: ImageStorageRepository
) {
    suspend operator fun invoke(newName: String, newEmail: String, localImageUriString: String?): Result<Boolean> {
        return runCatching {
            var finalImageUri = profileRepository.getProfile().profileImageUri

            if (!localImageUriString.isNullOrBlank()) {
                if (!imageRepository.isAlreadySandboxed(localImageUriString)) {
                    val sandboxedUri = imageRepository.sandboxAndCompressImage(localImageUriString)
                    if (sandboxedUri != null) finalImageUri = sandboxedUri
                } else {
                    finalImageUri = localImageUriString
                }
            }

            val remoteUpdateVerification = profileRepository.updateRemoteProfile(newName, newEmail).getOrThrow()

            profileRepository.saveProfile(
                UserDomainModel(
                    id = "",
                    name = newName,
                    email = newEmail,
                    profileImageUri = finalImageUri
                )
            )
            profileRepository.refreshActiveUserSession()

            remoteUpdateVerification
        }
    }
}