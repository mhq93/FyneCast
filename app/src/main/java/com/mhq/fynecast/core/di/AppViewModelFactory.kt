package com.mhq.fynecast.core.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.alerts.presentation.AlertsViewModel
import com.mhq.fynecast.auth.presentation.editprofile.EditProfileViewModel
import com.mhq.fynecast.auth.presentation.forgotpassword.ForgotPasswordViewModel
import com.mhq.fynecast.auth.presentation.login.LoginViewModel
import com.mhq.fynecast.auth.presentation.signup.SignupViewModel
import com.mhq.fynecast.core.presentation.SessionViewModel
import com.mhq.fynecast.favorites.presentation.FavoritesViewModel
import com.mhq.fynecast.home.presentation.HomeViewModel
import com.mhq.fynecast.map.presentation.MapPickerViewModel
import com.mhq.fynecast.settings.presentation.SettingsViewModel

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as FyneCastApplication).container

/** The single place where ViewModels are wired to the DI container. */
val AppViewModelFactory: ViewModelProvider.Factory = viewModelFactory {

    initializer {
        val container = container()
        AlertsViewModel(
            getAlertsUseCase = container.getAlertsUseCase,
            getTrackedAlertIdsUseCase = container.getTrackedAlertIdsUseCase,
            observeCurrentLocationUseCase = container.observeCurrentLocationUseCase,
            toggleAlertsUseCase = container.toggleAlertsUseCase,
            toggleNotificationsUseCase = container.toggleNotificationsUseCase,
            handleNotificationPermissionResultUseCase = container.handleNotificationPermissionResultUseCase,
        )
    }

    initializer {
        val container = container()
        EditProfileViewModel(
            observeUserProfileUseCase = container.observeUserProfileUseCase,
            validateEditProfileUseCase = container.validateEditProfileUseCase,
            updateUserProfileUseCase = container.updateUserProfileUseCase
        )
    }

    initializer {
        val container = container()
        ForgotPasswordViewModel(
            validateLoginFormUseCase = container.validateLoginFormUseCase,
            sendPasswordResetUseCase = container.sendPasswordResetUseCase
        )
    }

    initializer {
        val container = container()
        LoginViewModel(
            loginWithEmailUseCase = container.loginWithEmailUseCase,
            validateLoginFormUseCase = container.validateLoginFormUseCase,
            authenticateWithSocialTokenUseCase = container.authenticateWithSocialTokenUseCase
        )
    }

    initializer {
        val container = container()
        SignupViewModel(
            validateSignupFormUseCase = container.validateSignupFormUseCase,
            registerWithEmailUseCase = container.registerWithEmailUseCase,
            authenticateWithSocialTokenUseCase = container.authenticateWithSocialTokenUseCase
        )
    }

    initializer {
        SessionViewModel(
            checkUserLoggedIn = container().checkUserLoggedInUseCase,
            refreshUserSession = container().refreshUserSessionUseCase
        )
    }

    initializer {
        val container = container()
        FavoritesViewModel(
            getFavoritesUseCase = container.getFavoritesUseCase,
            deleteFavoriteUseCase = container.deleteFavoriteUseCase,
            restoreFavoriteUseCase = container.restoreFavoriteUseCase,
            refreshAllFavoritesUseCase = container.refreshAllFavoritesUseCase,
            selectCityUseCase = container.selectCityUseCase
        )
    }

    initializer {
        val container = container()
        HomeViewModel(
            loadCurrentWeatherUseCase = container.loadCurrentWeatherUseCase,
            getLocationSuggestionsUseCase = container.getLocationSuggestionsUseCase,
            observeFavoriteStatusUseCase = container.observeFavoriteStatusUseCase,
            observeSignedInUserIdUseCase = container.observeSignedInUserIdUseCase,
            observeSelectedCityUseCase = container.observeSelectedCityUseCase,
            resolveStartupLocationUseCase = container.resolveStartupLocationUseCase,
            refreshFavoriteSnapshotUseCase = container.refreshFavoriteSnapshotUseCase,
            toggleFavoriteUseCase = container.toggleFavoriteUseCase,
            deleteFavoriteUseCase = container.deleteFavoriteUseCase,
            saveLastSearchedCityUseCase = container.saveLastSearchedCityUseCase,
            reverseGeocodeLocationUseCase = container.reverseGeocodeLocationUseCase,
            localizePlaceNameUseCase = container.localizePlaceNameUseCase
        )
    }

    initializer {
        val container = container()
        MapPickerViewModel(
            reverseGeocodeLocationUseCase = container().reverseGeocodeLocationUseCase,
            selectCityUseCase = container.selectCityUseCase
        )
    }

    initializer {
        val container = container()
        SettingsViewModel(
            observeUserProfileUseCase = container.observeUserProfileUseCase,
            observeUserPreferencesUseCase = container.observeUserPreferencesUseCase,
            setUnitSystem = container.setUnitSystemUseCase,
            setLanguage = container.setLanguageUseCase,
            setDarkMode = container.setDarkModeUseCase,
            toggleNotifications = container.toggleNotificationsUseCase,
            handleNotificationPermissionResult = container.handleNotificationPermissionResultUseCase,
            syncNotificationPermission = container.syncNotificationPermissionUseCase,
            consumeNotificationPrompt = container.consumeNotificationPromptUseCase,
            logoutUserUseCase = container.logoutUserUseCase
        )
    }
}