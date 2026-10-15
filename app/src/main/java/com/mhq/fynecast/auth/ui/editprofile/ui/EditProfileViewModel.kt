package com.mhq.fynecast.auth.ui.editprofile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.auth.domain.models.ValidationResult
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.UpdateUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ValidateEditProfileUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
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

class EditProfileViewModel(
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val validateEditProfileUseCase: ValidateEditProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : ViewModel() {

    private val _editProfileUiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)

    private val _uiEventChannel = Channel<String>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()

    private val _navigationEventChannel = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEventChannel.receiveAsFlow()

    private val _profileImageUri = MutableStateFlow<String?>(null)
    val profileImageUri = _profileImageUri.asStateFlow()
    fun onProfileImagePicked(uriString: String) {
        _profileImageUri.value = uriString
    }

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()
    fun onUsernameChanged(newUsername: String) {
        _username.value = newUsername
    }
    val isUsernameValid = _username
        .map { validateEditProfileUseCase.validateUsernameFormat(it) }
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
        .map { validateEditProfileUseCase.validateEmailFormat(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    init {
        viewModelScope.launch {
            observeUserProfileUseCase().collect { profile ->
                _username.value = profile.name
                _userEmail.value = profile.email
                _profileImageUri.value = profile.profileImageUri
            }
        }
    }

    fun handleSavingProfileChanges() {
        when (
            val validation = validateEditProfileUseCase(
                _username.value,
                _userEmail.value
            )
        ) {
            is ValidationResult.Success -> {
                if (_editProfileUiState.value is EditProfileUiState.Loading) return
                _editProfileUiState.value = EditProfileUiState.Loading

                viewModelScope.launch {
                    _uiEventChannel.send("Processing image and saving changes...")

                    updateUserProfileUseCase(
                        newName = _username.value,
                        newEmail = _userEmail.value,
                        localImageUriString = _profileImageUri.value
                    )
                        .onSuccess {
                            _editProfileUiState.value = EditProfileUiState.Success
                            _uiEventChannel.send("Profile changes saved successfully!")
                            delay(SUCCESS_MESSAGE_DISPLAY_DELAY_MS.milliseconds)
                            _navigationEventChannel.send(Unit)
                        }
                        .onFailure { e ->
                            val fallbackMessage = e.localizedMessage ?: "Failed to update profile settings."
                            _editProfileUiState.value = EditProfileUiState.Error(fallbackMessage)
                            _uiEventChannel.send(fallbackMessage)
                        }
                }
            }

            is ValidationResult.Failure -> {
                viewModelScope.launch { _uiEventChannel.send(validation.errorMessage) }
            }
        }
    }

    companion object {
        private const val SUCCESS_MESSAGE_DISPLAY_DELAY_MS = 1500L

        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                EditProfileViewModel(
                    observeUserProfileUseCase = app.container.observeUserProfileUseCase,
                    validateEditProfileUseCase = app.container.validateEditProfileUseCase,
                    updateUserProfileUseCase = app.container.updateUserProfileUseCase
                )
            }
        }
    }
}