package com.mhq.fynecast.favorites.data.database

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "cityName", "countryName"]
)
data class FavoriteEntity(
    val userId: String,
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val temperatureF: Double,
    val weatherIconUrl: String
)