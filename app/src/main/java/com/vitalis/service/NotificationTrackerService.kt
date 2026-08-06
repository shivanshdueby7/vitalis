package com.vitalis.service

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.vitalis.database.NotificationEntity
import com.vitalis.database.VitalisDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationTrackerService : NotificationListenerService() {

    private val db by lazy { VitalisDatabase.getInstance(this) }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val postedNotifications = mutableMapOf<String, Long>()

    companion object {
        fun isNotificationListenerEnabled(context: Context): Boolean {
            val packageName = context.packageName
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return flat?.contains(packageName) == true
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val key = sbn.key
            val packageName = sbn.packageName
            val postTime = sbn.postTime
            val extras = sbn.notification.extras

            postedNotifications[key] = postTime

            val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
            val text = extras.getString(android.app.Notification.EXTRA_TEXT) ?: ""
            val category = sbn.notification.category ?: ""

            val entity = NotificationEntity(
                package_name = packageName,
                app_name = getAppName(packageName),
                title = title,
                body = text,
                category = categorizeNotification(category, packageName, text),
                timestamp = postTime,
                date = getDateFromTimestamp(postTime),
                is_dismissed = false,
                dismissed_at = 0,
                time_to_dismiss_seconds = 0,
                synced = false
            )

            scope.launch {
                db.notificationDao().insert(entity)
            }
        } catch (e: Exception) {
            android.util.Log.e("Vitalis", "Notification error", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        try {
            val key = sbn.key
            val postedAt = postedNotifications.remove(key)

            if (postedAt != null) {
                val dismissedAt = System.currentTimeMillis()
                val timeToDismiss = (dismissedAt - postedAt) / 1000

                scope.launch {
                    db.notificationDao().markRecentDismissed(postedAt, dismissedAt, timeToDismiss)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Vitalis", "Notification remove error", e)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun categorizeNotification(category: String, packageName: String, text: String): String {
        val lowerPkg = packageName.lowercase()
        val lowerText = text.lowercase()

        return when {
            category == android.app.Notification.CATEGORY_MESSAGE ||
            category == android.app.Notification.CATEGORY_EMAIL -> "communication"
            category == android.app.Notification.CATEGORY_CALL -> "call"
            category == android.app.Notification.CATEGORY_ALARM -> "alarm"
            category == android.app.Notification.CATEGORY_REMINDER -> "reminder"
            lowerPkg.containsAny("instagram", "facebook", "twitter", "tiktok", "snapchat", "reddit", "whatsapp", "telegram", "x.", "threads") -> "social"
            lowerPkg.containsAny("gmail", "outlook", "slack", "teams") -> "work"
            lowerPkg.containsAny("news", "bbc", "cnn", "reuters") -> "news"
            lowerPkg.containsAny("amazon", "flipkart", "shopping") ||
            lowerText.containsAny("offer", "sale", "discount", "deal", "coupon") -> "promotional"
            lowerPkg.containsAny("game", "play") -> "entertainment"
            else -> "other"
        }
    }

    private fun String.containsAny(vararg substrings: String): Boolean {
        return substrings.any { this.contains(it) }
    }

    private fun getDateFromTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}
