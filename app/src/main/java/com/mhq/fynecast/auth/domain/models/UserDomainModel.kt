package com.mhq.fynecast.auth.domain.models

data class UserDomainModel(
    val id: String,
    val name: String,
    val email: String,
    val profileImageUri: String? = null
)