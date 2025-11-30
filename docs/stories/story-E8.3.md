# Story E8.3: Create TripRepository and MovementEventRepository with domain models

**Story ID**: E8.3
**Epic**: 8 - Movement Tracking & Intelligent Path Detection
**Priority**: Must-Have
**Estimate**: 3 story points (1-2 days)
**Status**: Done
**Created**: 2025-11-30
**PRD Reference**: PRD-movement-tracking.md, ANDROID_APP_SPEC.md

---

## Story

As a developer,
I need repository interfaces and implementations with domain models,
so that data access is abstracted from the rest of the app.

## Acceptance Criteria

### AC E8.3.1: Trip Domain Model Created
**Given** the domain layer needs Trip representation
**Then** Trip domain model should include:
  - All fields from TripEntity mapped to Kotlin types
  - startTime/endTime as Instant
  - startLocation/endLocation as LatLng
  - dominantMode as TransportationMode enum
  - modesUsed as Set<TransportationMode>
  - modeBreakdown as Map<TransportationMode, Long>
  - Computed properties: isActive, averageSpeedKmh, durationSeconds

### AC E8.3.2: MovementEvent Domain Model Created
**Given** the domain layer needs MovementEvent representation
**Then** MovementEvent domain model should include:
  - id, timestamp, tripId
  - previousMode/newMode as TransportationMode enum
  - detectionSource as DetectionSource enum
  - confidence, detectionLatencyMs
  - Nested EventLocation data class
  - Nested DeviceState data class
  - Nested SensorTelemetry data class

### AC E8.3.3: TripRepository Interface Created
**Given** the repository pattern is used
**Then** TripRepository interface should include:
  - insert(trip: Trip): Result<String>
  - update(trip: Trip): Result<Unit>
  - getTripById(id: String): Trip?
  - observeTripById(id: String): Flow<Trip?>
  - getActiveTrip(): Trip?
  - observeActiveTrip(): Flow<Trip?>
  - observeRecentTrips(limit: Int): Flow<List<Trip>>
  - getTripsBetween(start: Instant, end: Instant): List<Trip>
  - incrementLocationCount(tripId: String, distance: Double)
  - observeTodayStats(): Flow<TodayTripStats>

### AC E8.3.4: TripRepositoryImpl Implemented
**Given** TripRepository interface exists
**Then** TripRepositoryImpl should:
  - Inject TripDao
  - Implement all interface methods
  - Map between Entity and Domain models
  - Handle JSON serialization for modes

### AC E8.3.5: MovementEventRepository Interface Created
**Given** movement events need repository access
**Then** MovementEventRepository interface should include:
  - recordEvent(...): Result<Long>
  - observeRecentEvents(limit: Int): Flow<List<MovementEvent>>
  - observeEventsByTrip(tripId: String): Flow<List<MovementEvent>>
  - observeLatestEvent(): Flow<MovementEvent?>
  - getEventsBetween(start: Instant, end: Instant): List<MovementEvent>
  - observeUnsyncedCount(): Flow<Int>

### AC E8.3.6: MovementEventRepositoryImpl Implemented
**Given** MovementEventRepository interface exists
**Then** MovementEventRepositoryImpl should:
  - Inject MovementEventDao
  - Implement all interface methods
  - Map between Entity and Domain models
  - Handle nullable telemetry fields

### AC E8.3.7: Entity-Domain Mapping Functions
**Given** entities need to convert to domain models
**Then** mapping functions should:
  - Convert TripEntity to Trip and vice versa
  - Convert MovementEventEntity to MovementEvent and vice versa
  - Handle JSON parsing for modes
  - Handle nullable fields gracefully

### AC E8.3.8: Hilt Bindings Added
**Given** DI is managed by Hilt
**Then** RepositoryModule should include:
  - @Binds for TripRepository -> TripRepositoryImpl
  - @Binds for MovementEventRepository -> MovementEventRepositoryImpl

## Tasks / Subtasks

- [x] Task 1: Create Trip Domain Model (AC: E8.3.1)
  - [x] Create Trip data class in domain/model/Trip.kt
  - [x] Add all required fields with proper types
  - [x] Add computed property isActive
  - [x] Add computed property averageSpeedKmh
  - [x] Add computed property durationSeconds
  - [x] Create TodayTripStats data class

- [x] Task 2: Create MovementEvent Domain Model (AC: E8.3.2)
  - [x] Create MovementEvent data class in domain/model/MovementEvent.kt
  - [x] Create nested EventLocation data class
  - [x] Create nested DeviceState data class
  - [x] Create nested SensorTelemetry data class
  - [x] Create MovementContext data class

- [x] Task 3: Create TripRepository Interface (AC: E8.3.3)
  - [x] Create TripRepository interface in data/repository/TripRepository.kt
  - [x] Define all required methods
  - [x] Use proper return types (Flow, Result, nullable)

- [x] Task 4: Implement TripRepositoryImpl (AC: E8.3.4, E8.3.7)
  - [x] Create TripRepositoryImpl class
  - [x] Inject TripDao via constructor
  - [x] Implement all interface methods
  - [x] Create toEntity() extension function for Trip
  - [x] Create toDomain() extension function for TripEntity
  - [x] Handle JSON serialization for modesUsed and modeBreakdown

- [x] Task 5: Create MovementEventRepository Interface (AC: E8.3.5)
  - [x] Create MovementEventRepository interface in data/repository/MovementEventRepository.kt
  - [x] Define all required methods
  - [x] Include recordEvent with all telemetry parameters

- [x] Task 6: Implement MovementEventRepositoryImpl (AC: E8.3.6, E8.3.7)
  - [x] Create MovementEventRepositoryImpl class
  - [x] Inject MovementEventDao via constructor
  - [x] Implement all interface methods
  - [x] Create toEntity() mapping for recording events
  - [x] Create toDomain() mapping for querying events
  - [x] Handle nullable telemetry fields

- [x] Task 7: Add Hilt Bindings (AC: E8.3.8)
  - [x] Add TripRepository binding to RepositoryModule
  - [x] Add MovementEventRepository binding to RepositoryModule
  - [x] Ensure @Singleton scope

- [x] Task 8: Testing (All ACs)
  - [x] Unit test Trip domain model computed properties
  - [x] Unit test Entity-Domain mapping functions
  - [x] Unit test TripRepository methods with mocked DAO
  - [x] Unit test MovementEventRepository methods with mocked DAO
  - [x] Test JSON serialization/deserialization

## Dev Notes

### Domain Model Definitions

See ANDROID_APP_SPEC.md Appendix A for complete domain model definitions.

```kotlin
data class Trip(
    val id: String,
    val state: TripState,
    val startTime: Instant,
    val endTime: Instant?,
    val startLocation: LatLng,
    val endLocation: LatLng?,
    val totalDistanceMeters: Double,
    val locationCount: Int,
    val dominantMode: TransportationMode,
    val modesUsed: Set<TransportationMode>,
    val modeBreakdown: Map<TransportationMode, Long>,
    val startTrigger: TripTrigger,
    val endTrigger: TripTrigger?,
    val isSynced: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isActive: Boolean
        get() = state == TripState.ACTIVE || state == TripState.PENDING_END

    val durationSeconds: Long?
        get() = endTime?.let { it.epochSecond - startTime.epochSecond }

    val averageSpeedKmh: Double?
        get() = durationSeconds?.let { duration ->
            if (duration > 0) (totalDistanceMeters / 1000.0) / (duration / 3600.0)
            else null
        }
}
```

### MovementEvent Domain Model

```kotlin
data class MovementEvent(
    val id: Long,
    val timestamp: Instant,
    val tripId: String?,
    val previousMode: TransportationMode,
    val newMode: TransportationMode,
    val detectionSource: DetectionSource,
    val confidence: Float,
    val detectionLatencyMs: Long,
    val location: EventLocation?,
    val deviceState: DeviceState?,
    val sensorTelemetry: SensorTelemetry?,
    val isSynced: Boolean,
)

data class EventLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val speed: Float?,
)

data class DeviceState(
    val batteryLevel: Int?,
    val batteryCharging: Boolean?,
    val networkType: String?,
    val networkStrength: Int?,
)

data class SensorTelemetry(
    val accelerometerMagnitude: Float?,
    val accelerometerVariance: Float?,
    val accelerometerPeakFrequency: Float?,
    val gyroscopeMagnitude: Float?,
    val stepCount: Int?,
    val significantMotion: Boolean?,
    val activityType: String?,
    val activityConfidence: Int?,
)
```

### Files to Create/Modify

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/domain/model/Trip.kt`
- `app/src/main/java/three/two/bit/phonemanager/domain/model/MovementEvent.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/TripRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/TripRepositoryImpl.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/MovementEventRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/MovementEventRepositoryImpl.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/di/RepositoryModule.kt`

### Dependencies

- Story E8.1 (Entities)
- Story E8.2 (DAOs)

### References

- [Source: ANDROID_APP_SPEC.md - Appendix A: Domain Models]
- [Source: Epic-E8-Movement-Tracking.md - Story E8.3]

---

## Dev Agent Record

### Debug Log

- Build compiled successfully with all domain models and repositories
- All unit tests pass for TripRepository and MovementEventRepository
- Fixed kotlinx.datetime Instant.epochSeconds property name (was epochSecond)
- Updated TripDao.observeCompletedTrips to accept limit parameter
- Fixed observeTodayStats to use Flow-only methods in combine()

### Completion Notes

Implementation completed successfully:
- Trip domain model: Complete with TripState, TripTrigger enums, LatLng, TodayTripStats
- MovementEvent domain model: Complete with DetectionSource, EventLocation, DeviceState, NetworkType, SensorTelemetry, MovementContext
- TripRepository: Full interface with 18 methods covering CRUD, queries, statistics, sync
- MovementEventRepository: Full interface with 17 methods covering CRUD, queries, statistics, sync
- Entity-Domain mapping: toDomain() and toEntity() extension functions in both entity files
- Hilt bindings added to RepositoryModule with @Singleton scope
- Test coverage: 25+ unit tests for both repositories

---

## File List

### Created Files
- `app/src/main/java/three/two/bit/phonemanager/domain/model/Trip.kt`
- `app/src/main/java/three/two/bit/phonemanager/domain/model/MovementEvent.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/TripRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/TripRepositoryImpl.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/MovementEventRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/MovementEventRepositoryImpl.kt`
- `app/src/test/java/three/two/bit/phonemanager/data/repository/TripRepositoryTest.kt`
- `app/src/test/java/three/two.bit/phonemanager/data/repository/MovementEventRepositoryTest.kt`

### Modified Files
- `app/src/main/java/three/two.bit/phonemanager/data/model/TripEntity.kt` (added toDomain/toEntity mappings)
- `app/src/main/java/three.two.bit/phonemanager/data/model/MovementEventEntity.kt` (added toDomain/toEntity mappings)
- `app/src/main/java/three/two.bit/phonemanager/data/database/TripDao.kt` (added limit to observeCompletedTrips)
- `app/src/main/java/three/two.bit/phonemanager/di/RepositoryModule.kt` (added bindings)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-30 | Martin (PM) | Story created from Epic E8 specification |
| 2025-11-30 | Dev Agent | Implementation completed - all tasks done |

---

**Last Updated**: 2025-11-30
**Status**: Done
**Dependencies**: E8.1, E8.2
