package com.mhq.fynecast.auth.ui.signup.ui

import com.mhq.fynecast.core.ui.text.UiText

sealed interface SignupUiState {
    object Idle : SignupUiState
    object Loading : SignupUiState
    object Success : SignupUiState
    data class Error(val message: UiText) : SignupUiState
}