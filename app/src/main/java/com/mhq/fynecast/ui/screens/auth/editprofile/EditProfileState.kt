package com.mhq.fynecast.ui.screens.auth.editprofile

sealed interface EditProfileState {
    object Idle : EditProfileState
    object Loading : EditProfileState
    object Success : EditProfileState
    data class Error(val message: String) : EditProfileState
}