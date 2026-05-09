package com.mhq.fynecast.ui.screens.navigation

sealed class Screen(val route: String) {
    object Home: Screen("home_screen")
    object Favorites: Screen("favorites_screen")
    object Alerts: Screen("alerts_screen")
    object Settings: Screen("settings_screen")
}