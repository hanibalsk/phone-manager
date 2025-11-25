# Story E2.2: Discreet Notification

**Story ID**: E2.2
**Epic**: 2 - Secret Mode
**Priority**: Must-Have
**Estimate**: 1 story point (half day)
**Status**: Draft
**Created**: 2025-11-25
**PRD Reference**: Feature 1 (FR-1.2)

---

## Story

As a user,
I want the foreground notification to be discreet,
so that others don't notice the tracking.

## Acceptance Criteria

### AC E2.2.1: Generic Notification Title
**Given** secret mode is enabled
**And** background tracking is active
**When** the foreground notification is displayed
**Then** the title should be generic: "Service running"

### AC E2.2.2: Neutral Notification Icon
**Given** secret mode is enabled
**When** the foreground notification is displayed
**Then** the icon should be neutral (no GPS/location symbol)
**And** should use a generic system-style icon

### AC E2.2.3: Low Importance Notification
**Given** secret mode is enabled
**When** the foreground notification is displayed
**Then** importance should be `IMPORTANCE_MIN` or `IMPORTANCE_LOW`
**And** the notification should not appear prominently

### AC E2.2.4: Silent Notification
**Given** secret mode is enabled
**When** the foreground notification is displayed
**Then** there should be no sound
**And** there should be no vibration

### AC E2.2.5: Normal Mode Notification
**Given** secret mode is disabled
**When** the foreground notification is displayed
**Then** the notification should use normal title, icon, and importance
**And** standard notification behavior applies

### AC E2.2.6: Dynamic Notification Switching
**Given** tracking is active
**When** secret mode is toggled
**Then** the notification should update to match the new mode
**Without** interrupting the tracking service

## Tasks / Subtasks

- [ ] Task 1: Create Notification Variants (AC: E2.2.1, E2.2.2, E2.2.3, E2.2.4)
  - [ ] Create secret mode notification channel (IMPORTANCE_MIN)
  - [ ] Create discreet notification builder
  - [ ] Add neutral icon to drawable resources
- [ ] Task 2: Implement Notification Switching (AC: E2.2.5, E2.2.6)
  - [ ] Observe secret mode state in LocationTrackingService
  - [ ] Update notification dynamically on mode change
  - [ ] Maintain foreground service while switching
- [ ] Task 3: Update LocationTrackingService (All ACs)
  - [ ] Inject PreferencesRepository for secret mode state
  - [ ] Create method to build notification based on mode
  - [ ] Subscribe to secret mode changes
- [ ] Task 4: Testing (All ACs)
  - [ ] Manual test notification in secret mode
  - [ ] Manual test notification in normal mode
  - [ ] Test dynamic switching while tracking

## Dev Notes

### Architecture
- Extend existing LocationTrackingService notification logic
- Create two notification channels (normal, discreet)
- Use NotificationManager.notify() to update in-place

### Implementation Details
```kotlin
// Notification channels
private fun createNotificationChannels() {
    val normalChannel = NotificationChannel(
        CHANNEL_ID_NORMAL,
        "Location Tracking",
        NotificationManager.IMPORTANCE_DEFAULT
    )
    val secretChannel = NotificationChannel(
        CHANNEL_ID_SECRET,
        "Background Service",
        NotificationManager.IMPORTANCE_MIN
    ).apply {
        setSound(null, null)
        enableVibration(false)
    }
    notificationManager.createNotificationChannels(listOf(normalChannel, secretChannel))
}

// Build notification based on mode
private fun buildNotification(isSecretMode: Boolean): Notification {
    return if (isSecretMode) {
        NotificationCompat.Builder(this, CHANNEL_ID_SECRET)
            .setContentTitle("Service running")
            .setSmallIcon(R.drawable.ic_service_neutral)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
    } else {
        NotificationCompat.Builder(this, CHANNEL_ID_NORMAL)
            .setContentTitle("Location tracking active")
            .setSmallIcon(R.drawable.ic_location)
            .build()
    }
}
```

### Files to Create/Modify
- `service/LocationTrackingService.kt` (MODIFY - notification variants)
- `res/drawable/ic_service_neutral.xml` (NEW - neutral icon)
- `di/ServiceModule.kt` (MODIFY - inject PreferencesRepository)

### References
- [Source: PRD FR-1.2.1-1.2.4 - Notification Behavior requirements]
- [Source: epics.md - Story 2.2 description]
- [Source: solution-architecture.md - LocationTrackingService]

## Dev Agent Record

### Context Reference
- `docs/story-context-2.2.xml` - Generated 2025-11-25

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
**Dependencies**: Story E2.1 (Secret Mode Activation) - secret_mode_enabled must exist
