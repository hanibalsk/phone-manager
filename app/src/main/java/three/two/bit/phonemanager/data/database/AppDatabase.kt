package three.two.bit.phonemanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import three.two.bit.phonemanager.data.model.GeofenceEntity
import three.two.bit.phonemanager.data.model.GeofenceEventEntity
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.data.model.LocationQueueEntity
import three.two.bit.phonemanager.data.model.MovementEventEntity
import three.two.bit.phonemanager.data.model.PendingDeviceLinkEntity
import three.two.bit.phonemanager.data.model.ProximityAlertEntity
import three.two.bit.phonemanager.data.model.TripEntity
import three.two.bit.phonemanager.data.model.WebhookEntity

/**
 * Story 0.2.3/E4.2/E5.1/E6.1/E6.2/E6.3/E8.1: AppDatabase - Room database for Phone Manager
 *
 * Version 2: Added LocationQueueEntity for upload queue management
 * Version 3: Added sync tracking fields to LocationEntity (Story E4.2)
 * Version 4: Added ProximityAlertEntity table (Story E5.1)
 * Version 5: Added GeofenceEntity table (Story E6.1)
 * Version 6: Added GeofenceEventEntity table (Story E6.2)
 * Version 7: Added WebhookEntity table (Story E6.3)
 * Version 8: Added TripEntity and MovementEventEntity tables, extended LocationEntity (Story E8.1)
 * Version 9: Added PendingDeviceLinkEntity table (Story UGM-1.4)
 */
@Database(
    entities = [
        LocationEntity::class,
        LocationQueueEntity::class,
        ProximityAlertEntity::class,
        GeofenceEntity::class,
        GeofenceEventEntity::class,
        WebhookEntity::class,
        TripEntity::class,
        MovementEventEntity::class,
        PendingDeviceLinkEntity::class,
    ],
    version = 9,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun locationQueueDao(): LocationQueueDao
    abstract fun proximityAlertDao(): ProximityAlertDao
    abstract fun geofenceDao(): GeofenceDao
    abstract fun geofenceEventDao(): GeofenceEventDao
    abstract fun webhookDao(): WebhookDao
    abstract fun tripDao(): TripDao
    abstract fun movementEventDao(): MovementEventDao
    abstract fun pendingDeviceLinkDao(): PendingDeviceLinkDao

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

        /**
         * Story E8.1: Migration from version 7 to 8
         * Creates trips and movement_events tables, extends locations table
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create trips table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trips (
                        id TEXT PRIMARY KEY NOT NULL,
                        state TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        startLatitude REAL NOT NULL,
                        startLongitude REAL NOT NULL,
                        endLatitude REAL,
                        endLongitude REAL,
                        totalDistanceMeters REAL NOT NULL DEFAULT 0,
                        locationCount INTEGER NOT NULL DEFAULT 0,
                        dominantMode TEXT NOT NULL,
                        modesUsedJson TEXT NOT NULL,
                        modeBreakdownJson TEXT NOT NULL,
                        startTrigger TEXT NOT NULL,
                        endTrigger TEXT,
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        syncedAt INTEGER,
                        serverId TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """,
                )

                // 2. Create trips indexes
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trips_startTime ON trips(startTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trips_state ON trips(state)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trips_isSynced ON trips(isSynced)")

                // 3. Create movement_events table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS movement_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        tripId TEXT,
                        previousMode TEXT NOT NULL,
                        newMode TEXT NOT NULL,
                        detectionSource TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        detectionLatencyMs INTEGER NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        accuracy REAL,
                        speed REAL,
                        batteryLevel INTEGER,
                        batteryCharging INTEGER,
                        networkType TEXT,
                        networkStrength INTEGER,
                        accelerometerMagnitude REAL,
                        accelerometerVariance REAL,
                        accelerometerPeakFrequency REAL,
                        gyroscopeMagnitude REAL,
                        stepCount INTEGER,
                        significantMotion INTEGER,
                        activityType TEXT,
                        activityConfidence INTEGER,
                        distanceFromLastLocation REAL,
                        timeSinceLastLocation INTEGER,
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        syncedAt INTEGER,
                        FOREIGN KEY (tripId) REFERENCES trips(id) ON DELETE SET NULL
                    )
                """,
                )

                // 4. Create movement_events indexes
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movement_events_timestamp ON movement_events(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movement_events_tripId ON movement_events(tripId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movement_events_isSynced ON movement_events(isSynced)")

                // 5. Add new columns to locations table
                db.execSQL("ALTER TABLE locations ADD COLUMN transportationMode TEXT")
                db.execSQL("ALTER TABLE locations ADD COLUMN detectionSource TEXT")
                db.execSQL("ALTER TABLE locations ADD COLUMN modeConfidence REAL")
                db.execSQL("ALTER TABLE locations ADD COLUMN tripId TEXT")
                db.execSQL("ALTER TABLE locations ADD COLUMN correctedLatitude REAL")
                db.execSQL("ALTER TABLE locations ADD COLUMN correctedLongitude REAL")
                db.execSQL("ALTER TABLE locations ADD COLUMN correctionSource TEXT")
                db.execSQL("ALTER TABLE locations ADD COLUMN correctedAt INTEGER")

                // 6. Create locations indexes for new columns
                db.execSQL("CREATE INDEX IF NOT EXISTS index_locations_tripId ON locations(tripId)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_locations_transportationMode ON locations(transportationMode)",
                )
            }
        }

        /**
         * Story UGM-1.4: Migration from version 8 to 9
         * Creates pending_device_links table for offline queue
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_device_links (
                        deviceId TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL DEFAULT 0
                    )
                    """,
                )
            }
        }
    }
}
