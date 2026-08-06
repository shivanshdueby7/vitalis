package com.vitalis.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AppUsageEntity::class,
        AppSessionEntity::class,
        ScreenEventEntity::class,
        NotificationEntity::class,
        ActivitySessionEntity::class,
        SensorDataEntity::class,
        LocationTrackEntity::class,
        FocusSessionEntity::class,
        DailySummaryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VitalisDatabase : RoomDatabase() {

    abstract fun appUsageDao(): AppUsageDao
    abstract fun appSessionDao(): AppSessionDao
    abstract fun screenEventDao(): ScreenEventDao
    abstract fun notificationDao(): NotificationDao
    abstract fun activitySessionDao(): ActivitySessionDao
    abstract fun sensorDataDao(): SensorDataDao
    abstract fun locationTrackDao(): LocationTrackDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun dailySummaryDao(): DailySummaryDao

    companion object {
        @Volatile
        private var INSTANCE: VitalisDatabase? = null

        fun getInstance(context: Context): VitalisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VitalisDatabase::class.java,
                    "vitalis_database"
                )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
