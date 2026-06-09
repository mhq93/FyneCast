package com.mhq.fynecast.favorites.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.FyneCastApplication
import com.mhq.fynecast.favorites.database.FavoriteEntity
import com.mhq.fynecast.favorites.domain.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.FetchFavoritesUseCase
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val fetchFavoritesUseCase: FetchFavoritesUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,
) : ViewModel() {

    val favorites = fetchFavoritesUseCase()

    fun deleteFavorite(favoriteEntity: FavoriteEntity) {
        viewModelScope.launch {
            deleteFavoriteUseCase(favoriteEntity = favoriteEntity)
        }
    }

    fun onFavoriteClick(favorite: FavoriteEntity, onNavigateHome: () -> Unit) {
        onNavigateHome()
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as FyneCastApplication)
                FavoritesViewModel(
                    fetchFavoritesUseCase = application.container.fetchFavoritesUseCase,
                    deleteFavoriteUseCase = application.container.deleteFavoritesUseCase
                )
            }
        }
    }
}