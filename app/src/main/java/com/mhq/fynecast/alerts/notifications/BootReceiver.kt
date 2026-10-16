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
 * Broadcast receiver that listens for device boot completion to restore active alert tracking.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val container = (context.applicationContext as FyneCastApplication).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.restoreAlertTrackingUseCase()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore alert tracking after boot", e)
            } finally {
                pending.finish()
            }
        }
    }
    private companion object {
        const val TAG = "BootReceiver"
    }
}