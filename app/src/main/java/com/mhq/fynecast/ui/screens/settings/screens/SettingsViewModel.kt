package com.mhq.fynecast.ui.screens.settings.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.data.repository.AppRepository
import com.mhq.fynecast.FyneCastApplication
import com.mhq.fynecast.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: AppRepository,
    private val profileRepository: UserProfileRepository
) : ViewModel() {

    // Listens reactively to changes pushed from EditProfile
    val profileName: StateFlow<String> = profileRepository.profileState
        .map { it.uesrname }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Username")

    val profileEmail: StateFlow<String> = profileRepository.profileState
        .map { it.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "username@gmail.com")

    val profileImageUri: StateFlow<String?> = profileRepository.profileState
        .map { it.profileImageUri }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 2. All 4 Segmented button reactive preferences configurations state flows
    private val _isMetric = MutableStateFlow(true) // Metric (true) vs Imperial (false)
    val isMetric: StateFlow<Boolean> = _isMetric.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true) // On (true) vs Off (false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _currentLanguage = MutableStateFlow("English") // Arabic vs English
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true) // Light (false) vs Dark (true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        loadSavedUserPreferences()
    }

    private fun loadSavedUserPreferences() {
        viewModelScope.launch {
            // Asynchronously fetch preferences values from local storage repository layer on launch
        }
    }

    // 3. Thread-safe preference mutation triggers
    fun toggleUnitSystem(isMetric: Boolean) {
        viewModelScope.launch {
            _isMetric.value = isMetric
            // TODO: Persist state update using DataStore or SharedPreferences
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _notificationsEnabled.value = enabled
            // TODO: Persist state update using DataStore or SharedPreferences
        }
    }

    private val _localeEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val localeEvent = _localeEvent.asSharedFlow()

    fun toggleLanguage(language: String) {
        //Log.d("Settings", "toggleLanguage called: $language")
        _currentLanguage.value = language
        _localeEvent.tryEmit(if (language == "Arabic") "ar" else "en")
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            _isDarkMode.value = enabled
            // TODO: Persist state update using DataStore or SharedPreferences
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FyneCastApplication)
                SettingsViewModel(
                    repository = application.container.appRepository,
                    profileRepository = application.container.userProfileRepository
                )
            }
        }
    }
}