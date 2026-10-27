package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class UpdateUserProfileUseCaseTest {

    private val oldImage = "file:///app/profile/old.jpg"
    private val newImage = "file:///app/profile/new.jpg"
    private val pickedFromGallery = "content://gallery/photo/42"

    private val currentProfile = UserDomainModel(
        id = "uid-1",
        name = "Mohamed",
        email = "m@example.com",
        profileImageUri = oldImage,
        hasPasswordLogin = true
    )

    private val profileRepo = FakeUserProfileRepository(currentProfile)
    private val imageRepo = FakeImageStorageRepository(
        sandboxedUris = setOf(oldImage, newImage),
        sandboxResult = newImage
    )
    private val authRepo = FakeAuthRepository()

    private val useCase = UpdateUserProfileUseCase(profileRepo, imageRepo, authRepo)

    @Test
    fun `changing only the name saves the new name and keeps email and picture`() = runTest {
        val result = useCase("Mo", "m@example.com", null, null)

        assertEquals(Result.success(false), result)
        assertEquals("Mo" to "m@example.com", profileRepo.remoteCalls.single())
        val saved = profileRepo.savedProfiles.single()
        assertEquals("Mo", saved.name)
        assertEquals("m@example.com", saved.email)
        assertEquals(oldImage, saved.profileImageUri)
        assertTrue(imageRepo.deletedUris.isEmpty())
    }

    @Test
    fun `a new email waits for verification so the old email stays saved`() = runTest {
        profileRepo.remoteResult = Result.success(true)

        val result = useCase("Mohamed", "new@example.com", null, null)

        assertEquals(Result.success(true), result)
        assertEquals("m@example.com", profileRepo.savedProfiles.single().email)
    }

    @Test
    fun `a new email that needs no verification is saved right away`() = runTest {
        profileRepo.remoteResult = Result.success(false)

        useCase("Mohamed", "new@example.com", null, null)

        assertEquals("new@example.com", profileRepo.savedProfiles.single().email)
    }

    @Test
    fun `a new picture is saved first and only then is the old one deleted`() = runTest {
        val result = useCase("Mohamed", "m@example.com", null, pickedFromGallery)

        assertTrue(result.isSuccess)
        assertEquals(listOf(pickedFromGallery), imageRepo.sandboxRequests)
        assertEquals(newImage, profileRepo.savedProfiles.single().profileImageUri)
        assertEquals(listOf(oldImage), imageRepo.deletedUris)
    }

    @Test
    fun `a picture that is already in app storage is used as it is`() = runTest {
        useCase("Mohamed", "m@example.com", null, newImage)

        assertTrue(imageRepo.sandboxRequests.isEmpty())
        assertEquals(newImage, profileRepo.savedProfiles.single().profileImageUri)
        assertTrue(imageRepo.deletedUris.isEmpty())
    }

    @Test
    fun `an old picture outside app storage is never deleted`() = runTest {
        val external = currentProfile.copy(profileImageUri = "content://somewhere/else.jpg")
        profileRepo.profile = external

        useCase("Mohamed", "m@example.com", null, pickedFromGallery)

        assertTrue(imageRepo.deletedUris.isEmpty())
    }

    @Test
    fun `if the online update fails nothing is saved and the new copy is removed`() = runTest {
        profileRepo.remoteResult = Result.failure(Exception("offline"))

        val result = useCase("Mohamed", "m@example.com", null, pickedFromGallery)

        assertTrue(result.isFailure)
        assertTrue(profileRepo.savedProfiles.isEmpty())
        // The new copy is cleaned up and the user's current picture is untouched.
        assertEquals(listOf(newImage), imageRepo.deletedUris)
    }

    @Test
    fun `if only the final refresh fails the saved picture is not deleted`() = runTest {
        profileRepo.refreshThrows = true

        val result = useCase("Mohamed", "m@example.com", null, pickedFromGallery)

        assertTrue(result.isFailure)
        assertEquals(1, profileRepo.savedProfiles.size)
        // The profile already points at the new file, so deleting it would break the avatar.
        assertTrue(imageRepo.deletedUris.isEmpty())
    }

    @Test
    fun `a social account switching to a new email adds password login`() = runTest {
        profileRepo.profile = currentProfile.copy(hasPasswordLogin = false)
        profileRepo.remoteResult = Result.success(false)

        val result = useCase("Mohamed", "new@example.com", "Secret#123", null)

        assertEquals(Result.success(true), result)
        // The account's own email is sent to Firebase, not the new one.
        assertEquals("Mohamed" to "m@example.com", profileRepo.remoteCalls.single())
        assertEquals("new@example.com" to "Secret#123", authRepo.addEmailCalls.single())
        assertEquals("m@example.com", profileRepo.savedProfiles.single().email)
    }

    @Test
    fun `a social account switching email without a password fails and saves nothing`() = runTest {
        profileRepo.profile = currentProfile.copy(hasPasswordLogin = false)

        val result = useCase("Mohamed", "new@example.com", null, null)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(authRepo.addEmailCalls.isEmpty())
        assertTrue(profileRepo.savedProfiles.isEmpty())
    }

    @Test
    fun `a social account keeping its email does not touch password login`() = runTest {
        profileRepo.profile = currentProfile.copy(hasPasswordLogin = false)

        val result = useCase("Mo", "M@Example.com", null, null)

        assertTrue(result.isSuccess)
        assertTrue(authRepo.addEmailCalls.isEmpty())
        assertFalse(result.getOrThrow())
        assertEquals(oldImage, profileRepo.savedProfiles.single().profileImageUri)
    }
}
