package com.mhq.fynecast.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.google.android.gms.location.LocationServices
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mhq.fynecast.data.database.FavoritesDatabase
import com.mhq.fynecast.data.network.location.LocationApiService
import com.mhq.fynecast.data.network.weather.WeatherApiService
import com.mhq.fynecast.data.repository.AppRepository
import com.mhq.fynecast.data.repository.FavoritesRepoImpl
import com.mhq.fynecast.data.repository.FavoritesRepository
import com.mhq.fynecast.data.repository.PhotonLocationRepository
import com.mhq.fynecast.data.repository.UserProfileRepository
import com.mhq.fynecast.data.repository.WeatherApiRepository
import com.mhq.fynecast.domain.usecases.favorites.DeleteFavoriteUseCase
import com.mhq.fynecast.domain.usecases.favorites.FetchFavoritesUseCase
import com.mhq.fynecast.domain.usecases.favorites.InsertFavoriteUseCase
import com.mhq.fynecast.domain.usecases.favorites.ToggleFavoriteUseCase
import com.mhq.fynecast.data.network.location.DefaultLocationTracker
import com.mhq.fynecast.data.network.location.LocationTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAppContainer(private val context: Context) : AppContainer {

    //Networking weather data...
    private val weatherApiBaseUrl = "https://api.weatherapi.com/"
    private val weatherApiJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val weatherApiRetrofit = Retrofit.Builder()
        .addConverterFactory(weatherApiJson.asConverterFactory("application/json".toMediaType()))
        .baseUrl(weatherApiBaseUrl)
        .build()

    private val weatherApiRetrofitService: WeatherApiService by lazy {
        weatherApiRetrofit.create(WeatherApiService::class.java)
    }

    override val appRepository: AppRepository by lazy {
        WeatherApiRepository(weatherApiRetrofitService)
    }

    //Location...
    override val locationTracker: LocationTracker by lazy {
        DefaultLocationTracker(
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context),
            application = context as Application
        )
    }

    //location autocomplete search...
    private val photonBaseUrl = "https://photon.komoot.io/"
    private val photonJson = Json {
        ignoreUnknownKeys = true // MANDATORY for Photon
        coerceInputValues = true
    }
    private val photonRetrofit: Retrofit = Retrofit.Builder()
        .addConverterFactory(photonJson.asConverterFactory("application/json".toMediaType()))
        .baseUrl(photonBaseUrl)
        .build()
    private val photonApiRetrofitService: LocationApiService by lazy {
        photonRetrofit.create(LocationApiService::class.java)
    }

    override val photonLocationRepository: PhotonLocationRepository by lazy {
        PhotonLocationRepository(photonApiRetrofitService)
    }

    override val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepository()
    }

    //Room...
    private val database = Room.databaseBuilder(
        context,
        FavoritesDatabase::class.java,
        "favorites_db"
    ).build()
    private val dao = database.favoriteDao()
    private val repository = FavoritesRepoImpl(dao)
    override val insertFavoriteUseCase = InsertFavoriteUseCase(repository)
    override val fetchFavoritesUseCase = FetchFavoritesUseCase(repository)
    override val deleteFavoritesUseCase = DeleteFavoriteUseCase(repository)
    override val toggleFavoriteUseCase = ToggleFavoriteUseCase(repository)
    override val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepoImpl(database.favoriteDao())
    }
}