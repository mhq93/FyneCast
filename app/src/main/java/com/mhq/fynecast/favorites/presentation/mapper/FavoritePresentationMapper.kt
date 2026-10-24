package com.mhq.fynecast.favorites.presentation.mapper

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.presentation.model.FavoriteCityPresentationModel

fun FavoriteCityDomainModel.toPresentation(): FavoriteCityPresentationModel =
    FavoriteCityPresentationModel(
        id = id,
        cityName = cityName,
        countryName = countryName,
        temperatureC = temperatureC,
        temperatureF = temperatureF,
        weatherIconUrl = weatherIconUrl
    )
