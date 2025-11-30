# Story E8.10: Create Trip Detail Screen with route map and path toggle

**Story ID**: E8.10
**Epic**: 8 - Movement Tracking & Intelligent Path Detection
**Priority**: Must-Have
**Estimate**: 4 story points (2 days)
**Status**: Planned
**Created**: 2025-11-30
**PRD Reference**: PRD-movement-tracking.md, ANDROID_APP_SPEC.md

---

## Story

As a user,
I want to view detailed information about a trip,
so that I can see the route, statistics, and mode breakdown.

## Acceptance Criteria

### AC E8.10.1: Trip Detail Screen Created
**Given** the user navigates to Trip Detail
**Then** TripDetailScreen should be created with:
  - TripDetailViewModel for state management
  - Navigation route: Screen.TripDetail with tripId parameter
  - Material3 design following app patterns

### AC E8.10.2: Map View with Route
**Given** a trip has location data
**Then** the map should show:
  - Route polyline connecting all locations
  - Start marker (green circle)
  - End marker (red circle)
  - Map automatically fits bounds to route

### AC E8.10.3: Raw vs Corrected Path Toggle
**Given** the trip may have corrected path data
**Then** the user should be able to:
  - Toggle between raw GPS path (dotted line)
  - And corrected/snapped path (solid line)
  - See clear visual distinction between modes

### AC E8.10.4: Trip Information Display
**Given** trip details are shown
**Then** display:
  - Trip name (editable)
  - Date of trip
  - Total duration
  - Total distance
  - Start time and location name
  - End time and location name

### AC E8.10.5: Mode Breakdown Chart
**Given** trip has multiple transportation modes
**Then** show:
  - Horizontal bar chart showing mode percentages
  - Mode icon with percentage for each mode
  - Visual breakdown with mode colors

### AC E8.10.6: Trip Statistics
**Given** trip statistics need display
**Then** show:
  - Average speed (km/h)
  - Total location points
  - Movement events count
  - Path corrected status (Yes/No)

### AC E8.10.7: Edit Trip Name
**Given** user wants to rename a trip
**Then** provide:
  - Edit icon in header
  - Dialog to enter new name
  - Save and update display

### AC E8.10.8: Export to GPX
**Given** user wants to export trip data
**Then** provide:
  - Export button
  - Generate GPX file with route
  - Share intent to save/share file

### AC E8.10.9: Delete Trip
**Given** user wants to delete the trip
**Then** provide:
  - Delete button
  - Confirmation dialog
  - Navigate back after deletion

### AC E8.10.10: Location Point Interaction
**Given** the map shows route points
**When** user taps a point on map
**Then** show popup with:
  - Timestamp
  - Transportation mode at that point
  - Speed if available

## Tasks / Subtasks

- [ ] Task 1: Create TripDetailViewModel (AC: E8.10.1)
  - [ ] Create TripDetailViewModel with @HiltViewModel
  - [ ] Inject TripRepository, LocationRepository, MovementEventRepository
  - [ ] Get tripId from SavedStateHandle
  - [ ] Create trip StateFlow
  - [ ] Create locations StateFlow
  - [ ] Create movementEvents StateFlow
  - [ ] Create isLoading StateFlow

- [ ] Task 2: Add Map State to ViewModel (AC: E8.10.3)
  - [ ] Create showCorrectedPath StateFlow (default false)
  - [ ] Create selectedLocationIndex StateFlow
  - [ ] Implement togglePathView() function
  - [ ] Implement selectLocation(index: Int?) function

- [ ] Task 3: Add Trip Actions to ViewModel (AC: E8.10.7, E8.10.8, E8.10.9)
  - [ ] Implement updateTripName(name: String) function
  - [ ] Implement exportToGpx(): Flow<Result<File>> function
  - [ ] Implement deleteTrip(): Flow<Result<Unit>> function

- [ ] Task 4: Create TripDetailScreen Composable (AC: E8.10.1)
  - [ ] Create TripDetailScreen.kt file
  - [ ] Add Scaffold with TopAppBar
  - [ ] Add back navigation
  - [ ] Add edit name icon in header
  - [ ] Create scrollable content layout

- [ ] Task 5: Create TripMap Component (AC: E8.10.2, E8.10.3)
  - [ ] Create TripMap.kt composable
  - [ ] Integrate Google Maps Compose
  - [ ] Draw route polyline from locations
  - [ ] Add start marker (green)
  - [ ] Add end marker (red)
  - [ ] Implement fitBounds to route
  - [ ] Support raw vs corrected path toggle
  - [ ] Style raw path as dotted, corrected as solid

- [ ] Task 6: Create Path Toggle UI (AC: E8.10.3)
  - [ ] Add toggle button/switch on map
  - [ ] Label "Raw" and "Corrected"
  - [ ] Connect to ViewModel state
  - [ ] Disable if no corrected path available

- [ ] Task 7: Create Trip Info Section (AC: E8.10.4)
  - [ ] Display trip name with edit icon
  - [ ] Display formatted date
  - [ ] Display duration and distance
  - [ ] Display start/end times with location

- [ ] Task 8: Create ModeBreakdownChart Component (AC: E8.10.5)
  - [ ] Create ModeBreakdownChart.kt composable
  - [ ] Create horizontal stacked bar
  - [ ] Show mode icons and percentages
  - [ ] Use mode-specific colors

- [ ] Task 9: Create TripStatistics Component (AC: E8.10.6)
  - [ ] Create TripStatistics.kt composable
  - [ ] Display average speed
  - [ ] Display location count
  - [ ] Display movement events count
  - [ ] Display path corrected status

- [ ] Task 10: Implement Edit Name Dialog (AC: E8.10.7)
  - [ ] Create EditTripNameDialog composable
  - [ ] Add text input field
  - [ ] Add save/cancel buttons
  - [ ] Validate input (not empty)

- [ ] Task 11: Implement GPX Export (AC: E8.10.8)
  - [ ] Create GPX file generation logic
  - [ ] Include all location points with timestamps
  - [ ] Include trip metadata
  - [ ] Trigger share intent

- [ ] Task 12: Implement Delete with Confirmation (AC: E8.10.9)
  - [ ] Add delete button in action bar
  - [ ] Show confirmation AlertDialog
  - [ ] Call deleteTrip on confirmation
  - [ ] Navigate back on success

- [ ] Task 13: Implement Map Point Tap (AC: E8.10.10)
  - [ ] Add marker click handler
  - [ ] Show info window popup
  - [ ] Display timestamp and mode

- [ ] Task 14: Add Navigation Route
  - [ ] Add Screen.TripDetail route with {tripId}
  - [ ] Add navGraphBuilder entry
  - [ ] Parse tripId from route

- [ ] Task 15: Testing (All ACs)
  - [ ] Unit test ViewModel state management
  - [ ] Unit test GPX export generation
  - [ ] UI test map rendering
  - [ ] UI test edit name flow
  - [ ] UI test delete flow

## Dev Notes

### Screen Wireframe

```
┌─────────────────────────────────────────┐
│ ← Trip Details                     ✏️   │
├─────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐ │
│ │                                     │ │
│ │           [MAP VIEW]                │ │
│ │     Route polyline on map          │ │
│ │     Start (🟢) and End (🔴)         │ │
│ │     [Raw] [Corrected] toggle       │ │
│ │                                     │ │
│ └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│ 🚗 Home → Work                          │
│ November 30, 2025                       │
├─────────────────────────────────────────┤
│ ⏱️ Duration      │ 📏 Distance          │
│    45 min        │    12.5 km           │
├─────────────────────────────────────────┤
│ 🕐 Start         │ 🕐 End               │
│    8:15 AM       │    9:00 AM           │
│    📍 Home       │    📍 Work           │
├─────────────────────────────────────────┤
│ 📊 Mode Breakdown                       │
│ ┌─────────────────────────────────────┐ │
│ │ 🚗 Driving ████████████████░ 92%   │ │
│ │ 🚶 Walking ██░░░░░░░░░░░░░░░ 8%    │ │
│ └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│ 📈 Statistics                           │
│ • Average speed: 25 km/h                │
│ • Location points: 54                   │
│ • Movement events: 3                    │
│ • Path corrected: ✅ Yes                │
├─────────────────────────────────────────┤
│ [View Raw Data]  [Export GPX]  [Delete] │
└─────────────────────────────────────────┘
```

### GPX Format

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="PhoneManager">
  <metadata>
    <name>Home → Work</name>
    <time>2025-11-30T08:15:00Z</time>
  </metadata>
  <trk>
    <name>Trip</name>
    <trkseg>
      <trkpt lat="48.1234" lon="17.5678">
        <time>2025-11-30T08:15:00Z</time>
        <speed>0.5</speed>
      </trkpt>
      <!-- more points -->
    </trkseg>
  </trk>
</gpx>
```

### Files to Create

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/ui/tripdetail/TripDetailScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/tripdetail/TripDetailViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/tripdetail/components/TripMap.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/tripdetail/components/ModeBreakdownChart.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/tripdetail/components/TripStatistics.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/navigation/AppNavigation.kt`

### Dependencies

- Story E8.3 (TripRepository, LocationRepository)
- Story E8.9 (Navigation from Trip History)
- Google Maps Compose library

### References

- [Source: ANDROID_APP_SPEC.md - Section 6.2: Trip Detail Screen]
- [Source: ANDROID_APP_SPEC.md - Section 7.2: TripDetailViewModel]
- [Source: Epic-E8-Movement-Tracking.md - Story E8.10]

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
**Dependencies**: E8.3 (Repositories), E8.9 (Navigation)
