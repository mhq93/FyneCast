package com.mhq.fynecast.settings.domain.models

data class UserPreferences(
    val isMetric: Boolean,
    val notificationsEnabled: Boolean,
    val currentLanguage: String,
    val isDarkMode: Boolean,
    val lastSearchedCityName: String? = null,
    val lastSearchedCityLat: Double? = null,
    val lastSearchedCityLon: Double? = null
)

//data class UserPreferences(
//    val isMetric: Boolean,
//    val isDarkMode: Boolean,
//    val notificationsEnabled: Boolean,
//    val currentLanguage: String
//)