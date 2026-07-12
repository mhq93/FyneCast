package com.mhq.fynecast.core.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mhq.fynecast.BuildConfig
import com.mhq.fynecast.alerts.ui.AlertsViewModel
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.core.navigation.SessionViewModel
import com.mhq.fynecast.core.ui.globalcomponents.FyneCastApp
import com.mhq.fynecast.home.ui.HomeViewModel
import com.mhq.fynecast.settings.ui.SettingsViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val homeViewModel: HomeViewModel by lazy {
        ViewModelProvider(this, HomeViewModel.factory)[HomeViewModel::class.java]
    }

    private val alertsViewModel: AlertsViewModel by lazy {
        ViewModelProvider(this, AlertsViewModel.factory)[AlertsViewModel::class.java]
    }

    private val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider(this, SettingsViewModel.factory)[SettingsViewModel::class.java]
    }

    private val sessionViewModel: SessionViewModel by lazy {
        ViewModelProvider(this, SessionViewModel.factory)[SessionViewModel::class.java]
    }

    private var onLocationPermissionResult: ((Boolean) -> Unit)? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onLocationPermissionResult?.invoke(isGranted)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        settingsViewModel.onNotificationPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()//
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { false }//

        if (BuildConfig.DEBUG) {
            (applicationContext as? FyneCastApplication)?.container?.appInitializer?.initializeDebugFeatures()
        }

        lifecycleScope.launch {
            settingsViewModel.localeEvent.collect { langCode ->
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(langCode)
                )
            }
        }

        lifecycleScope.launch {
            settingsViewModel.requestNotificationPermissionEvent.collect {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val currentLangTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "en" }
        homeViewModel.refreshCurrentCity(langCode = currentLangTag)
        settingsViewModel.syncNotificationPermissionState()
    }
}