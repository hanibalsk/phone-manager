package three.two.bit.phonemanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.data.model.LocationQueueEntity
import three.two.bit.phonemanager.data.model.ProximityAlertEntity

/**
 * Story 0.2.3/E4.2/E5.1: AppDatabase - Room database for Phone Manager
 *
 * Version 2: Added LocationQueueEntity for upload queue management
 * Version 3: Added sync tracking fields to LocationEntity (Story E4.2)
 * Version 4: Added ProximityAlertEntity table (Story E5.1)
 */
@Database(
    entities = [
        LocationEntity::class,
        LocationQueueEntity::class,
        ProximityAlertEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun locationQueueDao(): LocationQueueDao
    abstract fun proximityAlertDao(): ProximityAlertDao

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

        /**
         * Story E5.1: Migration from version 3 to 4
         * Creates proximity_alerts table
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS proximity_alerts (
                        id TEXT PRIMARY KEY NOT NULL,
                        ownerDeviceId TEXT NOT NULL,
                        targetDeviceId TEXT NOT NULL,
                        radiusMeters INTEGER NOT NULL,
                        direction TEXT NOT NULL,
                        active INTEGER NOT NULL,
                        lastState TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        lastTriggeredAt INTEGER
                    )
                """,
                )
            }
        }
    }
}
