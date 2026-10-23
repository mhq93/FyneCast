package com.mhq.fynecast.home.domain.repository

import com.mhq.fynecast.core.domain.location.GeoCoordinates

interface DeviceLocationRepository {
    suspend fun getCurrentLocation(): GeoCoordinates?
}