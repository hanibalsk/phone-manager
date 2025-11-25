# Story E5.2: Proximity Alert Triggering

**Story ID**: E5.2
**Epic**: 5 - Proximity Alerts
**Priority**: Must-Have
**Estimate**: 2 story points (1-2 days)
**Status**: ContextReadyDraft
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

- [ ] Task 1: Implement Distance Calculation (AC: E5.2.1)
  - [ ] Create ProximityCalculator utility class
  - [ ] Implement Haversine or use Location.distanceTo()
  - [ ] Calculate distance for each active alert
- [ ] Task 2: Integrate with Polling Cycle (AC: E5.2.2)
  - [ ] Hook into MapViewModel or create ProximityManager
  - [ ] Call calculation after each successful poll
  - [ ] Pass current location and group member locations
- [ ] Task 3: Implement State Tracking (AC: E5.2.3, E5.2.4)
  - [ ] Update lastState in ProximityAlertEntity
  - [ ] Detect state transitions (OUTSIDE→INSIDE, INSIDE→OUTSIDE)
  - [ ] Only trigger on transitions, not continuous state
- [ ] Task 4: Implement Direction Logic (AC: E5.2.4)
  - [ ] Handle ENTER direction: trigger on OUTSIDE→INSIDE
  - [ ] Handle EXIT direction: trigger on INSIDE→OUTSIDE
  - [ ] Handle BOTH: trigger on either transition
- [ ] Task 5: Create Notification System (AC: E5.2.5)
  - [ ] Create ProximityNotificationManager
  - [ ] Build notification with target name
  - [ ] Add PendingIntent to open MapScreen
  - [ ] Create notification channel for alerts
- [ ] Task 6: Update Triggered Timestamp (AC: E5.2.6)
  - [ ] Update lastTriggeredAt in local database
  - [ ] Optionally sync to server via POST /api/proximity-alerts/{id}/events
- [ ] Task 7: Testing (All ACs)
  - [ ] Unit test distance calculation
  - [ ] Unit test state transition logic
  - [ ] Manual test notification with 2 devices

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
<!-- Add debug log references during implementation -->

### Completion Notes List
<!-- Add completion notes during implementation -->

### File List
<!-- Add list of files created/modified during implementation -->

---

**Last Updated**: 2025-11-25
**Status**: ContextReadyDraft
**Dependencies**: Story E5.1 (Proximity Alert Definition), Story E3.3 (Real-Time Location Polling)
