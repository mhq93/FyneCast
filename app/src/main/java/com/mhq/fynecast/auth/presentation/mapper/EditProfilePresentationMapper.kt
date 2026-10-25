package com.mhq.fynecast.auth.presentation.mapper

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.presentation.model.EditProfilePresentationModel

fun UserDomainModel.toEditProfilePresentation(): EditProfilePresentationModel =
    EditProfilePresentationModel(
        name = name,
        email = email,
        profileImageUri = profileImageUri,
        hasPasswordLogin = hasPasswordLogin
    )
