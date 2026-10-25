package com.mhq.fynecast.auth.presentation.model

/** What the Edit Profile screen needs to seed its fields. The UI never sees the domain model. */
data class EditProfilePresentationModel(
    val name: String,
    val email: String,
    val profileImageUri: String?,
    /** False for Google/Facebook accounts that have no password yet. */
    val hasPasswordLogin: Boolean
)
