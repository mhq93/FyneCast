package com.mhq.fynecast.favorites.domain.models

import kotlin.math.roundToLong

data class FavoriteCityDomainModel(
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val temperatureF: Double,
    val weatherIconUrl: String
) {
    /** The only identity of a favorite:
     * its location, rounded to about 1 km. */
    val id: String get() = favoriteId(latitude, longitude)
}

fun favoriteId(latitude: Double, longitude: Double): String =
    "${(latitude * 100).roundToLong() / 100.0},${(longitude * 100).roundToLong() / 100.0}"