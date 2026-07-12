package com.mhq.fynecast.auth.ui.editprofile.ui

sealed interface EditProfileUiState {
    object Idle : EditProfileUiState
    object Loading : EditProfileUiState
    object Success : EditProfileUiState
    data class Error(val message: String) : EditProfileUiState
}