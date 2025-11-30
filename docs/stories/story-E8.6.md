# Story E8.6: Integrate movement event recording into TransportationModeManager

**Story ID**: E8.6
**Epic**: 8 - Movement Tracking & Intelligent Path Detection
**Priority**: Must-Have
**Estimate**: 2 story points (1 day)
**Status**: Done
**Created**: 2025-11-30
**PRD Reference**: PRD-movement-tracking.md, ANDROID_APP_SPEC.md

---

## Story

As a system,
I need to record movement events whenever the transportation state changes,
so that all mode transitions are logged with telemetry.

## Acceptance Criteria

### AC E8.6.1: TransportationModeManager Subscribes to State Changes
**Given** TransportationModeManager emits state changes
**When** transportationState flow emits a new state
**Then** the manager should:
  - Subscribe to its own transportationState flow
  - Filter for distinct mode changes (not confidence-only updates)
  - Track previous state and timestamp

### AC E8.6.2: Movement Event Recording on Mode Change
**Given** a mode change is detected
**When** previousMode != newMode
**Then** system should:
  - Call SensorTelemetryCollector.collect()
  - Get current location from LocationManager
  - Record MovementEventEntity via MovementEventRepository

### AC E8.6.3: Movement Event Fields Populated
**Given** a movement event is being recorded
**Then** the event should include:
  - previousMode from last state
  - newMode from current state
  - source (DetectionSource) from current state
  - confidence from current state
  - detectionLatencyMs calculated from timestamps
  - location snapshot (lat, lon, accuracy, speed)
  - Full telemetry from SensorTelemetryCollector
  - tripId from TripManager.activeTrip if available

### AC E8.6.4: Detection Latency Calculation
**Given** mode change timing is important
**Then** detectionLatencyMs should:
  - Be calculated as: currentTime - previousStateTimestamp
  - Represent time between when state changed and when it was detected
  - Be logged for debugging purposes

### AC E8.6.5: Integration with TripManager
**Given** trip tracking may be active
**Then** the event should:
  - Include tripId if activeTrip exists
  - Include null tripId if no active trip

### AC E8.6.6: Error Handling
**Given** event recording should not block mode detection
**Then** the system should:
  - Record events asynchronously (fire-and-forget)
  - Log errors but not crash
  - Continue mode detection even if recording fails

## Tasks / Subtasks

- [x] Task 1: Add Dependencies to TransportationModeManager (AC: E8.6.1)
  - [x] Inject MovementEventRepository
  - [x] Inject SensorTelemetryCollector
  - [x] Inject TripManager (using dagger.Lazy to break circular dependency)
  - [x] Add lastState and lastStateTimestamp tracking variables

- [x] Task 2: Create State Change Subscription (AC: E8.6.1)
  - [x] Subscribe to transportationState flow in startMonitoring method
  - [x] Use distinctUntilChanged with mode comparison
  - [x] Track previousState before updating lastState
  - [x] Update lastStateTimestamp on each state change

- [x] Task 3: Implement recordMovementEvent Method (AC: E8.6.2, E8.6.3)
  - [x] Create private fun recordMovementEvent() launching coroutine
  - [x] Accept previousState, newState, previousTimestamp parameters
  - [x] Call sensorTelemetryCollector.collect()
  - [x] Get location from locationManager.getLastKnownLocation()
  - [x] Calculate detectionLatencyMs

- [x] Task 4: Build MovementEvent Data (AC: E8.6.3, E8.6.5)
  - [x] Map previousState.mode to previousMode
  - [x] Map newState.mode to newMode
  - [x] Map newState.source to detectionSource (with DetectionSource mapping)
  - [x] Derive confidence from source type
  - [x] Include tripId from tripManagerLazy.get().activeTrip.value?.id
  - [x] Include all telemetry fields (DeviceState, SensorTelemetry)

- [x] Task 5: Call Repository to Record Event (AC: E8.6.2)
  - [x] Call movementEventRepository.recordEvent() with all parameters
  - [x] Handle Result success/failure with logging

- [x] Task 6: Calculate Detection Latency (AC: E8.6.4)
  - [x] Calculate: System.currentTimeMillis() - previousTimestamp
  - [x] Log latency value for debugging with Timber
  - [x] Include in event record

- [x] Task 7: Implement Async Error Handling (AC: E8.6.6)
  - [x] Launch recording in managerScope
  - [x] Wrap recording in try-catch
  - [x] Log errors with Timber
  - [x] Ensure mode detection continues on failure

- [x] Task 8: Testing (All ACs)
  - [x] Existing tests continue to pass
  - [x] Build compiles successfully with circular dependency resolved

## Dev Notes

### TransportationModeManager Integration Code

```kotlin
@Singleton
class TransportationModeManager @Inject constructor(
    // ... existing dependencies ...
    private val movementEventRepository: MovementEventRepository,
    private val sensorTelemetryCollector: SensorTelemetryCollector,
    private val tripManager: TripManager,
) {
    private var lastState: TransportationState? = null
    private var lastStateTimestamp: Long = 0L

    init {
        transportationState
            .distinctUntilChanged { old, new -> old.mode == new.mode }
            .onEach { newState ->
                val previousState = lastState
                val previousTimestamp = lastStateTimestamp

                lastState = newState
                lastStateTimestamp = System.currentTimeMillis()

                if (previousState != null && previousState.mode != newState.mode) {
                    recordMovementEvent(previousState, newState, previousTimestamp)
                }
            }
            .launchIn(managerScope)
    }

    private suspend fun recordMovementEvent(
        previousState: TransportationState,
        newState: TransportationState,
        previousTimestamp: Long,
    ) {
        try {
            val telemetry = sensorTelemetryCollector.collect()
            val location = locationManager.getLastKnownLocation()
            val detectionLatency = System.currentTimeMillis() - previousTimestamp

            movementEventRepository.recordEvent(
                previousMode = previousState.mode,
                newMode = newState.mode,
                source = newState.source,
                confidence = newState.confidence,
                detectionLatencyMs = detectionLatency,
                location = location,
                telemetry = telemetry,
                tripId = tripManager.activeTrip.value?.id,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to record movement event")
        }
    }
}
```

### Event Recording Flow

```
transportationState emits
       ↓
distinctUntilChanged (mode only)
       ↓
Check previousState != null
       ↓
Check previousMode != newMode
       ↓
recordMovementEvent()
  ├─ collect telemetry
  ├─ get location
  ├─ calculate latency
  └─ save to repository
```

### Files to Modify

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/movement/TransportationModeManager.kt`

### Dependencies

- Story E8.3 (MovementEventRepository)
- Story E8.5 (SensorTelemetryCollector)
- Story E8.4 (TripManager)

### References

- [Source: ANDROID_APP_SPEC.md - Section 4.3: Integration in TransportationModeManager]
- [Source: Epic-E8-Movement-Tracking.md - Story E8.6]

---

## Dev Agent Record

### Debug Log

- Initial build failed with circular dependency: TransportationModeManager → TripManager → TransportationModeManager
- Resolved by using dagger.Lazy<TripManager> for lazy injection
- Added imports for flow operations (distinctUntilChanged, launchIn, onEach)
- Created mapping functions for DetectionSource (movement → domain) and NetworkType
- Used @Suppress("MissingPermission") for getLastKnownLocation()

### Completion Notes

Implementation completed successfully:
- Added dependencies: MovementEventRepository, SensorTelemetryCollector, Lazy<TripManager>
- State tracking: lastState and lastStateTimestamp volatile variables
- State subscription: distinctUntilChanged filtering on mode, launchIn managerScope
- recordMovementEvent: Async coroutine with full error handling
- Field mapping: DetectionSource enum mapping, confidence derived from source type
- Telemetry integration: DeviceState and SensorTelemetry from SensorTelemetryCollector
- Location capture: getLastKnownLocation() with fallback providers
- Latency calculation: currentTime - previousTimestamp with debug logging

---

## File List

### Created Files
- None

### Modified Files
- `app/src/main/java/three.two.bit/phonemanager/movement/TransportationModeManager.kt`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-30 | Martin (PM) | Story created from Epic E8 specification |
| 2025-11-30 | Dev Agent | Implementation completed - all tasks done |

---

**Last Updated**: 2025-11-30
**Status**: Done
**Dependencies**: E8.3, E8.4, E8.5
