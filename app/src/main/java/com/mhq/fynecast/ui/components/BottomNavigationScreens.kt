package com.mhq.fynecast.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationScreens(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Signup : NavigationScreens("signup")
    object Login : NavigationScreens("login")
    object ForgotPassword : NavigationScreens("forgot_password")
    object EditProfile : NavigationScreens("edit_profile")
    object Home : NavigationScreens("home", "Home", Icons.Default.Home)
    object Favorites : NavigationScreens("favorites", "Favorites", Icons.Default.Favorite)
    object Alerts : NavigationScreens("alerts", "Alerts", Icons.Default.Notifications)
    object Settings : NavigationScreens("settings", "Settings", Icons.Default.Settings)
}