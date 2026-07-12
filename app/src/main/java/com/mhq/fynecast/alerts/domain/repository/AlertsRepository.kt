package com.mhq.fynecast.alerts.domain.repository

import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import kotlinx.coroutines.flow.Flow

interface AlertsRepository {
    fun getAlerts(): Flow<List<AlertDomainModel>>
    fun getTrackedAlertIds(): Flow<List<String>>
    suspend fun getTrackedAlertsDirectly(): List<TrackedAlertDomainModel>
    suspend fun trackAlert(alert: AlertDomainModel, cityName: String)
    suspend fun cancelTracking(alertId: String, cancelWorker: Boolean)
    suspend fun cancelAllTracking()
}