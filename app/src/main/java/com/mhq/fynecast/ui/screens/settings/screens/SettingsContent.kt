package com.mhq.fynecast.ui.screens.settings.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.ui.components.GlassyCard
import com.mhq.fynecast.ui.theme.BabyBlue
import com.mhq.fynecast.ui.theme.DuskBlue
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.LightBabyBlue
import com.mhq.fynecast.ui.theme.LightIceBlue
import com.mhq.fynecast.ui.theme.LightMiddayBlue
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    notificationsEnabled: Boolean,
    isDarkMode: Boolean,
    isMetric: Boolean,
    profileName: String,
    profileEmail: String,
    profileImageUri: String?,
    currentLanguage: String,
    onNotificationsToggle: (Boolean) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onMetricToggle: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val settingsImageModel = profileImageUri ?: R.drawable.ic_google_primary_light
    val isLightThemeActive = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val dynamicBackgroundGradient = if (isLightThemeActive) {
        listOf(LightMiddayBlue, LightBabyBlue, LightIceBlue)
    } else {
        listOf(MidnightBlue, DuskBlue, LilacBlue, BabyBlue)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(dynamicBackgroundGradient))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isLightThemeActive) Color.Black.copy(alpha = 0.03f) else MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.07f
                    )
                )
                .statusBarsPadding()
                .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // SECTION A: Profile Header (Grows down from the top)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 16.dp)
            ) {
                AsyncImage(
                    model = settingsImageModel,
                    contentDescription = stringResource(R.string.profile_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            CircleShape
                        )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = profileName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = profileEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onEditProfileClick,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = 0.1f
                        )
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = stringResource(R.string.edit_profile),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // DYNAMIC BUFFER AREA 1: Safely separates header from options card
            Spacer(modifier = Modifier.height(4.dp))

            // SECTION B: The Core Preferences Box (Centered horizontally and vertically)
            GlassyCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                padding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ROW 1: Units (Metric / Imperial)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.units),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.width(180.dp)) {
                            if (isRtl) {
                                SegmentedButton(
                                    selected = !isMetric,
                                    onClick = { onMetricToggle(false) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = stringResource(R.string.imperial),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                SegmentedButton(
                                    selected = isMetric,
                                    onClick = { onMetricToggle(true) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = stringResource(R.string.metric),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                SegmentedButton(
                                    selected = isMetric,
                                    onClick = { onMetricToggle(true) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = stringResource(R.string.metric),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                SegmentedButton(
                                    selected = !isMetric,
                                    onClick = { onMetricToggle(false) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = stringResource(R.string.imperial),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // ROW 2: Notifications (On / Off)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.notifications),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.width(180.dp)) {
                            if (isRtl) {
                                SegmentedButton(
                                    selected = !notificationsEnabled,
                                    onClick = { onNotificationsToggle(false) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = stringResource(R.string.off),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                SegmentedButton(
                                    selected = notificationsEnabled,
                                    onClick = { onNotificationsToggle(true) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = stringResource(R.string.on),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                SegmentedButton(
                                    selected = notificationsEnabled,
                                    onClick = { onNotificationsToggle(true) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = stringResource(R.string.on),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                SegmentedButton(
                                    selected = !notificationsEnabled,
                                    onClick = { onNotificationsToggle(false) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = stringResource(R.string.off),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // ROW 3: Language (English / Arabic)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.language),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.width(180.dp)) {
                            if (isRtl) {
                                SegmentedButton(
                                    selected = currentLanguage == "Arabic",
                                    onClick = { onLanguageChange("Arabic") },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = "العربية",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                SegmentedButton(
                                    selected = currentLanguage == "English",
                                    onClick = { onLanguageChange("English") },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = "English",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                SegmentedButton(
                                    selected = currentLanguage == "English",
                                    onClick = { onLanguageChange("English") },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = "English",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                SegmentedButton(
                                    selected = currentLanguage == "Arabic",
                                    onClick = { onLanguageChange("Arabic") },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = "العربية",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // ROW 4: Dark Mode (Light / Dark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.dark_mode),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.width(180.dp)) {
                            if (isRtl) {
                                SegmentedButton(
                                    selected = isDarkMode,
                                    onClick = { onDarkModeToggle(true) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = stringResource(R.string.dark),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                SegmentedButton(
                                    selected = !isDarkMode,
                                    onClick = { onDarkModeToggle(false) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = stringResource(R.string.light),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                SegmentedButton(
                                    selected = !isDarkMode,
                                    onClick = { onDarkModeToggle(false) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = stringResource(R.string.light),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                SegmentedButton(
                                    selected = isDarkMode,
                                    onClick = { onDarkModeToggle(true) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = customSegmentedColors()
                                ) {
                                    Text(
                                        text = stringResource(R.string.dark),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // DYNAMIC BUFFER AREA 2: Expands flexibly to push the logout button downward on long viewports
            Spacer(modifier = Modifier.height(4.dp))
            // SECTION C: System Exit Button (Pinned cleanly right above your bottom navigation bar edge)
            Button(
                onClick = onLogoutClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F).copy(alpha = 0.15f),
                    contentColor = Color(0xFFEF5350)
                ),
                border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.3f)),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.logout),
                        color = Color(0xFFEF5350),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.bodyMedium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun customSegmentedColors(): SegmentedButtonColors {
    // Checks if the active backdrop background is bright/light
    val isLightThemeActive = MaterialTheme.colorScheme.background.luminance() > 0.5f

    return SegmentedButtonDefaults.colors(
        activeContainerColor = MidnightBlue,
        activeContentColor = NeonGreen,
        // FIX: High-contrast adaptive shading rules ensure legibility on any backdrop palette
        inactiveContainerColor = if (isLightThemeActive) Color.Black.copy(alpha = 0.05f) else MaterialTheme.colorScheme.onBackground.copy(
            alpha = 0.04f
        ),
        inactiveContentColor = if (isLightThemeActive) MidnightBlue.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    FyneCastTheme() {
        SettingsContent(
            notificationsEnabled = true,
            isDarkMode = true,
            isMetric = true,
            profileName = "name",
            profileEmail = "email",
            onNotificationsToggle = {},
            onDarkModeToggle = {},
            onMetricToggle = {},
            onEditProfileClick = {},
            onLogoutClick = {},
            contentPadding = PaddingValues(0.dp),
            currentLanguage = "",
            onLanguageChange = {},
            profileImageUri = null
        )
    }
}