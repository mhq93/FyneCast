package com.mhq.fynecast.core.di

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mhq.fynecast.alerts.notifications.data.infra.AndroidNotificationPermissionChecker
import com.mhq.fynecast.alerts.notifications.domain.usecases.HandleNotificationPermissionResultUseCase
import com.mhq.fynecast.alerts.notifications.domain.infra.NotificationPermissionChecker
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.notifications.domain.usecases.SyncNotificationPermissionUseCase
import com.mhq.fynecast.alerts.notifications.domain.usecases.ToggleNotificationsUseCase
import com.mhq.fynecast.alerts.data.database.AlertsDatabase
import com.mhq.fynecast.alerts.data.repoimpl.AlertServiceControllerImpl
import com.mhq.fynecast.alerts.data.repoimpl.AlertsRepoImpl
import com.mhq.fynecast.alerts.domain.repository.AlertServiceController
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.alerts.domain.usecases.GetAlertsUseCase
import com.mhq.fynecast.alerts.domain.usecases.GetTrackedAlertIdsUseCase
import com.mhq.fynecast.alerts.domain.usecases.ToggleAlertsUseCase
import com.mhq.fynecast.auth.data.repoimpl.auth.AuthRepositoryImpl
import com.mhq.fynecast.auth.data.repoimpl.profile.ImageStorageRepositoryImpl
import com.mhq.fynecast.auth.data.repoimpl.auth.SocialAuthCredentialProviderImpl
import com.mhq.fynecast.auth.data.repoimpl.profile.UserProfileRepositoryImpl
import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository
import com.mhq.fynecast.auth.domain.repository.profile.ImageStorageRepository
import com.mhq.fynecast.auth.domain.repository.auth.SocialAuthCredentialProvider
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
import com.mhq.fynecast.auth.ui.editprofile.ImageStorageManager
import com.mhq.fynecast.core.data.infra.AppInitializerImpl
import com.mhq.fynecast.core.domain.infra.AppInitializer
import com.mhq.fynecast.auth.domain.usecases.session.CheckUserLoggedInUseCase
import com.mhq.fynecast.favorites.data.database.FavoritesDatabase
import com.mhq.fynecast.favorites.data.repoimpl.FavoritesRepoImpl
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.InsertFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.ToggleFavoriteUseCase
import com.mhq.fynecast.home.data.api.location.LocationApiService
import com.mhq.fynecast.home.data.api.weather.WeatherApiService
import com.mhq.fynecast.home.data.infra.AndroidLocationStatusChecker
import com.mhq.fynecast.home.data.repoimpl.location.DefaultLocationRepoImpl
import com.mhq.fynecast.home.data.repoimpl.location.PhotonLocationRepoImpl
import com.mhq.fynecast.home.data.repoimpl.weather.WeatherRepoImpl
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository
import com.mhq.fynecast.home.domain.repository.LocationProviderStatusChecker
import com.mhq.fynecast.home.domain.repository.LocationRepository
import com.mhq.fynecast.home.domain.repository.WeatherRepository
import com.mhq.fynecast.home.domain.usecases.FetchWeatherDataUseCase
import com.mhq.fynecast.home.domain.usecases.GetCurrentLocationUseCase
import com.mhq.fynecast.home.domain.usecases.GetLastSearchedCityUseCase
import com.mhq.fynecast.home.domain.usecases.GetLocationSuggestionsUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveCurrentCityNameUseCase
import com.mhq.fynecast.home.domain.usecases.ObserveFavoriteStatusUseCase
import com.mhq.fynecast.map.domain.usecases.ReverseGeocodeLocationUseCase
import com.mhq.fynecast.settings.data.repoimpl.UserPreferencesRepositoryImpl
import com.mhq.fynecast.settings.domain.repository.UserPreferencesRepository
import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.UpdatePreferenceUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAppContainer(
    private val context: Context,
    private val prefsDataStore: DataStore<Preferences>,
    private val sessionDataStore: DataStore<Preferences>
) : AppContainer {

    override val appInitializer: AppInitializer by lazy {
        AppInitializerImpl(context.applicationContext as Application)
    }

    // Infrastructure Components
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val imageStorageManager: ImageStorageManager by lazy { ImageStorageManager(context) }


    // Networking — Weather
    private val weatherApiBaseUrl = "https://api.weatherapi.com/"
    private val weatherApiJson =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
    private val weatherApiRetrofit = Retrofit.Builder()
        .addConverterFactory(weatherApiJson.asConverterFactory("application/json".toMediaType()))
        .baseUrl(weatherApiBaseUrl)
        .build()
    private val weatherApiRetrofitService: WeatherApiService by lazy {
        weatherApiRetrofit.create(WeatherApiService::class.java)
    }
    override val weatherRepository: WeatherRepository by lazy {
        WeatherRepoImpl(weatherApiService = weatherApiRetrofitService)
    }
    override val fetchWeatherDataUseCase: FetchWeatherDataUseCase by lazy {
        FetchWeatherDataUseCase(weatherRepository)
    }
    override val observeCurrentCityNameUseCase: ObserveCurrentCityNameUseCase by lazy {
        ObserveCurrentCityNameUseCase(weatherRepository)
    }


    // Networking — Photon
    private val photonBaseUrl = "https://photon.komoot.io/"
    private val photonJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val photonRetrofit: Retrofit = Retrofit.Builder()
        .addConverterFactory(photonJson.asConverterFactory("application/json".toMediaType()))
        .baseUrl(photonBaseUrl)
        .build()
    private val photonApiRetrofitService: LocationApiService by lazy {
        photonRetrofit.create(LocationApiService::class.java)
    }



    // Repositories
    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseAuth, sessionDataStore)
    }
    override val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepositoryImpl(firebaseAuth, sessionDataStore)
    }
    override val imageStorageRepository: ImageStorageRepository by lazy {
        ImageStorageRepositoryImpl(context, imageStorageManager)
    }


    // Location Tracker
    private val locationProviderStatusChecker: LocationProviderStatusChecker by lazy {
        AndroidLocationStatusChecker(appContext = context)
    }
    override val deviceLocationRepository: DeviceLocationRepository by lazy {
        DefaultLocationRepoImpl(
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context),
            statusChecker = locationProviderStatusChecker
        )
    }
    override val locationRepository: LocationRepository by lazy {
        PhotonLocationRepoImpl(photonApiRetrofitService)
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
        SocialAuthCredentialProviderImpl(
            serverClientId = context.getString(R.string.default_web_client_id)
        )
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
    override val authenticateWithSocialProviderUseCase: AuthenticateWithSocialProviderUseCase by lazy {
        AuthenticateWithSocialProviderUseCase(
            socialAuthCredentialProvider = socialAuthCredentialProvider,
            authenticateWithSocialTokenUseCase = authenticateWithSocialTokenUseCase
        )
    }
    override val sendPasswordResetUseCase by lazy {
        SendPasswordResetUseCase(authRepository, validateLoginFormUseCase)
    }


    // Profile & Logout Use Cases
    override val validateEditProfileUseCase by lazy {
        ValidateEditProfileUseCase()
    }
    override val observeUserProfileUseCase by lazy {
        ObserveUserProfileUseCase(userProfileRepository)
    }
    override val updateUserProfileUseCase by lazy {
        UpdateUserProfileUseCase(
            userProfileRepository,
            imageStorageRepository
        )
    }
    override val checkUserLoggedInUseCase: CheckUserLoggedInUseCase by lazy {
        CheckUserLoggedInUseCase(authRepository)
    }
    override val logoutUserUseCase by lazy {
        LogoutUserUseCase(authRepository, userProfileRepository)
    }

    // Preferences Use Cases
    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(prefsDataStore)
    }
    override val observeUserPreferencesUseCase by lazy {
        ObserveUserPreferencesUseCase(userPreferencesRepository)
    }
    override val updatePreferenceUseCase by lazy {
        UpdatePreferenceUseCase(userPreferencesRepository)
    }


    // Favorites Use Cases
    override val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepoImpl(favoriteDao = favoritesDatabase.favoriteDao())
    }
    private val favoritesDatabase: FavoritesDatabase by lazy {
        Room.databaseBuilder(
            context,
            FavoritesDatabase::class.java,
            "favorites_db").build()
    }
    override val insertFavoriteUseCase: InsertFavoriteUseCase by lazy {
        InsertFavoriteUseCase(favoritesRepository)
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


    // Alerts Use Cases
    private val alertServiceController: AlertServiceController by lazy {
        AlertServiceControllerImpl(context)
    }
    override val alertsRepository: AlertsRepository by lazy {
        AlertsRepoImpl(
            alertDao = alertsDatabase.alertDao(),
            alertServiceController = alertServiceController
        )
    }
    private val alertsDatabase: AlertsDatabase by lazy {
        Room.databaseBuilder(context, AlertsDatabase::class.java, "alerts_db").build()
    }
    override val getAlertsUseCase: GetAlertsUseCase by lazy {
        GetAlertsUseCase(weatherRepository)
    }
    override val toggleAlertsUseCase: ToggleAlertsUseCase by lazy {
        ToggleAlertsUseCase(alertsRepository)
    }
    override val getTrackedAlertIdsUseCase: GetTrackedAlertIdsUseCase by lazy {
        GetTrackedAlertIdsUseCase(alertsRepository)
    }

    //Notifications...
    override val notificationPermissionChecker: NotificationPermissionChecker by lazy {
        AndroidNotificationPermissionChecker(context = context)
    }
    override val toggleNotificationsUseCase by lazy {
        ToggleNotificationsUseCase(updatePreferenceUseCase, notificationPermissionChecker)
    }
    override val handleNotificationPermissionResultUseCase by lazy {
        HandleNotificationPermissionResultUseCase(updatePreferenceUseCase)
    }
    override val syncNotificationPermissionUseCase by lazy {
        SyncNotificationPermissionUseCase(
            observeUserPreferencesUseCase,
            updatePreferenceUseCase,
            notificationPermissionChecker
        )
    }
}