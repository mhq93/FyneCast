package com.mhq.fynecast.alerts.notifications

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.mhq.fynecast.alerts.notifications.data.infra.AlertsNotificationManager
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AlertsForegroundService : LifecycleService() {

    private val appContainer by lazy { (applicationContext as FyneCastApplication).container }
    private val notificationManager by lazy { AlertsNotificationManager(this) }
    private val activeAlerts = ConcurrentHashMap<String, Pair<String, String>>()

    override fun onCreate() {
        super.onCreate()
        postSummary()

        lifecycleScope.launch {
            appContainer.observeUserPreferencesUseCase()
                .map { it.notificationsEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (!enabled) {
                        activeAlerts.clear()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                val alertId = intent.getStringExtra(KEY_ALERT_ID) ?: return cleanStartCheck()
                val event = intent.getStringExtra(KEY_EVENT) ?: return cleanStartCheck()
                val headline = intent.getStringExtra(KEY_HEADLINE) ?: return cleanStartCheck()

                if (!appContainer.notificationPermissionChecker.isGranted()) {
                    return cleanStartCheck()
                }

                activeAlerts[alertId] = event to headline
                postSummary()
            }

            ACTION_REMOVE_ALERT -> {
                val alertId = intent.getStringExtra(KEY_ALERT_ID)
                if (alertId != null) activeAlerts.remove(alertId)

                if (activeAlerts.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    postSummary()
                }
            }

            ACTION_CANCEL_ALL -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    appContainer.alertsRepository.cancelAllTracking()
                }
            }

            ACTION_STOP -> {
                activeAlerts.clear()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun postSummary() {
        val notification = notificationManager.buildTrackingNotification(activeAlerts.values.toList())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun cleanStartCheck(): Int {
        if (activeAlerts.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        const val ACTION_START = "com.mhq.fynecast.action.ALERT_START"
        const val ACTION_STOP = "com.mhq.fynecast.action.ALERT_STOP"
        const val ACTION_REMOVE_ALERT = "com.mhq.fynecast.action.ACTION_REMOVE_ALERT"
        const val ACTION_CANCEL_ALL = "com.mhq.fynecast.action.ALERT_CANCEL_ALL"
        const val KEY_ALERT_ID = "alert_id"
        const val KEY_EVENT = "event"
        const val KEY_HEADLINE = "headline"
        const val FOREGROUND_NOTIFICATION_ID = 1001

        fun removeAlert(context: Context, alertId: String) {
            val intent = Intent(context, AlertsForegroundService::class.java).apply {
                action = ACTION_REMOVE_ALERT
                putExtra(KEY_ALERT_ID, alertId)
            }
            context.startService(intent)
        }

        fun start(context: Context, alertId: String, event: String, headline: String) {
            val intent = Intent(context, AlertsForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(KEY_ALERT_ID, alertId)
                putExtra(KEY_EVENT, event)
                putExtra(KEY_HEADLINE, headline)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AlertsForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }
}