# Story E3.1: Google Maps Integration

**Story ID**: E3.1
**Epic**: 3 - Real-Time Map & Group Display
**Priority**: Must-Have (Critical)
**Estimate**: 2 story points (1-2 days)
**Status**: Draft
**Created**: 2025-11-25
**PRD Reference**: Feature 2 (FR-2.1)

---

## Story

As a user,
I want to see my current location on a map,
so that I can orient myself.

## Acceptance Criteria

### AC E3.1.1: Google Maps SDK Integration
**Given** the app is launched
**When** I navigate to the Map screen
**Then** a Google Map should be displayed using Maps Compose

### AC E3.1.2: Current Location Marker
**Given** the Map screen is open
**And** location permissions are granted
**When** the map loads
**Then** my current location should be displayed with a distinctive marker
**And** the marker should be visually prominent (blue dot or custom marker)

### AC E3.1.3: Map Centering
**Given** I open the Map screen
**When** the map finishes loading
**Then** the map should center on my current device location
**And** zoom level should show reasonable area (approximately 15-16 zoom)

### AC E3.1.4: Standard Map Interactions
**Given** the Map is displayed
**When** I interact with the map
**Then** I should be able to pan (drag)
**And** I should be able to zoom (pinch/buttons)
**And** I should be able to rotate (two-finger rotate)

### AC E3.1.5: Map Screen and ViewModel
**Given** proper architecture
**When** MapScreen is implemented
**Then** it should follow MVVM pattern with MapViewModel
**And** state should be managed via StateFlow

## Tasks / Subtasks

- [ ] Task 1: Add Google Maps Dependencies (AC: E3.1.1)
  - [ ] Add maps-compose dependency to build.gradle.kts
  - [ ] Add Google Play Services Maps dependency
  - [ ] Configure API key in AndroidManifest.xml
- [ ] Task 2: Create MapScreen Composable (AC: E3.1.1, E3.1.4)
  - [ ] Create MapScreen.kt with GoogleMap composable
  - [ ] Configure map UI settings (zoom controls, compass, etc.)
  - [ ] Enable standard gestures
- [ ] Task 3: Create MapViewModel (AC: E3.1.2, E3.1.3, E3.1.5)
  - [ ] Create MapUiState data class
  - [ ] Implement current location fetching
  - [ ] Manage camera position state
- [ ] Task 4: Display Current Location Marker (AC: E3.1.2, E3.1.3)
  - [ ] Add Marker for current device location
  - [ ] Create distinctive marker icon/color
  - [ ] Implement initial camera positioning
- [ ] Task 5: Update Navigation (AC: E3.1.1)
  - [ ] Add Map route to NavHost
  - [ ] Add bottom navigation or tab for Map
- [ ] Task 6: Testing (All ACs)
  - [ ] Manual test map display
  - [ ] Manual test location marker
  - [ ] Manual test map gestures

## Dev Notes

### Architecture
- Use `maps-compose` library for Jetpack Compose integration
- Follow existing MVVM pattern
- Observe location from LocationRepository or FusedLocationProvider

### Dependencies to Add
```kotlin
// build.gradle.kts
implementation("com.google.maps.android:maps-compose:4.3.0")
implementation("com.google.android.gms:play-services-maps:18.2.0")
```

### Implementation Details
```kotlin
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            uiState.currentLocation ?: LatLng(0.0, 0.0),
            15f
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = true),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            rotationGesturesEnabled = true
        )
    ) {
        uiState.currentLocation?.let { location ->
            Marker(
                state = MarkerState(position = location),
                title = "You",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
            )
        }
    }
}
```

### API Key Setup
- Add `MAPS_API_KEY` to local.properties
- Reference in AndroidManifest.xml via manifestPlaceholders

### Files to Create/Modify
- `ui/map/MapScreen.kt` (NEW)
- `ui/map/MapViewModel.kt` (NEW)
- `ui/navigation/PhoneManagerNavHost.kt` (MODIFY)
- `app/build.gradle.kts` (MODIFY - add dependencies)
- `AndroidManifest.xml` (MODIFY - add API key meta-data)

### References
- [Source: PRD FR-2.1.1-2.1.4 - Map Display requirements]
- [Source: PRD Section 1.3 - Maps: Google Maps SDK for Compose]
- [Source: epics.md - Story 3.1 description]

## Dev Agent Record

### Context Reference
- `docs/story-context-3.1.xml` - Generated 2025-11-25

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
**Dependencies**: Epic 0 Foundation (location infrastructure)
