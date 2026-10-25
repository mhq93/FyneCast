package com.mhq.fynecast.auth.presentation.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.usecases.auth.SendPasswordResetUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.auth.presentation.mapper.toUiText
import com.mhq.fynecast.core.presentation.text.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    /** Pure check used by the UI to show the field error while typing. */
    fun isEmailValid(email: String): Boolean =
        validateLoginFormUseCase.validateEmailFormat(email)

    fun onPasswordResetAction(email: String) {
        val state = _forgotPasswordUiState.value
        if (state is ForgotPasswordUiState.Loading || state is ForgotPasswordUiState.Success) return
        viewModelScope.launch {
            _forgotPasswordUiState.value = ForgotPasswordUiState.Loading
            sendPasswordResetUseCase(email)
                .onSuccess {
                    _forgotPasswordUiState.value = ForgotPasswordUiState.Success
                    _uiEventChannel.send(UiText.res(R.string.msg_reset_link_sent, email))
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