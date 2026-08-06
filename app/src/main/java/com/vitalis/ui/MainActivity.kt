package com.vitalis.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.vitalis.R
import com.vitalis.database.TopAppUsage
import com.vitalis.database.VitalisDatabase
import com.vitalis.service.NotificationTrackerService
import com.vitalis.service.TrackingForegroundService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val db by lazy { VitalisDatabase.getInstance(this) }
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var trackingStatus: TextView
    private lateinit var tvScreenTime: TextView
    private lateinit var tvAppsUsed: TextView
    private lateinit var tvPickups: TextView
    private lateinit var tvLastEvent: TextView
    private lateinit var tvNotifications: TextView
    private lateinit var tvFirstUnlock: TextView
    private lateinit var usageAccessStatus: TextView
    private lateinit var notificationAccessStatus: TextView
    private lateinit var batteryOptStatus: TextView
    private lateinit var btnStartTracking: Button
    private lateinit var btnStopTracking: Button
    private lateinit var btnGrantUsageAccess: Button
    private lateinit var btnGrantNotificationAccess: Button
    private lateinit var btnRequestBatteryOptimization: Button
    private lateinit var cardTracking: CardView
    private lateinit var cardStats: CardView
    private lateinit var cardTopApps: CardView
    private lateinit var cardTopAppsList: LinearLayout
    private lateinit var cardPermissions: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupUI()
        checkPermissions()
        loadTodayStats()
        animateCardsOnLoad()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        loadTodayStats()
    }

    private fun bindViews() {
        trackingStatus = findViewById(R.id.trackingStatus)
        tvScreenTime = findViewById(R.id.tvScreenTime)
        tvAppsUsed = findViewById(R.id.tvAppsUsed)
        tvPickups = findViewById(R.id.tvPickups)
        tvLastEvent = findViewById(R.id.tvLastEvent)
        tvNotifications = findViewById(R.id.tvNotifications)
        tvFirstUnlock = findViewById(R.id.tvFirstUnlock)
        usageAccessStatus = findViewById(R.id.usageAccessStatus)
        notificationAccessStatus = findViewById(R.id.notificationAccessStatus)
        batteryOptStatus = findViewById(R.id.batteryOptStatus)
        btnStartTracking = findViewById(R.id.btnStartTracking)
        btnStopTracking = findViewById(R.id.btnStopTracking)
        btnGrantUsageAccess = findViewById(R.id.btnGrantUsageAccess)
        btnGrantNotificationAccess = findViewById(R.id.btnGrantNotificationAccess)
        btnRequestBatteryOptimization = findViewById(R.id.btnRequestBatteryOptimization)
        cardTracking = findViewById(R.id.cardTracking)
        cardStats = findViewById(R.id.cardStats)
        cardTopApps = findViewById(R.id.cardTopApps)
        cardTopAppsList = findViewById(R.id.cardTopAppsList)
        cardPermissions = findViewById(R.id.cardPermissions)
    }

    private fun setupUI() {
        btnStartTracking.setOnClickListener { startTracking() }
        btnStopTracking.setOnClickListener { stopTracking() }
        btnGrantUsageAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        btnGrantNotificationAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        btnRequestBatteryOptimization.setOnClickListener {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun checkPermissions() {
        val hasUsageAccess = hasUsageStatsPermission()
        val hasNotificationAccess = NotificationTrackerService.isNotificationListenerEnabled(this)
        val powerManager = getSystemService(android.os.PowerManager::class.java)
        val isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(packageName)

        updatePermissionStatus(usageAccessStatus, hasUsageAccess, "Granted", "Not Granted")
        updatePermissionStatus(notificationAccessStatus, hasNotificationAccess, "Granted", "Not Granted")
        updatePermissionStatus(batteryOptStatus, isIgnoringBattery, "Exempt", "Not Exempt")
    }

    private fun updatePermissionStatus(tv: TextView, granted: Boolean, yesText: String, noText: String) {
        tv.text = if (granted) yesText else noText
        tv.setTextColor(
            ContextCompat.getColor(this, if (granted) R.color.status_green else R.color.status_red)
        )
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    private fun startTracking() {
        try {
            TrackingForegroundService.startService(this)
            animateStatusChange("Tracking Active", R.color.status_green)
            showToast("Tracking started")
        } catch (e: Exception) {
            showToast("Error: ${e.message}")
        }
    }

    private fun stopTracking() {
        try {
            TrackingForegroundService.stopService(this)
            animateStatusChange("Tracking Stopped", R.color.status_red)
            showToast("Tracking stopped")
        } catch (e: Exception) {
            showToast("Error: ${e.message}")
        }
    }

    private fun animateStatusChange(text: String, colorRes: Int) {
        ObjectAnimator.ofFloat(trackingStatus, "alpha", 1f, 0f, 1f).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        trackingStatus.text = text
        trackingStatus.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private fun loadTodayStats() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            try {
                db.appSessionDao().getTotalScreenTime(today).collectLatest { totalSeconds ->
                    val seconds = totalSeconds ?: 0
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    animateValueChange(tvScreenTime, String.format("%dh %dm", hours, minutes))
                }
            } catch (e: Exception) {
                android.util.Log.e("Vitalis", "Screen time error", e)
            }
        }

        lifecycleScope.launch {
            try {
                db.appSessionDao().getUniqueAppsCount(today).collectLatest { count ->
                    animateValueChange(tvAppsUsed, count.toString())
                }
            } catch (e: Exception) {
                android.util.Log.e("Vitalis", "Apps count error", e)
            }
        }

        lifecycleScope.launch {
            try {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val pickupCount = db.screenEventDao().getPickupCount(startOfDay, System.currentTimeMillis())
                animateValueChange(tvPickups, pickupCount.toString())

                val unlockEvents = db.screenEventDao().getRange(startOfDay, System.currentTimeMillis())
                    .filter { it.event_type == "unlocked" }
                    .sortedBy { it.timestamp }
                if (unlockEvents.isNotEmpty()) {
                    tvFirstUnlock.text = formatTime(unlockEvents.first().timestamp)
                }

                val notifCount = db.notificationDao().getCountInRange(startOfDay, System.currentTimeMillis())
                animateValueChange(tvNotifications, notifCount.toString())
            } catch (e: Exception) {
                android.util.Log.e("Vitalis", "Stats error", e)
            }
        }

        lifecycleScope.launch {
            try {
                db.screenEventDao().getLatest()?.let { event ->
                    tvLastEvent.text = "${formatEventType(event.event_type)} at ${formatTime(event.timestamp)}"
                }
            } catch (e: Exception) {
                android.util.Log.e("Vitalis", "Last event error", e)
            }
        }

        lifecycleScope.launch {
            try {
                val topApps = db.appSessionDao().getTopApps(today, 5)
                renderTopApps(topApps)
            } catch (e: Exception) {
                android.util.Log.e("Vitalis", "Top apps error", e)
            }
        }
    }

    private fun renderTopApps(apps: List<TopAppUsage>) {
        cardTopAppsList.removeAllViews()

        if (apps.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No data yet. Start tracking to see your top apps."
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                textSize = 13f
                setPadding(0, 16, 0, 16)
                gravity = android.view.Gravity.CENTER
            }
            cardTopAppsList.addView(emptyView)
            return
        }

        val maxDuration = apps.maxOfOrNull { it.total } ?: 1L

        apps.forEachIndexed { index, app ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_top_app, cardTopAppsList, false)
            val rank = view.findViewById<TextView>(R.id.tvRank)
            val appName = view.findViewById<TextView>(R.id.tvAppName)
            val appTime = view.findViewById<TextView>(R.id.tvAppTime)
            val bar = view.findViewById<View>(R.id.viewBar)

            rank.text = "${index + 1}"
            appName.text = app.app_name
            appTime.text = formatDuration(app.total)

            val barWidth = (app.total.toFloat() / maxDuration.toFloat() * 200f).coerceAtLeast(8f)
            bar.layoutParams = LinearLayout.LayoutParams(
                barWidth.toInt(),
                6
            ).apply {
                setMargins(0, 0, 0, 0)
            }

            val colorRes = when (index) {
                0 -> R.color.accent
                1 -> R.color.accent_blue
                2 -> R.color.accent_orange
                else -> R.color.text_secondary
            }
            bar.setBackgroundColor(ContextCompat.getColor(this, colorRes))

            cardTopAppsList.addView(view)
        }
    }

    private fun animateValueChange(tv: TextView, newValue: String) {
        if (tv.text.toString() != newValue) {
            ObjectAnimator.ofFloat(tv, "alpha", 1f, 0.5f, 1f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            tv.text = newValue
        }
    }

    private fun animateCardsOnLoad() {
        val cards = listOf(cardTracking, cardStats, cardTopApps, cardPermissions)
        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 50f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay((index * 120).toLong())
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatEventType(eventType: String): String {
        return when (eventType) {
            "screen_on" -> "Screen On"
            "screen_off" -> "Screen Off"
            "unlocked" -> "Unlocked"
            else -> eventType.replace("_", " ").capitalize()
        }
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
