package com.mhq.fynecast.favorites.presentation

/** One-off events for the Favorites screen (snackbars). */
sealed interface FavoritesEvent {
    data class Removed(val cityName: String) : FavoritesEvent
    data class RemoveFailed(val cityName: String) : FavoritesEvent
}
