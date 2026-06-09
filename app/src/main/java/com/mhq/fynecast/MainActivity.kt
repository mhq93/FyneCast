package com.mhq.fynecast

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.mhq.fynecast.ui.screens.favorites.screens.FavoritesViewModel
import com.mhq.fynecast.ui.screens.favorites.screens.FavoritesViewModelFactory
import com.mhq.fynecast.ui.screens.home.screens.HomeViewModel
import com.mhq.fynecast.ui.screens.settings.screens.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : AppCompatActivity() {
    private val homeViewModel: HomeViewModel by viewModels { HomeViewModel.factory }
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            homeViewModel.fetchWeatherData("Cairo")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //Splash Screen Setup...
        val splashScreen = installSplashScreen()
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        lifecycleScope.launch {
            delay(2000.milliseconds)
            keepSplashScreen = false
        }

        val container = (this.application as FyneCastApplication).container
        val favoritesViewModel: FavoritesViewModel by viewModels {
            FavoritesViewModelFactory(
                fetchFavoritesUseCase = container.fetchFavoritesUseCase,
                deleteFavoriteUseCase = container.deleteFavoritesUseCase,
            )
        }

        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.factory }

            LaunchedEffect(Unit) {
                settingsViewModel.localeEvent.collect { localeTag ->
                    withContext(Dispatchers.Main) {
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(localeTag)
                        )
                    }
                }
            }

            FyneCastApp(
                onRequestPermission = {
                    val fineGranted = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!fineGranted && !shouldShowRequestPermissionRationale(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        })
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

    override fun onResume() {
        super.onResume()
        homeViewModel.fetchWeatherData("Cairo")
    }
}