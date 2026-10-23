package com.mhq.fynecast.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.mhq.fynecast.R

/** Only the four tabs shown in the bottom bar. */
enum class BottomNavTab(
    val destination: AppDestination,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    Home(AppDestination.Home, R.string.home, Icons.Default.Home),
    Favorites(AppDestination.Favorites, R.string.favorites, Icons.Default.Favorite),
    Alerts(AppDestination.Alerts, R.string.alerts, Icons.Default.Notifications),
    Settings(AppDestination.Settings, R.string.settings, Icons.Default.Settings)
}