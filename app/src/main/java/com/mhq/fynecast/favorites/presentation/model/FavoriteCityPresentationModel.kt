package com.mhq.fynecast.favorites.presentation.model

/** What the Favorites screen needs to show one city. The UI never sees the domain model. */
data class FavoriteCityPresentationModel(
    /** Stable identity of the row; the UI sends it back to the ViewModel. */
    val id: String,
    val cityName: String,
    val countryName: String,
    val temperatureC: Double,
    val temperatureF: Double,
    val weatherIconUrl: String
)