package com.mhq.fynecast.alerts.notifications.ui

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
import com.mhq.fynecast.core.ui.withAppLocale

class AlertsNotificationManager(baseContext: Context) {

    private val context: Context = baseContext.withAppLocale()
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun buildTrackingNotification(activeAlerts: List<Pair<String, String>>): Notification {
        val title = if (activeAlerts.size == 1) {
            context.getString(R.string.alert_tracking_title_single, activeAlerts.first().first)
        } else {
            context.getString(R.string.alert_tracking_title_multiple, activeAlerts.size)
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

        val cancelAllIntent = Intent(
            context,
            AlertsForegroundService::class.java
        ).apply {
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
            .setContentText(
                if (activeAlerts.size == 1) activeAlerts.first().second
                else context.getString(R.string.alert_tracking_text_multiple)
            )
            .setContentIntent(clickPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .addAction(
                R.drawable.ic_cancel,
                context.getString(R.string.cancel_tracking),
                cancelAllPendingIntent
            )
            .build()
    }

    fun showAlarmNotification(
        clearedAlerts: List<TrackedAlertDomainModel>,
        notificationId: Int
    ) {
        val title = if (clearedAlerts.size == 1) {
            context.getString(R.string.alert_lifted_title_single, clearedAlerts.first().event)
        } else {
            context.getString(R.string.alert_lifted_title_multiple, clearedAlerts.size)
        }

        val body = if (clearedAlerts.size == 1) {
            context.getString(R.string.alert_lifted_body_single, clearedAlerts.first().event, clearedAlerts.first().cityName)
        } else {
            context.getString(R.string.alert_lifted_body_multiple, clearedAlerts.joinToString(", ") { it.event })
        }

        val notification = NotificationCompat
            .Builder(context, FyneCastApplication.CHANNEL_ALERT_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(notificationId, notification)
    }
}