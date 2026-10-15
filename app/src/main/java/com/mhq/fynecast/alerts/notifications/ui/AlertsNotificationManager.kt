package com.mhq.fynecast.alerts.notifications.ui

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.domain.models.TrackedAlertDomainModel
import com.mhq.fynecast.alerts.notifications.AlertTrackingReceiver
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.core.ui.MainActivity
import com.mhq.fynecast.core.ui.withAppLocale
import kotlin.jvm.java

class AlertsNotificationManager(baseContext: Context) {

    private val context: Context = baseContext.withAppLocale()
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

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
            context.getString(
                R.string.alert_lifted_body_single,
                clearedAlerts.first().event,
                clearedAlerts.first().cityName
            )
        } else {
            context.getString(
                R.string.alert_lifted_body_multiple,
                clearedAlerts.joinToString(", ") { it.event })
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

    fun showTrackingNotification(alerts: List<TrackedAlertDomainModel>) {
        if (alerts.isEmpty()) {
            cancelTrackingNotification()
            return
        }
        val title = if (alerts.size == 1)
            context.getString(R.string.alert_tracking_title_single, alerts.first().event)
        else
            context.getString(R.string.alert_tracking_title_multiple, alerts.size)

        val clickPendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification =
            NotificationCompat.Builder(context, FyneCastApplication.CHANNEL_ALERT_TRACKING)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(
                    if (alerts.size == 1) alerts.first().headline
                    else context.getString(R.string.alert_tracking_text_multiple)
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(alerts.joinToString("\n") { "• ${it.headline}" })
                )
                .setContentIntent(clickPendingIntent)
                .setDeleteIntent(
                    receiverIntent(
                        AlertTrackingReceiver.ACTION_TRACKING_DISMISSED,
                        requestCode = 1
                    )
                )
                .addAction(
                    R.drawable.ic_cancel, context.getString(R.string.cancel_tracking),
                    receiverIntent(AlertTrackingReceiver.ACTION_CANCEL_ALL, requestCode = 2)
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()

        notificationManager.notify(TRACKING_NOTIFICATION_ID, notification)
    }

    fun cancelTrackingNotification() = notificationManager.cancel(TRACKING_NOTIFICATION_ID)

    private fun receiverIntent(action: String, requestCode: Int) = PendingIntent.getBroadcast(
        context, requestCode,
        Intent(context, AlertTrackingReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    companion object {
        const val TRACKING_NOTIFICATION_ID = 1001
    }
}