package com.mhq.fynecast.di

import com.mhq.fynecast.domain.usecases.favorites.FetchFavoritesUseCase
import com.mhq.fynecast.domain.usecases.favorites.InsertFavoriteUseCase
import com.mhq.fynecast.data.repository.AppRepository
import com.mhq.fynecast.domain.usecases.favorites.DeleteFavoriteUseCase
import com.mhq.fynecast.domain.usecases.favorites.ToggleFavoriteUseCase
import com.mhq.fynecast.data.network.location.LocationTracker
import com.mhq.fynecast.data.repository.PhotonLocationRepository
import com.mhq.fynecast.data.repository.FavoritesRepository
import com.mhq.fynecast.data.repository.UserProfileRepository

interface AppContainer {
    val appRepository: AppRepository
    val favoritesRepository: FavoritesRepository
    val photonLocationRepository: PhotonLocationRepository
    val userProfileRepository: UserProfileRepository
    val insertFavoriteUseCase: InsertFavoriteUseCase
    val fetchFavoritesUseCase: FetchFavoritesUseCase
    val deleteFavoritesUseCase: DeleteFavoriteUseCase
    val toggleFavoriteUseCase: ToggleFavoriteUseCase
    val locationTracker: LocationTracker
}