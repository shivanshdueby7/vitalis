package com.vitalis.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usage: AppUsageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usages: List<AppUsageEntity>)

    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY duration_seconds DESC")
    fun getDailyUsage(date: String): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE date BETWEEN :startDate AND :endDate ORDER BY date, start_time")
    fun getRangeUsage(startDate: String, endDate: String): Flow<List<AppUsageEntity>>

    @Query("SELECT SUM(duration_seconds) FROM app_usage WHERE date = :date")
    fun getTotalScreenTime(date: String): Flow<Long?>

    @Query("SELECT package_name, app_name, SUM(duration_seconds) as total, category FROM app_usage WHERE date = :date GROUP BY package_name ORDER BY total DESC LIMIT :limit")
    suspend fun getTopApps(date: String, limit: Int = 10): List<TopAppUsage>

    @Query("SELECT * FROM app_usage WHERE synced = 0")
    suspend fun getUnsynced(): List<AppUsageEntity>

    @Query("UPDATE app_usage SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT COUNT(DISTINCT package_name) FROM app_usage WHERE date = :date")
    fun getUniqueAppsCount(date: String): Flow<Int>

    @Query("DELETE FROM app_usage WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("""
        INSERT INTO app_usage (package_name, app_name, start_time, end_time, duration_seconds, launch_count, is_system_app, category, date, synced)
        VALUES (:package_name, :app_name, :start_time, :end_time, :duration_seconds, :launch_count, :is_system_app, :category, :date, :synced)
        ON CONFLICT(package_name, date) DO UPDATE SET
            app_name = excluded.app_name,
            start_time = excluded.start_time,
            end_time = excluded.end_time,
            duration_seconds = excluded.duration_seconds,
            category = excluded.category,
            synced = excluded.synced
    """)
    suspend fun upsert(
        package_name: String,
        app_name: String,
        start_time: Long,
        end_time: Long,
        duration_seconds: Long,
        launch_count: Int,
        is_system_app: Boolean,
        category: String,
        date: String,
        synced: Boolean
    ): Long
}

@Dao
interface AppSessionDao {
    @Insert
    suspend fun insert(session: AppSessionEntity): Long

    @Insert
    suspend fun insertAll(sessions: List<AppSessionEntity>)

    @Query("SELECT * FROM app_sessions WHERE date = :date ORDER BY start_time")
    fun getDailySessions(date: String): Flow<List<AppSessionEntity>>

    @Query("SELECT package_name, app_name, SUM(duration_seconds) as total, category FROM app_sessions WHERE date = :date GROUP BY package_name ORDER BY total DESC LIMIT :limit")
    suspend fun getTopApps(date: String, limit: Int = 10): List<TopAppUsage>

    @Query("SELECT SUM(duration_seconds) FROM app_sessions WHERE date = :date")
    fun getTotalScreenTime(date: String): Flow<Long?>

    @Query("SELECT COUNT(DISTINCT package_name) FROM app_sessions WHERE date = :date")
    fun getUniqueAppsCount(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_sessions WHERE date = :date")
    fun getSessionCount(date: String): Flow<Int>

    @Query("SELECT AVG(duration_seconds) FROM app_sessions WHERE date = :date")
    fun getAvgSessionDuration(date: String): Flow<Long?>
}

@Dao
interface ScreenEventDao {
    @Insert
    suspend fun insert(event: ScreenEventEntity): Long

    @Insert
    suspend fun insertAll(events: List<ScreenEventEntity>)

    @Query("SELECT * FROM screen_events WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp")
    suspend fun getRange(start: Long, end: Long): List<ScreenEventEntity>

    @Query("SELECT * FROM screen_events WHERE synced = 0")
    suspend fun getUnsynced(): List<ScreenEventEntity>

    @Query("UPDATE screen_events SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM screen_events WHERE event_type = 'screen_on' AND timestamp BETWEEN :start AND :end")
    suspend fun getPickupCount(start: Long, end: Long): Int

    @Query("SELECT * FROM screen_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): ScreenEventEntity?
}

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: NotificationEntity): Long

    @Insert
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("SELECT * FROM notifications WHERE date = :date ORDER BY timestamp DESC")
    fun getDailyNotifications(date: String): Flow<List<NotificationEntity>>

    @Query("SELECT package_name, COUNT(*) as count FROM notifications WHERE date = :date GROUP BY package_name ORDER BY count DESC LIMIT :limit")
    suspend fun getTopNotifyingApps(date: String, limit: Int = 10): List<TopNotifyingApp>

    @Query("SELECT COUNT(*) FROM notifications WHERE timestamp BETWEEN :start AND :end")
    suspend fun getCountInRange(start: Long, end: Long): Int

    @Query("SELECT * FROM notifications WHERE synced = 0")
    suspend fun getUnsynced(): List<NotificationEntity>

    @Query("UPDATE notifications SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("UPDATE notifications SET is_dismissed = 1, dismissed_at = :dismissedAt, time_to_dismiss_seconds = (:dismissedAt - timestamp) / 1000 WHERE id = :id")
    suspend fun markDismissed(id: Long, dismissedAt: Long)

    @Query("UPDATE notifications SET is_dismissed = 1, dismissed_at = :dismissedAt, time_to_dismiss_seconds = :timeToDismiss WHERE id = (SELECT id FROM notifications WHERE timestamp = :postedAt AND is_dismissed = 0 ORDER BY id ASC LIMIT 1)")
    suspend fun markRecentDismissed(postedAt: Long, dismissedAt: Long, timeToDismiss: Long)
}

@Dao
interface ActivitySessionDao {
    @Insert
    suspend fun insert(session: ActivitySessionEntity): Long

    @Insert
    suspend fun insertAll(sessions: List<ActivitySessionEntity>)

    @Update
    suspend fun update(session: ActivitySessionEntity)

    @Query("SELECT * FROM activity_sessions WHERE date = :date ORDER BY start_time")
    fun getDailySessions(date: String): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM activity_sessions WHERE activity_type = :type AND date = :date ORDER BY start_time")
    fun getSessionsByType(date: String, type: String): Flow<List<ActivitySessionEntity>>

    @Query("SELECT SUM(duration_seconds) FROM activity_sessions WHERE date = :date AND activity_type = :type")
    fun getTotalDurationByType(date: String, type: String): Flow<Long?>

    @Query("SELECT SUM(distance_meters) FROM activity_sessions WHERE date = :date")
    fun getTotalDistance(date: String): Flow<Double?>

    @Query("SELECT SUM(steps) FROM activity_sessions WHERE date = :date")
    fun getTotalSteps(date: String): Flow<Int?>

    @Query("SELECT * FROM activity_sessions WHERE synced = 0")
    suspend fun getUnsynced(): List<ActivitySessionEntity>

    @Query("UPDATE activity_sessions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT * FROM activity_sessions WHERE end_time = 0 ORDER BY start_time DESC LIMIT 1")
    suspend fun getActiveSession(): ActivitySessionEntity?
}

@Dao
interface SensorDataDao {
    @Insert
    suspend fun insert(data: SensorDataEntity): Long

    @Insert
    suspend fun insertAll(data: List<SensorDataEntity>)

    @Query("SELECT * FROM sensor_data WHERE sensor_type = :type AND timestamp BETWEEN :start AND :end ORDER BY timestamp")
    suspend fun getRange(type: String, start: Long, end: Long): List<SensorDataEntity>

    @Query("DELETE FROM sensor_data WHERE timestamp < :cutoff AND aggregated = 0")
    suspend fun deleteOldRawData(cutoff: Long)

    @Query("SELECT * FROM sensor_data WHERE synced = 0 LIMIT 1000")
    suspend fun getUnsynced(): List<SensorDataEntity>

    @Query("UPDATE sensor_data SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}

@Dao
interface LocationTrackDao {
    @Insert
    suspend fun insert(track: LocationTrackEntity): Long

    @Insert
    suspend fun insertAll(tracks: List<LocationTrackEntity>)

    @Query("SELECT * FROM location_tracks WHERE session_id = :sessionId ORDER BY timestamp")
    suspend fun getSessionPoints(sessionId: Long): List<LocationTrackEntity>

    @Query("SELECT * FROM location_tracks WHERE synced = 0")
    suspend fun getUnsynced(): List<LocationTrackEntity>

    @Query("UPDATE location_tracks SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM location_tracks WHERE session_id = :sessionId")
    suspend fun getPointCount(sessionId: Long): Int
}

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE date = :date ORDER BY start_time")
    fun getDailySessions(date: String): Flow<List<FocusSessionEntity>>

    @Query("SELECT SUM(duration_seconds) FROM focus_sessions WHERE date = :date AND completed = 1")
    fun getTotalFocusTime(date: String): Flow<Long?>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE date = :date AND completed = 1")
    fun getCompletedCount(date: String): Flow<Int>
}

@Dao
interface DailySummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: DailySummaryEntity): Long

    @Update
    suspend fun update(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    fun getDailySummary(date: String): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC LIMIT :limit")
    fun getRecentSummaries(limit: Int = 30): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries WHERE synced = 0")
    suspend fun getUnsynced(): List<DailySummaryEntity>

    @Query("UPDATE daily_summaries SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}

// Data classes for aggregated queries
data class TopAppUsage(
    val package_name: String,
    val app_name: String,
    val total: Long,
    val category: String
)

data class TopNotifyingApp(
    val package_name: String,
    val count: Int
)
