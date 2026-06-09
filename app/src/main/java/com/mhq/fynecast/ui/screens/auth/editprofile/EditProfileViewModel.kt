package com.mhq.fynecast.ui.screens.auth.editprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.FyneCastApplication
import com.mhq.fynecast.data.repository.UserProfileRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val profileRepository: UserProfileRepository
) : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _profileImageUri = MutableStateFlow<String?>(null)
    val profileImageUri: StateFlow<String?> = _profileImageUri.asStateFlow()

    private val _saveState = MutableStateFlow<EditProfileState>(EditProfileState.Idle)
    val saveState: StateFlow<EditProfileState> = _saveState.asStateFlow()

    // Combined UI channel handling both successful notifications and data validation exceptions
    private val _uiEventChannel = Channel<String>()
    val uiEvent = _uiEventChannel.receiveAsFlow()

    private val usernameRegex = Regex("^[a-zA-Z0-9_]{3,15}$")
    private val emailRegex = Regex("[a-zA-Z\\d._-]+@[a-z]+\\.+[a-z]+")

    init {
        // Architecture Win: Populates the form fields reactively from the source repository state
        viewModelScope.launch {
            profileRepository.profileState.collect { profile ->
                _userName.value = profile.uesrname
                _userEmail.value = profile.email
                _profileImageUri.value = profile.profileImageUri
            }
        }
    }

    // Reactive input formatting status indicators
    val isUsernameValid: StateFlow<Boolean> = _userName
        .map { text -> text.isEmpty() || text.matches(usernameRegex) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isEmailValid: StateFlow<Boolean> = _userEmail
        .map { email -> email.isEmpty() || email.matches(emailRegex) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Button remains unlocked continuously to ensure user always gets direct warning prompts on click
    val isSaveEnabled: StateFlow<Boolean> = _saveState
        .map { state -> state !is EditProfileState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // UI Field state mutation callbacks
    fun onUsernameChanged(value: String) { _userName.value = value }
    fun onUserEmailChanged(value: String) { _userEmail.value = value.trim() }
    fun onProfileImagePicked(uriString: String) { _profileImageUri.value = uriString }

    // Explicit evaluator function supplying clear user UI warning strings
    fun getFormValidationError(): String? {
        return when {
            _userName.value.isBlank() -> "Name field cannot be left empty."
            !_userName.value.matches(usernameRegex) -> "Username must be 3-15 characters without spaces or special symbols."
            _userEmail.value.isBlank() -> "Email field cannot be left empty."
            !_userEmail.value.matches(emailRegex) -> "Please enter a valid email address."
            else -> null
        }
    }

    fun saveProfileChanges() {
        val validationError = getFormValidationError()
        if (validationError != null) {
            viewModelScope.launch { _uiEventChannel.send(validationError) }
            return
        }

        viewModelScope.launch {
            _saveState.value = EditProfileState.Loading
            try {
                _uiEventChannel.send("Uploading image and saving changes...")

                // Suspends over the thread pool while uploading to Firebase Storage and updating states
                profileRepository.updateProfile(
                    newName = _userName.value,
                    newEmail = _userEmail.value,
                    localImageUriString = _profileImageUri.value
                )

                _saveState.value = EditProfileState.Success
                _uiEventChannel.send("Profile changes saved successfully!")
            } catch (e: Exception) {
                val fallbackMessage = e.localizedMessage ?: "Failed to update profile settings."
                _saveState.value = EditProfileState.Error(fallbackMessage)
                _uiEventChannel.send(fallbackMessage)
            }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // Adjust FyneCastApplication to match your actual main Application file name
                val application = (this[APPLICATION_KEY] as FyneCastApplication)
                EditProfileViewModel(
                    profileRepository = application.container.userProfileRepository
                )
            }
        }
    }
}