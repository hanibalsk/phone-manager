# Story E8.11: Create Movement Events Screen for developer debugging

**Story ID**: E8.11
**Epic**: 8 - Movement Tracking & Intelligent Path Detection
**Priority**: Should-Have
**Estimate**: 3 story points (1-2 days)
**Status**: Planned
**Created**: 2025-11-30
**PRD Reference**: PRD-movement-tracking.md, ANDROID_APP_SPEC.md

---

## Story

As a developer,
I want to view the movement events log,
so that I can verify detection accuracy and debug issues.

## Acceptance Criteria

### AC E8.11.1: Movement Events Screen Created
**Given** the developer navigates to Movement Events
**Then** MovementEventsScreen should be created with:
  - MovementEventsViewModel for state management
  - Navigation route: Screen.MovementEvents
  - Material3 design following app patterns
  - Access from Settings screen (developer section)

### AC E8.11.2: Live Mode Toggle
**Given** developer wants real-time updates
**Then** provide:
  - Toggle for live mode
  - Auto-refresh when new events arrive
  - Visual indicator when live mode is active

### AC E8.11.3: Event List Display
**Given** movement events are displayed
**Then** each event should show:
  - Timestamp formatted
  - Mode transition (e.g., "STATIONARY → IN_VEHICLE")
  - Detection source
  - Confidence percentage

### AC E8.11.4: Expandable Event Details
**Given** user wants more detail
**When** an event is tapped/expanded
**Then** show:
  - Full location (lat, lon, accuracy)
  - Device state (battery, network)
  - Complete sensor telemetry
  - Detection latency

### AC E8.11.5: Export Functionality
**Given** developer wants to analyze events externally
**Then** provide:
  - Export button in header
  - Export format options (JSON, CSV)
  - Share intent for file

### AC E8.11.6: Clear Old Events
**Given** developer wants to clean up data
**Then** provide:
  - Clear events action (>7 days old)
  - Confirmation dialog
  - Success message

### AC E8.11.7: Filter by Mode Transition
**Given** developer wants to find specific events
**Then** provide:
  - Filter by mode transition
  - Multiple filter options
  - Clear filter action

### AC E8.11.8: Pagination
**Given** there are many events
**Then** implement:
  - Load 50 events initially
  - "Load More" button
  - Loading indicator

### AC E8.11.9: Statistics Display
**Given** developer wants overview
**Then** show in header:
  - Total event count
  - Unsynced event count

## Tasks / Subtasks

- [ ] Task 1: Create MovementEventsViewModel (AC: E8.11.1)
  - [ ] Create MovementEventsViewModel with @HiltViewModel
  - [ ] Inject MovementEventRepository
  - [ ] Create events StateFlow
  - [ ] Create isLoading StateFlow
  - [ ] Create isLiveMode StateFlow

- [ ] Task 2: Add Statistics to ViewModel (AC: E8.11.9)
  - [ ] Create totalEventCount StateFlow
  - [ ] Create unsyncedCount StateFlow
  - [ ] Observe from repository

- [ ] Task 3: Implement Live Mode (AC: E8.11.2)
  - [ ] Create toggleLiveMode() function
  - [ ] Collect from observeRecentEvents flow
  - [ ] Update events list on new events

- [ ] Task 4: Implement Actions (AC: E8.11.5, E8.11.6)
  - [ ] Implement exportEvents(format: ExportFormat): Flow<Result<File>>
  - [ ] Implement clearOldEvents(beforeDays: Int)
  - [ ] Create ExportFormat enum (JSON, CSV)

- [ ] Task 5: Implement Pagination (AC: E8.11.8)
  - [ ] Implement loadMoreEvents() function
  - [ ] Track current page
  - [ ] Handle loading state

- [ ] Task 6: Create MovementEventsScreen (AC: E8.11.1)
  - [ ] Create MovementEventsScreen.kt file
  - [ ] Add Scaffold with TopAppBar
  - [ ] Add export icon in header
  - [ ] Add statistics in subtitle

- [ ] Task 7: Create Event List UI (AC: E8.11.3)
  - [ ] Create LazyColumn for events
  - [ ] Show timestamp
  - [ ] Show mode transition
  - [ ] Show source and confidence

- [ ] Task 8: Create MovementEventCard Component (AC: E8.11.3, E8.11.4)
  - [ ] Create MovementEventCard.kt composable
  - [ ] Implement expandable state
  - [ ] Show summary when collapsed
  - [ ] Show full details when expanded
  - [ ] Include all telemetry fields

- [ ] Task 9: Implement Live Mode Toggle UI (AC: E8.11.2)
  - [ ] Add toggle in header
  - [ ] Show "Live" indicator when active
  - [ ] Connect to ViewModel

- [ ] Task 10: Implement Export UI (AC: E8.11.5)
  - [ ] Add export button
  - [ ] Show format selection dialog
  - [ ] Generate file and share

- [ ] Task 11: Implement Filter UI (AC: E8.11.7)
  - [ ] Add filter chips
  - [ ] Filter by mode transition
  - [ ] Add clear filter action

- [ ] Task 12: Implement Clear Events Dialog (AC: E8.11.6)
  - [ ] Add clear action in menu
  - [ ] Show confirmation dialog
  - [ ] Clear and show result

- [ ] Task 13: Add Navigation Route
  - [ ] Add Screen.MovementEvents route
  - [ ] Add navGraphBuilder entry
  - [ ] Add link from Settings screen

- [ ] Task 14: Testing (All ACs)
  - [ ] Unit test ViewModel live mode
  - [ ] Unit test export generation
  - [ ] Unit test pagination
  - [ ] UI test event card expansion

## Dev Notes

### Screen Wireframe

```
┌─────────────────────────────────────────┐
│ ← Movement Events                  📤   │
├─────────────────────────────────────────┤
│ Live Mode: 🟢 Active               [OFF]│
├─────────────────────────────────────────┤
│ [10:32:15] STATIONARY → IN_VEHICLE     │
│   Source: BLUETOOTH_CAR (95%)           │
│   📍 48.1234, 17.5678 (±10m)           │
│   🔋 75% │ 📶 WiFi (-65 dBm)           │
│   📊 Accel: 9.81 m/s² (var: 0.15)      │
│   🦶 Steps: 1234                        │
│   ⏱️ Latency: 250ms                     │
├─────────────────────────────────────────┤
│ [10:30:00] WALKING → STATIONARY        │
│   Source: ACTIVITY_RECOGNITION (88%)    │
│   📍 48.1230, 17.5670 (±15m)           │
│   🔋 76% │ 📶 Mobile (-85 dBm)         │
│   📊 Accel: 9.78 m/s² (var: 0.45)      │
├─────────────────────────────────────────┤
│           ▼ Load More                   │
└─────────────────────────────────────────┘
```

### Export Formats

**JSON Format:**
```json
{
  "exportedAt": "2025-11-30T10:32:15Z",
  "eventCount": 150,
  "events": [
    {
      "id": 1,
      "timestamp": "2025-11-30T10:32:15Z",
      "previousMode": "STATIONARY",
      "newMode": "IN_VEHICLE",
      "source": "BLUETOOTH_CAR",
      "confidence": 0.95,
      "location": { "latitude": 48.1234, "longitude": 17.5678 },
      "telemetry": { ... }
    }
  ]
}
```

**CSV Format:**
```csv
id,timestamp,previousMode,newMode,source,confidence,latitude,longitude
1,2025-11-30T10:32:15Z,STATIONARY,IN_VEHICLE,BLUETOOTH_CAR,0.95,48.1234,17.5678
```

### Files to Create

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/ui/movementevents/MovementEventsScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/movementevents/MovementEventsViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/movementevents/components/MovementEventCard.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/navigation/AppNavigation.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsScreen.kt`

### Dependencies

- Story E8.3 (MovementEventRepository)

### References

- [Source: ANDROID_APP_SPEC.md - Section 6.3: Movement Events Screen]
- [Source: ANDROID_APP_SPEC.md - Section 7.3: MovementEventsViewModel]
- [Source: Epic-E8-Movement-Tracking.md - Story E8.11]

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
**Dependencies**: E8.3 (Repository)
