package com.mhq.fynecast.settings.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.settings.presentation.model.LanguageOption
import com.mhq.fynecast.settings.ui.components.SettingsLogoutButton
import com.mhq.fynecast.settings.ui.components.SettingsPanel
import com.mhq.fynecast.settings.ui.components.SettingsProfileHeader

@Composable
fun SettingsContent(
    profileName: String,
    profileEmail: String,
    profileImageUri: String?,
    onEditProfileClick: () -> Unit,
    isMetric: Boolean,
    onMetricToggle: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    currentLanguage: LanguageOption,
    onLanguageChange: (LanguageOption) -> Unit,
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    isLoggingOut: Boolean = false,
    onLogoutClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsProfileHeader(
                profileName = profileName,
                profileEmail = profileEmail,
                profileImageUri = profileImageUri,
                onEditProfileClick = onEditProfileClick,
            )
            SettingsPanel(
                isMetric = isMetric,
                onMetricToggle = onMetricToggle,
                notificationsEnabled = notificationsEnabled,
                onNotificationsToggle = onNotificationsToggle,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                isDarkMode = isDarkMode,
                onDarkModeToggle = onDarkModeToggle
            )
            SettingsLogoutButton(
                isLoggingOut = isLoggingOut,
                onLogoutClick = onLogoutClick,
            )
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    FyneCastTheme() {
        SettingsContent(
            profileName = "name",
            profileEmail = "email",
            profileImageUri = null,
            onEditProfileClick = {},
            isMetric = true,
            onMetricToggle = {},
            notificationsEnabled = true,
            onNotificationsToggle = {},
            currentLanguage = LanguageOption.English,
            onLanguageChange = {},
            isDarkMode = true,
            onDarkModeToggle = {},
            onLogoutClick = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}