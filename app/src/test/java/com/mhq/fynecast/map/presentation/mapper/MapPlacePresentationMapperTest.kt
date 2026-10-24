package com.mhq.fynecast.map.presentation.mapper

import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import org.junit.Assert.assertEquals
import org.junit.Test

class MapPlacePresentationMapperTest {

    @Test
    fun `place name shown on the card is the full name of the resolved city`() {
        val city = CitySuggestionModel(fullName = "Alexandria, Egypt", latitude = 31.2, longitude = 29.9)

        assertEquals("Alexandria, Egypt", city.toPresentation().name)
    }
}