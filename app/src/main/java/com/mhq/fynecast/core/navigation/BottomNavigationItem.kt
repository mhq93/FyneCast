package com.mhq.fynecast.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.mhq.fynecast.R

//sealed class BottomNavigationItem(val route: String, val title: String? = null, val icon: ImageVector? = null) {
//    object Signup : BottomNavigationItem("signup")
//    object Login : BottomNavigationItem("login")
//    object ForgotPassword : BottomNavigationItem("forgot_password")
//    object EditProfile : BottomNavigationItem("edit_profile")
//    object Home : BottomNavigationItem("home", "Home", Icons.Default.Home)
//    object Favorites : BottomNavigationItem("favorites", "Favorites", Icons.Default.Favorite)
//    object Alerts : BottomNavigationItem("alerts", "Alerts", Icons.Default.Notifications)
//    object Settings : BottomNavigationItem("settings", "Settings", Icons.Default.Settings)
//}

sealed class BottomNavigationItem(
    val route: String,
    @StringRes val titleRes: Int? = null,
    val icon: ImageVector? = null
) {
    object Signup : BottomNavigationItem("signup")
    object Login : BottomNavigationItem("login")
    object ForgotPassword : BottomNavigationItem("forgot_password")
    object EditProfile : BottomNavigationItem("edit_profile")

    object Home : BottomNavigationItem("home", R.string.home, Icons.Default.Home)
    object Favorites : BottomNavigationItem("favorites", R.string.favorites, Icons.Default.Favorite)
    object Alerts : BottomNavigationItem("alerts", R.string.alerts, Icons.Default.Notifications)
    object Settings : BottomNavigationItem("settings", R.string.settings, Icons.Default.Settings)
}
