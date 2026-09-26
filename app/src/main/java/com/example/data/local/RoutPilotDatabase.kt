package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SensorReadingEntity::class,
        HazardEntity::class,
        RoadStatusEntity::class,
        RouteRequestEntity::class,
        HistoryEventEntity::class,
        JourneyEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RoutPilotDatabase : RoomDatabase() {
    abstract fun routPilotDao(): RoutPilotDao

    companion object {
        @Volatile
        private var INSTANCE: RoutPilotDatabase? = null

        fun getInstance(context: Context): RoutPilotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoutPilotDatabase::class.java,
                    "routpilot_offline_cache.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
