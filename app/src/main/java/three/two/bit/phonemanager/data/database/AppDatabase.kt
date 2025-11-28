package three.two.bit.phonemanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import three.two.bit.phonemanager.data.model.GeofenceEntity
import three.two.bit.phonemanager.data.model.GeofenceEventEntity
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.data.model.LocationQueueEntity
import three.two.bit.phonemanager.data.model.ProximityAlertEntity
import three.two.bit.phonemanager.data.model.WebhookEntity

/**
 * Story 0.2.3/E4.2/E5.1/E6.1/E6.2/E6.3: AppDatabase - Room database for Phone Manager
 *
 * Version 2: Added LocationQueueEntity for upload queue management
 * Version 3: Added sync tracking fields to LocationEntity (Story E4.2)
 * Version 4: Added ProximityAlertEntity table (Story E5.1)
 * Version 5: Added GeofenceEntity table (Story E6.1)
 * Version 6: Added GeofenceEventEntity table (Story E6.2)
 * Version 7: Added WebhookEntity table (Story E6.3)
 */
@Database(
    entities = [
        LocationEntity::class,
        LocationQueueEntity::class,
        ProximityAlertEntity::class,
        GeofenceEntity::class,
        GeofenceEventEntity::class,
        WebhookEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun locationQueueDao(): LocationQueueDao
    abstract fun proximityAlertDao(): ProximityAlertDao
    abstract fun geofenceDao(): GeofenceDao
    abstract fun geofenceEventDao(): GeofenceEventDao
    abstract fun webhookDao(): WebhookDao

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

        /**
         * Story E6.1: Migration from version 4 to 5
         * Creates geofences table
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS geofences (
                        id TEXT PRIMARY KEY NOT NULL,
                        deviceId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        radiusMeters INTEGER NOT NULL,
                        transitionTypes TEXT NOT NULL,
                        webhookId TEXT,
                        active INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """,
                )
            }
        }

        /**
         * Story E6.2: Migration from version 5 to 6
         * Creates geofence_events table
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS geofence_events (
                        id TEXT PRIMARY KEY NOT NULL,
                        deviceId TEXT NOT NULL,
                        geofenceId TEXT NOT NULL,
                        eventType TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        webhookDelivered INTEGER NOT NULL DEFAULT 0,
                        webhookResponseCode INTEGER
                    )
                """,
                )
            }
        }

        /**
         * Story E6.3: Migration from version 6 to 7
         * Creates webhooks table
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS webhooks (
                        id TEXT PRIMARY KEY NOT NULL,
                        ownerDeviceId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        targetUrl TEXT NOT NULL,
                        secret TEXT NOT NULL,
                        enabled INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """,
                )
            }
        }
    }
}
