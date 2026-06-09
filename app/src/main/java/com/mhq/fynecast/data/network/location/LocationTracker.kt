package com.mhq.fynecast.data.network.location

import android.location.Location

interface LocationTracker {
    suspend fun getCurrentLocation(): Location?
}