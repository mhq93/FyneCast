package com.mhq.fynecast.alerts.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mhq.fynecast.core.di.FyneCastApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Broadcast receiver handling actions related to alert tracking notifications (e.g. cancel all, dismiss).
 */
class AlertTrackingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as FyneCastApplication).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_CANCEL_ALL -> container.cancelAllAlertTrackingUseCase()
                    ACTION_TRACKING_DISMISSED -> container.refreshTrackingNotificationUseCase()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle ${intent.action}", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlertTrackingReceiver"
        const val ACTION_CANCEL_ALL = "com.mhq.fynecast.action.ALERT_CANCEL_ALL"
        const val ACTION_TRACKING_DISMISSED = "com.mhq.fynecast.action.TRACKING_DISMISSED"
    }
}