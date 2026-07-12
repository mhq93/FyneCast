package com.mhq.fynecast.core.di

import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.SyncNotificationPermissionUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.alerts.domain.usecases.GetAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertIdsUseCase
import com.mhq.fynecast.alerts.domain.usecases.ToggleAlertsUseCase
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.auth.SocialAuthCredentialProvider
import com.mhq.fynecast.auth.domain.repository.profile.ImageStorageRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialTokenUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.LoginWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.RegisterWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.SendPasswordResetUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.UpdateUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ValidateEditProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialProviderUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.CompleteAuthSessionUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateSignupFormUseCase
import com.mhq.fynecast.core.domain.infra.AppInitializer
import com.mhq.fynecast.auth.domain.usecases.session.CheckUserLoggedInUseCase
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.InsertFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository
import com.mhq.fynecast.home.domain.repository.LocationRepository
import com.mhq.fynecast.home.domain.repository.WeatherRepository
import com.mhq.fynecast.home.domain.usecases.FetchWeatherDataUseCase
import com.mhq.fynecast.home.domain.usecases.GetCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.GetLastSearchedCityUseCase
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentCityNameUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase

interface AppContainer {

    val appInitializer: AppInitializer

    // Core Repositories & Trackers
    val authRepository: AuthRepository
    val userProfileRepository: UserProfileRepository
    val imageStorageRepository: ImageStorageRepository
    val locationRepository: LocationRepository
    val favoritesRepository: FavoritesRepository

    val weatherRepository: WeatherRepository
    val fetchWeatherDataUseCase: FetchWeatherDataUseCase
    val observeCurrentCityNameUseCase: ObserveCurrentCityNameUseCase

    // --- Authentication & Profile Use Cases ---
    val validateSignupFormUseCase: ValidateSignupFormUseCase
    val validateLoginFormUseCase: ValidateLoginFormUseCase
    val registerWithEmailUseCase: RegisterWithEmailUseCase
    val loginWithEmailUseCase: LoginWithEmailUseCase
    val authenticateWithSocialTokenUseCase: AuthenticateWithSocialTokenUseCase
    val authenticateWithSocialProviderUseCase: AuthenticateWithSocialProviderUseCase
    val completeAuthSessionUseCase: CompleteAuthSessionUseCase
    val sendPasswordResetUseCase: SendPasswordResetUseCase
    val updateUserProfileUseCase: UpdateUserProfileUseCase
    val validateEditProfileUseCase: ValidateEditProfileUseCase
    val observeUserProfileUseCase: ObserveUserProfileUseCase
    val userPreferencesRepository: UserPreferencesRepository
    val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase
    val updatePreferenceUseCase: UpdatePreferenceUseCase
    val logoutUserUseCase: LogoutUserUseCase
    val checkUserLoggedInUseCase: CheckUserLoggedInUseCase

    // Location Use Cases
    val deviceLocationRepository: DeviceLocationRepository
    val getCurrentLocationUseCase: GetCurrentLocationUseCase
    val getLocationSuggestionsUseCase: GetLocationSuggestionsUseCase
    val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase
    val getLastSearchedCityUseCase: GetLastSearchedCityUseCase

    // Favorites Use Cases
    val insertFavoriteUseCase: InsertFavoriteUseCase
    val getFavoritesUseCase: GetFavoritesUseCase
    val deleteFavoriteUseCase: DeleteFavoriteUseCase
    val toggleFavoriteUseCase: ToggleFavoriteUseCase
    val observeFavoriteStatusUseCase: ObserveFavoriteStatusUseCase

    // Alerts Use Cases
    val alertsRepository: AlertsRepository
    val getAlertsUseCase: GetAlertsUseCase
    val toggleAlertsUseCase: ToggleAlertsUseCase
    val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase

    // Notifications Use Cases
    val notificationPermissionChecker: NotificationPermissionChecker
    val toggleNotificationsUseCase: ToggleNotificationsUseCase
    val handleNotificationPermissionResultUseCase: HandleNotificationPermissionResultUseCase
    val syncNotificationPermissionUseCase: SyncNotificationPermissionUseCase
}