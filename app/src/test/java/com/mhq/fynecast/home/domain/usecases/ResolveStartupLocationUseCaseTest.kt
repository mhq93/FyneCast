package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.core.domain.location.GeoCoordinates
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository
import com.mhq.fynecast.testing.FakeUserPreferencesRepository
import com.mhq.fynecast.testing.preferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** A fake GPS: you decide what it returns (or throws), and it counts how often it was asked. */
private class FakeDeviceLocationRepository(
    var result: GeoCoordinates? = null,
    var error: Throwable? = null
) : DeviceLocationRepository {
    var calls = 0

    override suspend fun getCurrentLocation(): GeoCoordinates? {
        calls++
        error?.let { throw it }
        return result
    }
}

class ResolveStartupLocationUseCaseTest {

    private val gps = FakeDeviceLocationRepository()

    private fun useCase(
        savedCity: Boolean = false,
        defaultCity: String = "Cairo"
    ): ResolveStartupLocationUseCase {
        val prefs = if (savedCity) {
            preferences().copy(
                lastSearchedCityName = "Alexandria, Egypt",
                lastSearchedCityLat = 31.2,
                lastSearchedCityLon = 29.9
            )
        } else {
            preferences()
        }
        return ResolveStartupLocationUseCase(
            getCurrentLocationUseCase = GetCurrentLocationUseCase(gps),
            getLastSearchedCityUseCase = GetLastSearchedCityUseCase(FakeUserPreferencesRepository(prefs)),
            defaultCity = defaultCity
        )
    }

    @Test
    fun `uses the device location when allowed and available`() = runTest {
        gps.result = GeoCoordinates(30.04, 31.23)

        val result = useCase(savedCity = true)(useDeviceLocation = true)

        assertTrue(result.fromDevice)
        assertEquals("30.04,31.23", result.query)
        assertNull(result.name)
        assertEquals(30.04, result.latitude)
        assertEquals(31.23, result.longitude)
    }

    @Test
    fun `does not even ask the GPS when device location is not allowed`() = runTest {
        gps.result = GeoCoordinates(30.04, 31.23)

        val result = useCase(savedCity = true)(useDeviceLocation = false)

        assertEquals(0, gps.calls)
        assertFalse(result.fromDevice)
        assertEquals("Alexandria, Egypt", result.name)
    }

    @Test
    fun `falls back to the last searched city when the GPS finds nothing`() = runTest {
        gps.result = null

        val result = useCase(savedCity = true)(useDeviceLocation = true)

        assertFalse(result.fromDevice)
        assertEquals("31.2,29.9", result.query)
        assertEquals("Alexandria, Egypt", result.name)
        assertEquals(31.2, result.latitude)
        assertEquals(29.9, result.longitude)
    }

    @Test
    fun `falls back to the last searched city when the GPS throws`() = runTest {
        gps.error = IllegalStateException("no fix")

        val result = useCase(savedCity = true)(useDeviceLocation = true)

        assertFalse(result.fromDevice)
        assertEquals("Alexandria, Egypt", result.name)
    }

    @Test
    fun `falls back to the default city when there is no GPS and no saved city`() = runTest {
        val result = useCase(savedCity = false)(useDeviceLocation = true)

        assertFalse(result.fromDevice)
        assertEquals("Cairo", result.query)
        assertNull(result.name)
        assertNull(result.latitude)
        assertNull(result.longitude)
    }

    @Test
    fun `uses the default city that was passed in`() = runTest {
        val result = useCase(savedCity = false, defaultCity = "Paris")(useDeviceLocation = false)

        assertEquals("Paris", result.query)
    }

    @Test
    fun `a cancelled coroutine is not swallowed as a GPS failure`() = runTest {
        gps.error = CancellationException("cancelled")

        try {
            useCase(savedCity = true)(useDeviceLocation = true)
            fail("CancellationException should have been rethrown")
        } catch (expected: CancellationException) {
            // correct: cancellation must keep travelling up
        }
    }
}
