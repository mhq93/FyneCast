package com.mhq.fynecast.core.di

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.datastore.preferences.preferencesDataStore
import com.mhq.fynecast.BuildConfig
import com.mhq.fynecast.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Context.internalPrefsStore by preferencesDataStore(name = "user_preferences")
private val Context.internalSessionStore by preferencesDataStore(name = "user_session")

class FyneCastApplication : Application() {

    lateinit var container: AppContainer

    /** Lives as long as the app process; for work that belongs to no screen. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Pass both pure DataStore file handles straight down into the constructor graph
        container = DefaultAppContainer(
            context = this,
            prefsDataStore = internalPrefsStore,
            sessionDataStore = internalSessionStore
        )
        if (BuildConfig.DEBUG) container.appInitializer.initializeDebugFeatures()
        createNotificationChannels()

        // Keeps the saved favorite names in the app language for as long as the app lives.
        applicationScope.launch { container.keepFavoriteNamesLocalizedUseCase() }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannels(
            listOf(
                // 1. Silent, persistent background tracking channel
                NotificationChannel(
                    CHANNEL_ALERT_TRACKING,
                    getString(R.string.channel_alert_tracking_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.channel_alert_tracking_description)
                    setSound(null, null)
                    enableVibration(false)
                },

                // 2. High-priority critical emergency alarm channel
                NotificationChannel(
                    CHANNEL_ALERT_ALARM,
                    getString(R.string.channel_alert_alarm_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = getString(R.string.channel_alert_alarm_description)
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