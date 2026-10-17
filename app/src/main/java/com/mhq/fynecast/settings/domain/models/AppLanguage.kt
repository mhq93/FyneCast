package com.mhq.fynecast.settings.domain.models

enum class AppLanguage(val storedName: String, val code: String) {
    English("English", "en"),
    Arabic("Arabic", "ar");

    companion object {
        fun fromStoredName(name: String?): AppLanguage =
            entries.firstOrNull { it.storedName == name } ?: English
    }
}