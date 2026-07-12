package com.mhq.fynecast.alerts.notifications.data.infra

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.notifications.AlertsForegroundService
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.core.ui.MainActivity

class AlertsNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun buildTrackingNotification(activeAlerts: List<Pair<String, String>>): Notification {
        val title = if (activeAlerts.size == 1) {
            "⚠️ Live Tracking: ${activeAlerts.first().first}"
        } else {
            "⚠️ Tracking ${activeAlerts.size} Weather Alerts"
        }
        val body = activeAlerts.joinToString("\n") { "• ${it.second}" }

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val clickPendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelAllIntent = Intent(context, AlertsForegroundService::class.java).apply {
            action = AlertsForegroundService.ACTION_CANCEL_ALL
        }
        val cancelAllPendingIntent = PendingIntent.getService(
            context,
            0,
            cancelAllIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, FyneCastApplication.CHANNEL_ALERT_TRACKING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(if (activeAlerts.size == 1) activeAlerts.first().second else "Expand to view tracked alerts")
            .setContentIntent(clickPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .addAction(
                R.drawable.ic_cancel,
                "Cancel Tracking",
                cancelAllPendingIntent
            )
            .build()
    }

    fun showAlarmNotification(clearedAlerts: List<TrackedAlertDomainModel>) {
        val title = if (clearedAlerts.size == 1) {
            "${clearedAlerts.first().event} has been lifted"
        } else {
            "${clearedAlerts.size} weather alerts have been lifted"
        }

        val body = if (clearedAlerts.size == 1) {
            "The ${clearedAlerts.first().event} warning for ${clearedAlerts.first().cityName} is no longer active"
        } else {
            clearedAlerts.joinToString(", ") { it.event } + " warnings have been cleared"
        }

        val notification = NotificationCompat.Builder(context, FyneCastApplication.CHANNEL_ALERT_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("✅ $title")
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}