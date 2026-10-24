package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.testing.FakeAlertsRepository
import com.mhq.fynecast.testing.FakeTimeProvider
import com.mhq.fynecast.testing.alert
import com.mhq.fynecast.testing.location
import com.mhq.fynecast.testing.tracked
import com.mhq.fynecast.testing.weather
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckAlertConditionsUseCaseTest {

    private val repository = FakeAlertsRepository()
    private val time = FakeTimeProvider(now = 1_000L)
    private val check = CheckAlertConditionsUseCase(repository, time)
    private val cairo = location()

    @Test
    fun `an alert past its expiry time is reported even without a weather snapshot`() = runTest {
        repository.tracked += tracked("a", expiresMillis = 500L)

        assertEquals(listOf("a"), check(cairo, null).map { it.id })
    }

    @Test
    fun `an alert that expires exactly now counts as expired`() = runTest {
        repository.tracked += tracked("a", expiresMillis = 1_000L)

        assertEquals(listOf("a"), check(cairo, null).map { it.id })
    }

    @Test
    fun `an unexpired alert is kept when there is no snapshot to compare against`() = runTest {
        repository.tracked += tracked("a", expiresMillis = 5_000L)

        assertEquals(emptyList<String>(), check(cairo, null).map { it.id })
    }

    @Test
    fun `an unexpired alert that disappeared from the snapshot is reported as lifted`() = runTest {
        repository.tracked += tracked("a", expiresMillis = 5_000L)

        val result = check(cairo, weather(alerts = emptyList()))

        assertEquals(listOf("a"), result.map { it.id })
    }

    @Test
    fun `an unexpired alert still present in the snapshot is kept`() = runTest {
        repository.tracked += tracked("a", expiresMillis = 5_000L)

        val result = check(cairo, weather(alerts = listOf(alert("a"))))

        assertEquals(emptyList<String>(), result.map { it.id })
    }

    @Test
    fun `a snapshot of a far away city is ignored so alerts are not wrongly lifted`() = runTest {
        repository.tracked += tracked("a", expiresMillis = 5_000L)

        val result = check(cairo, weather(lat = 40.7, lon = -74.0, alerts = emptyList()))

        assertEquals(emptyList<String>(), result.map { it.id })
    }

    @Test
    fun `a snapshot slightly off the tracked point still counts as the same place`() = runTest {
        repository.tracked += tracked("a", expiresMillis = 5_000L)

        val result = check(cairo, weather(lat = 30.05, lon = 31.05, alerts = emptyList()))

        assertEquals(listOf("a"), result.map { it.id })
    }

    @Test
    fun `only alerts tracked at the given location are considered`() = runTest {
        repository.tracked += tracked("cairo", lat = 30.0, lon = 31.0, expiresMillis = 500L)
        repository.tracked += tracked("paris", lat = 48.8, lon = 2.3, expiresMillis = 500L)

        assertEquals(listOf("cairo"), check(cairo, null).map { it.id })
    }
}
