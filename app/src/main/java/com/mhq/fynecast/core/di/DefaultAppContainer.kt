package com.mhq.fynecast.core.di

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mhq.fynecast.BuildConfig
import com.mhq.fynecast.alerts.data.database.AlertsDatabase
import com.mhq.fynecast.alerts.data.repoimpl.AlertTrackingControllerImpl
import com.mhq.fynecast.alerts.data.repoimpl.AlertsRepoImpl
import com.mhq.fynecast.alerts.domain.repository.AlertTrackingController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
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
import com.mhq.fynecast.alerts.notifications.data.infra.AndroidNotificationPermissionChecker
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.alerts.notifications.domain.usecases.ConsumeNotificationPromptUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.SyncNotificationPermissionUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.auth.data.repoimpl.auth.AuthRepositoryImpl
import com.mhq.fynecast.auth.data.repoimpl.auth.SocialAuthCredentialProviderImpl
import com.mhq.fynecast.auth.data.repoimpl.profile.ImageStorageRepositoryImpl
import com.mhq.fynecast.auth.data.repoimpl.profile.UserProfileRepositoryImpl
import com.mhq.fynecast.auth.data.repository.auth.SocialAuthCredentialProvider
import com.mhq.fynecast.auth.data.storage.ImageStorageManager
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.ImageStorageRepository
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
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
import com.mhq.fynecast.core.data.infra.AppInitializerImpl
import com.mhq.fynecast.core.data.infra.FirebaseCurrentUserProvider
import com.mhq.fynecast.core.data.location.SelectedCityRepositoryImpl
import com.mhq.fynecast.core.data.time.SystemTimeProvider
import com.mhq.fynecast.core.domain.infra.AppInitializer
import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import com.mhq.fynecast.core.domain.time.TimeProvider
import com.mhq.fynecast.favorites.data.database.FavoritesDatabase
import com.mhq.fynecast.favorites.data.repoimpl.FavoritesRepoImpl
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshAllFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RefreshFavoriteSnapshotUseCase
import com.mhq.fynecast.favorites.domain.usecases.RestoreFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.data.api.location.LocationApiService
import com.mhq.fynecast.home.data.api.weather.WeatherApiService
import com.mhq.fynecast.home.data.infra.AndroidLocationStatusChecker
import com.mhq.fynecast.home.data.infra.AndroidPlaceNameSource
import com.mhq.fynecast.home.data.repoimpl.location.DefaultLocationRepoImpl
import com.mhq.fynecast.home.data.repoimpl.location.PhotonLocationRepoImpl
import com.mhq.fynecast.home.data.repoimpl.weather.InMemoryCurrentWeatherStore
import com.mhq.fynecast.home.data.repoimpl.weather.WeatherRepoImpl
import com.mhq.fynecast.core.domain.weather.CurrentWeatherStore
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository
import com.mhq.fynecast.home.domain.repository.LocationProviderStatusChecker
import com.mhq.fynecast.core.domain.location.LocationRepository
import com.mhq.fynecast.core.domain.location.ObserveSelectedCityUseCase
import com.mhq.fynecast.core.domain.location.SelectCityUseCase
import com.mhq.fynecast.core.domain.location.SelectedCityRepository
import com.mhq.fynecast.core.domain.weather.WeatherRepository
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
import com.mhq.fynecast.settings.data.repoimpl.UserPreferencesRepositoryImpl
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import com.mhq.fynecast.settings.domain.usecases.profile.ClearLastSearchedCityUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SaveLastSearchedCityUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetDarkModeUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetLanguageUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetNotificationsEnabledUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetUnitSystemUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class DefaultAppContainer(
    private val context: Context,
    private val prefsDataStore: DataStore<Preferences>,
    private val sessionDataStore: DataStore<Preferences>
) : AppContainer {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val appInitializer: AppInitializer by lazy {
        AppInitializerImpl(context.applicationContext as Application)
    }

    // Infrastructure Components
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val imageStorageManager: ImageStorageManager by lazy { ImageStorageManager(context) }

    private val networkClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Networking — Weather
    private val weatherApiBaseUrl = "https://api.weatherapi.com/"
    private val weatherApiJson =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
    private val weatherApiRetrofit = Retrofit.Builder()
        .client(networkClient)
        .addConverterFactory(
            weatherApiJson.asConverterFactory("application/json".toMediaType())
        )
        .baseUrl(weatherApiBaseUrl)
        .build()
    private val weatherApiRetrofitService: WeatherApiService by lazy {
        weatherApiRetrofit.create(WeatherApiService::class.java)
    }
    private val weatherRepository: WeatherRepository by lazy {
        WeatherRepoImpl(
            weatherApiService = weatherApiRetrofitService,
            apiKey = BuildConfig.WEATHER_API_KEY
        )
    }
    private val currentWeatherStore: CurrentWeatherStore by lazy {
        InMemoryCurrentWeatherStore()
    }
    override val getWeatherUseCase: GetWeatherUseCase by lazy {
        GetWeatherUseCase(weatherRepository)
    }
    override val loadCurrentWeatherUseCase: LoadCurrentWeatherUseCase by lazy {
        LoadCurrentWeatherUseCase(weatherRepository, currentWeatherStore)
    }
    override val observeCurrentLocationUseCase by lazy {
        ObserveCurrentLocationUseCase(
            currentWeatherStore
        )
    }

    // Networking — Photon
    private val photonBaseUrl = "https://photon.komoot.io/"
    private val photonJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val photonRetrofit = Retrofit.Builder()
        .client(networkClient)
        .addConverterFactory(photonJson.asConverterFactory("application/json".toMediaType()))
        .baseUrl(photonBaseUrl)
        .build()
    private val photonApiRetrofitService: LocationApiService by lazy {
        photonRetrofit.create(LocationApiService::class.java)
    }

    // Repositories
    private val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseAuth, sessionDataStore, socialAuthCredentialProvider)
    }
    private val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepositoryImpl(firebaseAuth, sessionDataStore, applicationScope)
    }
    private val imageStorageRepository: ImageStorageRepository by lazy {
        ImageStorageRepositoryImpl(context, imageStorageManager)
    }

    // Location Tracker
    private val locationProviderStatusChecker: LocationProviderStatusChecker by lazy {
        AndroidLocationStatusChecker(appContext = context)
    }
    private val deviceLocationRepository: DeviceLocationRepository by lazy {
        DefaultLocationRepoImpl(
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context),
            statusChecker = locationProviderStatusChecker
        )
    }
    private val locationRepository: LocationRepository by lazy {
        PhotonLocationRepoImpl(
            photonApiRetrofitService,
            AndroidPlaceNameSource(context)
        )
    }
    override val localizePlaceNameUseCase: LocalizePlaceNameUseCase by lazy {
        LocalizePlaceNameUseCase(locationRepository)
    }

    override val getCurrentLocationUseCase: GetCurrentLocationUseCase by lazy {
        GetCurrentLocationUseCase(deviceLocationRepository)
    }
    override val getLocationSuggestionsUseCase: GetLocationSuggestionsUseCase by lazy {
        GetLocationSuggestionsUseCase(locationRepository)
    }
    override val reverseGeocodeLocationUseCase: ReverseGeocodeLocationUseCase by lazy {
        ReverseGeocodeLocationUseCase(locationRepository)
    }
    override val getLastSearchedCityUseCase: GetLastSearchedCityUseCase by lazy {
        GetLastSearchedCityUseCase(userPreferencesRepository)
    }
    private val selectedCityRepository: SelectedCityRepository by lazy {
        SelectedCityRepositoryImpl()
    }
    override val selectCityUseCase: SelectCityUseCase by lazy {
        SelectCityUseCase(selectedCityRepository)
    }
    override val observeSelectedCityUseCase: ObserveSelectedCityUseCase by lazy {
        ObserveSelectedCityUseCase(selectedCityRepository)
    }
    override val resolveStartupLocationUseCase: ResolveStartupLocationUseCase by lazy {
        ResolveStartupLocationUseCase(getCurrentLocationUseCase, getLastSearchedCityUseCase)
    }


    // Signup & Login Use Cases
    override val validateSignupFormUseCase by lazy {
        ValidateSignupFormUseCase()
    }
    override val validateLoginFormUseCase by lazy {
        ValidateLoginFormUseCase()
    }
    override val registerWithEmailUseCase: RegisterWithEmailUseCase by lazy {
        RegisterWithEmailUseCase(
            authRepository = authRepository,
            completeAuthSessionUseCase = completeAuthSessionUseCase
        )
    }
    override val loginWithEmailUseCase: LoginWithEmailUseCase by lazy {
        LoginWithEmailUseCase(
            authRepository = authRepository,
            completeAuthSessionUseCase = completeAuthSessionUseCase
        )
    }
    private val socialAuthCredentialProvider: SocialAuthCredentialProvider by lazy {
        SocialAuthCredentialProviderImpl(appContext = context)
    }
    override val completeAuthSessionUseCase: CompleteAuthSessionUseCase by lazy {
        CompleteAuthSessionUseCase(
            authRepository = authRepository,
            userProfileRepository = userProfileRepository
        )
    }
    override val authenticateWithSocialTokenUseCase: AuthenticateWithSocialTokenUseCase by lazy {
        AuthenticateWithSocialTokenUseCase(
            authRepository = authRepository,
            completeAuthSessionUseCase = completeAuthSessionUseCase
        )
    }
    override val sendPasswordResetUseCase by lazy {
        SendPasswordResetUseCase(authRepository)
    }


    // Profile & Logout Use Cases
    private val currentUserProvider: CurrentUserProvider by lazy {
        FirebaseCurrentUserProvider(firebaseAuth)
    }
    override val validateEditProfileUseCase by lazy {
        ValidateEditProfileUseCase()
    }
    override val observeUserProfileUseCase by lazy {
        ObserveUserProfileUseCase(userProfileRepository)
    }
    override val refreshUserSessionUseCase: RefreshUserSessionUseCase by lazy {
        RefreshUserSessionUseCase(userProfileRepository, authRepository, logoutUserUseCase)
    }
    override val observeSignedInUserIdUseCase by lazy {
        ObserveSignedInUserIdUseCase(
            currentUserProvider
        )
    }
    override val updateUserProfileUseCase by lazy {
        UpdateUserProfileUseCase(userProfileRepository, imageStorageRepository, authRepository)
    }
    override val checkUserLoggedInUseCase: CheckUserLoggedInUseCase by lazy {
        CheckUserLoggedInUseCase(authRepository)
    }
    override val logoutUserUseCase by lazy {
        LogoutUserUseCase(
            authRepository,
            userProfileRepository,
            cancelAllAlertTrackingUseCase,
            clearLastSearchedCityUseCase
        )
    }

    // Preferences Use Cases
    private val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(prefsDataStore)
    }
    override val observeUserPreferencesUseCase by lazy {
        ObserveUserPreferencesUseCase(userPreferencesRepository)
    }
    override val setUnitSystemUseCase by lazy { SetUnitSystemUseCase(userPreferencesRepository) }
    override val setLanguageUseCase by lazy { SetLanguageUseCase(userPreferencesRepository) }
    override val setDarkModeUseCase by lazy { SetDarkModeUseCase(userPreferencesRepository) }
    override val saveLastSearchedCityUseCase by lazy {
        SaveLastSearchedCityUseCase(userPreferencesRepository)
    }
    private val setNotificationsEnabledUseCase by lazy {
        SetNotificationsEnabledUseCase(userPreferencesRepository)
    }
    private val clearLastSearchedCityUseCase by lazy {
        ClearLastSearchedCityUseCase(userPreferencesRepository)
    }


    // Favorites Use Cases
    private val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepoImpl(favoritesDatabase.favoriteDao(), currentUserProvider)
    }
    private val favoritesDatabase: FavoritesDatabase by lazy {
        Room.databaseBuilder(
            context,
            FavoritesDatabase::class.java,
            "favorites_db"
        ).build()
    }
    override val getFavoritesUseCase: GetFavoritesUseCase by lazy {
        GetFavoritesUseCase(favoritesRepository)
    }
    override val deleteFavoriteUseCase: DeleteFavoriteUseCase by lazy {
        DeleteFavoriteUseCase(favoritesRepository)
    }
    override val toggleFavoriteUseCase: ToggleFavoriteUseCase by lazy {
        ToggleFavoriteUseCase(favoritesRepository)
    }
    override val observeFavoriteStatusUseCase: ObserveFavoriteStatusUseCase by lazy {
        ObserveFavoriteStatusUseCase(favoritesRepository)
    }
    override val refreshFavoriteSnapshotUseCase: RefreshFavoriteSnapshotUseCase by lazy {
        RefreshFavoriteSnapshotUseCase(favoritesRepository)
    }
    override val refreshAllFavoritesUseCase: RefreshAllFavoritesUseCase by lazy {
        RefreshAllFavoritesUseCase(favoritesRepository, weatherRepository)
    }
    override val restoreFavoriteUseCase: RestoreFavoriteUseCase by lazy {
        RestoreFavoriteUseCase(favoritesRepository)
    }

    // Time
    private val timeProvider: TimeProvider by lazy { SystemTimeProvider() }

    // Alerts
    private val alertTrackingController: AlertTrackingController by lazy {
        AlertTrackingControllerImpl(context)
    }
    private val alertsDatabase: AlertsDatabase by lazy {
        Room.databaseBuilder(
            context,
            AlertsDatabase::class.java,
            "alerts_db"
        ).build()
    }
    private val alertsRepository: AlertsRepository by lazy {
        AlertsRepoImpl(alertDao = alertsDatabase.alertDao())
    }
    override val getAlertsUseCase: GetAlertsUseCase by lazy {
        GetAlertsUseCase(currentWeatherStore)
    }
    override val trackAlertUseCase: TrackAlertUseCase by lazy {
        TrackAlertUseCase(
            alertsRepository,
            alertTrackingController,
            timeProvider,
            notificationPermissionChecker,
            observeUserPreferencesUseCase
        )
    }
    override val untrackAlertUseCase: UntrackAlertUseCase by lazy {
        UntrackAlertUseCase(alertsRepository, alertTrackingController)
    }
    override val cancelAllAlertTrackingUseCase: CancelAllAlertTrackingUseCase by lazy {
        CancelAllAlertTrackingUseCase(alertsRepository, alertTrackingController)
    }
    override val checkAlertConditionsUseCase: CheckAlertConditionsUseCase by lazy {
        CheckAlertConditionsUseCase(alertsRepository, timeProvider)
    }
    override val toggleAlertsUseCase: ToggleAlertsUseCase by lazy {
        ToggleAlertsUseCase(alertsRepository, trackAlertUseCase, untrackAlertUseCase)
    }
    override val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase by lazy {
        GetTrackedAlertIdsUseCase(alertsRepository)
    }
    override val getTrackedAlertStatusUseCase: GetTrackedAlertStatusUseCase by lazy {
        GetTrackedAlertStatusUseCase(alertsRepository, timeProvider)
    }
    override val restoreAlertTrackingUseCase: RestoreAlertTrackingUseCase by lazy {
        RestoreAlertTrackingUseCase(
            alertsRepository,
            alertTrackingController,
            untrackAlertUseCase,
            timeProvider
        )
    }

    override val refreshTrackingNotificationUseCase by lazy {
        RefreshTrackingNotificationUseCase(alertsRepository, alertTrackingController)
    }

    override val notificationPermissionChecker: NotificationPermissionChecker by lazy {
        AndroidNotificationPermissionChecker(context = context)
    }
    override val toggleNotificationsUseCase by lazy {
        ToggleNotificationsUseCase(
            setNotificationsEnabledUseCase = setNotificationsEnabledUseCase,
            notificationPermissionChecker = notificationPermissionChecker,
            cancelAllAlertTrackingUseCase = cancelAllAlertTrackingUseCase
        )
    }
    override val handleNotificationPermissionResultUseCase by lazy {
        HandleNotificationPermissionResultUseCase(setNotificationsEnabledUseCase)
    }
    override val syncNotificationPermissionUseCase by lazy {
        SyncNotificationPermissionUseCase(
            observeUserPreferencesUseCase = observeUserPreferencesUseCase,
            setNotificationsEnabledUseCase = setNotificationsEnabledUseCase,
            notificationPermissionChecker = notificationPermissionChecker,
            cancelAllAlertTrackingUseCase = cancelAllAlertTrackingUseCase
        )
    }
    override val consumeNotificationPromptUseCase by lazy {
        ConsumeNotificationPromptUseCase(
            notificationPermissionChecker,
            userPreferencesRepository
        )
    }
}