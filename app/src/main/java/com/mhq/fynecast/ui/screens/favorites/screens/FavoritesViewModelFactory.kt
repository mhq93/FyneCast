package com.mhq.fynecast.ui.screens.favorites.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mhq.fynecast.domain.usecases.favorites.DeleteFavoriteUseCase
import com.mhq.fynecast.domain.usecases.favorites.FetchFavoritesUseCase

class FavoritesViewModelFactory(
    private val fetchFavoritesUseCase: FetchFavoritesUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FavoritesViewModel(
            fetchFavoritesUseCase = fetchFavoritesUseCase,
            deleteFavoriteUseCase = deleteFavoriteUseCase,
        ) as T
    }
}