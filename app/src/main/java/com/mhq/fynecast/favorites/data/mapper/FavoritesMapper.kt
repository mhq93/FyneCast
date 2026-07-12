package com.mhq.fynecast.favorites.data.mapper

import com.mhq.fynecast.favorites.data.database.FavoriteEntity
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel

fun FavoriteEntity.toDomain() = FavoriteCityDomainModel(
    cityName = cityName,
    countryName = countryName,
    latitude = latitude,
    longitude = longitude,
    temperatureC = temperatureC,
    temperatureF = temperatureF,
    weatherIconUrl = weatherIconUrl
)

fun FavoriteCityDomainModel.toEntity() = FavoriteEntity(
    cityName = cityName,
    countryName = countryName,
    latitude = latitude,
    longitude = longitude,
    temperatureC = temperatureC,
    temperatureF = temperatureF,
    weatherIconUrl = weatherIconUrl
)