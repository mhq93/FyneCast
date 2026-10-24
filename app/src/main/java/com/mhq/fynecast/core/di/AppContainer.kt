package com.mhq.fynecast.core.di

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.alerts.domain.usecases.CheckAlertConditionsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertIdsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertStatusUseCase
import com.mhq.fynecast.alerts.domain.usecases.RefreshTrackingNotificationUseCase
import com.mhq.fynecast.alerts.domain.usecases.RestoreAlertTrackingUseCase
import com.mhq.fynecast.alerts.domain.usecases.ToggleAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.TrackAlertUseCase
import com.mhq.fynecast.alerts.domain.usecases.UntrackAlertUseCase
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.alerts.notifications.domain.usecases.ConsumeNotificationPromptUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.SyncNotificationPermissionUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.AuthenticateWithSocialTokenUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.CompleteAuthSessionUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.LoginWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.RegisterWithEmailUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.SendPasswordResetUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateLoginFormUseCase
import com.mhq.fynecast.auth.domain.usecases.auth.ValidateSignupFormUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ObserveUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.RefreshUserSessionUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.UpdateUserProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.profile.ValidateEditProfileUseCase
import com.mhq.fynecast.auth.domain.usecases.session.CheckUserLoggedInUseCase
import com.mhq.fynecast.auth.domain.usecases.session.LogoutUserUseCase
import com.mhq.fynecast.core.domain.infra.AppInitializer
import com.mhq.fynecast.core.domain.location.ObserveSelectedCityUseCase
import com.mhq.fynecast.core.domain.location.SelectCityUseCase
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshAllFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshFavoriteSnapshotUseCase
import com.mhq.fynecast.favorites.domain.usecases.RestoreFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.domain.usecases.GetCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.GetLastSearchedCityUseCase
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.GetWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.LoadCurrentWeatherUseCase
import com.mhq.fynecast.home.domain.usecases.LocalizePlaceNameUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveSignedInUserIdUseCase
import com.mhq.fynecast.home.domain.usecases.ResolveStartupLocationUseCase
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SaveLastSearchedCityUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetDarkModeUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetLanguageUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetUnitSystemUseCase

interface AppContainer {

    val appInitializer: AppInitializer

    val getWeatherUseCase: GetWeatherUseCase
    val loadCurrentWeatherUseCase: LoadCurrentWeatherUseCase
    val observeCurrentLocationUseCase: ObserveCurrentLocationUseCase

    // --- Authentication & Profile Use Cases ---
    val validateSignupFormUseCase: ValidateSignupFormUseCase
    val validateLoginFormUseCase: ValidateLoginFormUseCase
    val registerWithEmailUseCase: RegisterWithEmailUseCase
    val loginWithEmailUseCase: LoginWithEmailUseCase
    val authenticateWithSocialTokenUseCase: AuthenticateWithSocialTokenUseCase
    val completeAuthSessionUseCase: CompleteAuthSessionUseCase
    val sendPasswordResetUseCase: SendPasswordResetUseCase
    val updateUserProfileUseCase: UpdateUserProfileUseCase
    val validateEditProfileUseCase: ValidateEditProfileUseCase
    val observeUserProfileUseCase: ObserveUserProfileUseCase
    val refreshUserSessionUseCase: RefreshUserSessionUseCase
    val observeSignedInUserIdUseCase: ObserveSignedInUserIdUseCase
    val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase
    val logoutUserUseCase: LogoutUserUseCase
    val checkUserLoggedInUseCase: CheckUserLoggedInUseCase
    val setUnitSystemUseCase: SetUnitSystemUseCase
    val setLanguageUseCase: SetLanguageUseCase
    val setDarkModeUseCase: SetDarkModeUseCase
    val saveLastSearchedCityUseCase: SaveLastSearchedCityUseCase

    // Location Use Cases
    val getCurrentLocationUseCase: GetCurrentLocationUseCase
    val getLocationSuggestionsUseCase: GetLocationSuggestionsUseCase
    val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase
    val getLastSearchedCityUseCase: GetLastSearchedCityUseCase
    val resolveStartupLocationUseCase: ResolveStartupLocationUseCase
    val selectCityUseCase: SelectCityUseCase
    val observeSelectedCityUseCase: ObserveSelectedCityUseCase
    val localizePlaceNameUseCase: LocalizePlaceNameUseCase
    val refreshAllFavoritesUseCase: RefreshAllFavoritesUseCase

    // Favorites Use Cases
    val getFavoritesUseCase: GetFavoritesUseCase
    val deleteFavoriteUseCase: DeleteFavoriteUseCase
    val toggleFavoriteUseCase: ToggleFavoriteUseCase
    val observeFavoriteStatusUseCase: ObserveFavoriteStatusUseCase
    val refreshFavoriteSnapshotUseCase: RefreshFavoriteSnapshotUseCase
    val restoreFavoriteUseCase: RestoreFavoriteUseCase

    // Alerts Use Cases
    val getAlertsUseCase: GetAlertsUseCase
    val toggleAlertsUseCase: ToggleAlertsUseCase
    val trackAlertUseCase: TrackAlertUseCase
    val untrackAlertUseCase: UntrackAlertUseCase
    val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase
    val checkAlertConditionsUseCase: CheckAlertConditionsUseCase
    val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase
    val getTrackedAlertStatusUseCase: GetTrackedAlertStatusUseCase
    val restoreAlertTrackingUseCase: RestoreAlertTrackingUseCase
    val refreshTrackingNotificationUseCase: RefreshTrackingNotificationUseCase

    // Notifications Use Cases
    val notificationPermissionChecker: NotificationPermissionChecker
    val toggleNotificationsUseCase: ToggleNotificationsUseCase
    val handleNotificationPermissionResultUseCase: HandleNotificationPermissionResultUseCase
    val syncNotificationPermissionUseCase: SyncNotificationPermissionUseCase
    val consumeNotificationPromptUseCase: ConsumeNotificationPromptUseCase
}