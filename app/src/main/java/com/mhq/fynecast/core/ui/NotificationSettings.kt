package com.mhq.fynecast.core.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings

fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    )
}