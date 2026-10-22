package com.mhq.fynecast.auth.ui.login.ui

import com.mhq.fynecast.core.ui.text.UiText

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    object Success : LoginUiState
    data class Error(val message: UiText) : LoginUiState
}