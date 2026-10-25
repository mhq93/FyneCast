package com.mhq.fynecast.alerts.data.repoimpl

import com.mhq.fynecast.alerts.data.datasource.AlertsLocalDataSource
import com.mhq.fynecast.alerts.data.mapper.toDomain
import com.mhq.fynecast.alerts.data.mapper.toEntity
import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import com.mhq.fynecast.core.domain.location.TrackedLocation
import com.mhq.fynecast.core.domain.weather.AlertDomainModel

/**
 * Concrete implementation of the [AlertsRepository] on top of a local data source.
 */
class AlertsRepoImpl(private val localDataSource: AlertsLocalDataSource) : AlertsRepository {
    override fun getTrackedAlertIds() =
        localDataSource.observeTrackedAlertIds()

    override suspend fun getTrackedAlerts() =
        localDataSource.getAll().map { it.toDomain() }

    override suspend fun saveTrackedAlert(alert: AlertDomainModel, location: TrackedLocation) =
        localDataSource.insert(alert.toEntity(location))

    override suspend fun deleteTrackedAlert(alertId: String) =
        localDataSource.deleteById(alertId)

    override suspend fun deleteAllTrackedAlerts() =
        localDataSource.deleteAll()
}
