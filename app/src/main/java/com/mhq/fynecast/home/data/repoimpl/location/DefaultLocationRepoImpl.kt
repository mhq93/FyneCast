package com.mhq.fynecast.home.data.repoimpl.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mhq.fynecast.home.domain.models.GeoCoordinates
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository
import com.mhq.fynecast.home.domain.repository.LocationProviderStatusChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

class DefaultLocationRepoImpl(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val statusChecker: LocationProviderStatusChecker
) : DeviceLocationRepository {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): GeoCoordinates? {
        if (!statusChecker.hasLocationPermissions() || !statusChecker.isLocationEnabled()) return null

        val location = try {
            withTimeoutOrNull(FRESH_FIX_TIMEOUT_MILLIS.milliseconds) { requestFreshLocation() }
                ?: withTimeoutOrNull(LAST_KNOWN_TIMEOUT_MILLIS.milliseconds) { lastKnownLocation() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null   // e.g. permission revoked between the check and the request
        }

        return location?.let { GeoCoordinates(latitude = it.latitude, longitude = it.longitude) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellation.cancel() }

            fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
                .addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
        }

    /** A recent cached fix only; an old one could belong to a different city. */
    @SuppressLint("MissingPermission")
    private suspend fun lastKnownLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location?.takeIf { it.ageMillis() <= MAX_CACHED_FIX_AGE_MILLIS })
                }
                .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
                .addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
        }

    private fun Location.ageMillis(): Long =
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000

    private companion object {
        const val LAST_KNOWN_TIMEOUT_MILLIS = 2_000L
        const val FRESH_FIX_TIMEOUT_MILLIS = 8_000L
        const val MAX_CACHED_FIX_AGE_MILLIS = 30 * 60 * 1000L
    }

}