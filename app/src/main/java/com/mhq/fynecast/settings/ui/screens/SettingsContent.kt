package com.mhq.fynecast.settings.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.settings.presentation.model.LanguageOption
import com.mhq.fynecast.settings.ui.components.SettingsLogoutButton
import com.mhq.fynecast.settings.ui.components.SettingsPanel
import com.mhq.fynecast.settings.ui.components.SettingsProfileHeader

private val PageEdgePadding = 24.dp

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
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = contentPadding.calculateBottomPadding()
        // The height the page can use. When the content is shorter, its three blocks are spread
        // over this height; when it is taller, the page scrolls.
        val pageHeight =
            (maxHeight - statusBarHeight - bottomInset - PageEdgePadding * 2).coerceAtLeast(0.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = PageEdgePadding,
                    bottom = bottomInset + PageEdgePadding
                )
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = pageHeight)
            ) {
                // Each block keeps at least 20 dp from its neighbours; extra height is shared out.
                SettingsProfileHeader(
                    profileName = profileName,
                    profileEmail = profileEmail,
                    profileImageUri = profileImageUri,
                    onEditProfileClick = onEditProfileClick,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                SettingsPanel(
                    isMetric = isMetric,
                    onMetricToggle = onMetricToggle,
                    notificationsEnabled = notificationsEnabled,
                    onNotificationsToggle = onNotificationsToggle,
                    currentLanguage = currentLanguage,
                    onLanguageChange = onLanguageChange,
                    isDarkMode = isDarkMode,
                    onDarkModeToggle = onDarkModeToggle,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                SettingsLogoutButton(
                    isLoggingOut = isLoggingOut,
                    onLogoutClick = onLogoutClick,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsPreviewContent(isDarkMode: Boolean) {
    FyneCastTheme(isDarkMode = isDarkMode) {
        SettingsContent(
            profileName = "Mohamed",
            profileEmail = "mohamed@example.com",
            profileImageUri = null,
            onEditProfileClick = {},
            isMetric = true,
            onMetricToggle = {},
            notificationsEnabled = true,
            onNotificationsToggle = {},
            currentLanguage = LanguageOption.English,
            onLanguageChange = {},
            isDarkMode = isDarkMode,
            onDarkModeToggle = {},
            onLogoutClick = {},
            contentPadding = PaddingValues(0.dp)
        )
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFE0F2FE, heightDp = 800)
@Composable
private fun SettingsLightPreview() = SettingsPreviewContent(isDarkMode = false)

@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F172A,
    heightDp = 800
)
@Composable
private fun SettingsDarkPreview() = SettingsPreviewContent(isDarkMode = true)
