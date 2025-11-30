# Story E8.1: Create TripEntity and MovementEventEntity with Room database migration

**Story ID**: E8.1
**Epic**: 8 - Movement Tracking & Intelligent Path Detection
**Priority**: Must-Have
**Estimate**: 3 story points (1-2 days)
**Status**: Done
**Created**: 2025-11-30
**PRD Reference**: PRD-movement-tracking.md, ANDROID_APP_SPEC.md

---

## Story

As a developer,
I need the database schema for trips and movement events,
so that trip data can be persisted locally.

## Acceptance Criteria

### AC E8.1.1: TripEntity Created
**Given** the database schema is being designed
**When** TripEntity is implemented
**Then** it should include all fields per spec:
  - id (String UUID, primary key)
  - state (String: IDLE, ACTIVE, PENDING_END, COMPLETED)
  - startTime, endTime (Long timestamps)
  - startLatitude, startLongitude, endLatitude, endLongitude
  - totalDistanceMeters, locationCount (statistics)
  - dominantMode, modesUsedJson, modeBreakdownJson
  - startTrigger, endTrigger
  - isSynced, syncedAt, serverId
  - createdAt, updatedAt

### AC E8.1.2: MovementEventEntity Created
**Given** the database schema is being designed
**When** MovementEventEntity is implemented
**Then** it should include all fields per spec:
  - id (Long auto-increment, primary key)
  - timestamp, tripId
  - previousMode, newMode, detectionSource, confidence, detectionLatencyMs
  - latitude, longitude, accuracy, speed (location snapshot)
  - batteryLevel, batteryCharging, networkType, networkStrength (device state)
  - accelerometerMagnitude/Variance/PeakFrequency, gyroscopeMagnitude, stepCount, significantMotion, activityType, activityConfidence (telemetry)
  - distanceFromLastLocation, timeSinceLastLocation
  - isSynced, syncedAt

### AC E8.1.3: LocationEntity Extended
**Given** the existing LocationEntity
**When** it is enhanced for movement tracking
**Then** new fields should be added:
  - transportationMode (String nullable)
  - detectionSource (String nullable)
  - modeConfidence (Float nullable)
  - tripId (String nullable)
  - correctedLatitude, correctedLongitude (Double nullable)
  - correctionSource (String nullable)
  - correctedAt (Long nullable)

### AC E8.1.4: MIGRATION_7_8 Implemented
**Given** the database needs to be upgraded
**When** MIGRATION_7_8 runs
**Then** it should:
  - Create trips table with all columns and defaults
  - Create movement_events table with all columns
  - Add new columns to locations table
  - Create all required indexes
  - Not lose any existing data

### AC E8.1.5: Indexes Created
**Given** query performance is important
**Then** indexes should exist for:
  - trips: startTime, state, isSynced
  - movement_events: timestamp, tripId, isSynced
  - locations: tripId, transportationMode

### AC E8.1.6: Foreign Key Constraint
**Given** movement_events reference trips
**Then** foreign key should:
  - Reference trips(id) from movement_events(tripId)
  - Use SET NULL on delete

## Tasks / Subtasks

- [x] Task 1: Create TripEntity (AC: E8.1.1)
  - [x] Create TripEntity data class in data/model/TripEntity.kt
  - [x] Add @Entity annotation with tableName "trips"
  - [x] Add all fields with proper types and defaults
  - [x] Add @PrimaryKey annotation to id field
  - [x] Add @Index annotations for startTime, state, isSynced

- [x] Task 2: Create MovementEventEntity (AC: E8.1.2, E8.1.6)
  - [x] Create MovementEventEntity data class in data/model/MovementEventEntity.kt
  - [x] Add @Entity annotation with tableName "movement_events"
  - [x] Add all fields with proper types and defaults
  - [x] Add @PrimaryKey(autoGenerate = true) to id field
  - [x] Add @Index annotations for timestamp, tripId, isSynced
  - [x] Add @ForeignKey to TripEntity with SET_NULL on delete

- [x] Task 3: Extend LocationEntity (AC: E8.1.3)
  - [x] Add transportationMode field (String nullable)
  - [x] Add detectionSource field (String nullable)
  - [x] Add modeConfidence field (Float nullable)
  - [x] Add tripId field (String nullable)
  - [x] Add correctedLatitude, correctedLongitude fields
  - [x] Add correctionSource, correctedAt fields
  - [x] Add @Index for tripId and transportationMode

- [x] Task 4: Implement MIGRATION_7_8 (AC: E8.1.4, E8.1.5)
  - [x] Create MIGRATION_7_8 object in AppDatabase.kt
  - [x] Add CREATE TABLE for trips with all columns
  - [x] Add CREATE TABLE for movement_events with FK constraint
  - [x] Add ALTER TABLE for locations with new columns
  - [x] Create all indexes for new tables
  - [x] Create indexes for new location columns

- [x] Task 5: Update AppDatabase (AC: E8.1.4)
  - [x] Update database version from 7 to 8
  - [x] Add TripEntity and MovementEventEntity to @Database entities
  - [x] Add MIGRATION_7_8 to migrations list
  - [x] Add abstract fun for TripDao and MovementEventDao (placeholders)

- [x] Task 6: Testing (All ACs)
  - [x] Write migration test for v7 to v8
  - [x] Verify all columns exist after migration
  - [x] Verify indexes are created
  - [x] Verify foreign key constraint works
  - [x] Test that existing location data is preserved

## Dev Notes

### Entity Definitions Reference

See ANDROID_APP_SPEC.md sections 2.2-2.5 for complete entity definitions.

### TripEntity Fields

```kotlin
@Entity(
    tableName = "trips",
    indices = [
        Index("startTime"),
        Index("state"),
        Index("isSynced")
    ]
)
data class TripEntity(
    @PrimaryKey val id: String,
    val state: String,
    val startTime: Long,
    val endTime: Long?,
    val startLatitude: Double,
    val startLongitude: Double,
    val endLatitude: Double?,
    val endLongitude: Double?,
    val totalDistanceMeters: Double = 0.0,
    val locationCount: Int = 0,
    val dominantMode: String,
    val modesUsedJson: String,
    val modeBreakdownJson: String,
    val startTrigger: String,
    val endTrigger: String?,
    val isSynced: Boolean = false,
    val syncedAt: Long? = null,
    val serverId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
```

### Migration SQL

```sql
-- Create trips table
CREATE TABLE IF NOT EXISTS trips (
    id TEXT PRIMARY KEY NOT NULL,
    state TEXT NOT NULL,
    startTime INTEGER NOT NULL,
    endTime INTEGER,
    ...
)

-- Create indexes
CREATE INDEX IF NOT EXISTS index_trips_startTime ON trips(startTime)
```

### Files to Create/Modify

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/data/model/TripEntity.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/model/MovementEventEntity.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/data/model/LocationEntity.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/database/AppDatabase.kt`

### Dependencies

- Room database (existing)
- Existing LocationEntity

### References

- [Source: ANDROID_APP_SPEC.md - Section 2: Database Schema]
- [Source: Epic-E8-Movement-Tracking.md - Story E8.1]

---

## Dev Agent Record

### Debug Log

- Build compiled successfully after all entities created
- All unit tests pass for TripEntity, MovementEventEntity, and migration SQL validation
- Pre-existing test issue in SettingsViewModelTest.kt fixed (missing permissionManager parameter)

### Completion Notes

Implementation completed successfully:
- TripEntity created with all 20 fields per spec, including state machine states and sync tracking
- MovementEventEntity created with 28 fields for comprehensive telemetry capture
- LocationEntity extended with 8 new fields for transportation mode context
- MIGRATION_7_8 creates both tables, adds columns to locations, creates all indexes
- Foreign key constraint implemented with SET NULL on delete
- TripDao and MovementEventDao placeholders created for E8.2
- DatabaseModule updated with new migration and DAO providers

---

## File List

### Created Files
- `app/src/main/java/three/two/bit/phonemanager/data/model/TripEntity.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/model/MovementEventEntity.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/database/TripDao.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/database/MovementEventDao.kt`
- `app/src/test/java/three/two/bit/phonemanager/data/model/TripEntityTest.kt`
- `app/src/test/java/three/two/bit/phonemanager/data/model/MovementEventEntityTest.kt`
- `app/src/test/java/three/two/bit/phonemanager/data/database/AppDatabaseMigrationTest.kt`

### Modified Files
- `app/src/main/java/three/two/bit/phonemanager/data/model/LocationEntity.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/database/AppDatabase.kt`
- `app/src/main/java/three/two/bit/phonemanager/di/DatabaseModule.kt`
- `app/src/test/java/three/two/bit/phonemanager/ui/settings/SettingsViewModelTest.kt` (bug fix)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-30 | Martin (PM) | Story created from Epic E8 specification |
| 2025-11-30 | Dev Agent | Implementation completed - all tasks done |

---

**Last Updated**: 2025-11-30
**Status**: Done
**Dependencies**: None (foundation story)
