package com.vitalis.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "app_usage",
    indices = [
        Index(value = ["package_name", "date"], unique = true),
        Index("start_time"),
        Index("date")
    ]
)
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val package_name: String,
    val app_name: String,
    val start_time: Long,
    val end_time: Long,
    val duration_seconds: Long,
    val launch_count: Int = 0,
    val is_system_app: Boolean = false,
    val category: String = "unknown",
    val date: String,
    val synced: Boolean = false
)

@Entity(
    tableName = "app_sessions",
    indices = [
        Index("package_name"),
        Index("start_time"),
        Index("date")
    ]
)
data class AppSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val package_name: String,
    val app_name: String,
    val start_time: Long,
    val end_time: Long,
    val duration_seconds: Long,
    val category: String = "unknown",
    val date: String,
    val context_switch: Boolean = false,
    val synced: Boolean = false
)

@Entity(
    tableName = "screen_events",
    indices = [Index("timestamp"), Index("event_type")]
)
data class ScreenEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val event_type: String,
    val timestamp: Long,
    val synced: Boolean = false
)

@Entity(
    tableName = "notifications",
    indices = [
        Index("package_name"),
        Index("timestamp"),
        Index("category"),
        Index("date")
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val package_name: String,
    val app_name: String,
    val title: String,
    val body: String,
    val category: String,
    val timestamp: Long,
    val date: String,
    val is_dismissed: Boolean = false,
    val dismissed_at: Long = 0,
    val time_to_dismiss_seconds: Long = 0,
    val synced: Boolean = false
)

@Entity(
    tableName = "activity_sessions",
    indices = [
        Index("activity_type"),
        Index("start_time"),
        Index("date")
    ]
)
data class ActivitySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activity_type: String,
    val start_time: Long,
    val end_time: Long = 0,
    val duration_seconds: Long = 0,
    val distance_meters: Double = 0.0,
    val avg_speed_kmh: Double = 0.0,
    val max_speed_kmh: Double = 0.0,
    val calories: Double = 0.0,
    val steps: Int = 0,
    val elevation_gain_meters: Double = 0.0,
    val confidence: Float = 0f,
    val date: String,
    val synced: Boolean = false
)

@Entity(
    tableName = "sensor_data",
    indices = [Index("sensor_type"), Index("timestamp")]
)
data class SensorDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sensor_type: String,
    val timestamp: Long,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val magnitude: Float = 0f,
    val aggregated: Boolean = false,
    val synced: Boolean = false
)

@Entity(
    tableName = "location_tracks",
    indices = [
        Index("session_id"),
        Index("timestamp"),
        Index("activity_type")
    ]
)
data class LocationTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val session_id: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speed_ms: Float = 0f,
    val accuracy_meters: Float = 0f,
    val timestamp: Long,
    val activity_type: String,
    val synced: Boolean = false
)

@Entity(
    tableName = "focus_sessions",
    indices = [Index("start_time"), Index("date")]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val start_time: Long,
    val end_time: Long = 0,
    val duration_seconds: Long = 0,
    val interruptions: Int = 0,
    val blocked_apps_count: Int = 0,
    val goal: String = "",
    val completed: Boolean = false,
    val rating: Int = 0,
    val date: String,
    val synced: Boolean = false
)

@Entity(
    tableName = "daily_summaries",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailySummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val screen_time_seconds: Long = 0,
    val total_apps_used: Int = 0,
    val total_notifications: Int = 0,
    val total_steps: Int = 0,
    val total_distance_meters: Double = 0.0,
    val total_active_seconds: Long = 0,
    val total_idle_seconds: Long = 0,
    val sleep_start_time: Long = 0,
    val sleep_end_time: Long = 0,
    val sleep_duration_seconds: Long = 0,
    val focus_sessions_count: Int = 0,
    val focus_total_seconds: Long = 0,
    val pickups_count: Int = 0,
    val avg_session_duration_seconds: Long = 0,
    val context_switches: Int = 0,
    val synced: Boolean = false
)
