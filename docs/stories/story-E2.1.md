# Story E2.1: Secret Mode Activation

**Story ID**: E2.1
**Epic**: 2 - Secret Mode
**Priority**: Must-Have
**Estimate**: 1 story point (half day)
**Status**: Draft
**Created**: 2025-11-25
**PRD Reference**: Feature 1 (FR-1.1, FR-1.3)

---

## Story

As a user,
I want to enable secret mode,
so that the app is minimally visible on my device.

## Acceptance Criteria

### AC E2.1.1: Secret Mode DataStore Setting
**Given** the app is installed
**When** secret mode state changes
**Then** `secret_mode_enabled: Boolean` SHALL be stored in DataStore

### AC E2.1.2: Hidden Activation Gesture - Long Press
**Given** I am on the main screen
**When** I long-press on the app logo for 3 seconds
**Then** secret mode should toggle (enable if disabled, disable if enabled)
**And** no visible confirmation should appear

### AC E2.1.3: Hidden Activation Gesture - Tap Sequence
**Given** I am on the main screen
**When** I tap the app version number 5 times quickly
**Then** secret mode should toggle
**And** no visible confirmation should appear

### AC E2.1.4: No UI Indication
**Given** secret mode is enabled
**When** I view the app UI
**Then** there SHALL be no visible indication that secret mode is enabled
**And** no "Secret mode ON" text or icons visible

### AC E2.1.5: Suppress Toast Messages
**Given** secret mode is enabled
**When** location tracking starts/stops
**Then** no "Location tracking is ON/OFF" toasts should appear

### AC E2.1.6: Disable Verbose Logging
**Given** secret mode is enabled
**When** the app runs
**Then** verbose Logcat logging SHALL be disabled
**And** no sensitive information (coordinates, device names) logged

## Tasks / Subtasks

- [ ] Task 1: Add Secret Mode to PreferencesRepository (AC: E2.1.1)
  - [ ] Add `secret_mode_enabled` Boolean to DataStore
  - [ ] Add getter/setter methods
  - [ ] Add Flow for observing state
- [ ] Task 2: Implement Hidden Activation Gestures (AC: E2.1.2, E2.1.3)
  - [ ] Add long-press gesture detector on app logo (3 seconds)
  - [ ] Add tap counter on version text (5 taps within 2 seconds)
  - [ ] Toggle secret mode on gesture detection
- [ ] Task 3: Suppress UI Feedback in Secret Mode (AC: E2.1.4, E2.1.5)
  - [ ] Conditionally hide any secret mode indicators
  - [ ] Suppress toast messages when secret mode enabled
  - [ ] Update HomeViewModel to check secret mode state
- [ ] Task 4: Control Logging Verbosity (AC: E2.1.6)
  - [ ] Create conditional Timber tree based on secret mode
  - [ ] Filter out sensitive data (coordinates, names) in secret mode
- [ ] Task 5: Testing (All ACs)
  - [ ] Unit test PreferencesRepository secret mode
  - [ ] Manual test gesture activation
  - [ ] Verify no visible indicators

## Dev Notes

### Architecture
- Secret mode is purely client-side (FR-1.4.1)
- Use existing PreferencesRepository pattern
- Timber tree switching for log control

### Implementation Details
```kotlin
// Long press detection
Modifier.pointerInput(Unit) {
    detectTapGestures(
        onLongPress = {
            if (System.currentTimeMillis() - pressStartTime >= 3000) {
                viewModel.toggleSecretMode()
            }
        }
    )
}

// Tap counter for version
var tapCount = 0
var lastTapTime = 0L
onClick = {
    val now = System.currentTimeMillis()
    if (now - lastTapTime < 500) tapCount++ else tapCount = 1
    lastTapTime = now
    if (tapCount >= 5) {
        viewModel.toggleSecretMode()
        tapCount = 0
    }
}
```

### Files to Create/Modify
- `data/preferences/PreferencesRepository.kt` (MODIFY - add secret_mode)
- `ui/home/HomeScreen.kt` (MODIFY - add gesture detectors)
- `ui/home/HomeViewModel.kt` (MODIFY - add toggleSecretMode)
- `PhoneManagerApp.kt` (MODIFY - conditional Timber tree)

### References
- [Source: PRD FR-1.1.1-1.1.3 - Secret Mode Setting requirements]
- [Source: PRD FR-1.3.1-1.3.4 - UI Behavior requirements]
- [Source: PRD FR-1.4.1 - Secret mode purely client-side]
- [Source: epics.md - Story 2.1 description]

## Dev Agent Record

### Context Reference
- `docs/story-context-2.1.xml` - Generated 2025-11-25

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
**Dependencies**: None (client-only feature)
