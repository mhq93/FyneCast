package com.mhq.fynecast.auth.presentation.editprofile

import com.mhq.fynecast.core.presentation.text.UiText

sealed interface EditProfileUiState {
    object Idle : EditProfileUiState
    object Loading : EditProfileUiState
    object Success : EditProfileUiState
    data class Error(val message: UiText) : EditProfileUiState
}