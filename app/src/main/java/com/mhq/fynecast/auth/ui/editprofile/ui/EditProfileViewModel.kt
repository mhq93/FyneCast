package com.mhq.fynecast.auth.ui.editprofile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.UpdateUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ValidateEditProfileUseCase
import com.mhq.fynecast.auth.ui.toUiText
import com.mhq.fynecast.core.ui.text.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class EditProfileViewModel(
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val validateEditProfileUseCase: ValidateEditProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : ViewModel() {

    private val _editProfileUiState =
        MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)

    private val _uiEventChannel = Channel<UiText>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()

    private val _navigationEventChannel = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEventChannel.receiveAsFlow()

    /**
     * The stored profile, emitted once it is loaded (null until then).
     * The UI copies it into its own fields one time; later refreshes of the profile
     * never overwrite what the user is typing.
     */
    val initialProfile: StateFlow<UserDomainModel?> = flow {
        emit(observeUserProfileUseCase().first { it.id.isNotEmpty() })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Pure checks used by the UI to show field errors while typing. */
    fun isUsernameValid(username: String): Boolean =
        validateEditProfileUseCase.validateUsernameFormat(username)

    fun isEmailValid(email: String): Boolean =
        validateEditProfileUseCase.validateEmailFormat(email)

    fun onSaveProfileAction(username: String, email: String, profileImageUri: String?) {
        when (val validation = validateEditProfileUseCase(username, email)) {
            is ValidationResult.Success -> {
                if (_editProfileUiState.value is EditProfileUiState.Loading) return
                _editProfileUiState.value = EditProfileUiState.Loading

                viewModelScope.launch {
                    _uiEventChannel.send(UiText.res(R.string.msg_profile_processing))

                    updateUserProfileUseCase(
                        newName = username,
                        newEmail = email,
                        localImageUriString = profileImageUri
                    )
                        .onSuccess { verificationSent ->
                            _editProfileUiState.value = EditProfileUiState.Success
                            _uiEventChannel.send(
                                UiText.res(
                                    if (verificationSent) R.string.msg_profile_saved_verify_email
                                    else R.string.msg_profile_saved
                                )
                            )
                            delay(SUCCESS_MESSAGE_DISPLAY_DELAY_MS.milliseconds)
                            _navigationEventChannel.send(Unit)
                        }
                        .onFailure { e ->
                            val message = e.toUiText(R.string.error_profile_update_failed)
                            _editProfileUiState.value = EditProfileUiState.Error(message)
                            _uiEventChannel.send(message)
                        }
                }
            }

            is ValidationResult.Failure -> {
                viewModelScope.launch { _uiEventChannel.send(validation.error.toUiText()) }
            }
        }
    }

    companion object {
        private const val SUCCESS_MESSAGE_DISPLAY_DELAY_MS = 1500L
    }
}