package com.mhq.fynecast.ui.screens.auth.login.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible: StateFlow<Boolean> = _passwordVisible.asStateFlow()

    // Firebase authentication execution state tracking
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // One-time UI Channel trigger for snackbar error events
    private val _errorChannel = Channel<String>()
    val errorEvent = _errorChannel.receiveAsFlow()

    private val emailRegex = Regex("[a-zA-Z\\d._-]+@[a-z]+\\.+[a-z]+")

    // Reactive email string check
    val isEmailValid: StateFlow<Boolean> = _email
        .map { email -> email.isEmpty() || email.matches(emailRegex) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // The button stays active at all times unless an active login request is running
    val isLoginEnabled: StateFlow<Boolean> = _loginState
        .map { state -> state !is LoginState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Form evaluation safety trigger checking everything thoroughly on click
    fun getFormValidationError(): String? {
        return when {
            _email.value.isBlank() -> "Email field cannot be empty."
            !_email.value.matches(emailRegex) -> "Please, enter a valid email address."
            _password.value.isBlank() -> "Password field cannot be empty."
            else -> null
        }
    }

    // Safety gate validating entries before launching Firebase auth tasks
    fun validateAndLogin(onSuccess: () -> Unit) {
        val validationError = getFormValidationError()
        if (validationError != null) {
            viewModelScope.launch { _errorChannel.send(validationError) }
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // Suspends safely over the network until Firebase responds
                firebaseAuth.signInWithEmailAndPassword(_email.value, _password.value).await()
                _loginState.value = LoginState.Success
                onSuccess()
            } catch (e: Exception) {
                val failureMessage = e.localizedMessage ?: "Invalid email or password."
                _loginState.value = LoginState.Error(failureMessage)
                _errorChannel.send(failureMessage)
            }
        }
    }

    // State Mutation Methods
    fun onEmailChanged(value: String) {
        _email.value = value.trim()
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
    }

    fun togglePasswordVisibility() {
        _passwordVisible.value = !_passwordVisible.value
    }
}