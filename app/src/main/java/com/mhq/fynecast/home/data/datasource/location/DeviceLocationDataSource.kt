package com.mhq.fynecast.home.data.datasource.location

import android.location.Location

/** The phone's position from the device itself (no permission or settings checks here). */
interface DeviceLocationDataSource {
    /** A fresh fix, else a recent cached one, else null. Never throws except on cancellation. */
    suspend fun getCurrentLocation(): Location?
}
