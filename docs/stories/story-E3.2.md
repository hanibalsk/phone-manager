# Story E3.2: Group Members on Map

**Story ID**: E3.2
**Epic**: 3 - Real-Time Map & Group Display
**Priority**: Must-Have
**Estimate**: 2 story points (1-2 days)
**Status**: ContextReadyDraft
**Created**: 2025-11-25
**PRD Reference**: Feature 2 (FR-2.2)

---

## Story

As a user,
I want to see all group members' locations on the map,
so that I know where everyone is.

## Acceptance Criteria

### AC E3.2.1: Display Group Member Markers
**Given** I am on the Map screen
**And** I am registered in a group with other devices
**When** the map loads
**Then** markers should be displayed for all devices in my groupId
**And** each marker should be positioned at the device's last known location

### AC E3.2.2: Display Name Labels
**Given** group member markers are displayed
**When** I view a marker
**Then** the marker should display the device's displayName
**And** the name should be visible near or on the marker

### AC E3.2.3: Visual Distinction
**Given** markers are displayed for me and group members
**When** I view the map
**Then** my marker should be visually distinct from group member markers
**And** different colors or icons should differentiate owner vs. members

### AC E3.2.4: Marker Info Window
**Given** group member markers are displayed
**When** I tap on a group member's marker
**Then** an info window should appear showing:
  - Device display name
  - Last update time (e.g., "5 min ago")

### AC E3.2.5: Handle Missing Locations
**Given** a group member has no lastLocation (null)
**When** rendering markers
**Then** that member should be omitted from the map
**Or** shown in a separate "offline" list

## Tasks / Subtasks

- [ ] Task 1: Fetch Group Members with Locations (AC: E3.2.1)
  - [ ] Use DeviceRepository.getGroupMembers() from E1.2
  - [ ] Filter members with valid lastLocation
  - [ ] Store in MapUiState
- [ ] Task 2: Display Group Member Markers (AC: E3.2.1, E3.2.2)
  - [ ] Iterate through group members
  - [ ] Create Marker for each with valid location
  - [ ] Set title to displayName
- [ ] Task 3: Implement Visual Distinction (AC: E3.2.3)
  - [ ] Use different marker color for group members (e.g., red/orange)
  - [ ] Keep current device marker blue
  - [ ] Consider custom marker icons
- [ ] Task 4: Implement Info Window (AC: E3.2.4)
  - [ ] Create custom InfoWindowContent composable
  - [ ] Show displayName and relative time
  - [ ] Handle marker click to show info
- [ ] Task 5: Handle Edge Cases (AC: E3.2.5)
  - [ ] Filter out members with null lastLocation
  - [ ] Optionally show "offline members" section
- [ ] Task 6: Testing (All ACs)
  - [ ] Manual test with 2+ devices in same group
  - [ ] Test marker tapping and info windows
  - [ ] Test visual distinction

## Dev Notes

### Architecture
- Extend MapViewModel from E3.1
- Reuse DeviceRepository.getGroupMembers() from E1.2
- Add groupMembers to MapUiState

### Implementation Details
```kotlin
data class MapUiState(
    val currentLocation: LatLng? = null,
    val groupMembers: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GoogleMap(...) {
        // Current device marker (blue)
        uiState.currentLocation?.let { location ->
            Marker(
                state = MarkerState(position = location),
                title = "You",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
            )
        }

        // Group member markers (red/orange)
        uiState.groupMembers.forEach { member ->
            member.lastLocation?.let { location ->
                Marker(
                    state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                    title = member.displayName,
                    snippet = "Last seen: ${formatRelativeTime(location.timestamp)}",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
            }
        }
    }
}
```

### Files to Create/Modify
- `ui/map/MapViewModel.kt` (MODIFY - add group members)
- `ui/map/MapScreen.kt` (MODIFY - add group markers)
- `ui/map/components/MemberInfoWindow.kt` (NEW - optional custom)

### References
- [Source: PRD FR-2.2.1-2.2.4 - Group Member Display requirements]
- [Source: epics.md - Story 3.2 description]
- [Source: Story E1.2 - DeviceRepository.getGroupMembers()]

## Dev Agent Record

### Context Reference
- `docs/story-context-E3.2.xml` - Generated 2025-11-25

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
**Dependencies**: Story E3.1 (Google Maps Integration), Story E1.2 (Group Member Discovery)
