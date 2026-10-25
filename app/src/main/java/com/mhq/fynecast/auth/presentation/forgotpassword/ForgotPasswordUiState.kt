package com.mhq.fynecast.auth.presentation.forgotpassword

import com.mhq.fynecast.core.presentation.text.UiText

sealed interface ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState
    object Loading : ForgotPasswordUiState
    object Success : ForgotPasswordUiState
    data class Error(val message: UiText) : ForgotPasswordUiState
}