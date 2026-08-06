package com.vitalis.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vitalis.service.TrackingForegroundService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            TrackingForegroundService.startService(context.applicationContext)
        }
    }
}
