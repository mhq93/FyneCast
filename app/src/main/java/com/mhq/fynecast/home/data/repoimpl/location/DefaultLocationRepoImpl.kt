package com.mhq.fynecast.home.data.repoimpl.location

import com.mhq.fynecast.core.domain.location.GeoCoordinates
import com.mhq.fynecast.home.data.datasource.location.DeviceLocationDataSource
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository
import com.mhq.fynecast.home.domain.repository.LocationProviderStatusChecker

class DefaultLocationRepoImpl(
    private val deviceLocationDataSource: DeviceLocationDataSource,
    private val statusChecker: LocationProviderStatusChecker
) : DeviceLocationRepository {

    override suspend fun getCurrentLocation(): GeoCoordinates? {
        if (!statusChecker.hasLocationPermissions() || !statusChecker.isLocationEnabled()) return null

        return deviceLocationDataSource.getCurrentLocation()
            ?.let { GeoCoordinates(latitude = it.latitude, longitude = it.longitude) }
    }
}
