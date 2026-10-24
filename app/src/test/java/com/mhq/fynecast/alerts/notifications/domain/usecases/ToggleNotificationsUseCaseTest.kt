package com.mhq.fynecast.alerts.notifications.domain.usecases

import com.mhq.fynecast.alerts.domain.usecases.CancelAllAlertTrackingUseCase
import com.mhq.fynecast.settings.domain.usecases.profile.SetNotificationsEnabledUseCase
import com.mhq.fynecast.testing.EventLog
import com.mhq.fynecast.testing.FakeAlertTrackingController
import com.mhq.fynecast.testing.FakeAlertsRepository
import com.mhq.fynecast.testing.FakeNotificationPermissionChecker
import com.mhq.fynecast.testing.FakeUserPreferencesRepository
import com.mhq.fynecast.testing.preferences
import com.mhq.fynecast.testing.tracked
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ToggleNotificationsUseCaseTest {

    private val log = EventLog()
    private val alerts = FakeAlertsRepository(log)
    private val controller = FakeAlertTrackingController(log)
    private val prefs = FakeUserPreferencesRepository(preferences(notificationsEnabled = true), log)
    private val permission = FakeNotificationPermissionChecker(granted = true)

    private val toggle = ToggleNotificationsUseCase(
        setNotificationsEnabledUseCase = SetNotificationsEnabledUseCase(prefs),
        notificationPermissionChecker = permission,
        cancelAllAlertTrackingUseCase = CancelAllAlertTrackingUseCase(alerts, controller)
    )

    @Test
    fun `turning notifications off stops all tracking and saves the preference`() = runTest {
        alerts.tracked += tracked("a")
        alerts.tracked += tracked("b", lat = 48.8, lon = 2.3)

        val result = toggle(false)

        assertEquals(ToggleNotificationsUseCase.Result.Disabled, result)
        assertTrue(alerts.tracked.isEmpty())
        assertEquals(listOf("a", "b"), controller.cancelledWorkers)
        assertFalse(prefs.current.notificationsEnabled)
    }

    @Test
    fun `tracking is torn down before the preference is switched off`() = runTest {
        alerts.tracked += tracked("a")

        toggle(false)

        val events = log.events
        assertTrue(events.indexOf("repo:deleteAll") < events.indexOf("prefs:notifications=false"))
    }

    @Test
    fun `if tearing down fails the preference stays enabled`() = runTest {
        alerts.tracked += tracked("a")
        alerts.failOnDeleteAll = true

        try {
            toggle(false)
            fail("expected the failure to propagate")
        } catch (expected: IllegalStateException) {
            // the UI must not claim notifications are off while workers still run
        }

        assertTrue(prefs.current.notificationsEnabled)
    }

    @Test
    fun `turning notifications on with permission saves the preference`() = runTest {
        prefs.current = preferences(notificationsEnabled = false)
        permission.granted = true

        val result = toggle(true)

        assertEquals(ToggleNotificationsUseCase.Result.Enabled, result)
        assertTrue(prefs.current.notificationsEnabled)
    }

    @Test
    fun `turning notifications on without permission asks for it and changes nothing`() = runTest {
        prefs.current = preferences(notificationsEnabled = false)
        permission.granted = false

        val result = toggle(true)

        assertEquals(ToggleNotificationsUseCase.Result.PermissionRequired, result)
        assertFalse(prefs.current.notificationsEnabled)
    }
}
