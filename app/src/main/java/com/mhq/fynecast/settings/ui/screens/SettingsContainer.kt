package com.mhq.fynecast.settings.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mhq.fynecast.settings.ui.SettingsViewModel

@Composable
fun SettingsContainer(
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory),
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {

    val profileName by settingsViewModel.profileName.collectAsStateWithLifecycle()
    val profileEmail by settingsViewModel.profileEmail.collectAsStateWithLifecycle()
    val profileImageUri by settingsViewModel.profileImageUri.collectAsStateWithLifecycle()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()

    val isMetric by settingsViewModel.isMetric.collectAsStateWithLifecycle()
    val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        SettingsContent(
            isMetric = isMetric,
            onMetricToggle = settingsViewModel::toggleUnitSystem,
            isDarkMode = isDarkMode,
            onDarkModeToggle = settingsViewModel::toggleDarkMode,
            notificationsEnabled = notificationsEnabled,
            onNotificationsToggle = settingsViewModel::toggleNotifications,
            profileName = profileName,
            profileEmail = profileEmail,
            profileImageUri = profileImageUri,
            currentLanguage = currentLanguage,
            onLanguageChange = settingsViewModel::toggleLanguage,
            onEditProfileClick = onEditProfileClick,
            onLogoutClick = {
                settingsViewModel.logOutUserSession()
                onLogoutClick()
            },
            contentPadding = contentPadding,
            modifier = modifier
        )
    }
}