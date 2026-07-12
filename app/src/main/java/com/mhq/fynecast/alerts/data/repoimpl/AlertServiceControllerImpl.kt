package com.mhq.fynecast.alerts.data.repoimpl

import android.content.Context
import androidx.work.WorkManager
import com.mhq.fynecast.alerts.notifications.AlertsForegroundService
import com.mhq.fynecast.alerts.domain.repository.AlertServiceController

class AlertServiceControllerImpl(
    private val context: Context
) : AlertServiceController {

    override fun startTracking(alertId: String, event: String, headline: String) {
        AlertsForegroundService.start(context, alertId, event, headline)
    }

    override fun stopTracking() {
        AlertsForegroundService.stop(context)
    }

    override fun removeAlertFromTracking(alertId: String) {
        AlertsForegroundService.removeAlert(context, alertId)
    }

    override fun cancelTrackingWorker(alertId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(alertId)
    }
}
