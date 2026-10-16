package com.mhq.fynecast.favorites.domain.mapper

import com.mhq.fynecast.favorites.data.database.FavoriteEntity
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel

fun FavoriteCityDomainModel.toEntity(userId: String) = FavoriteEntity(
    userId = userId,
    locationKey = id,
    cityName = cityName,
    countryName = countryName,
    latitude = latitude,
    longitude = longitude,
    temperatureC = temperatureC,
    temperatureF = temperatureF,
    weatherIconUrl = weatherIconUrl
)