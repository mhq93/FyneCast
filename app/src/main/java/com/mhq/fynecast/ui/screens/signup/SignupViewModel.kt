package com.mhq.fynecast.ui.screens.signup

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SignupViewModel: ViewModel(){
    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName
    fun onUsernameChanged(value: String){
        _userName.value = value
    }
//    val usernameHasErrors by derivedStateOf {
//        if (_userName.isNotEmpty()) {
//            !android.util.Patterns.EMAIL_ADDRESS.matcher(_userName).matches()
//        } else {
//            false
//        }
//    }

    fun validateUsername(username: String): Boolean{
        val usernameRegex = Regex("^[a-zA-Z0-9_]{3,15}$")
        return username.matches(usernameRegex)
    }

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail
    fun onUserEmailChanged(value: String){
        _userEmail.value = value
    }
//    fun validateEmail(email: String): ErrorStatus {
//        val emailPattern = Regex("[a-zA-Z\\d._-]+@[a-z]+\\.+[a-z]+")
//        return when {
//
//            email.trim().isEmpty() -> {
//                ErrorStatus(true, UiText.StringResource(R.string.required))
//            }
//
//            !email.trim().matches(emailPattern) -> {
//                ErrorStatus(true, UiText.StringResource(R.string.valid_e_mail))
//            }
//
//            else -> {
//                ErrorStatus(false)
//            }
//        }
//    }

    private val _userPassword = MutableStateFlow("")
    val userPassword: StateFlow<String> = _userPassword
    fun onUserPasswordChanged(value: String){
        _userPassword.value = value
    }

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword
    fun onConfirmPasswordChanged(value: String){
        _confirmPassword.value = value
    }

    private val _arePasswordsVisible = MutableStateFlow(false)
    val arePasswordsVisible: StateFlow<Boolean> = _arePasswordsVisible
    fun togglePasswordsVisibility() {
        _arePasswordsVisible.value = !_arePasswordsVisible.value
    }

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn = _isSignedIn

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn
}