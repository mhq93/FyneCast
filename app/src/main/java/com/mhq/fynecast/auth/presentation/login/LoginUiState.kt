package com.mhq.fynecast.auth.presentation.login

import com.mhq.fynecast.core.presentation.text.UiText

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    object Success : LoginUiState
    data class Error(val message: UiText) : LoginUiState
}