package com.mhq.fynecast.favorites.data.database

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "locationKey"])
data class FavoriteEntity(
    val userId: String,
    val locationKey: String,
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val temperatureF: Double,
    val weatherIconUrl: String
)