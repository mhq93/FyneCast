package com.mhq.fynecast.registeration.signup

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SignupViewModel : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()
    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()
    private val _userPassword = MutableStateFlow("")
    val userPassword: StateFlow<String> = _userPassword.asStateFlow()
    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()
    private val _arePasswordsVisible = MutableStateFlow(false)
    val arePasswordsVisible: StateFlow<Boolean> = _arePasswordsVisible.asStateFlow()


    // Pure regex validation expressions
    private val usernameRegex = Regex("^[a-zA-Z0-9_]{3,15}$")
    private val emailRegex = Regex("[a-zA-Z\\d._-]+@[a-z]+\\.+[a-z]+")

    // Helper syntax matching UI validation expectations
    fun validateUsername(username: String): Boolean {
        return username.matches(usernameRegex)
    }

    // Reactive validation states transformed cleanly via .map
    val isUsernameValid: StateFlow<Boolean> = _userName
        .map { text -> text.isEmpty() || text.matches(usernameRegex) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isEmailValid: StateFlow<Boolean> = _userEmail
        .map { email -> email.isEmpty() || email.matches(emailRegex) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val doPasswordsMatch: StateFlow<Boolean> = combine(_userPassword, _confirmPassword) { pass, confirm ->
        confirm.isEmpty() || pass == confirm
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSubmitEnabled: StateFlow<Boolean> = combine(
        _userName, _userEmail, _userPassword, _confirmPassword
    ) { u, e, p, c ->
        u.isNotBlank() && e.isNotBlank() && p.isNotBlank() && p == c
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun getFormValidationError(): String? {
        return when {
            !userName.value.matches(usernameRegex) ->
                "Username must be 3-15 characters and contain no spaces or special characters."
            !userEmail.value.matches(emailRegex) ->
                "Please enter a valid email address."
            else -> null
        }
    }

    // Complete safety form gate checker
    //    val isSubmitEnabled: StateFlow<Boolean> = combine(
    //        _userName, _userEmail, _userPassword, _confirmPassword
    //    ) { u, e, p, c ->
    //        u.matches(usernameRegex) && e.matches(emailRegex) && p.isNotBlank() && p == c
    //    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // State Mutation Methods
    fun onUsernameChanged(value: String) { _userName.value = value }
    fun onUserEmailChanged(value: String) { _userEmail.value = value }
    fun onUserPasswordChanged(value: String) { _userPassword.value = value }
    fun onConfirmPasswordChanged(value: String) { _confirmPassword.value = value }
    fun togglePasswordsVisibility() { _arePasswordsVisible.value = !_arePasswordsVisible.value }
}