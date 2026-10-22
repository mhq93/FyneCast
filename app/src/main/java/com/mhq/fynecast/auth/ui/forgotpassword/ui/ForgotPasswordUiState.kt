package com.mhq.fynecast.auth.ui.forgotpassword.ui

import com.mhq.fynecast.core.ui.text.UiText

sealed interface ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState
    object Loading : ForgotPasswordUiState
    object Success : ForgotPasswordUiState
    data class Error(val message: UiText) : ForgotPasswordUiState
}