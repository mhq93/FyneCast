package com.mhq.fynecast.favorites.ui

sealed interface FavoritesEvent {
    data class Removed(val cityName: String) : FavoritesEvent
    data class RemoveFailed(val cityName: String) : FavoritesEvent
}