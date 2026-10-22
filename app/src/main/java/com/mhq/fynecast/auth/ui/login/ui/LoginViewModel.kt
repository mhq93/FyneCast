package com.mhq.fynecast.auth.ui.login.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialTokenUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.LoginWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.auth.ui.toUiText
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.core.ui.text.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val validateLoginFormUseCase: ValidateLoginFormUseCase,
    private val authenticateWithSocialTokenUseCase: AuthenticateWithSocialTokenUseCase
) : ViewModel() {

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    private val _uiEventChannel = Channel<UiText>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()
    private val _navigationEvent = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()
    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail.trim()
    }
    val isEmailValid = _email
        .map { validateLoginFormUseCase.validateEmailFormat(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()
    fun onPasswordChanged(newPassword: String) {
        _password.value = newPassword
    }
    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible = _passwordVisible.asStateFlow()
    fun togglePasswordVisibility() {
        _passwordVisible.value = !_passwordVisible.value
    }

    fun validateThenLogin() {
        if (_loginUiState.value is LoginUiState.Loading) return
        when (val validation = validateLoginFormUseCase(_email.value, _password.value)) {
            is ValidationResult.Success -> {
                viewModelScope.launch {
                    _loginUiState.value = LoginUiState.Loading
                    loginWithEmailUseCase(_email.value, _password.value)
                        .onSuccess {
                            _loginUiState.value = LoginUiState.Success
                            _navigationEvent.send(Unit)
                        }
                        .onFailure { handleLoginFailure(it) }
                }
            }
            is ValidationResult.Failure -> {
                viewModelScope.launch { _uiEventChannel.send(validation.error.toUiText()) }
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
        if (_loginUiState.value is LoginUiState.Loading) return
        _loginUiState.value = LoginUiState.Loading
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
                    _loginUiState.value = LoginUiState.Success
                    _navigationEvent.send(Unit)
                }
                .onFailure {
                    _loginUiState.value = LoginUiState.Idle
                    _uiEventChannel.send(failureMessage)
                }
        } finally {
            // Canceled sign-in (user backed out): never stay stuck in Loading.
            if (_loginUiState.value is LoginUiState.Loading) _loginUiState.value = LoginUiState.Idle
        }
    }

    private suspend fun handleLoginFailure(error: Throwable) {
        val message = error.toUiText(R.string.error_login_failed)
        _loginUiState.value = LoginUiState.Error(message)
        _uiEventChannel.send(message)
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                LoginViewModel(
                    loginWithEmailUseCase = app.container.loginWithEmailUseCase,
                    validateLoginFormUseCase = app.container.validateLoginFormUseCase,
                    authenticateWithSocialTokenUseCase = app.container.authenticateWithSocialTokenUseCase                )
            }
        }
    }
}