package com.mhq.fynecast.core.ui

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

fun Context.withAppLocale(): Context {
    val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: return this
    val config = Configuration(resources.configuration).apply { setLocale(locale) }
    return createConfigurationContext(config)
}