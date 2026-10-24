package com.mhq.fynecast.alerts.presentation.mapper

import com.mhq.fynecast.alerts.presentation.model.AlertSeverity
import com.mhq.fynecast.testing.alert
import org.junit.Assert.assertEquals
import org.junit.Test

class AlertPresentationMapperTest {

    @Test
    fun `known severities are recognised whatever their letter case`() {
        assertEquals(AlertSeverity.Extreme, "Extreme".toAlertSeverity())
        assertEquals(AlertSeverity.Severe, "SEVERE".toAlertSeverity())
        assertEquals(AlertSeverity.Moderate, " moderate ".toAlertSeverity())
    }

    @Test
    fun `minor, unknown and empty severities map to Other`() {
        assertEquals(AlertSeverity.Other, "Minor".toAlertSeverity())
        assertEquals(AlertSeverity.Other, "Unknown".toAlertSeverity())
        assertEquals(AlertSeverity.Other, "".toAlertSeverity())
    }

    @Test
    fun `mapping keeps the texts and the original severity label`() {
        val domain = alert(id = "a1").copy(
            headline = "Flood warning",
            event = "Flood",
            severity = "Severe",
            description = "Water is rising",
            instruction = "Move uphill"
        )

        val result = domain.toPresentation()

        assertEquals("a1", result.id)
        assertEquals("Flood warning", result.headline)
        assertEquals("Flood", result.event)
        assertEquals("Water is rising", result.description)
        assertEquals("Move uphill", result.instruction)
        assertEquals(AlertSeverity.Severe, result.severity)
        assertEquals("Severe", result.severityLabel)
    }
}