package com.mhq.fynecast.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.SyncNotificationPermissionUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeUserProfile: ObserveUserProfileUseCase,
    observeUserPreferences: ObserveUserPreferencesUseCase,
    private val updatePreference: UpdatePreferenceUseCase,
    private val toggleNotifications: ToggleNotificationsUseCase,
    private val handleNotificationPermissionResult: HandleNotificationPermissionResultUseCase,
    private val syncNotificationPermission: SyncNotificationPermissionUseCase,
    private val logoutUser: LogoutUserUseCase
) : ViewModel() {

    private val _localeEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val localeEvent = _localeEvent.asSharedFlow()
    private val _requestNotificationPermissionEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestNotificationPermissionEvent = _requestNotificationPermissionEvent.asSharedFlow()
    private val profileFlow = observeUserProfile()
    private val preferencesFlow = observeUserPreferences()

    val profileName: StateFlow<String> =
        profileFlow.map { it.name }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                "Username"
            )

    val profileEmail: StateFlow<String> =
        profileFlow.map { it.email }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                "username@gmail.com"
            )

    val profileImageUri: StateFlow<String?> =
        profileFlow.map { it.profileImageUri }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    val isMetric: StateFlow<Boolean> =
        preferencesFlow.map { it.isMetric }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                true
            )

    fun toggleUnitSystem(isMetric: Boolean) {
        viewModelScope.launch {
            updatePreference.metricSystem(isMetric)
        }
    }

    val notificationsEnabled: StateFlow<Boolean> =
        preferencesFlow.map { it.notificationsEnabled }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                true
            )

    fun toggleNotifications(wantsEnabled: Boolean) {
        viewModelScope.launch {
            when (toggleNotifications.invoke(wantsEnabled)) {
                ToggleNotificationsUseCase.Result.PermissionRequired ->
                    _requestNotificationPermissionEvent.tryEmit(Unit)
                else -> Unit
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            handleNotificationPermissionResult(granted)
        }
    }

    fun syncNotificationPermissionState() {
        viewModelScope.launch {
            syncNotificationPermission()
        }
    }

    val currentLanguage: StateFlow<String> =
        preferencesFlow.map { it.currentLanguage }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                "English"
            )

    fun toggleLanguage(language: String) {
        viewModelScope.launch { updatePreference.language(language) }
    }

    val isDarkMode: StateFlow<Boolean> =
        preferencesFlow.map { it.isDarkMode }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                true
            )

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { updatePreference.darkMode(enabled) }
    }

    fun logOutUserSession() {
        viewModelScope.launch {
            logoutUser()
        }
    }

    init {
        viewModelScope.launch {
            currentLanguage.collect { language ->
                _localeEvent.tryEmit(if (language == "Arabic") "ar" else "en")
            }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)
                val container = app.container
                SettingsViewModel(
                    observeUserProfile = container.observeUserProfileUseCase,
                    observeUserPreferences = container.observeUserPreferencesUseCase,
                    updatePreference = container.updatePreferenceUseCase,
                    toggleNotifications = container.toggleNotificationsUseCase,
                    handleNotificationPermissionResult = container.handleNotificationPermissionResultUseCase,
                    syncNotificationPermission = container.syncNotificationPermissionUseCase,
                    logoutUser = container.logoutUserUseCase
                )
            }
        }
    }
}