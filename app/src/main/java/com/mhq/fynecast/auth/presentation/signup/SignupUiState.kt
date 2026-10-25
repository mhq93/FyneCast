package com.mhq.fynecast.auth.presentation.signup

import com.mhq.fynecast.core.presentation.text.UiText

sealed interface SignupUiState {
    object Idle : SignupUiState
    object Loading : SignupUiState
    object Success : SignupUiState
    data class Error(val message: UiText) : SignupUiState
}