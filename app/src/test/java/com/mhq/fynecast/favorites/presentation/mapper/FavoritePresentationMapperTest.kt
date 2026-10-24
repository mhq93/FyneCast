package com.mhq.fynecast.favorites.presentation.mapper

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritePresentationMapperTest {

    private val city = FavoriteCityDomainModel(
        cityName = "Alexandria",
        countryName = "Egypt",
        latitude = 31.2001,
        longitude = 29.9187,
        temperatureC = 24.0,
        temperatureF = 75.2,
        weatherIconUrl = "//cdn.weatherapi.com/icon.png"
    )

    @Test
    fun `row keeps the identity of the favorite`() {
        assertEquals(city.id, city.toPresentation().id)
    }

    @Test
    fun `row carries both temperatures so the screen can pick the unit`() {
        val row = city.toPresentation()

        assertEquals(24.0, row.temperatureC, 0.0)
        assertEquals(75.2, row.temperatureF, 0.0)
    }

    @Test
    fun `row shows the same names and icon as the domain model`() {
        val row = city.toPresentation()

        assertEquals("Alexandria", row.cityName)
        assertEquals("Egypt", row.countryName)
        assertEquals("//cdn.weatherapi.com/icon.png", row.weatherIconUrl)
    }
}