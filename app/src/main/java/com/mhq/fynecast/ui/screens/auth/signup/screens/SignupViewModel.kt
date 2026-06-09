package com.mhq.fynecast.ui.screens.auth.signup.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignupViewModel : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()

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

    // Firebase registration state handle
    private val _signupState = MutableStateFlow<SignupState>(SignupState.Idle)
    val signupState: StateFlow<SignupState> = _signupState.asStateFlow()

    // One-time UI channel for snackbar validations without polluting persistent states
    private val _validationErrorChannel = Channel<String>()
    val validationErrorEvent = _validationErrorChannel.receiveAsFlow()

    private val usernameRegex = Regex("^[a-zA-Z0-9_]{3,15}$")
    private val emailRegex = Regex("[a-zA-Z\\d._-]+@[a-z]+\\.+[a-z]+")

    val isUsernameValid: StateFlow<Boolean> = _userName
        .map { text -> text.isEmpty() || text.matches(usernameRegex) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isEmailValid: StateFlow<Boolean> = _userEmail
        .map { email -> email.isEmpty() || email.matches(emailRegex) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val doPasswordsMatch: StateFlow<Boolean> = combine(_userPassword, _confirmPassword) { pass, confirm ->
        confirm.isEmpty() || pass == confirm
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Keep the button clickable at all times unless an active Firebase request is loading
    val isSubmitEnabled: StateFlow<Boolean> = _signupState
        .map { state -> state !is SignupState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Update validation logic to check every field thoroughly on click
    fun validateAndProceed(): Boolean {
        val error = when {
            _userName.value.isBlank() -> "Name field cannot be empty."
            !_userName.value.matches(usernameRegex) -> "Name must be 3-15 characters without spaces or special characters."
            _userEmail.value.isBlank() -> "Email field cannot be empty."
            !_userEmail.value.matches(emailRegex) -> "Please, enter a valid email address."
            _userPassword.value.isBlank() -> "Password field cannot be empty."
            _userPassword.value.length < 6 -> "Password must be at least 6 characters long."
            _confirmPassword.value.isBlank() -> "Please, confirm your password."
            _userPassword.value != _confirmPassword.value -> "Passwords do not match."
            else -> null
        }

        if (error != null) {
            viewModelScope.launch { _validationErrorChannel.send(error) }
            return false
        }
        return true
    }

    // Firebase Sign Up backend implementation
    fun registerWithFirebase(onSuccess: () -> Unit) {
        if (!validateAndProceed()) return

        viewModelScope.launch {
            _signupState.value = SignupState.Loading
            try {
                // Suspends safely until network request answers
                firebaseAuth.createUserWithEmailAndPassword(_userEmail.value, _userPassword.value).await()

                _signupState.value = SignupState.Success
                onSuccess()
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "An unexpected authentication error occurred."
                _signupState.value = SignupState.Error(errorMessage)
                _validationErrorChannel.send(errorMessage) // Surface server exception down to snackbar
            }
        }
    }

    fun onUsernameChanged(value: String) { _userName.value = value }
    fun onUserEmailChanged(value: String) { _userEmail.value = value }
    fun onUserPasswordChanged(value: String) { _userPassword.value = value }
    fun onConfirmPasswordChanged(value: String) { _confirmPassword.value = value }
    fun togglePasswordsVisibility() { _arePasswordsVisible.value = !_arePasswordsVisible.value }
}