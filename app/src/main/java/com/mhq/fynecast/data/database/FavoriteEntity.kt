package com.mhq.fynecast.data.database

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["cityName", "countryName"]
    //primaryKeys = ["latitude", "longitude"]
)
data class FavoriteEntity(
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val favCityTemp: Double,
    val favCityWeatherIcon: String
)