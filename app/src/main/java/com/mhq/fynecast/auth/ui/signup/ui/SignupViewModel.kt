package com.mhq.fynecast.auth.ui.signup.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialProviderUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.RegisterWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateSignupFormUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SignupViewModel(
    private val validateSignupFormUseCase: ValidateSignupFormUseCase,
    private val registerWithEmailUseCase: RegisterWithEmailUseCase,
    private val authenticateWithSocialProviderUseCase: AuthenticateWithSocialProviderUseCase
) : ViewModel() {

    private val _signupUiState = MutableStateFlow<SignupUiState>(SignupUiState.Idle)
    private val _uiEventChannel = Channel<String>()
    val uiEvent = _uiEventChannel.receiveAsFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()
    fun onUsernameChanged(newUsername: String) {
        _username.value = newUsername
    }
    val isUsernameValid = _username
        .map { validateSignupFormUseCase.validateUsername(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    private val _userEmail = MutableStateFlow("")
    val userEmail = _userEmail.asStateFlow()
    fun onUserEmailChanged(newEmail: String) {
        _userEmail.value = newEmail
    }
    val isEmailValid = _userEmail
        .map { validateSignupFormUseCase.validateEmail(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    private val _userPassword = MutableStateFlow("")
    val userPassword = _userPassword.asStateFlow()
    fun onUserPasswordChanged(newPassword: String) {
        _userPassword.value = newPassword
    }
    val isPasswordValid = _userPassword
        .map { validateSignupFormUseCase.validatePassword(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword = _confirmPassword.asStateFlow()
    fun onConfirmPasswordChanged(newConfirmedPassword: String) {
        _confirmPassword.value = newConfirmedPassword
    }

    private val _arePasswordsVisible = MutableStateFlow(false)
    val arePasswordsVisible = _arePasswordsVisible.asStateFlow()
    fun togglePasswordsVisibility() {
        _arePasswordsVisible.value = !_arePasswordsVisible.value
    }

    val doPasswordsMatch = combine(_userPassword, _confirmPassword) { password, confirmedPassword ->
        validateSignupFormUseCase.validatePasswordMatch(password, confirmedPassword)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    fun validateThenRegister(onSuccess: () -> Unit) {
        if (_signupUiState.value is SignupUiState.Loading) return

        val validation = validateSignupFormUseCase(
            username = _username.value,
            email = _userEmail.value,
            password = _userPassword.value,
            confirmPassword = _confirmPassword.value
        )

        if (validation is ValidationResult.Failure) {
            viewModelScope.launch {
                _uiEventChannel.send(validation.errorMessage)
            }
            return
        }

        viewModelScope.launch {
            _signupUiState.value = SignupUiState.Loading
            registerWithEmailUseCase(
                username = _username.value,
                email = _userEmail.value,
                password = _userPassword.value
            )
                .onSuccess {
                    _signupUiState.value = SignupUiState.Success
                    _uiEventChannel.send("Account created successfully.")
                    onSuccess()
                }
                .onFailure { error ->
                    val msg = error.localizedMessage ?: "An unexpected authentication error occurred."
                    _signupUiState.value = SignupUiState.Error(msg)
                    _uiEventChannel.send(msg)
                }
        }
    }

    fun onGoogleAuthenticate(
        context: Context,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            authenticateWithSocialProviderUseCase.withGoogle(context)
                .onSuccess { onSuccess() }
                .onFailure { _uiEventChannel.send("Google sign-in failed") }
        }
    }

    fun onFacebookAuthenticate(
        activity: ComponentActivity,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            authenticateWithSocialProviderUseCase.withFacebook(activity)
                .onSuccess { onSuccess() }
                .onFailure { _uiEventChannel.send("Facebook sign-in failed") }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                SignupViewModel(
                    validateSignupFormUseCase = app.container.validateSignupFormUseCase,
                    registerWithEmailUseCase = app.container.registerWithEmailUseCase,
                    authenticateWithSocialProviderUseCase = app.container.authenticateWithSocialProviderUseCase
                )
            }
        }
    }
}