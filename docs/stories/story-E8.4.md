# Story E8.4: Implement TripManager with state machine

**Story ID**: E8.4
**Epic**: 8 - Movement Tracking & Intelligent Path Detection
**Priority**: Must-Have
**Estimate**: 4 story points (2 days)
**Status**: Done
**Created**: 2025-11-30
**PRD Reference**: PRD-movement-tracking.md, ANDROID_APP_SPEC.md

---

## Story

As a system,
I need a TripManager that implements the trip state machine,
so that trips are automatically detected based on transportation mode changes.

## Acceptance Criteria

### AC E8.4.1: TripManager Interface Created
**Given** the trip detection system needs management
**Then** TripManager interface should include:
  - currentTripState: StateFlow<TripState>
  - activeTrip: StateFlow<Trip?>
  - startMonitoring(): Unit
  - stopMonitoring(): Unit
  - forceStartTrip(): Trip
  - forceEndTrip(): Trip?
  - getTripById(id: String): Trip?
  - getTripsInRange(startTime: Long, endTime: Long): List<Trip>
  - getRecentTrips(limit: Int): List<Trip>

### AC E8.4.2: State Machine Implementation
**Given** TripManagerImpl implements the interface
**Then** the state machine should implement:
  - IDLE → ACTIVE: When mode changes from STATIONARY/UNKNOWN to movement
  - ACTIVE → PENDING_END: When mode becomes STATIONARY
  - PENDING_END → ACTIVE: When movement resumes within grace period
  - PENDING_END → COMPLETED: When stationary threshold exceeded
  - COMPLETED → IDLE: After trip finalization

### AC E8.4.3: Trip Start Conditions
**Given** the system is monitoring mode changes
**When** mode changes from STATIONARY/UNKNOWN to movement mode
**Then** a new trip should start if:
  - Mode is WALKING, RUNNING, CYCLING, or IN_VEHICLE
  - 2+ consecutive location updates show movement
  - Location displacement > 10m from last position
  - Transition lasts > 30 seconds

### AC E8.4.4: Trip End Conditions with Grace Periods
**Given** an active trip exists
**When** mode becomes STATIONARY
**Then**:
  - Enter PENDING_END state
  - Start grace period timer (vehicle: 90s, walking: 60s)
  - If movement resumes within grace period → return to ACTIVE
  - If threshold exceeded → finalize trip with endTime = stationary_start_time

### AC E8.4.5: Anti-False-Positive Measures
**Given** trip detection is running
**Then** the system should:
  - Require 2+ consecutive movement locations
  - Ignore mode transitions lasting < 30 seconds
  - Validate location displacement > 10m
  - Not start trips for spurious GPS jumps

### AC E8.4.6: Trip Statistics Tracking
**Given** a trip is active
**Then** TripManager should:
  - Track mode segments (TripModeSegment)
  - Calculate dominant mode (highest duration)
  - Update modes used set
  - Update mode breakdown map (ms per mode)

### AC E8.4.7: Hilt Integration
**Given** DI is managed by Hilt
**Then** TripModule should provide:
  - TripManager singleton bound to TripManagerImpl
  - Inject TransportationModeManager, TripRepository, PreferencesRepository

## Tasks / Subtasks

- [x] Task 1: Create TripState and TripTrigger Enums (AC: E8.4.2)
  - [x] TripState and TripTrigger already exist in domain/model/Trip.kt
  - [x] Reused existing enums instead of creating duplicates

- [x] Task 2: Create TripModeSegment (AC: E8.4.6)
  - [x] Create TripModeSegment data class in trip/TripModeSegment.kt
  - [x] Include mode, startTime, endTime fields
  - [x] Add computed durationMs property
  - [x] Add durationSeconds and isActive computed properties
  - [x] Add end() helper method

- [x] Task 3: Create TripManager Interface (AC: E8.4.1)
  - [x] Create TripManager interface in trip/TripManager.kt
  - [x] Define all StateFlow properties
  - [x] Define lifecycle methods
  - [x] Define manual control methods
  - [x] Define query methods

- [x] Task 4: Implement TripManagerImpl Core (AC: E8.4.2, E8.4.7)
  - [x] Create TripManagerImpl class in trip/TripManagerImpl.kt
  - [x] Inject TransportationModeManager, TripRepository, PreferencesRepository
  - [x] Implement currentTripState StateFlow
  - [x] Implement activeTrip StateFlow
  - [x] Add CoroutineScope for background operations

- [x] Task 5: Implement State Machine Logic (AC: E8.4.2)
  - [x] Subscribe to TransportationModeManager.transportationState
  - [x] Implement IDLE → ACTIVE transition
  - [x] Implement ACTIVE → PENDING_END transition
  - [x] Implement PENDING_END → ACTIVE transition
  - [x] Implement PENDING_END → COMPLETED transition
  - [x] Implement trip finalization logic

- [x] Task 6: Implement Trip Start Logic (AC: E8.4.3, E8.4.5)
  - [x] Create canStartTrip() validation method
  - [x] Check for 2+ consecutive movement detections
  - [x] Validate transition duration > 30s
  - [x] Create trip with start trigger ACTIVITY_DETECTION

- [x] Task 7: Implement Trip End Logic (AC: E8.4.4)
  - [x] Create startPendingEndTimer() method
  - [x] Implement grace period (vehicle: 90s, walking: 60s)
  - [x] Implement timer with Job cancellation on movement
  - [x] Set endTime = stationaryStartTime (not current time)
  - [x] Set end trigger to STATIONARY_DETECTION

- [x] Task 8: Implement Mode Segment Tracking (AC: E8.4.6)
  - [x] Track active mode segment during trip
  - [x] Update mode breakdown on mode change
  - [x] Calculate dominant mode on trip end
  - [x] Build modesUsed set

- [x] Task 9: Implement Manual Controls (AC: E8.4.1)
  - [x] Implement forceStartTrip() with MANUAL trigger
  - [x] Implement forceEndTrip() with MANUAL trigger
  - [x] Handle edge cases (already active, no active trip)

- [x] Task 10: Create TripModule (AC: E8.4.7)
  - [x] Create TripModule Hilt module in di/TripModule.kt
  - [x] Bind TripManager to TripManagerImpl
  - [x] Ensure singleton scope

- [x] Task 11: Testing (All ACs)
  - [x] Unit test initial state
  - [x] Unit test monitoring lifecycle
  - [x] Unit test force start/end
  - [x] Unit test query methods
  - [x] Unit test TripModeSegment
  - [x] Unit test location updates

## Dev Notes

### State Machine Diagram

```
        ┌─────────────────────────────────────────────────────┐
        │                                                     │
        v                                                     │
    ┌──────┐     movement      ┌────────┐                    │
    │ IDLE │ ───────────────> │ ACTIVE │                     │
    └──────┘                   └───┬────┘                     │
        ^                          │                          │
        │                          │ stationary               │
        │                          v                          │
        │                   ┌─────────────┐    movement       │
        │                   │ PENDING_END │ ──────────────────┘
        │                   └──────┬──────┘
        │                          │
        │    threshold exceeded    │
        │                          v
        │                   ┌───────────┐
        └───────────────── │ COMPLETED │
                           └───────────┘
```

### Grace Period Configuration

| Previous Mode | Grace Period | Configurable Key |
|--------------|--------------|------------------|
| IN_VEHICLE | 90 seconds | trip_vehicle_grace_seconds |
| WALKING | 60 seconds | trip_walking_grace_seconds |
| CYCLING | 60 seconds | trip_walking_grace_seconds |
| RUNNING | 60 seconds | trip_walking_grace_seconds |

### TripManager Interface

```kotlin
interface TripManager {
    val currentTripState: StateFlow<TripState>
    val activeTrip: StateFlow<Trip?>

    suspend fun startMonitoring()
    fun stopMonitoring()

    suspend fun forceStartTrip(): Trip
    suspend fun forceEndTrip(): Trip?

    suspend fun getTripById(id: String): Trip?
    suspend fun getTripsInRange(startTime: Long, endTime: Long): List<Trip>
    suspend fun getRecentTrips(limit: Int = 10): List<Trip>
}
```

### Files to Create

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/trip/TripState.kt`
- `app/src/main/java/three/two/bit/phonemanager/trip/TripTrigger.kt`
- `app/src/main/java/three/two/bit/phonemanager/trip/TripModeSegment.kt`
- `app/src/main/java/three/two/bit/phonemanager/trip/TripManager.kt`
- `app/src/main/java/three/two/bit/phonemanager/trip/TripManagerImpl.kt`
- `app/src/main/java/three/two/bit/phonemanager/di/TripModule.kt`

### Dependencies

- Story E8.3 (TripRepository)
- Story E8.8 (Trip detection preferences) - can implement with defaults first

### References

- [Source: ANDROID_APP_SPEC.md - Section 3: Trip Auto-Detection System]
- [Source: Epic-E8-Movement-Tracking.md - Story E8.4]

---

## Dev Agent Record

### Debug Log

- Build compiled successfully with TripManager implementation
- All unit tests pass (17 tests)
- Fixed TransportationMode.CYCLING (was incorrectly ON_BICYCLE)
- Reused existing TripState and TripTrigger from domain/model/Trip.kt

### Completion Notes

Implementation completed successfully:
- TripModeSegment: Data class for tracking mode segments with duration calculation
- TripManager interface: 9 methods for trip lifecycle, manual controls, and queries
- TripManagerImpl: Complete state machine with IDLE→ACTIVE→PENDING_END→COMPLETED transitions
- Anti-false-positive: 2+ consecutive movements, 30s minimum duration
- Grace periods: 90s for vehicles, 60s for walking/cycling/running
- Mode tracking: Segment-based tracking with dominant mode calculation
- TripModule: Hilt bindings with @Singleton scope
- Tests: 17 unit tests covering all major functionality

---

## File List

### Created Files
- `app/src/main/java/three/two.bit/phonemanager/trip/TripModeSegment.kt`
- `app/src/main/java/three/two.bit/phonemanager/trip/TripManager.kt`
- `app/src/main/java/three.two.bit/phonemanager/trip/TripManagerImpl.kt`
- `app/src/main/java/three.two.bit/phonemanager/di/TripModule.kt`
- `app/src/test/java/three.two.bit/phonemanager/trip/TripManagerTest.kt`

### Modified Files
- None

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-30 | Martin (PM) | Story created from Epic E8 specification |
| 2025-11-30 | Dev Agent | Implementation completed - all tasks done |

---

**Last Updated**: 2025-11-30
**Status**: Done
**Dependencies**: E8.3 (Repositories), E8.8 (Preferences - optional, can use defaults)
