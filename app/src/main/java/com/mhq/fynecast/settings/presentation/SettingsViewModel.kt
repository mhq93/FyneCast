package com.mhq.fynecast.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.alerts.notifications.domain.usecases.ConsumeNotificationPromptUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.SyncNotificationPermissionUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetDarkModeUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetLanguageUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetUnitSystemUseCase
import com.mhq.fynecast.settings.presentation.mapper.toDomain
import com.mhq.fynecast.settings.presentation.mapper.toPresentation
import com.mhq.fynecast.settings.presentation.model.LanguageOption
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
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

    private val profileFlow = observeUserProfileUseCase().filter { it.id.isNotEmpty() }
    private val preferencesFlow = observeUserPreferencesUseCase()
    private val _requestNotificationPermissionEvent = Channel<Unit>(Channel.BUFFERED)
    val requestNotificationPermissionEvent = _requestNotificationPermissionEvent.receiveAsFlow()
    private var isAwaitingTogglePermission = false
    private val _notificationsBlockedChannel = Channel<Unit>(Channel.BUFFERED)
    val notificationsBlocked = _notificationsBlockedChannel.receiveAsFlow()
    private val _logoutCompleteChannel = Channel<Unit>(Channel.BUFFERED)
    val logoutComplete = _logoutCompleteChannel.receiveAsFlow()
    private val _logoutFailedChannel = Channel<Unit>(Channel.BUFFERED)
    val logoutFailed = _logoutFailedChannel.receiveAsFlow()
    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()
    private val _isMetric = MutableStateFlow(true)
    val isMetric: StateFlow<Boolean> = _isMetric.asStateFlow()
    private val _currentLanguage = MutableStateFlow(LanguageOption.English)
    val currentLanguage: StateFlow<LanguageOption> = _currentLanguage.asStateFlow()
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()
    private val _arePreferencesLoaded = MutableStateFlow(false)
    /** True once the first preferences read has finished; the splash screen waits for it. */
    val arePreferencesLoaded: StateFlow<Boolean> = _arePreferencesLoaded.asStateFlow()

    val profileName: StateFlow<String> =
        profileFlow.map { it.name }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                "Username"
            )

    val profileEmail: StateFlow<String> =
        profileFlow.map { it.email }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                "username@gmail.com"
            )

    val profileImageUri: StateFlow<String?> =
        profileFlow.map { it.profileImageUri }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                null
            )

    init {
        viewModelScope.launch {
            viewModelScope.launch {
                // A new signed-in profile means the previous logout is over.
                profileFlow.collect { _isLoggingOut.value = false }
            }

            preferencesFlow.collect { prefs ->
                _isMetric.value = prefs.isMetric
                _currentLanguage.value = prefs.currentLanguage.toPresentation()
                _isDarkMode.value = prefs.isDarkMode
                // Last, so the splash only lifts once every value above is set.
                _arePreferencesLoaded.value = true
            }
        }
    }

    fun onUnitSystemChanged(isMetric: Boolean) {
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
            if (consumeNotificationPrompt()) _requestNotificationPermissionEvent.trySend(Unit)
        }
    }

    fun toggleNotifications(wantsEnabled: Boolean) {
        viewModelScope.launch {
            when (toggleNotifications.invoke(wantsEnabled)) {
                ToggleNotificationsUseCase.Result.PermissionRequired -> {
                    isAwaitingTogglePermission = true
                    _requestNotificationPermissionEvent.trySend(Unit)
                }
                else -> Unit
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean, isBlockedForGood: Boolean) {
        val fromToggle = isAwaitingTogglePermission
        isAwaitingTogglePermission = false
        viewModelScope.launch {
            handleNotificationPermissionResult(granted)
            if (!granted && isBlockedForGood && fromToggle) {
                _notificationsBlockedChannel.send(Unit)
            }
        }
    }

    fun onAppResumed() {
        viewModelScope.launch {
            syncNotificationPermission()
        }
    }

    fun onLanguageChanged(language: LanguageOption) {
        viewModelScope.launch { setLanguage(language.toDomain()) }
    }

    fun onThemeChanged(enabled: Boolean) {
        viewModelScope.launch { setDarkMode(enabled) }
    }

    fun onLogoutAction() {
        if (_isLoggingOut.value) return
        _isLoggingOut.value = true
        viewModelScope.launch {
            logoutUserUseCase().fold(
                onSuccess = { _logoutCompleteChannel.send(Unit) },
                onFailure = {
                    _isLoggingOut.value = false
                    _logoutFailedChannel.send(Unit)
                }
            )
        }
    }
}
