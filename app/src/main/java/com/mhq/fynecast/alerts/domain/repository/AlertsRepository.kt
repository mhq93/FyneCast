package com.mhq.fynecast.alerts.domain.repository

import com.mhq.fynecast.core.domain.weather.AlertDomainModel
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.core.domain.location.TrackedLocation
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining data operations for weather alerts and tracked items.
 */
interface AlertsRepository {
    /** Observes a flow of IDs for all tracked alerts. */
    fun getTrackedAlertIds(): Flow<List<String>>

    /** Retrieves a list of all currently tracked alerts. */
    suspend fun getTrackedAlerts(): List<TrackedAlertDomainModel>

    /** Saves an alert along with its location for tracking. */
    suspend fun saveTrackedAlert(alert: AlertDomainModel, location: TrackedLocation)

    /** Removes a tracked alert by its unique identifier. */
    suspend fun deleteTrackedAlert(alertId: String)

    /** Deletes all tracked alerts from storage. */
    suspend fun deleteAllTrackedAlerts()
}