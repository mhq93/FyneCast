package com.mhq.fynecast.favorites.domain.models

data class FavoriteCityDomainModel(
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val temperatureF: Double,
    val weatherIconUrl: String
)

val FavoriteCityDomainModel.stableKey: String get() = "$latitude,$longitude"