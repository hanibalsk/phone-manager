# Story E5.2: Proximity Alert Triggering

**Story ID**: E5.2
**Epic**: 5 - Proximity Alerts
**Priority**: Must-Have
**Estimate**: 2 story points (1-2 days)
**Status**: Foundation Complete (Calculation Logic Ready, Integration Deferred)
**Created**: 2025-11-25
**PRD Reference**: Feature 3 (FR-3.2, FR-3.3)

---

## Story

As a user,
I want to receive notifications when proximity alerts trigger,
so that I know when someone is nearby.

## Acceptance Criteria

### AC E5.2.1: Client-Side Distance Calculation
**Given** I have active proximity alerts
**When** a location poll cycle occurs
**Then** the app should calculate distance between my location and target devices
**And** use Haversine formula or Location.distanceTo()

### AC E5.2.2: Calculation During Poll Cycle
**Given** the map is polling for group member locations (E3.3)
**When** new locations are received
**Then** proximity alert distances should be recalculated
**And** this should happen automatically without user action

### AC E5.2.3: State Tracking
**Given** I have a proximity alert
**Then** the system should track lastState ("inside" or "outside") for each alert
**And** state should persist across app restarts

### AC E5.2.4: Trigger on State Transition Only
**Given** I have a proximity alert with direction="enter"
**And** the target is currently OUTSIDE the radius
**When** the target moves INSIDE the radius
**Then** the alert should trigger (state transition OUTSIDE→INSIDE)
**But** subsequent polls while still INSIDE should NOT trigger again (debounce)

### AC E5.2.5: Local Notification on Trigger
**Given** a proximity alert triggers
**When** the state transition is detected
**Then** a local notification should appear
**And** notification should include: "You are near {targetDisplayName}"
**And** notification should be actionable (tap to open map)

### AC E5.2.6: Update lastTriggeredAt
**Given** a proximity alert triggers
**When** the notification is shown
**Then** lastTriggeredAt should be updated
**And** synced to server (optional)

## Tasks / Subtasks

- [x] Task 1: Implement Distance Calculation (AC: E5.2.1)
  - [x] Create ProximityCalculator utility object
  - [x] Implement Haversine formula for great circle distance
  - [x] calculateDistance() for coordinate pairs and LatLng points
- [ ] Task 2: Integrate with Polling Cycle (AC: E5.2.2) - DEFERRED
  - [ ] Requires AlertRepository from E5.1
  - [ ] ProximityManager integration deferred until E5.1 complete
- [ ] Task 3: Implement State Tracking (AC: E5.2.3, E5.2.4) - FOUNDATION READY
  - [x] State transition detection in shouldTrigger() logic
  - [ ] lastState update in database (requires repository)
- [x] Task 4: Implement Direction Logic (AC: E5.2.4)
  - [x] Handle ENTER direction: trigger on OUTSIDE→INSIDE
  - [x] Handle EXIT direction: trigger on INSIDE→OUTSIDE
  - [x] Handle BOTH: trigger on either transition
  - [x] Debouncing: no trigger when state unchanged
- [ ] Task 5: Create Notification System (AC: E5.2.5) - DEFERRED
  - [ ] Requires ProximityManager integration
  - [ ] Notification manager deferred until triggering integrated
- [ ] Task 6: Update Triggered Timestamp (AC: E5.2.6) - DEFERRED
  - [ ] Requires AlertRepository and database operations
  - [ ] Deferred until E5.1 repository implemented
- [x] Task 7: Testing (All ACs)
  - [x] Unit test distance calculation (10 tests, all passing)
  - [x] Unit test state transition logic (comprehensive coverage)
  - [ ] Manual test notification (requires full integration)

## Dev Notes

### Architecture
- Create ProximityManager to coordinate calculation and triggering
- Integrate with existing polling from E3.3
- Use Android NotificationManager for local notifications

### Distance Calculation
```kotlin
object ProximityCalculator {
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] // distance in meters
    }

    fun checkProximity(
        myLocation: LatLng,
        targetLocation: LatLng,
        alert: ProximityAlert
    ): ProximityCheckResult {
        val distance = calculateDistance(
            myLocation.latitude, myLocation.longitude,
            targetLocation.latitude, targetLocation.longitude
        )
        val isInside = distance <= alert.radiusMeters
        val newState = if (isInside) ProximityState.INSIDE else ProximityState.OUTSIDE

        return ProximityCheckResult(
            distance = distance,
            newState = newState,
            triggered = shouldTrigger(alert, newState)
        )
    }

    private fun shouldTrigger(alert: ProximityAlert, newState: ProximityState): Boolean {
        if (alert.lastState == newState) return false // No transition

        return when (alert.direction) {
            AlertDirection.ENTER -> newState == ProximityState.INSIDE
            AlertDirection.EXIT -> newState == ProximityState.OUTSIDE
            AlertDirection.BOTH -> true
        }
    }
}
```

### Notification
```kotlin
class ProximityNotificationManager(private val context: Context) {

    fun showProximityAlert(alert: ProximityAlert, targetName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setContentTitle("Proximity Alert")
            .setContentText("You are near $targetName")
            .setSmallIcon(R.drawable.ic_proximity)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createMapPendingIntent())
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(alert.id.hashCode(), notification)
    }
}
```

### Files to Create/Modify
- `util/ProximityCalculator.kt` (NEW)
- `proximity/ProximityManager.kt` (NEW)
- `notification/ProximityNotificationManager.kt` (NEW)
- `ui/map/MapViewModel.kt` (MODIFY - integrate proximity check)
- `data/repository/AlertRepository.kt` (MODIFY - update state)

### References
- [Source: PRD FR-3.2.1-3.2.3 - Proximity Calculation requirements]
- [Source: PRD FR-3.3.1-3.3.4 - Alert Triggering requirements]
- [Source: PRD Acceptance Criteria - Proximity Alerts scenarios]
- [Source: epics.md - Story 5.2 description]

## Dev Agent Record

### Context Reference
- `/Users/martinjanci/cursor/phone-manager/docs/story-context-E5.2.xml` (Generated: 2025-11-25)

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

**Task 1: Implement Distance Calculation**
- Created ProximityCalculator object with pure functions
- Implemented Haversine formula for great circle distance (AC E5.2.1)
- Earth radius: 6371000 meters
- calculateDistance() overloads for coordinate pairs and LatLng points
- Returns distance in meters (Float)

**Task 4: Implement Direction Logic**
- Created checkProximity() method combining distance calculation and state logic
- Implemented shouldTrigger() with direction-specific logic:
  - ENTER: Triggers on OUTSIDE→INSIDE transition
  - EXIT: Triggers on INSIDE→OUTSIDE transition
  - BOTH: Triggers on any state transition
- AC E5.2.4: Debouncing via state comparison (no trigger when state unchanged)
- Returns ProximityCheckResult with distance, newState, triggered flag

**Task 7: Testing**
- Created ProximityCalculatorTest with 10 comprehensive tests:
  - Distance calculation (same point, different points)
  - State detection (INSIDE, OUTSIDE)
  - ENTER direction triggering (transition and debouncing)
  - EXIT direction triggering (transition and debouncing)
  - BOTH direction triggering (both transitions and debouncing)
- All 10 tests passing
- Code formatted with Spotless

**Tasks 2, 3, 5, 6: Deferred**
- Polling integration (Task 2) requires AlertRepository from E5.1
- State persistence (Task 3) requires AlertRepository database operations
- Notification system (Task 5) requires ProximityManager integration
- Timestamp updates (Task 6) requires AlertRepository
- Foundation complete for future integration

### Completion Notes List

**Story E5.2 Implementation - Foundation Complete**:
- Task 1 completed successfully (distance calculation with Haversine)
- Task 4 completed successfully (direction logic and state transitions)
- Task 7 completed successfully (10 comprehensive unit tests)
- Tasks 2, 3, 5, 6 deferred pending E5.1 completion (AlertRepository required)
- ProximityCalculator utility ready for integration
- State transition logic thoroughly tested
- Build successful, all tests passing

**Acceptance Criteria Status**:
- AC E5.2.1: ✅ Complete (Haversine distance calculation)
- AC E5.2.2: Foundation ready, integration deferred
- AC E5.2.3: Logic complete, persistence deferred
- AC E5.2.4: ✅ Complete (state transition detection and debouncing)
- AC E5.2.5: Deferred (requires integration)
- AC E5.2.6: Deferred (requires integration)

**Note**: This story focuses on proximity calculation and triggering logic. Integration components (Tasks 2, 3, 5, 6) deferred as they require AlertRepository from E5.1.

### File List

**Created:**
- app/src/main/java/three/two/bit/phonemanager/util/ProximityCalculator.kt
- app/src/test/java/three/two/bit/phonemanager/util/ProximityCalculatorTest.kt

**Modified:**
- None (standalone utility implementation)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-25 | Claude | Initial story creation |
| 2025-11-25 | Claude | Task 1: Implemented Haversine distance calculation |
| 2025-11-25 | Claude | Task 4: Implemented state transition and direction logic |
| 2025-11-25 | Claude | Task 7: All tests passing (10 comprehensive tests) |
| 2025-11-25 | Claude | Tasks 2, 3, 5, 6: Deferred pending E5.1 AlertRepository |
| 2025-11-25 | Claude | Story E5.2 FOUNDATION - Calculation Logic Complete, Integration Deferred |

---

**Last Updated**: 2025-11-25
**Status**: Foundation Complete (Calculation Logic Ready, Integration Deferred)
**Dependencies**: Story E5.1 (Proximity Alert Definition) - Foundation Complete (requires full completion for integration), Story E3.3 (Real-Time Location Polling) - Approved
