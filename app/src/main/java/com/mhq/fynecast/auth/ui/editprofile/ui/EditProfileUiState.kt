package com.mhq.fynecast.auth.ui.editprofile.ui

import com.mhq.fynecast.core.ui.text.UiText

sealed interface EditProfileUiState {
    object Idle : EditProfileUiState
    object Loading : EditProfileUiState
    object Success : EditProfileUiState
    data class Error(val message: UiText) : EditProfileUiState
}