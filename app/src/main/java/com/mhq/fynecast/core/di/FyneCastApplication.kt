package com.mhq.fynecast.core.di

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.datastore.preferences.preferencesDataStore

private val Context.internalPrefsStore by preferencesDataStore(name = "user_preferences")
private val Context.internalSessionStore by preferencesDataStore(name = "user_session")

class FyneCastApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Pass both pure DataStore file handles straight down into the constructor graph
        container = DefaultAppContainer(
            context = this,
            prefsDataStore = internalPrefsStore,
            sessionDataStore = internalSessionStore
        )
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannels(
            listOf(
                // 1. Silent, persistent background tracking channel
                NotificationChannel(
                    CHANNEL_ALERT_TRACKING,
                    "Active Weather Alerts",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows while you are tracking a weather alert"
                    setSound(null, null)
                    enableVibration(false)
                },

                // 2. High-priority critical emergency alarm channel
                NotificationChannel(
                    CHANNEL_ALERT_ALARM,
                    "Weather Alert Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Sounds when a tracked weather alert expires or escalates"
                    setBypassDnd(true)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
            )
        )
    }

    companion object {
        lateinit var instance: FyneCastApplication
            private set

        const val CHANNEL_ALERT_TRACKING = "channel_alert_tracking"
        const val CHANNEL_ALERT_ALARM = "channel_alert_alarm"
    }
}