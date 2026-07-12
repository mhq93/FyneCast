package com.mhq.fynecast.auth.ui.forgotpassword.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.auth.domain.usecases.auth.SendPasswordResetUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.auth.ui.signup.ui.SignupUiState
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val validateLoginFormUseCase: ValidateLoginFormUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase
) : ViewModel() {

    private val _forgotPasswordUiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    private val _uiEventChannel = Channel<String>()
    val uiEvent = _uiEventChannel.receiveAsFlow()

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

    fun handlePasswordReset(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _forgotPasswordUiState.value = ForgotPasswordUiState.Loading
            sendPasswordResetUseCase(_email.value)
                .onSuccess {
                    _forgotPasswordUiState.value = ForgotPasswordUiState.Success
                    _uiEventChannel.send("Reset link successfully sent to ${_email.value}")
                    onSuccess()
                }
                .onFailure { error ->
                    val msg = error.localizedMessage ?: "Failed to send reset link. Try again."
                    _forgotPasswordUiState.value = ForgotPasswordUiState.Error(msg)
                    _uiEventChannel.send(msg)
                }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                ForgotPasswordViewModel(
                    validateLoginFormUseCase = app.container.validateLoginFormUseCase,
                    sendPasswordResetUseCase = app.container.sendPasswordResetUseCase
                )
            }
        }
    }
}