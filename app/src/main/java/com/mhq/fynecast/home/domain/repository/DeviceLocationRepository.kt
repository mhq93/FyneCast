package com.mhq.fynecast.home.domain.repository

import com.mhq.fynecast.home.domain.models.GeoCoordinates

interface DeviceLocationRepository {
    suspend fun getCurrentLocation(): GeoCoordinates?
}