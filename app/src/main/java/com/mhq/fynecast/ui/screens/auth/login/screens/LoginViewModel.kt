package com.mhq.fynecast.registeration.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LoginViewModel : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible: StateFlow<Boolean> = _passwordVisible.asStateFlow()

    private val emailRegex = Regex("[a-zA-Z\\d._-]+@[a-z]+\\.+[a-z]+")

    // Reactive email string check
    val isEmailValid: StateFlow<Boolean> = _email
        .map { email -> email.isEmpty() || email.matches(emailRegex) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Unlocks button inputs automatically once text entries are populated
    val isLoginEnabled: StateFlow<Boolean> = combine(_email, _password) { e, p ->
        e.matches(emailRegex) && p.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Form evaluation safety trigger
    fun getFormValidationError(): String? {
        return if (!_email.value.matches(emailRegex)) "Please enter a valid email address." else null
    }

    // State Mutation Methods
    fun onEmailChanged(value: String) { _email.value = value.trim() }
    fun onPasswordChanged(value: String) { _password.value = value }
    fun togglePasswordVisibility() { _passwordVisible.value = !_passwordVisible.value }
}
