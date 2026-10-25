package com.mhq.fynecast.auth.presentation.mapper

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.presentation.model.EditProfilePresentationModel
import org.junit.Assert.assertEquals
import org.junit.Test

class EditProfilePresentationMapperTest {

    @Test
    fun `maps every field the edit screen needs`() {
        val user = UserDomainModel(
            id = "uid-1",
            name = "Mohamed",
            email = "m@example.com",
            profileImageUri = "content://image/1",
            hasPasswordLogin = false
        )

        assertEquals(
            EditProfilePresentationModel(
                name = "Mohamed",
                email = "m@example.com",
                profileImageUri = "content://image/1",
                hasPasswordLogin = false
            ),
            user.toEditProfilePresentation()
        )
    }

    @Test
    fun `keeps a missing profile image as null`() {
        val user = UserDomainModel(id = "uid-2", name = "A", email = "a@b.com")

        assertEquals(null, user.toEditProfilePresentation().profileImageUri)
    }
}
