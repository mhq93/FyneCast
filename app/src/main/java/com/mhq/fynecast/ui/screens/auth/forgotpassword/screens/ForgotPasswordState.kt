package com.mhq.fynecast.ui.screens.auth.forgotpassword.screens

sealed interface ForgotPasswordState {
    object Idle : ForgotPasswordState
    object Loading : ForgotPasswordState
    object Success : ForgotPasswordState
    data class Error(val message: String) : ForgotPasswordState
}