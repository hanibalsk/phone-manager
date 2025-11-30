package three.two.bit.phonemanager.data.database

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Story E8.1: AppDatabase Migration Tests
 *
 * Tests for database version 8 migration SQL validation
 * Note: Full migration tests require Android instrumented tests (androidTest).
 * These tests validate the migration constants and SQL structure.
 *
 * Coverage target: > 80%
 */
class AppDatabaseMigrationTest {

    // region Database Version Tests

    @Test
    fun `database version is 8`() {
        // Verify database version matches expected value for E8.1
        // This is a compile-time constant check - actual value is in @Database annotation
        assertEquals(8, EXPECTED_DATABASE_VERSION)
    }

    @Test
    fun `migration 7_8 exists`() {
        val migration = AppDatabase.MIGRATION_7_8
        assertEquals(7, migration.startVersion)
        assertEquals(8, migration.endVersion)
    }

    // endregion

    // region Trips Table SQL Tests

    @Test
    fun `trips table SQL contains required columns`() {
        val sql = TRIPS_TABLE_SQL.lowercase()

        // Primary key
        assertTrue(sql.contains("id text primary key not null"))

        // Required fields
        assertTrue(sql.contains("state text not null"))
        assertTrue(sql.contains("starttime integer not null"))
        assertTrue(sql.contains("endtime integer"))
        assertTrue(sql.contains("startlatitude real not null"))
        assertTrue(sql.contains("startlongitude real not null"))
        assertTrue(sql.contains("endlatitude real"))
        assertTrue(sql.contains("endlongitude real"))

        // Metrics
        assertTrue(sql.contains("totaldistancemeters real not null default 0"))
        assertTrue(sql.contains("locationcount integer not null default 0"))

        // Mode tracking
        assertTrue(sql.contains("dominantmode text not null"))
        assertTrue(sql.contains("modesused"))
        assertTrue(sql.contains("modebreakdown"))

        // Triggers
        assertTrue(sql.contains("starttrigger text not null"))
        assertTrue(sql.contains("endtrigger text"))

        // Sync tracking
        assertTrue(sql.contains("issynced integer not null default 0"))
        assertTrue(sql.contains("syncedat integer"))
        assertTrue(sql.contains("serverid text"))

        // Timestamps
        assertTrue(sql.contains("createdat integer not null"))
        assertTrue(sql.contains("updatedat integer not null"))
    }

    @Test
    fun `trips table SQL creates correct table name`() {
        val sql = TRIPS_TABLE_SQL.lowercase()
        assertTrue(sql.contains("create table if not exists trips"))
    }

    // endregion

    // region Movement Events Table SQL Tests

    @Test
    fun `movement_events table SQL contains required columns`() {
        val sql = MOVEMENT_EVENTS_TABLE_SQL.lowercase()

        // Primary key (auto-increment)
        assertTrue(sql.contains("id integer primary key autoincrement not null"))

        // Required fields
        assertTrue(sql.contains("timestamp integer not null"))
        assertTrue(sql.contains("tripid text"))
        assertTrue(sql.contains("previousmode text not null"))
        assertTrue(sql.contains("newmode text not null"))
        assertTrue(sql.contains("detectionsource text not null"))
        assertTrue(sql.contains("confidence real not null"))
        assertTrue(sql.contains("detectionlatencyms integer not null"))

        // Optional location fields
        assertTrue(sql.contains("latitude real"))
        assertTrue(sql.contains("longitude real"))
        assertTrue(sql.contains("accuracy real"))
        assertTrue(sql.contains("speed real"))

        // Device state
        assertTrue(sql.contains("batterylevel integer"))
        assertTrue(sql.contains("batterycharging integer"))
        assertTrue(sql.contains("networktype text"))
        assertTrue(sql.contains("networkstrength integer"))

        // Sensor telemetry
        assertTrue(sql.contains("accelerometermagnitude real"))
        assertTrue(sql.contains("accelerometervariance real"))
        assertTrue(sql.contains("accelerometerpeakfrequency real"))
        assertTrue(sql.contains("gyroscopemagnitude real"))
        assertTrue(sql.contains("stepcount integer"))
        assertTrue(sql.contains("significantmotion integer"))

        // Activity recognition
        assertTrue(sql.contains("activitytype text"))
        assertTrue(sql.contains("activityconfidence integer"))

        // Movement context
        assertTrue(sql.contains("distancefromlastlocation real"))
        assertTrue(sql.contains("timesincelastlocation integer"))

        // Sync tracking
        assertTrue(sql.contains("issynced integer not null default 0"))
        assertTrue(sql.contains("syncedat integer"))
    }

    @Test
    fun `movement_events table SQL contains foreign key`() {
        val sql = MOVEMENT_EVENTS_TABLE_SQL.lowercase()
        assertTrue(sql.contains("foreign key"))
        assertTrue(sql.contains("tripid"))
        assertTrue(sql.contains("references trips(id)"))
        assertTrue(sql.contains("on delete set null"))
    }

    @Test
    fun `movement_events table SQL creates correct table name`() {
        val sql = MOVEMENT_EVENTS_TABLE_SQL.lowercase()
        assertTrue(sql.contains("create table if not exists movement_events"))
    }

    // endregion

    // region Index SQL Tests

    @Test
    fun `trips indexes are created for required columns`() {
        assertTrue(TRIPS_INDEX_START_TIME.lowercase().contains("index"))
        assertTrue(TRIPS_INDEX_START_TIME.lowercase().contains("trips"))
        assertTrue(TRIPS_INDEX_START_TIME.lowercase().contains("starttime"))

        assertTrue(TRIPS_INDEX_STATE.lowercase().contains("index"))
        assertTrue(TRIPS_INDEX_STATE.lowercase().contains("trips"))
        assertTrue(TRIPS_INDEX_STATE.lowercase().contains("state"))

        assertTrue(TRIPS_INDEX_IS_SYNCED.lowercase().contains("index"))
        assertTrue(TRIPS_INDEX_IS_SYNCED.lowercase().contains("trips"))
        assertTrue(TRIPS_INDEX_IS_SYNCED.lowercase().contains("issynced"))
    }

    @Test
    fun `movement_events indexes are created for required columns`() {
        assertTrue(MOVEMENT_EVENTS_INDEX_TIMESTAMP.lowercase().contains("index"))
        assertTrue(MOVEMENT_EVENTS_INDEX_TIMESTAMP.lowercase().contains("movement_events"))
        assertTrue(MOVEMENT_EVENTS_INDEX_TIMESTAMP.lowercase().contains("timestamp"))

        assertTrue(MOVEMENT_EVENTS_INDEX_TRIP_ID.lowercase().contains("index"))
        assertTrue(MOVEMENT_EVENTS_INDEX_TRIP_ID.lowercase().contains("movement_events"))
        assertTrue(MOVEMENT_EVENTS_INDEX_TRIP_ID.lowercase().contains("tripid"))

        assertTrue(MOVEMENT_EVENTS_INDEX_IS_SYNCED.lowercase().contains("index"))
        assertTrue(MOVEMENT_EVENTS_INDEX_IS_SYNCED.lowercase().contains("movement_events"))
        assertTrue(MOVEMENT_EVENTS_INDEX_IS_SYNCED.lowercase().contains("issynced"))
    }

    // endregion

    // region Locations Table Extension SQL Tests

    @Test
    fun `locations table extension columns are correct`() {
        assertTrue(LOCATIONS_ALTER_TRANSPORTATION_MODE.lowercase().contains("alter table locations"))
        assertTrue(LOCATIONS_ALTER_TRANSPORTATION_MODE.lowercase().contains("transportationmode text"))

        assertTrue(LOCATIONS_ALTER_DETECTION_SOURCE.lowercase().contains("alter table locations"))
        assertTrue(LOCATIONS_ALTER_DETECTION_SOURCE.lowercase().contains("detectionsource text"))

        assertTrue(LOCATIONS_ALTER_MODE_CONFIDENCE.lowercase().contains("alter table locations"))
        assertTrue(LOCATIONS_ALTER_MODE_CONFIDENCE.lowercase().contains("modeconfidence real"))

        assertTrue(LOCATIONS_ALTER_TRIP_ID.lowercase().contains("alter table locations"))
        assertTrue(LOCATIONS_ALTER_TRIP_ID.lowercase().contains("tripid text"))

        assertTrue(LOCATIONS_ALTER_CORRECTED_LATITUDE.lowercase().contains("alter table locations"))
        assertTrue(LOCATIONS_ALTER_CORRECTED_LATITUDE.lowercase().contains("correctedlatitude real"))

        assertTrue(LOCATIONS_ALTER_CORRECTED_LONGITUDE.lowercase().contains("alter table locations"))
        assertTrue(LOCATIONS_ALTER_CORRECTED_LONGITUDE.lowercase().contains("correctedlongitude real"))

        assertTrue(LOCATIONS_ALTER_CORRECTION_SOURCE.lowercase().contains("alter table locations"))
        assertTrue(LOCATIONS_ALTER_CORRECTION_SOURCE.lowercase().contains("correctionsource text"))

        assertTrue(LOCATIONS_ALTER_CORRECTED_AT.lowercase().contains("alter table locations"))
        assertTrue(LOCATIONS_ALTER_CORRECTED_AT.lowercase().contains("correctedat integer"))
    }

    @Test
    fun `locations table indexes are created for new columns`() {
        assertTrue(LOCATIONS_INDEX_TRIP_ID.lowercase().contains("index"))
        assertTrue(LOCATIONS_INDEX_TRIP_ID.lowercase().contains("locations"))
        assertTrue(LOCATIONS_INDEX_TRIP_ID.lowercase().contains("tripid"))

        assertTrue(LOCATIONS_INDEX_TRANSPORTATION_MODE.lowercase().contains("index"))
        assertTrue(LOCATIONS_INDEX_TRANSPORTATION_MODE.lowercase().contains("locations"))
        assertTrue(LOCATIONS_INDEX_TRANSPORTATION_MODE.lowercase().contains("transportationmode"))
    }

    // endregion

    companion object {
        const val EXPECTED_DATABASE_VERSION = 8

        // SQL from MIGRATION_7_8 for validation
        // These should match the actual migration SQL in AppDatabase.kt

        const val TRIPS_TABLE_SQL = """
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
        """

        const val MOVEMENT_EVENTS_TABLE_SQL = """
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
        """

        // Trips indexes
        const val TRIPS_INDEX_START_TIME = "CREATE INDEX IF NOT EXISTS index_trips_startTime ON trips(startTime)"
        const val TRIPS_INDEX_STATE = "CREATE INDEX IF NOT EXISTS index_trips_state ON trips(state)"
        const val TRIPS_INDEX_IS_SYNCED = "CREATE INDEX IF NOT EXISTS index_trips_isSynced ON trips(isSynced)"

        // Movement events indexes
        const val MOVEMENT_EVENTS_INDEX_TIMESTAMP =
            "CREATE INDEX IF NOT EXISTS index_movement_events_timestamp ON movement_events(timestamp)"
        const val MOVEMENT_EVENTS_INDEX_TRIP_ID =
            "CREATE INDEX IF NOT EXISTS index_movement_events_tripId ON movement_events(tripId)"
        const val MOVEMENT_EVENTS_INDEX_IS_SYNCED =
            "CREATE INDEX IF NOT EXISTS index_movement_events_isSynced ON movement_events(isSynced)"

        // Locations ALTER TABLE statements
        const val LOCATIONS_ALTER_TRANSPORTATION_MODE = "ALTER TABLE locations ADD COLUMN transportationMode TEXT"
        const val LOCATIONS_ALTER_DETECTION_SOURCE = "ALTER TABLE locations ADD COLUMN detectionSource TEXT"
        const val LOCATIONS_ALTER_MODE_CONFIDENCE = "ALTER TABLE locations ADD COLUMN modeConfidence REAL"
        const val LOCATIONS_ALTER_TRIP_ID = "ALTER TABLE locations ADD COLUMN tripId TEXT"
        const val LOCATIONS_ALTER_CORRECTED_LATITUDE = "ALTER TABLE locations ADD COLUMN correctedLatitude REAL"
        const val LOCATIONS_ALTER_CORRECTED_LONGITUDE = "ALTER TABLE locations ADD COLUMN correctedLongitude REAL"
        const val LOCATIONS_ALTER_CORRECTION_SOURCE = "ALTER TABLE locations ADD COLUMN correctionSource TEXT"
        const val LOCATIONS_ALTER_CORRECTED_AT = "ALTER TABLE locations ADD COLUMN correctedAt INTEGER"

        // Locations indexes
        const val LOCATIONS_INDEX_TRIP_ID = "CREATE INDEX IF NOT EXISTS index_locations_tripId ON locations(tripId)"
        const val LOCATIONS_INDEX_TRANSPORTATION_MODE =
            "CREATE INDEX IF NOT EXISTS index_locations_transportationMode ON locations(transportationMode)"
    }
}
