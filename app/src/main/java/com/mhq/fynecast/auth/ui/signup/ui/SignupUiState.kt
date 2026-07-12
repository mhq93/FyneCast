package com.mhq.fynecast.auth.ui.signup.ui

sealed interface SignupUiState {
    object Idle : SignupUiState
    object Loading : SignupUiState
    object Success : SignupUiState
    data class Error(val message: String) : SignupUiState
}