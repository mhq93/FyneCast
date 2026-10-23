package com.mhq.fynecast.core.navigation

/** Every screen route in the app. */
enum class AppDestination(val route: String) {
    Signup("signup"),
    Login("login"),
    ForgotPassword("forgot_password"),
    EditProfile("edit_profile"),
    MapPicker("map_picker"),
    Home("home"),
    Favorites("favorites"),
    Alerts("alerts"),
    Settings("settings")
}