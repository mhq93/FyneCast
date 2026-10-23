package com.mhq.fynecast.auth.ui.forgotpassword.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.usecases.auth.SendPasswordResetUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.auth.ui.toUiText
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.core.ui.text.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ForgotPasswordViewModel(
    private val validateLoginFormUseCase: ValidateLoginFormUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase
) : ViewModel() {

    private val _forgotPasswordUiState =
        MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
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

    fun handlePasswordReset() {
        val state = _forgotPasswordUiState.value
        if (state is ForgotPasswordUiState.Loading || state is ForgotPasswordUiState.Success) return
        viewModelScope.launch {
            _forgotPasswordUiState.value = ForgotPasswordUiState.Loading
            sendPasswordResetUseCase(_email.value)
                .onSuccess {
                    _forgotPasswordUiState.value = ForgotPasswordUiState.Success
                    _uiEventChannel.send(UiText.res(R.string.msg_reset_link_sent, _email.value))
                    delay(SUCCESS_MESSAGE_DISPLAY_DELAY_MS.milliseconds)
                    _navigationEvent.send(Unit)
                }
                .onFailure { error ->
                    val msg = error.toUiText(R.string.error_reset_link_failed)
                    _forgotPasswordUiState.value = ForgotPasswordUiState.Error(msg)
                    _uiEventChannel.send(msg)
                }
        }
    }

    companion object {
        private const val SUCCESS_MESSAGE_DISPLAY_DELAY_MS = 1500L
    }
}