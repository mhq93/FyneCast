package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.settings.domain.usecases.profile.ObserveUserPreferencesUseCase
import com.mhq.fynecast.testing.FakeAlertTrackingController
import com.mhq.fynecast.testing.FakeAlertsRepository
import com.mhq.fynecast.testing.FakeNotificationPermissionChecker
import com.mhq.fynecast.testing.FakeTimeProvider
import com.mhq.fynecast.testing.FakeUserPreferencesRepository
import com.mhq.fynecast.testing.alert
import com.mhq.fynecast.testing.location
import com.mhq.fynecast.testing.preferences
import com.mhq.fynecast.testing.tracked
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackAndToggleAlertsUseCaseTest {

    private val alerts = FakeAlertsRepository()
    private val controller = FakeAlertTrackingController()
    private val time = FakeTimeProvider(now = 1_000L)
    private val permission = FakeNotificationPermissionChecker(granted = true)
    private val prefs = FakeUserPreferencesRepository(preferences(notificationsEnabled = true))
    private val cairo = location()

    private val track = TrackAlertUseCase(
        alertsRepository = alerts,
        alertTrackingController = controller,
        timeProvider = time,
        notificationPermissionChecker = permission,
        observeUserPreferencesUseCase = ObserveUserPreferencesUseCase(prefs)
    )
    private val untrack = UntrackAlertUseCase(alerts, controller)
    private val toggle = ToggleAlertsUseCase(alerts, track, untrack)

    // ---- TrackAlertUseCase ----

    @Test
    fun `an alert without an expiry time cannot be tracked`() = runTest {
        val result = track(alert(expiresMillis = null), cairo)

        assertEquals(TrackAlertUseCase.Result.ExpiryUnknown, result)
        assertTrue(alerts.tracked.isEmpty())
    }

    @Test
    fun `an alert that already expired cannot be tracked`() = runTest {
        assertEquals(TrackAlertUseCase.Result.AlreadyExpired, track(alert(expiresMillis = 1_000L), cairo))
        assertEquals(TrackAlertUseCase.Result.AlreadyExpired, track(alert(expiresMillis = 999L), cairo))
        assertTrue(alerts.tracked.isEmpty())
    }

    @Test
    fun `tracking needs notifications enabled in the app`() = runTest {
        prefs.current = preferences(notificationsEnabled = false)

        assertEquals(TrackAlertUseCase.Result.NotificationsRequired, track(alert(), cairo))
        assertTrue(alerts.tracked.isEmpty())
    }

    @Test
    fun `tracking needs the system notification permission`() = runTest {
        permission.granted = false

        assertEquals(TrackAlertUseCase.Result.NotificationsRequired, track(alert(), cairo))
        assertTrue(alerts.tracked.isEmpty())
    }

    @Test
    fun `tracking saves the alert, shows the notification and schedules the checks`() = runTest {
        val result = track(alert("a"), cairo)

        assertEquals(TrackAlertUseCase.Result.Tracked, result)
        assertEquals(listOf("a"), alerts.tracked.map { it.id })
        assertEquals(listOf("a"), controller.shownNotifications.single().map { it.id })
        assertEquals(listOf("a"), controller.scheduledWorkers)
        assertEquals(listOf(cairo), controller.scheduledLiftedChecks)
    }

    // ---- ToggleAlertsUseCase ----

    @Test
    fun `toggling an untracked alert tracks it`() = runTest {
        assertEquals(ToggleAlertsUseCase.Result.Tracked, toggle(alert("a"), cairo))
        assertEquals(listOf("a"), alerts.tracked.map { it.id })
    }

    @Test
    fun `toggling an expired untracked alert reports it as expired`() = runTest {
        assertEquals(
            ToggleAlertsUseCase.Result.AlreadyExpired,
            toggle(alert("a", expiresMillis = 500L), cairo)
        )
    }

    @Test
    fun `toggling a tracked alert untracks it and cancels its worker`() = runTest {
        alerts.tracked += tracked("a")

        val result = toggle(alert("a"), cairo)

        assertEquals(ToggleAlertsUseCase.Result.Untracked, result)
        assertTrue(alerts.tracked.isEmpty())
        assertEquals(listOf("a"), controller.cancelledWorkers)
    }

    @Test
    fun `the lifted-alert check is cancelled when the last alert at a location is untracked`() = runTest {
        alerts.tracked += tracked("a")

        toggle(alert("a"), cairo)

        assertEquals(listOf(cairo), controller.cancelledLiftedChecks)
    }

    @Test
    fun `the lifted-alert check stays while another alert is tracked at the same location`() = runTest {
        alerts.tracked += tracked("a")
        alerts.tracked += tracked("b")

        toggle(alert("a"), cairo)

        assertTrue(controller.cancelledLiftedChecks.isEmpty())
        assertEquals(listOf("b"), alerts.tracked.map { it.id })
    }
}
