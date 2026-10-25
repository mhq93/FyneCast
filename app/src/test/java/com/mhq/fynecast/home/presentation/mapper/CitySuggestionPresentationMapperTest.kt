package com.mhq.fynecast.home.presentation.mapper

import com.mhq.fynecast.core.domain.location.CitySuggestionModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CitySuggestionPresentationMapperTest {

    @Test
    fun `row shows the full name of the suggestion`() {
        val suggestion = CitySuggestionModel("Alexandria, Egypt", 31.2, 29.9)

        assertEquals("Alexandria, Egypt", suggestion.toPresentation().name)
    }

    @Test
    fun `suggestions at different places get different ids`() {
        val first = CitySuggestionModel("Paris, France", 48.85, 2.35).toPresentation()
        val second = CitySuggestionModel("Paris, Texas", 33.66, -95.55).toPresentation()

        assertNotEquals(first.id, second.id)
    }

    @Test
    fun `the same place always gets the same id`() {
        val suggestion = CitySuggestionModel("Cairo, Egypt", 30.04, 31.23)

        assertEquals(suggestion.toPresentation().id, suggestion.toPresentation().id)
    }
}
