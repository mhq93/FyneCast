package com.mhq.fynecast.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.alerts.notifications.domain.usecases.ConsumeNotificationPromptUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.SyncNotificationPermissionUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.settings.domain.models.AppLanguage
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetDarkModeUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetLanguageUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetUnitSystemUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val setUnitSystem: SetUnitSystemUseCase,
    private val setLanguage: SetLanguageUseCase,
    private val setDarkMode: SetDarkModeUseCase,
    private val toggleNotifications: ToggleNotificationsUseCase,
    private val handleNotificationPermissionResult: HandleNotificationPermissionResultUseCase,
    private val syncNotificationPermission: SyncNotificationPermissionUseCase,
    private val consumeNotificationPrompt: ConsumeNotificationPromptUseCase,
    private val logoutUserUseCase: LogoutUserUseCase
) : ViewModel() {

    private val _localeEvent = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val localeEvent = _localeEvent.asSharedFlow()
    private val _requestNotificationPermissionEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestNotificationPermissionEvent = _requestNotificationPermissionEvent.asSharedFlow()
    private val profileFlow = observeUserProfileUseCase()
    private val preferencesFlow = observeUserPreferencesUseCase()
    private val _logoutCompleteChannel = Channel<Unit>(Channel.BUFFERED)
    val logoutComplete = _logoutCompleteChannel.receiveAsFlow()
    private val _logoutFailedChannel = Channel<Unit>(Channel.BUFFERED)
    val logoutFailed = _logoutFailedChannel.receiveAsFlow()
    private var isLoggingOut = false

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

    private val _isMetric = MutableStateFlow(true)
    val isMetric: StateFlow<Boolean> = _isMetric.asStateFlow()

    private val _currentLanguage = MutableStateFlow("English")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    private val _arePreferencesLoaded = MutableStateFlow(false)

    /** True once the first preferences read has finished; the splash screen waits for it. */
    val arePreferencesLoaded: StateFlow<Boolean> = _arePreferencesLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesFlow.collect { prefs ->
                _isMetric.value = prefs.isMetric
                _currentLanguage.value = prefs.currentLanguage
                _isDarkMode.value = prefs.isDarkMode
                // Last, so the splash only lifts once every value above is set.
                _arePreferencesLoaded.value = true
            }
        }
        viewModelScope.launch {
            preferencesFlow
                .map { AppLanguage.fromStoredName(it.currentLanguage).code }
                .distinctUntilChanged()
                .collect { _localeEvent.tryEmit(it) }
        }
    }

    fun toggleUnitSystem(isMetric: Boolean) {
        viewModelScope.launch {
            setUnitSystem(isMetric)
        }
    }

    // notificationsEnabled StateFlow: initial value true → false
    val notificationsEnabled: StateFlow<Boolean> =
        preferencesFlow.map { it.notificationsEnabled }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Called once the location permission flow finishes, whether it was granted or denied. */
    fun requestNotificationPermissionOnFirstLaunch() {
        viewModelScope.launch {
            if (consumeNotificationPrompt()) _requestNotificationPermissionEvent.tryEmit(Unit)
        }
    }

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

    fun toggleLanguage(language: String) {
        viewModelScope.launch { setLanguage(language) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { setDarkMode(enabled) }
    }

    fun logOutUserSession() {
        if (isLoggingOut) return
        isLoggingOut = true
        viewModelScope.launch {
            try {
                logoutUserUseCase().fold(
                    onSuccess = { _logoutCompleteChannel.send(Unit) },
                    onFailure = { _logoutFailedChannel.send(Unit) }
                )
            } finally {
                isLoggingOut = false
            }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)
                val container = app.container
                SettingsViewModel(
                    observeUserProfileUseCase = container.observeUserProfileUseCase,
                    observeUserPreferencesUseCase = container.observeUserPreferencesUseCase,
                    setUnitSystem = container.setUnitSystemUseCase,
                    setLanguage = container.setLanguageUseCase,
                    setDarkMode = container.setDarkModeUseCase,
                    toggleNotifications = container.toggleNotificationsUseCase,
                    handleNotificationPermissionResult = container.handleNotificationPermissionResultUseCase,
                    syncNotificationPermission = container.syncNotificationPermissionUseCase,
                    consumeNotificationPrompt = container.consumeNotificationPromptUseCase,
                    logoutUserUseCase = container.logoutUserUseCase
                )
            }
        }
    }
}