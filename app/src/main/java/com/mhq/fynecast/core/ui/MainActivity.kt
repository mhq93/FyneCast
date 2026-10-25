package com.mhq.fynecast.core.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mhq.fynecast.alerts.presentation.AlertsViewModel
import com.mhq.fynecast.core.di.AppViewModelFactory
import com.mhq.fynecast.core.presentation.SessionViewModel
import com.mhq.fynecast.core.ui.globalcomponents.FyneCastApp
import com.mhq.fynecast.home.presentation.HomeViewModel
import com.mhq.fynecast.settings.presentation.SettingsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val homeViewModel: HomeViewModel by lazy {
        ViewModelProvider(this, AppViewModelFactory)[HomeViewModel::class.java]
    }

    private val alertsViewModel: AlertsViewModel by lazy {
        ViewModelProvider(this, AppViewModelFactory)[AlertsViewModel::class.java]
    }

    private val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider(this, AppViewModelFactory)[SettingsViewModel::class.java]
    }

    private val sessionViewModel: SessionViewModel by lazy {
        ViewModelProvider(this, AppViewModelFactory)[SessionViewModel::class.java]
    }

    private var onLocationPermissionResult: ((Boolean) -> Unit)? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onLocationPermissionResult?.invoke(isGranted)
        settingsViewModel.requestNotificationPermissionOnFirstLaunch()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val blockedForGood = !granted && !ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )
        settingsViewModel.onNotificationPermissionResult(granted, blockedForGood)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !settingsViewModel.arePreferencesLoaded.value }

        lifecycleScope.launch {
            // Wait for the saved preferences, otherwise the default language is applied first.
            settingsViewModel.arePreferencesLoaded.first { it }
            settingsViewModel.currentLanguage
                .map { it.code }
                .distinctUntilChanged()
                .collect { langCode -> applyAppLanguage(langCode) }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsViewModel.requestNotificationPermissionEvent.collect {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        setContent {
            FyneCastApp(
                homeViewModel = homeViewModel,
                alertsViewModel = alertsViewModel,
                settingsViewModel = settingsViewModel,
                sessionViewModel = sessionViewModel,
                onRequestPermission = { activeLangCode ->
                    onLocationPermissionResult = { isGranted ->
                        homeViewModel.onLocationPermissionResult(
                            isGranted = isGranted,
                            langCode = activeLangCode
                        )
                    }
                    val alreadyGranted =
                        ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                    if (alreadyGranted) {
                        // Nothing to ask: skip the system permission screen, it flashes the app.
                        onLocationPermissionResult?.invoke(true)
                        settingsViewModel.requestNotificationPermissionOnFirstLaunch()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            )
        }
    }

    /**
     * Applies the chosen language, but skips it when the app already shows it:
     * applying recreates the activity and shows up as a black flash.
     */
    private fun applyAppLanguage(langCode: String) {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val effectiveLanguage = (appLocales[0] ?: resources.configuration.locales[0]).language
        if (effectiveLanguage != Locale.forLanguageTag(langCode).language) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
        }
    }

    override fun onResume() {
        super.onResume()
        val currentLangTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "en" }
        homeViewModel.onRefreshRequested(langCode = currentLangTag)
        settingsViewModel.onAppResumed()
        sessionViewModel.onAppResumed()
    }
}