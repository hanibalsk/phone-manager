package three.two.bit.phonemanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.data.model.LocationQueueEntity

/**
 * Story 0.2.3/E4.2: AppDatabase - Room database for Phone Manager
 *
 * Version 2: Added LocationQueueEntity for upload queue management
 * Version 3: Added sync tracking fields to LocationEntity (Story E4.2)
 */
@Database(
    entities = [
        LocationEntity::class,
        LocationQueueEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun locationQueueDao(): LocationQueueDao

    companion object {
        const val DATABASE_NAME = "phone_manager_db"

        /**
         * Story E4.2: Migration from version 2 to 3
         * Adds isSynced and syncedAt columns to locations table
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE locations ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE locations ADD COLUMN syncedAt INTEGER")
            }
        }
    }
}
