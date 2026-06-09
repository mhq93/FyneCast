package com.mhq.fynecast.ui.screens.auth.forgotpassword.screens

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

class ForgotPasswordViewModel : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _resetState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val resetState: StateFlow<ForgotPasswordState> = _resetState.asStateFlow()

    // Combined UI channel handling both success notification and validation exceptions
    private val _uiEventChannel = Channel<String>()
    val uiEvent = _uiEventChannel.receiveAsFlow()

    private val emailRegex = Regex("[a-zA-Z\\d._-]+@[a-z]+\\.+[a-z]+")

    // Reactive email validation flag
    val isEmailValid: StateFlow<Boolean> = _email
        .map { email -> email.isEmpty() || email.matches(emailRegex) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Active at all times unless the background Firebase link process is loading
    val isSubmitEnabled: StateFlow<Boolean> = _resetState
        .map { state -> state !is ForgotPasswordState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun onEmailChanged(value: String) {
        _email.value = value.trim()
    }

    // Input verification check returning errors directly
    fun getFormValidationError(): String? {
        return when {
            _email.value.isBlank() -> "Email cannot be empty."
            !_email.value.matches(emailRegex) -> "Please enter a valid email address."
            else -> null
        }
    }

    // Triggers the password reset execution flow asynchronously via Firebase
    fun sendPasswordReset(onSuccess: () -> Unit) {
        val error = getFormValidationError()
        if (error != null) {
            viewModelScope.launch { _uiEventChannel.send(error) }
            return
        }

        viewModelScope.launch {
            _resetState.value = ForgotPasswordState.Loading
            try {
                // Sends an automated password change link securely from your console setup
                firebaseAuth.sendPasswordResetEmail(_email.value).await()
                _resetState.value = ForgotPasswordState.Success

                _uiEventChannel.send("Reset link successfully sent to ${_email.value}")
                onSuccess()
            } catch (e: Exception) {
                val failureMessage = e.localizedMessage ?: "Failed to send reset link. Try again."
                _resetState.value = ForgotPasswordState.Error(failureMessage)
                _uiEventChannel.send(failureMessage)
            }
        }
    }
}