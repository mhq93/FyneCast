package com.mhq.fynecast.home.domain.usecases

import androidx.annotation.RequiresPermission
import com.mhq.fynecast.home.domain.models.GeoCoordinates
import com.mhq.fynecast.home.domain.repository.DeviceLocationRepository

class GetCurrentLocationUseCase(
    private val deviceLocationRepository: DeviceLocationRepository
) {
    suspend operator fun invoke(): GeoCoordinates? {
        return deviceLocationRepository.getCurrentLocation()
    }
}