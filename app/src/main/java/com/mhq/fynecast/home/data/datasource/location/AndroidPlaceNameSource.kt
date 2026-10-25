package com.mhq.fynecast.home.data.datasource.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/** Place names from Android's built-in Geocoder
 * (Google backend): works for any app language. */
class AndroidPlaceNameSource(context: Context) : PlaceNameSource {

    private val appContext = context.applicationContext

    override suspend fun nameAt(latitude: Double, longitude: Double, langCode: String): String? {
        val geocoder = geocoderFor(langCode) ?: return null
        val address = withTimeoutOrNull(LOOKUP_TIMEOUT_MILLIS.milliseconds) {
            reverseLookup(geocoder, latitude, longitude)
        } ?: return null
        return displayName(address)
    }

    override suspend fun localize(
        name: String,
        latitude: Double,
        longitude: Double,
        langCode: String
    ): String? {
        val geocoder = geocoderFor(langCode) ?: return null
        val addresses = withTimeoutOrNull(LOOKUP_TIMEOUT_MILLIS.milliseconds) {
            forwardLookup(geocoder, name)
        } ?: return null
        val closest = addresses
            .filter { it.hasLatitude() && it.hasLongitude() }
            .minByOrNull {
                distanceMeters(
                    latitude,
                    longitude,
                    it.latitude,
                    it.longitude
                )
            }
            ?: return null
        if (distanceMeters(
                latitude,
                longitude,
                closest.latitude,
                closest.longitude
            )
            > MAX_MATCH_DISTANCE_METERS
        ) return null
        return displayName(closest)
    }

    private fun geocoderFor(langCode: String): Geocoder? =
        if (Geocoder.isPresent())
            Geocoder(appContext, Locale.forLanguageTag(langCode))
        else
            null

    @Suppress("DEPRECATION")
    private suspend fun reverseLookup(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): Address? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive)
                                continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive)
                                continuation.resume(null)
                        }
                    }
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                } catch (e: IOException) {
                    null
                }
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun forwardLookup(geocoder: Geocoder, name: String): List<Address> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(
                    name,
                    MAX_RESULTS,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(emptyList())
                        }
                    }
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocationName(name, MAX_RESULTS) ?: emptyList()
                } catch (e: IOException) {
                    emptyList()
                }
            }
        }

    /** "City, Country" (or just the name when both are the same). */
    private fun displayName(address: Address): String? {
        val place = listOf(address.locality, address.subAdminArea, address.adminArea)
            .firstOrNull { !it.isNullOrBlank() } ?: address.countryName
        if (place.isNullOrBlank()) return null
        val country = address.countryName
        return if (!country.isNullOrBlank() && country != place) "$place, $country" else place
    }

    private fun distanceMeters(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double
    ): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            fromLatitude,
            fromLongitude,
            toLatitude,
            toLongitude,
            result
        )
        return result[0]
    }

    private companion object {
        const val LOOKUP_TIMEOUT_MILLIS = 3_000L
        const val MAX_RESULTS = 5
        const val MAX_MATCH_DISTANCE_METERS = 50_000f
    }
}
