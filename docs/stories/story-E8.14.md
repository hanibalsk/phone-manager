# Story E8.14: Update tracking notification with active trip status

**Story ID**: E8.14
**Epic**: 8 - Movement Tracking & Intelligent Path Detection
**Priority**: Should-Have
**Estimate**: 1 story point (0.5 day)
**Status**: Planned
**Created**: 2025-11-30
**PRD Reference**: PRD-movement-tracking.md, ANDROID_APP_SPEC.md

---

## Story

As a user,
I want the tracking notification to show my current trip status,
so that I know the app is tracking my journey.

## Acceptance Criteria

### AC E8.14.1: Trip Status in Notification
**Given** a trip is currently active
**When** the notification is updated
**Then** it should show:
  - Mode icon emoji (🚗 🚶 🏃 🚲)
  - "Trip in progress" text
  - Current duration (e.g., "23 min")
  - Current distance (e.g., "8.2 km")

### AC E8.14.2: Mode Icon Updates
**Given** the transportation mode changes during trip
**Then** the notification should:
  - Update mode icon to reflect current mode
  - Keep trip duration and distance
  - Update in real-time

### AC E8.14.3: Duration Updates
**Given** a trip is in progress
**Then** the notification should:
  - Update duration periodically (every minute)
  - Format as "X min" or "X hr Y min"

### AC E8.14.4: Distance Updates
**Given** new locations are captured during trip
**Then** the notification should:
  - Update distance when significant change occurs
  - Format as "X.X km"

### AC E8.14.5: Standard Notification When No Trip
**Given** no trip is active
**Then** the notification should:
  - Show standard tracking information
  - Show current mode and confidence
  - Show "Last update: X min ago"

### AC E8.14.6: Notification Format
**Given** notification text is displayed
**Then** format should be:
  - With trip: "🚗 Trip in progress • 23 min • 8.2 km"
  - Without trip: "Walking • 95% confidence"
  - Second line: "Last update: 2 min ago"

## Tasks / Subtasks

- [ ] Task 1: Add Trip Notification Content Builder (AC: E8.14.1)
  - [ ] Create buildTripNotificationContent() method in LocationTrackingService
  - [ ] Include mode emoji
  - [ ] Include duration
  - [ ] Include distance
  - [ ] Format as single line

- [ ] Task 2: Implement Mode Emoji Mapping (AC: E8.14.2)
  - [ ] Create getModeEmoji(mode: TransportationMode) function
  - [ ] Map WALKING → 🚶
  - [ ] Map RUNNING → 🏃
  - [ ] Map CYCLING → 🚲
  - [ ] Map IN_VEHICLE → 🚗
  - [ ] Map STATIONARY → 📍
  - [ ] Map UNKNOWN → ❓

- [ ] Task 3: Add Duration Formatting (AC: E8.14.3)
  - [ ] Calculate duration from trip.startTime
  - [ ] Format as "X min" or "X hr Y min"
  - [ ] Update when notification is rebuilt

- [ ] Task 4: Add Distance Formatting (AC: E8.14.4)
  - [ ] Get totalDistanceMeters from active trip
  - [ ] Format as "X.X km"
  - [ ] Update when notification is rebuilt

- [ ] Task 5: Update Notification Building Logic (AC: E8.14.5, E8.14.6)
  - [ ] Check if activeTrip exists
  - [ ] Build trip content if active
  - [ ] Build standard content if no trip
  - [ ] Include second line with last update time

- [ ] Task 6: Trigger Notification Updates (AC: E8.14.3, E8.14.4)
  - [ ] Update notification when trip state changes
  - [ ] Update notification on location capture
  - [ ] Throttle updates to avoid excessive rebuilds

- [ ] Task 7: Testing (All ACs)
  - [ ] Unit test notification content building
  - [ ] Unit test mode emoji mapping
  - [ ] Unit test duration formatting
  - [ ] Unit test distance formatting
  - [ ] Manual test notification appearance

## Dev Notes

### Notification Format Examples

**With Active Trip:**
```
📍 Tracking Active
🚗 Trip in progress • 23 min • 8.2 km
Last update: 2 min ago
```

**Without Active Trip:**
```
📍 Tracking Active
Walking • 95% confidence
Last update: 2 min ago
```

### Mode Emoji Mapping

| Mode | Emoji |
|------|-------|
| WALKING | 🚶 |
| RUNNING | 🏃 |
| CYCLING | 🚲 |
| IN_VEHICLE | 🚗 |
| STATIONARY | 📍 |
| UNKNOWN | ❓ |

### Implementation in LocationTrackingService

```kotlin
private fun updateNotification() {
    val activeTrip = tripManager.activeTrip.value
    val transportState = transportationModeManager.transportationState.value

    val contentText = if (activeTrip != null) {
        buildTripNotificationContent(activeTrip, transportState)
    } else {
        buildStandardNotificationContent(transportState)
    }

    val notification = notificationBuilder
        .setContentText(contentText)
        .setStyle(NotificationCompat.BigTextStyle()
            .bigText("$contentText\nLast update: ${getLastUpdateText()}")
        )
        .build()

    notificationManager.notify(NOTIFICATION_ID, notification)
}

private fun buildTripNotificationContent(
    trip: Trip,
    transportState: TransportationState
): String {
    val emoji = getModeEmoji(transportState.mode)
    val duration = formatDuration(trip.startTime)
    val distance = formatDistance(trip.totalDistanceMeters)
    return "$emoji Trip in progress • $duration • $distance"
}

private fun buildStandardNotificationContent(
    transportState: TransportationState
): String {
    val mode = transportState.mode.displayName
    val confidence = (transportState.confidence * 100).toInt()
    return "$mode • $confidence% confidence"
}

private fun getModeEmoji(mode: TransportationMode): String = when (mode) {
    TransportationMode.WALKING -> "🚶"
    TransportationMode.RUNNING -> "🏃"
    TransportationMode.CYCLING -> "🚲"
    TransportationMode.IN_VEHICLE -> "🚗"
    TransportationMode.STATIONARY -> "📍"
    TransportationMode.UNKNOWN -> "❓"
}
```

### Files to Modify

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/service/LocationTrackingService.kt`

### Dependencies

- Story E8.4 (TripManager)
- Story E8.7 (TripManager integration in service)

### References

- [Source: ANDROID_APP_SPEC.md - Section 6.6: Notification Enhancement]
- [Source: Epic-E8-Movement-Tracking.md - Story E8.14]

---

## Dev Agent Record

### Debug Log

*Implementation notes will be added during development*

### Completion Notes

*To be filled upon completion*

---

## File List

### Created Files
*To be filled during implementation*

### Modified Files
*To be filled during implementation*

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-30 | Martin (PM) | Story created from Epic E8 specification |

---

**Last Updated**: 2025-11-30
**Status**: Planned
**Dependencies**: E8.4, E8.7
