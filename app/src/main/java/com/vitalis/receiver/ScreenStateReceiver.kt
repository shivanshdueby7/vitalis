package com.vitalis.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vitalis.database.ScreenEventEntity
import com.vitalis.database.VitalisDatabase
import com.vitalis.service.TrackingForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScreenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventType = when (intent.action) {
            Intent.ACTION_SCREEN_ON -> "screen_on"
            Intent.ACTION_SCREEN_OFF -> "screen_off"
            Intent.ACTION_USER_PRESENT -> "unlocked"
            else -> return
        }

        val event = ScreenEventEntity(
            event_type = eventType,
            timestamp = System.currentTimeMillis(),
            synced = false
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                VitalisDatabase.getInstance(context.applicationContext)
                    .screenEventDao()
                    .insert(event)
            } catch (_: Exception) {
            }
        }

        if (eventType == "screen_on") {
            TrackingForegroundService.startService(context.applicationContext)
        }
    }
}
