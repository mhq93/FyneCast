package com.mhq.fynecast.settings.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.R
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.ui.openNotificationSettings
import com.mhq.fynecast.settings.presentation.SettingsViewModel

@Composable
fun SettingsContainer(
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory),
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val profileName by settingsViewModel.profileName.collectAsStateWithLifecycle()
    val profileEmail by settingsViewModel.profileEmail.collectAsStateWithLifecycle()
    val profileImageUri by settingsViewModel.profileImageUri.collectAsStateWithLifecycle()
    val isMetric by settingsViewModel.isMetric.collectAsStateWithLifecycle()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()
    val storedDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDarkMode = storedDarkMode ?: isSystemInDarkTheme()
    val isLoggingOut by settingsViewModel.isLoggingOut.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        settingsViewModel.logoutComplete.collect {
            onLogoutClick()
        }
    }

    LaunchedEffect(settingsViewModel, resources) {
        settingsViewModel.logoutFailed.collect {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(resources.getString(R.string.logout_failed))
        }
    }

    val context = LocalContext.current
    LaunchedEffect(settingsViewModel, resources, context) {
        settingsViewModel.notificationsBlocked.collect {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.notifications_settings_message),
                actionLabel = resources.getString(R.string.notifications_settings_confirm),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) context.openNotificationSettings()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SettingsContent(
            profileName = profileName,
            profileEmail = profileEmail,
            profileImageUri = profileImageUri,
            onEditProfileClick = onEditProfileClick,
            isMetric = isMetric,
            onMetricToggle = settingsViewModel::onUnitSystemChanged,
            notificationsEnabled = notificationsEnabled,
            onNotificationsToggle = settingsViewModel::toggleNotifications,
            currentLanguage = currentLanguage,
            onLanguageChange = settingsViewModel::onLanguageChanged,
            isDarkMode = isDarkMode,
            onDarkModeToggle = settingsViewModel::onThemeChanged,
            isLoggingOut = isLoggingOut,
            onLogoutClick = { settingsViewModel.onLogoutAction() },
            contentPadding = contentPadding,
            modifier = modifier
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding())
        )
    }
}