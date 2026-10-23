package com.mhq.fynecast.auth.ui.signup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialTokenUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.RegisterWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateSignupFormUseCase
import com.mhq.fynecast.auth.ui.toUiText
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.core.ui.text.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

class SignupViewModel(
    private val validateSignupFormUseCase: ValidateSignupFormUseCase,
    private val registerWithEmailUseCase: RegisterWithEmailUseCase,
    private val authenticateWithSocialTokenUseCase: AuthenticateWithSocialTokenUseCase
) : ViewModel() {

    private val _signupUiState = MutableStateFlow<SignupUiState>(SignupUiState.Idle)
    private val _uiEventChannel = Channel<UiText>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()
    private val _navigationEvent = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

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
        _userEmail.value = newEmail.trim()
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

    val doPasswordsMatch = combine(_userPassword, _confirmPassword) {
        password, confirmedPassword ->  validateSignupFormUseCase.validatePasswordMatch(
        password,
        confirmedPassword
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    fun validateThenRegister() {
        val state = _signupUiState.value
        if (state is SignupUiState.Loading || state is SignupUiState.Success) return
        val validation = validateSignupFormUseCase(
            username = _username.value,
            email = _userEmail.value,
            password = _userPassword.value,
            confirmPassword = _confirmPassword.value
        )
        if (validation is ValidationResult.Failure) {
            viewModelScope.launch { _uiEventChannel.send(validation.error.toUiText()) }
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
                    _uiEventChannel.send(UiText.res(R.string.msg_account_created))
                    delay(SUCCESS_MESSAGE_DISPLAY_DELAY_MS.milliseconds)
                    _navigationEvent.send(Unit)
                }
                .onFailure { error ->
                    val msg = error.toUiText(R.string.error_signup_failed)
                    _signupUiState.value = SignupUiState.Error(msg)
                    _uiEventChannel.send(msg)
                }
        }
    }

    suspend fun onGoogleAuthenticate(fetchToken: suspend () -> String) =
        runSocialSignIn(
            UiText.res(R.string.error_google_sign_in_failed),
            isGoogle = true,
            fetchToken
        )

    suspend fun onFacebookAuthenticate(fetchToken: suspend () -> String) =
        runSocialSignIn(
            UiText.res(R.string.error_facebook_sign_in_failed),
            isGoogle = false,
            fetchToken
        )

    private suspend fun runSocialSignIn(
        failureMessage: UiText,
        isGoogle: Boolean,
        fetchToken: suspend () -> String
    ) {
        val state = _signupUiState.value
        if (state is SignupUiState.Loading || state is SignupUiState.Success) return
        _signupUiState.value = SignupUiState.Loading
        try {
            val result = try {
                authenticateWithSocialTokenUseCase(fetchToken(), isGoogle = isGoogle)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            result
                .onSuccess {
                    _signupUiState.value = SignupUiState.Success
                    _navigationEvent.send(Unit)
                }
                .onFailure {
                    _signupUiState.value = SignupUiState.Idle
                    _uiEventChannel.send(failureMessage)
                }
        } finally {
            if (_signupUiState.value is SignupUiState.Loading) _signupUiState.value =
                SignupUiState.Idle
        }
    }

    companion object {
        private const val SUCCESS_MESSAGE_DISPLAY_DELAY_MS = 1500L
    }
}