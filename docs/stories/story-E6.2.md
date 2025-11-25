# Story E6.2: Geofence Events & Notifications

**Story ID**: E6.2
**Epic**: 6 - Geofencing with Webhooks
**Priority**: Must-Have
**Estimate**: 2 story points (1-2 days)
**Status**: Draft
**Created**: 2025-11-25
**PRD Reference**: Feature 5 (FR-5.2)

---

## Story

As a user,
I want local notifications on geofence events,
so that I know when I enter/exit zones.

## Acceptance Criteria

### AC E6.2.1: GeofenceBroadcastReceiver
**Given** Android Geofencing API detects a transition
**When** the system broadcasts the event
**Then** a GeofenceBroadcastReceiver should receive it
**And** extract geofence ID and transition type

### AC E6.2.2: Local Notification on Event
**Given** a geofence event is received
**When** processing the event
**Then** a local notification should be displayed
**And** title should indicate transition: "Entered {geofenceName}" or "Left {geofenceName}"
**And** notification should be high priority with sound

### AC E6.2.3: Send Event to Backend
**Given** a geofence event occurs
**When** the event is processed
**Then** the app should send event to server via `POST /api/geofence-events`
**And** payload should include: deviceId, geofenceId, eventType, timestamp, location

### AC E6.2.4: GeofenceEvent Entity
**Given** the data model is defined
**Then** GeofenceEvent should include:
  - id (UUID)
  - deviceId
  - geofenceId
  - eventType (ENTER, EXIT, DWELL)
  - timestamp
  - latitude, longitude
  - webhookDelivered (Boolean)
  - webhookResponseCode (optional)

### AC E6.2.5: Event Logging
**Given** a geofence event occurs
**When** the event is processed
**Then** it should be logged locally for history/debugging
**And** stored in Room database

### AC E6.2.6: Handle Multiple Geofences
**Given** the user has multiple geofences
**When** events occur for different geofences
**Then** each should trigger its own notification
**And** events should be correctly associated with their geofence

## Tasks / Subtasks

- [ ] Task 1: Create GeofenceBroadcastReceiver (AC: E6.2.1)
  - [ ] Register receiver in AndroidManifest.xml
  - [ ] Extract GeofencingEvent from Intent
  - [ ] Parse geofence IDs and transition types
  - [ ] Handle errors from geofencing system
- [ ] Task 2: Create GeofenceEvent Entity (AC: E6.2.4)
  - [ ] Create GeofenceEvent data class
  - [ ] Create GeofenceEventEntity for Room
  - [ ] Create GeofenceEventDao
- [ ] Task 3: Implement Local Notification (AC: E6.2.2)
  - [ ] Create notification channel for geofence alerts
  - [ ] Build notification with geofence name and event type
  - [ ] Set high priority with sound
  - [ ] Add PendingIntent to open app
- [ ] Task 4: Send Event to Backend (AC: E6.2.3)
  - [ ] Create GeofenceEventDto
  - [ ] Add POST endpoint to GeofenceApiService
  - [ ] Send event with current location
- [ ] Task 5: Implement Event Logging (AC: E6.2.5)
  - [ ] Save GeofenceEventEntity to Room
  - [ ] Include all event details
  - [ ] Support viewing event history (optional)
- [ ] Task 6: Handle Multiple Events (AC: E6.2.6)
  - [ ] Process each geofence in event separately
  - [ ] Use unique notification IDs per geofence
- [ ] Task 7: Create GeofenceEventProcessor (AC: All)
  - [ ] Orchestrate notification, backend send, and logging
  - [ ] Handle errors gracefully
  - [ ] Use coroutines for async operations
- [ ] Task 8: Testing (All ACs)
  - [ ] Manual test geofence entry/exit
  - [ ] Verify notification appearance
  - [ ] Verify backend event receipt

## Dev Notes

### Architecture
- BroadcastReceiver triggers processing
- GeofenceEventProcessor coordinates notification, API, and logging
- Use goAsync() or start service for long-running work

### GeofenceBroadcastReceiver
```kotlin
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Timber.e("Geofencing error: ${geofencingEvent?.errorCode}")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        // Start service or use goAsync() for processing
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val processor = GeofenceEventProcessor(context)
                triggeringGeofences.forEach { geofence ->
                    processor.processEvent(
                        geofenceId = geofence.requestId,
                        transitionType = transitionType,
                        location = geofencingEvent.triggeringLocation
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
```

### Notification
```kotlin
fun showGeofenceNotification(geofenceName: String, eventType: TransitionType) {
    val title = when (eventType) {
        TransitionType.ENTER -> "Entered $geofenceName"
        TransitionType.EXIT -> "Left $geofenceName"
        TransitionType.DWELL -> "At $geofenceName"
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_ID_GEOFENCE)
        .setContentTitle(title)
        .setContentText("Geofence alert triggered")
        .setSmallIcon(R.drawable.ic_geofence)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(geofenceId.hashCode(), notification)
}
```

### Files to Create
- `receiver/GeofenceBroadcastReceiver.kt` (NEW)
- `geofence/GeofenceEventProcessor.kt` (NEW)
- `domain/model/GeofenceEvent.kt` (NEW)
- `data/model/GeofenceEventEntity.kt` (NEW)
- `data/database/dao/GeofenceEventDao.kt` (NEW)
- `notification/GeofenceNotificationManager.kt` (NEW)
- `network/models/GeofenceEventDto.kt` (NEW)

### AndroidManifest.xml
```xml
<receiver
    android:name=".receiver.GeofenceBroadcastReceiver"
    android:exported="false" />
```

### References
- [Source: PRD FR-5.2.1-5.2.3 - Geofence Events requirements]
- [Source: PRD Data Model: GeofenceEvent]
- [Source: PRD Section 6.2 - POST /api/geofence-events spec]
- [Source: epics.md - Story 6.2 description]

## Dev Agent Record

### Context Reference
<!-- Path(s) to story context XML/JSON will be added here by context workflow -->

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
**Status**: Draft
**Dependencies**: Story E6.1 (Geofence Definition - geofences must be registered)
