# Story E8.12: Add Trip Detection settings section to Settings Screen

**Story ID**: E8.12
**Epic**: 8 - Movement Tracking & Intelligent Path Detection
**Priority**: Must-Have
**Estimate**: 2 story points (1 day)
**Status**: Planned
**Created**: 2025-11-30
**PRD Reference**: PRD-movement-tracking.md, ANDROID_APP_SPEC.md

---

## Story

As a user,
I want to configure trip detection parameters,
so that the system matches my travel patterns.

## Acceptance Criteria

### AC E8.12.1: Trip Detection Section Added
**Given** the Settings screen exists
**Then** add new "Trip Detection" section with:
  - Section header
  - Proper placement (after Movement Detection section)

### AC E8.12.2: Enable Trip Detection Toggle
**Given** user wants to control trip detection
**Then** provide:
  - Toggle switch "Enable Trip Detection"
  - Description: "Automatically detect and log trips"
  - Persist to preferences

### AC E8.12.3: Stationary Threshold Setting
**Given** user wants to customize trip end timing
**Then** provide:
  - "End Trip After Stationary" label
  - Segmented control with options: 1 min, 5 min, 10 min, 30 min
  - Persist to preferences

### AC E8.12.4: Minimum Trip Duration Setting
**Given** user wants to filter short trips
**Then** provide:
  - "Minimum Trip Duration" label
  - Stepper control (1-10 minutes)
  - Current value display
  - Persist to preferences

### AC E8.12.5: Minimum Trip Distance Setting
**Given** user wants to filter short distance trips
**Then** provide:
  - "Minimum Trip Distance" label
  - Stepper control (50-500 meters)
  - Current value display
  - Persist to preferences

### AC E8.12.6: Auto-Merge Toggle
**Given** user wants to control trip merging
**Then** provide:
  - Toggle switch "Auto-Merge Brief Stops"
  - Description: "Combine trips separated by short stops"
  - Persist to preferences

### AC E8.12.7: Navigation Links
**Given** user wants to access trip data
**Then** provide:
  - "View Trip History →" navigation link
  - "View Movement Events →" navigation link (developer section)

### AC E8.12.8: SettingsViewModel Updated
**Given** settings need state management
**Then** SettingsViewModel should include:
  - isTripDetectionEnabled StateFlow
  - tripStationaryThreshold StateFlow
  - tripMinimumDuration StateFlow
  - tripMinimumDistance StateFlow
  - isTripAutoMergeEnabled StateFlow
  - Setter functions for each preference

## Tasks / Subtasks

- [ ] Task 1: Update SettingsViewModel (AC: E8.12.8)
  - [ ] Inject PreferencesRepository (if not already)
  - [ ] Add isTripDetectionEnabled StateFlow
  - [ ] Add tripStationaryThreshold StateFlow
  - [ ] Add tripMinimumDuration StateFlow
  - [ ] Add tripMinimumDistance StateFlow
  - [ ] Add isTripAutoMergeEnabled StateFlow
  - [ ] Add setTripDetectionEnabled() function
  - [ ] Add setTripStationaryThreshold() function
  - [ ] Add setTripMinimumDuration() function
  - [ ] Add setTripMinimumDistance() function
  - [ ] Add setTripAutoMergeEnabled() function

- [ ] Task 2: Create Trip Detection Section Header (AC: E8.12.1)
  - [ ] Add section divider
  - [ ] Add "Trip Detection" header
  - [ ] Position after Movement Detection section

- [ ] Task 3: Create Enable Toggle (AC: E8.12.2)
  - [ ] Add SettingsSwitchRow for trip detection
  - [ ] Include title and description
  - [ ] Bind to ViewModel state
  - [ ] Connect to setter function

- [ ] Task 4: Create Stationary Threshold Control (AC: E8.12.3)
  - [ ] Create SegmentedButtonRow composable (or use existing)
  - [ ] Add options: 1, 5, 10, 30 minutes
  - [ ] Bind to ViewModel state
  - [ ] Connect to setter function

- [ ] Task 5: Create Minimum Duration Stepper (AC: E8.12.4)
  - [ ] Create SettingsStepperRow composable
  - [ ] Range 1-10 minutes
  - [ ] Show current value
  - [ ] Add +/- buttons
  - [ ] Bind to ViewModel state

- [ ] Task 6: Create Minimum Distance Stepper (AC: E8.12.5)
  - [ ] Create SettingsStepperRow composable
  - [ ] Range 50-500 meters (step 50)
  - [ ] Show current value with unit
  - [ ] Add +/- buttons
  - [ ] Bind to ViewModel state

- [ ] Task 7: Create Auto-Merge Toggle (AC: E8.12.6)
  - [ ] Add SettingsSwitchRow for auto-merge
  - [ ] Include title and description
  - [ ] Bind to ViewModel state
  - [ ] Connect to setter function

- [ ] Task 8: Add Navigation Links (AC: E8.12.7)
  - [ ] Add "View Trip History" navigation row
  - [ ] Add "View Movement Events" navigation row
  - [ ] Connect click handlers to navigation

- [ ] Task 9: Testing (All ACs)
  - [ ] Unit test ViewModel state management
  - [ ] UI test toggle interactions
  - [ ] UI test stepper interactions
  - [ ] UI test navigation links
  - [ ] Verify persistence to DataStore

## Dev Notes

### Settings Section Wireframe

```
┌─────────────────────────────────────────┐
│ Trip Detection                          │
├─────────────────────────────────────────┤
│ Enable Trip Detection              [ON] │
│ Automatically detect and log trips      │
├─────────────────────────────────────────┤
│ End Trip After Stationary              │
│ ○ 1 min │ ● 5 min │ ○ 10 min │ ○ 30 min│
├─────────────────────────────────────────┤
│ Minimum Trip Duration                   │
│ [2] minutes                        [-][+]│
├─────────────────────────────────────────┤
│ Minimum Trip Distance                   │
│ [100] meters                       [-][+]│
├─────────────────────────────────────────┤
│ Auto-Merge Brief Stops            [ON]  │
│ Combine trips separated by short stops  │
├─────────────────────────────────────────┤
│ [View Trip History →]                   │
│ [View Movement Events →]                │
└─────────────────────────────────────────┘
```

### SettingsViewModel Additions

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    // ... existing ...

    // Trip Detection Settings
    val isTripDetectionEnabled: StateFlow<Boolean> =
        preferencesRepository.isTripDetectionEnabled
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val tripStationaryThreshold: StateFlow<Int> =
        preferencesRepository.tripStationaryThresholdMinutes
            .stateIn(viewModelScope, SharingStarted.Eagerly, 5)

    val tripMinimumDuration: StateFlow<Int> =
        preferencesRepository.tripMinimumDurationMinutes
            .stateIn(viewModelScope, SharingStarted.Eagerly, 2)

    val tripMinimumDistance: StateFlow<Int> =
        preferencesRepository.tripMinimumDistanceMeters
            .stateIn(viewModelScope, SharingStarted.Eagerly, 100)

    val isTripAutoMergeEnabled: StateFlow<Boolean> =
        preferencesRepository.isTripAutoMergeEnabled
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setTripDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTripDetectionEnabled(enabled)
        }
    }

    // ... more setters ...
}
```

### Files to Modify

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsViewModel.kt`

### Dependencies

- Story E8.8 (Trip detection preferences in repository)
- Story E8.9 (Trip History navigation target)
- Story E8.11 (Movement Events navigation target)

### References

- [Source: ANDROID_APP_SPEC.md - Section 6.4: Settings Screen Enhancements]
- [Source: ANDROID_APP_SPEC.md - Section 7.4: SettingsViewModel Additions]
- [Source: Epic-E8-Movement-Tracking.md - Story E8.12]

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
**Dependencies**: E8.8, E8.9, E8.11
