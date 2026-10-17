package com.mhq.fynecast.auth.ui.login.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.auth.data.usecases.auth.AuthenticateWithSocialProviderUseCase
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.auth.LoginWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(
    private val validateLoginFormUseCase: ValidateLoginFormUseCase,
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val authenticateWithSocialProviderUseCase: AuthenticateWithSocialProviderUseCase
) : ViewModel() {

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginUiState = _loginUiState.asStateFlow()
    private val _uiEventChannel = Channel<String>(Channel.BUFFERED)
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
                viewModelScope.launch { _uiEventChannel.send(validation.errorMessage) }
            }
        }
    }

    suspend fun onGoogleAuthenticate(context: Context) =
        runSocialSignIn("Google sign-in failed") {
            authenticateWithSocialProviderUseCase.withGoogle(context)
        }

    suspend fun onFacebookAuthenticate(activity: ComponentActivity) =
        runSocialSignIn("Facebook sign-in failed") {
            authenticateWithSocialProviderUseCase.withFacebook(activity)
        }

    private suspend fun runSocialSignIn(
        failureMessage: String,
        signIn: suspend () -> Result<UserDomainModel>
    ) {
        if (_loginUiState.value is LoginUiState.Loading) return
        _loginUiState.value = LoginUiState.Loading
        try {
            signIn()
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
        val message = error.localizedMessage ?: "Invalid email or password configuration."
        _loginUiState.value = LoginUiState.Error(message)
        _uiEventChannel.send(message)
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                LoginViewModel(
                    validateLoginFormUseCase = app.container.validateLoginFormUseCase,
                    loginWithEmailUseCase = app.container.loginWithEmailUseCase,
                    authenticateWithSocialProviderUseCase = app.container.authenticateWithSocialProviderUseCase
                )
            }
        }
    }
}