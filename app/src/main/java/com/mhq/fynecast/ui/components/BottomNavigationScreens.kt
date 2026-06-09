package com.mhq.fynecast.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavigationScreens(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Signup : BottomNavigationScreens("signup")
    object Login : BottomNavigationScreens("login")
    object ForgotPassword : BottomNavigationScreens("forgot_password")
    object EditProfile : BottomNavigationScreens("edit_profile")
    object Home : BottomNavigationScreens("home", "Home", Icons.Default.Home)
    object Favorites : BottomNavigationScreens("favorites", "Favorites", Icons.Default.Favorite)
    object Alerts : BottomNavigationScreens("alerts", "Alerts", Icons.Default.Notifications)
    object Settings : BottomNavigationScreens("settings", "Settings", Icons.Default.Settings)
}