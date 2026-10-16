package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing IDs of all currently tracked alerts.
 */
class GetTrackedAlertIdsUseCase(private val alertsRepository: AlertsRepository) {
    /** Returns a flow emitting lists of tracked alert ID strings. */
    operator fun invoke(): Flow<List<String>> =
        alertsRepository.getTrackedAlertIds()
}