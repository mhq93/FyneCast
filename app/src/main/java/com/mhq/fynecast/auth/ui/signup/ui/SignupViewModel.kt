package com.mhq.fynecast.auth.ui.signup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialTokenUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.RegisterWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateSignupFormUseCase
import com.mhq.fynecast.auth.ui.toUiText
import com.mhq.fynecast.core.ui.text.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    /** Pure checks used by the UI to show field errors while typing. */
    fun isUsernameValid(username: String): Boolean =
        validateSignupFormUseCase.validateUsername(username)

    fun isEmailValid(email: String): Boolean =
        validateSignupFormUseCase.validateEmail(email)

    fun isPasswordValid(password: String): Boolean =
        validateSignupFormUseCase.validatePassword(password)

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean =
        validateSignupFormUseCase.validatePasswordMatch(password, confirmPassword)

    fun onSignupAction(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        val state = _signupUiState.value
        if (state is SignupUiState.Loading || state is SignupUiState.Success) return
        val validation = validateSignupFormUseCase(
            username = username,
            email = email,
            password = password,
            confirmPassword = confirmPassword
        )
        if (validation is ValidationResult.Failure) {
            viewModelScope.launch { _uiEventChannel.send(validation.error.toUiText()) }
            return
        }
        viewModelScope.launch {
            _signupUiState.value = SignupUiState.Loading
            registerWithEmailUseCase(
                username = username,
                email = email,
                password = password
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