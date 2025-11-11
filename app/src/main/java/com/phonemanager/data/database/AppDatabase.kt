package com.phonemanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.phonemanager.data.model.LocationEntity

/**
 * Epic 0.2.2: AppDatabase - Room database for Phone Manager
 * Stub implementation for Epic 1 development
 */
@Database(
    entities = [LocationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

    companion object {
        const val DATABASE_NAME = "phone_manager_db"
    }
}
