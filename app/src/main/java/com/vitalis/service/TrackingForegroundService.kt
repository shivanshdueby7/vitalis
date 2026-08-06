package com.vitalis.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.vitalis.R
import com.vitalis.database.AppSessionEntity
import com.vitalis.database.VitalisDatabase
import com.vitalis.ui.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TrackingForegroundService : LifecycleService() {

    private val db by lazy { VitalisDatabase.getInstance(this) }
    private var trackingJob: Job? = null
    private var usageStatsManager: UsageStatsManager? = null
    private var lastQueryTime: Long = 0

    // In-memory state
    private var currentForegroundApp: String? = null
    private var currentSessionStart: Long = 0
    private var contextSwitchCount: Int = 0

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "vitalis_tracking_channel"
        const val ACTION_START_TRACKING = "com.vitalis.ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.vitalis.ACTION_STOP_TRACKING"
        const val POLLING_INTERVAL_MS = 15_000L

        fun startService(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannel()
        lastQueryTime = System.currentTimeMillis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_TRACKING -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                startTracking()
            }
            ACTION_STOP_TRACKING -> {
                stopTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Vitalis Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Tracks your app usage and screen time"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vitalis is tracking")
            .setContentText("Monitoring your digital wellbeing")
            .setSmallIcon(R.drawable.ic_tracking)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startTracking() {
        trackingJob = lifecycleScope.launch {
            // Initial sync: process last 24 hours of events
            processHistoricalEvents()

            while (true) {
                try {
                    pollUsageEvents()
                    delay(POLLING_INTERVAL_MS)
                } catch (e: Exception) {
                    android.util.Log.e("Vitalis", "Tracking error", e)
                }
            }
        }
    }

    private fun stopTracking() {
        // Save current session if any
        if (currentForegroundApp != null && currentSessionStart > 0) {
            val now = System.currentTimeMillis()
            saveSession(currentForegroundApp!!, currentSessionStart, now)
            currentForegroundApp = null
            currentSessionStart = 0
        }
        trackingJob?.cancel()
    }

    private suspend fun processHistoricalEvents() {
        val now = System.currentTimeMillis()
        val twentyFourHoursAgo = now - (24 * 60 * 60 * 1000)

        val events = usageStatsManager?.queryEvents(twentyFourHoursAgo, now)
        events?.let { processEvents(it) }

        lastQueryTime = now
    }

    private suspend fun pollUsageEvents() {
        val now = System.currentTimeMillis()

        val events = usageStatsManager?.queryEvents(lastQueryTime, now)
        events?.let { processEvents(it) }

        lastQueryTime = now
    }

    private suspend fun processEvents(events: android.app.usage.UsageEvents) {
        val sessionsToSave = mutableListOf<AppSessionEntity>()
        val today = getTodayDate()

        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)

            val packageName = event.packageName
            if (packageName == this.packageName) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (currentForegroundApp != null && currentSessionStart > 0) {
                        val endTime = event.timeStamp
                        if (endTime > currentSessionStart) {
                            sessionsToSave.add(
                                createSession(
                                    currentForegroundApp!!,
                                    currentSessionStart,
                                    endTime,
                                    today
                                )
                            )
                            contextSwitchCount++
                        }
                    }

                    currentForegroundApp = packageName
                    currentSessionStart = event.timeStamp
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (currentForegroundApp == packageName && currentSessionStart > 0) {
                        val endTime = event.timeStamp
                        if (endTime > currentSessionStart) {
                            sessionsToSave.add(
                                createSession(packageName, currentSessionStart, endTime, today)
                            )
                        }
                        currentForegroundApp = null
                        currentSessionStart = 0
                    }
                }

                UsageEvents.Event.USER_INTERACTION -> {
                    // Track interaction for future idle detection
                }

                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    // Screen turned on
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    // Screen turned off - save current session
                    if (currentForegroundApp != null && currentSessionStart > 0) {
                        val endTime = event.timeStamp
                        if (endTime > currentSessionStart) {
                            sessionsToSave.add(
                                createSession(
                                    currentForegroundApp!!,
                                    currentSessionStart,
                                    endTime,
                                    today
                                )
                            )
                        }
                        currentForegroundApp = null
                        currentSessionStart = 0
                    }
                }
            }
        }

        if (sessionsToSave.isNotEmpty()) {
            db.appSessionDao().insertAll(sessionsToSave)
        }
    }

    private fun createSession(
        packageName: String,
        startTime: Long,
        endTime: Long,
        date: String
    ): AppSessionEntity {
        val duration = (endTime - startTime) / 1000
        val appName = getAppName(packageName)
        val category = categorizeApp(packageName)

        return AppSessionEntity(
            package_name = packageName,
            app_name = appName,
            start_time = startTime,
            end_time = endTime,
            duration_seconds = duration,
            category = category,
            date = date,
            context_switch = false,
            synced = false
        )
    }

    private suspend fun saveSession(packageName: String, startTime: Long, endTime: Long) {
        val today = getTodayDate()
        val session = createSession(packageName, startTime, endTime, today)
        db.appSessionDao().insert(session)
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun categorizeApp(packageName: String): String {
        val lower = packageName.lowercase()
        return when {
            lower.containsAny("instagram", "facebook", "twitter", "tiktok", "snapchat", "whatsapp", "telegram", "reddit", "x.", "threads") -> "social"
            lower.containsAny("youtube", "netflix", "spotify", "music", "game", "twitch", "disney", "hulu") -> "entertainment"
            lower.containsAny("gmail", "outlook", "slack", "teams", "zoom", "drive", "docs", "sheets", "notion", "evernote") -> "productivity"
            lower.containsAny("chrome", "firefox", "browser", "edge", "opera", "samsung internet") -> "browser"
            lower.containsAny("maps", "navigate", "uber", "ola", "waze") -> "navigation"
            lower.containsAny("camera", "gallery", "photos", "snapseed", "lightroom") -> "media"
            lower.containsAny("settings", "launcher", "system", "calculator", "clock", "calendar") -> "system"
            lower.containsAny("bank", "pay", "wallet", "finance", "stock") -> "finance"
            lower.containsAny("health", "fit", "step", "workout", "exercise") -> "health"
            else -> "other"
        }
    }

    private fun String.containsAny(vararg substrings: String): Boolean {
        return substrings.any { this.contains(it) }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }
}
