package com.mhq.fynecast.alerts.domain.repository

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedLocation
import kotlinx.coroutines.flow.Flow

interface AlertsRepository {
    fun getTrackedAlertIds(): Flow<List<String>>
    suspend fun getTrackedAlerts(): List<TrackedAlertDomainModel>
    suspend fun saveTrackedAlert(alert: AlertDomainModel, location: TrackedLocation)
    suspend fun deleteTrackedAlert(alertId: String)
    suspend fun deleteAllTrackedAlerts()
}