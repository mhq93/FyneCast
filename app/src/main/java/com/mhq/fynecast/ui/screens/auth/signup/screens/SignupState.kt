package com.mhq.fynecast.ui.screens.auth.signup.screens

sealed interface SignupState {
    object Idle : SignupState
    object Loading : SignupState
    object Success : SignupState
    data class Error(val message: String) : SignupState
}