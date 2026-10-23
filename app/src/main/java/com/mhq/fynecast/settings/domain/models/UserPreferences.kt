package com.mhq.fynecast.settings.domain.models

data class UserPreferences(
    val isMetric: Boolean,
    val notificationsEnabled: Boolean,
    val currentLanguage: AppLanguage,
    val isDarkMode: Boolean?,
    val lastSearchedCityName: String? = null,
    val lastSearchedCityLat: Double? = null,
    val lastSearchedCityLon: Double? = null,
    val notificationPromptShown: Boolean = false
)