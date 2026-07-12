package com.mhq.fynecast.home.domain.repository

interface LocationProviderStatusChecker {
    fun hasLocationPermissions(): Boolean
    fun isGpsProviderEnabled(): Boolean
}