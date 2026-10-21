package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.repository.profile.ImageStorageRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository

class UpdateUserProfileUseCase(
    private val profileRepository: UserProfileRepository,
    private val imageRepository: ImageStorageRepository
) {
    suspend operator fun invoke(
        newName: String,
        newEmail: String,
        localImageUriString: String?
    ): Result<Boolean> {
        val currentProfile = profileRepository.getProfile()
        var finalImageUri = currentProfile.profileImageUri
        var createdImageUri: String? = null

        if (!localImageUriString.isNullOrBlank()) {
            if (!imageRepository.isAlreadySandboxed(localImageUriString)) {
                val sandboxedUri = imageRepository.sandboxAndCompressImage(localImageUriString)
                if (sandboxedUri != null) {
                    finalImageUri = sandboxedUri
                    createdImageUri = sandboxedUri
                }
            } else {
                finalImageUri = localImageUriString
            }
        }

        var profileSaved = false

        return runCatching {
            val remoteUpdateVerification =
                profileRepository.updateRemoteProfile(newName, newEmail).getOrThrow()

            profileRepository.saveProfile(
                currentProfile.copy(
                    name = newName,
                     /** A changed email only applies after the user clicks the verification link,
                     so keep showing the current one until then.*/
                    email = if (remoteUpdateVerification) currentProfile.email else newEmail,
                    profileImageUri = finalImageUri
                )
            )
            profileSaved = true
            profileRepository.refreshActiveUserSession()

             /**The profile now durably references the new image,
              * so it's safe to purge the old one (ones).
              * Doing this any earlier risked leaving
              * the profile pointing at an image that was already deleted
              * if the save above failed.*/
            createdImageUri?.let { imageRepository.commitProfileImage(it) }


            remoteUpdateVerification
        }.onFailure {
             /**Only roll back if the profile never ended up pointing at this file.
              * If saveProfile already succeeded
              * and a later step (refreshActiveUserSession, commitProfileImage)
              * is what threw, deleting the file now would leave
              * the saved profile pointing at a file that no longer exists
              * — the exact thing this rollback exists to prevent.*/
            if (!profileSaved) {
                createdImageUri?.let { imageRepository.deleteProfileImage(it) }
            }
        }
    }
}