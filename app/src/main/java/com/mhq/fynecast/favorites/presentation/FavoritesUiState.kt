package com.mhq.fynecast.favorites.presentation

import com.mhq.fynecast.favorites.presentation.model.FavoriteCityPresentationModel

/** What the Favorites screen shows. */
sealed interface FavoritesUiState {
    /** The first load has not finished yet. */
    data object Loading : FavoritesUiState

    /** The user has no favorites. */
    data object Empty : FavoritesUiState

    data class Success(val favorites: List<FavoriteCityPresentationModel>) : FavoritesUiState
}