package com.mhq.fynecast.alerts.domain.usecases

import com.mhq.fynecast.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.Flow

class GetTrackedAlertIdsUseCase(private val alertsRepository: AlertsRepository) {
    operator fun invoke(): Flow<List<String>> =
        alertsRepository.getTrackedAlertIds()
}