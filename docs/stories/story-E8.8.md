# Story E8.8: Add trip detection preferences to PreferencesRepository

**Story ID**: E8.8
**Epic**: 8 - Movement Tracking & Intelligent Path Detection
**Priority**: Should-Have
**Estimate**: 2 story points (1 day)
**Status**: Done
**Created**: 2025-11-30
**PRD Reference**: PRD-movement-tracking.md, ANDROID_APP_SPEC.md

---

## Story

As a user,
I need configurable trip detection parameters,
so that I can adjust sensitivity to my travel patterns.

## Acceptance Criteria

### AC E8.8.1: Trip Detection Enabled Preference
**Given** user wants to control trip detection
**Then** PreferencesRepository should include:
  - isTripDetectionEnabled: Flow<Boolean>
  - setTripDetectionEnabled(enabled: Boolean)
  - Default value: true

### AC E8.8.2: Stationary Threshold Preference
**Given** user wants to customize when trips end
**Then** PreferencesRepository should include:
  - tripStationaryThresholdMinutes: Flow<Int>
  - setTripStationaryThresholdMinutes(minutes: Int)
  - Default: 5, Range: 1-30 minutes

### AC E8.8.3: Minimum Trip Duration Preference
**Given** user wants to filter short trips
**Then** PreferencesRepository should include:
  - tripMinimumDurationMinutes: Flow<Int>
  - setTripMinimumDurationMinutes(minutes: Int)
  - Default: 2, Range: 1-10 minutes

### AC E8.8.4: Minimum Trip Distance Preference
**Given** user wants to filter short distance trips
**Then** PreferencesRepository should include:
  - tripMinimumDistanceMeters: Flow<Int>
  - setTripMinimumDistanceMeters(meters: Int)
  - Default: 100, Range: 50-500 meters

### AC E8.8.5: Auto-Merge Preference
**Given** user wants to merge trips with brief stops
**Then** PreferencesRepository should include:
  - isTripAutoMergeEnabled: Flow<Boolean>
  - setTripAutoMergeEnabled(enabled: Boolean)
  - Default: true

### AC E8.8.6: Grace Period Preferences
**Given** different modes need different grace periods
**Then** PreferencesRepository should include:
  - tripVehicleGraceSeconds: Flow<Int>
  - setTripVehicleGraceSeconds(seconds: Int)
  - Default: 90, Range: 30-180 seconds
  - tripWalkingGraceSeconds: Flow<Int>
  - setTripWalkingGraceSeconds(seconds: Int)
  - Default: 60, Range: 30-120 seconds

### AC E8.8.7: Reactive Updates
**Given** preferences are used reactively
**Then** all preferences should:
  - Be exposed as Flow for reactive updates
  - Emit immediately on subscription with current value
  - TripManager should observe and adjust behavior accordingly

## Tasks / Subtasks

- [x] Task 1: Define DataStore Keys (AC: All)
  - [x] Add TRIP_DETECTION_ENABLED key
  - [x] Add TRIP_STATIONARY_THRESHOLD_MINUTES key
  - [x] Add TRIP_MINIMUM_DURATION_MINUTES key
  - [x] Add TRIP_MINIMUM_DISTANCE_METERS key
  - [x] Add TRIP_AUTO_MERGE_ENABLED key
  - [x] Add TRIP_VEHICLE_GRACE_SECONDS key
  - [x] Add TRIP_WALKING_GRACE_SECONDS key

- [x] Task 2: Update PreferencesRepository Interface (AC: E8.8.1-E8.8.6)
  - [x] Add isTripDetectionEnabled Flow property
  - [x] Add setTripDetectionEnabled suspend function
  - [x] Add tripStationaryThresholdMinutes Flow property
  - [x] Add setTripStationaryThresholdMinutes suspend function
  - [x] Add tripMinimumDurationMinutes Flow property
  - [x] Add setTripMinimumDurationMinutes suspend function
  - [x] Add tripMinimumDistanceMeters Flow property
  - [x] Add setTripMinimumDistanceMeters suspend function
  - [x] Add isTripAutoMergeEnabled Flow property
  - [x] Add setTripAutoMergeEnabled suspend function
  - [x] Add tripVehicleGraceSeconds Flow property
  - [x] Add setTripVehicleGraceSeconds suspend function
  - [x] Add tripWalkingGraceSeconds Flow property
  - [x] Add setTripWalkingGraceSeconds suspend function

- [x] Task 3: Implement in PreferencesRepositoryImpl (AC: All)
  - [x] Implement isTripDetectionEnabled with default true
  - [x] Implement setTripDetectionEnabled
  - [x] Implement tripStationaryThresholdMinutes with default 5
  - [x] Implement setTripStationaryThresholdMinutes with range validation
  - [x] Implement tripMinimumDurationMinutes with default 2
  - [x] Implement setTripMinimumDurationMinutes with range validation
  - [x] Implement tripMinimumDistanceMeters with default 100
  - [x] Implement setTripMinimumDistanceMeters with range validation
  - [x] Implement isTripAutoMergeEnabled with default true
  - [x] Implement setTripAutoMergeEnabled
  - [x] Implement tripVehicleGraceSeconds with default 90
  - [x] Implement setTripVehicleGraceSeconds with range validation
  - [x] Implement tripWalkingGraceSeconds with default 60
  - [x] Implement setTripWalkingGraceSeconds with range validation

- [x] Task 4: Add Range Validation Helpers (AC: E8.8.2-E8.8.4, E8.8.6)
  - [x] Use coerceIn for range validation
  - [x] Validate stationary threshold 1-30
  - [x] Validate minimum duration 1-10
  - [x] Validate minimum distance 50-500
  - [x] Validate vehicle grace 30-180
  - [x] Validate walking grace 30-120

- [x] Task 5: Update TripManager to Observe Preferences (AC: E8.8.7)
  - [x] PreferencesRepository already injected into TripManager
  - [x] Observe isTripDetectionEnabled
  - [x] Observe stationaryThreshold
  - [x] Observe grace periods
  - [x] Update behavior when preferences change (via cached values)

- [x] Task 6: Testing (All ACs)
  - [x] Existing tests continue to pass
  - [x] Build compiles successfully

## Dev Notes

### Preference Keys and Defaults

| Preference | Key | Default | Range |
|-----------|-----|---------|-------|
| Trip Detection Enabled | `trip_detection_enabled` | `true` | - |
| Stationary Threshold | `trip_stationary_threshold_minutes` | `5` | 1-30 |
| Minimum Duration | `trip_minimum_duration_minutes` | `2` | 1-10 |
| Minimum Distance | `trip_minimum_distance_meters` | `100` | 50-500 |
| Auto-Merge Enabled | `trip_auto_merge_enabled` | `true` | - |
| Vehicle Grace Period | `trip_vehicle_grace_seconds` | `90` | 30-180 |
| Walking Grace Period | `trip_walking_grace_seconds` | `60` | 30-120 |

### Interface Additions

```kotlin
interface PreferencesRepository {
    // ... existing ...

    // Trip Detection Settings
    val isTripDetectionEnabled: Flow<Boolean>
    suspend fun setTripDetectionEnabled(enabled: Boolean)

    val tripStationaryThresholdMinutes: Flow<Int>
    suspend fun setTripStationaryThresholdMinutes(minutes: Int)

    val tripMinimumDurationMinutes: Flow<Int>
    suspend fun setTripMinimumDurationMinutes(minutes: Int)

    val tripMinimumDistanceMeters: Flow<Int>
    suspend fun setTripMinimumDistanceMeters(meters: Int)

    val isTripAutoMergeEnabled: Flow<Boolean>
    suspend fun setTripAutoMergeEnabled(enabled: Boolean)

    val tripVehicleGraceSeconds: Flow<Int>
    suspend fun setTripVehicleGraceSeconds(seconds: Int)

    val tripWalkingGraceSeconds: Flow<Int>
    suspend fun setTripWalkingGraceSeconds(seconds: Int)
}
```

### Implementation Pattern

```kotlin
override val tripStationaryThresholdMinutes: Flow<Int> = dataStore.data
    .map { preferences ->
        preferences[TRIP_STATIONARY_THRESHOLD_MINUTES] ?: 5
    }

override suspend fun setTripStationaryThresholdMinutes(minutes: Int) {
    val validatedMinutes = minutes.coerceIn(1, 30)
    dataStore.edit { preferences ->
        preferences[TRIP_STATIONARY_THRESHOLD_MINUTES] = validatedMinutes
    }
}
```

### Files to Modify

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/data/preferences/PreferencesRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/preferences/PreferencesRepositoryImpl.kt`
- `app/src/main/java/three/two/bit/phonemanager/trip/TripManagerImpl.kt`

### Dependencies

- Existing DataStore infrastructure
- Story E8.4 (TripManager to observe preferences)

### References

- [Source: ANDROID_APP_SPEC.md - Section 3.5: Detection Algorithm Configuration]
- [Source: Epic-E8-Movement-Tracking.md - Story E8.8]

---

## Dev Agent Record

### Debug Log

- Added 7 new preference keys to PreferencesKeys object
- Added 7 interface methods (Flow + setter) to PreferencesRepository
- Implemented all 7 preference flows with error handling and defaults
- All setter methods use coerceIn for range validation:
  - stationaryThreshold: 1-30 minutes
  - minimumDuration: 1-10 minutes
  - minimumDistance: 50-500 meters
  - vehicleGrace: 30-180 seconds
  - walkingGrace: 30-120 seconds
- Added companion object constants for all default values
- Updated TripManagerImpl with @Volatile cached preference values
- Added observePreferences() method to watch all preference flows
- Updated getGraceSeconds() to use cached preference values
- Added tripDetectionEnabled check in startMonitoring()

### Completion Notes

Implementation completed successfully:
- All 7 trip detection preferences added to PreferencesRepository
- Range validation using coerceIn for numeric preferences
- TripManager observes all preferences via Flow and updates cached values
- Grace periods dynamically adjust based on user preferences
- Trip detection can be disabled via isTripDetectionEnabled

---

## File List

### Created Files
- None

### Modified Files
- `app/src/main/java/three.two.bit/phonemanager/data/preferences/PreferencesRepository.kt`
- `app/src/main/java/three.two.bit/phonemanager/trip/TripManagerImpl.kt`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-30 | Martin (PM) | Story created from Epic E8 specification |
| 2025-11-30 | Dev Agent | Implementation completed - all tasks done |

---

**Last Updated**: 2025-11-30
**Status**: Done
**Dependencies**: E8.4 (TripManager to use preferences)
