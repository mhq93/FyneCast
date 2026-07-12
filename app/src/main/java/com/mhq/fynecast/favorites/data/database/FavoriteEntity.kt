package com.mhq.fynecast.favorites.data.database

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["cityName", "countryName"]
)
data class FavoriteEntity(
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val temperatureF: Double,
    val weatherIconUrl: String
)