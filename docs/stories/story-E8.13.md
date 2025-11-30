# Story E8.13: Add active trip card and daily summary to Home Screen

**Story ID**: E8.13
**Epic**: 8 - Movement Tracking & Intelligent Path Detection
**Priority**: Should-Have
**Estimate**: 2 story points (1 day)
**Status**: Planned
**Created**: 2025-11-30
**PRD Reference**: PRD-movement-tracking.md, ANDROID_APP_SPEC.md

---

## Story

As a user,
I want to see my current trip status and daily summary on the home screen,
so that I have quick access to trip information.

## Acceptance Criteria

### AC E8.13.1: Active Trip Card
**Given** a trip is currently active
**Then** the home screen should show a card with:
  - Mode icon based on current transportation mode
  - "Active Trip" title
  - Start time
  - Current duration (updating)
  - Current distance
  - Location count
  - Progress indicator
  - "End Trip" button

### AC E8.13.2: Daily Summary Card
**Given** no trip is currently active
**Then** the home screen should show a card with:
  - "Today's Activity" title
  - Trip count for today
  - Total moving time
  - Total distance
  - "View History" navigation link

### AC E8.13.3: Card Tap Navigation
**Given** either card is displayed
**When** user taps the card
**Then** navigate to Trip History screen

### AC E8.13.4: HomeViewModel Updates
**Given** the home screen needs trip data
**Then** HomeViewModel should:
  - Observe activeTrip from TripManager
  - Observe daily statistics from TripRepository
  - Provide formatted duration and distance

### AC E8.13.5: End Trip Action
**Given** active trip card has "End Trip" button
**When** user taps the button
**Then**:
  - Call TripManager.forceEndTrip()
  - Show confirmation snackbar
  - Update card to daily summary

### AC E8.13.6: Real-time Updates
**Given** an active trip is in progress
**Then** the card should:
  - Update duration every second
  - Update distance when new locations arrive
  - Update location count

## Tasks / Subtasks

- [ ] Task 1: Update HomeViewModel for Trip Data (AC: E8.13.4)
  - [ ] Inject TripManager
  - [ ] Inject TripRepository
  - [ ] Create activeTrip StateFlow from TripManager
  - [ ] Create todayStats StateFlow from repository
  - [ ] Create formatted duration (updating timer)
  - [ ] Create formatted distance

- [ ] Task 2: Implement End Trip Action (AC: E8.13.5)
  - [ ] Add endActiveTrip() function to ViewModel
  - [ ] Call tripManager.forceEndTrip()
  - [ ] Handle result and emit event

- [ ] Task 3: Create Active Trip Card Component (AC: E8.13.1)
  - [ ] Create ActiveTripCard.kt composable
  - [ ] Display mode icon
  - [ ] Display "Active Trip" title
  - [ ] Display start time formatted
  - [ ] Display updating duration
  - [ ] Display current distance
  - [ ] Display location count
  - [ ] Add progress indicator/animation
  - [ ] Add "End Trip" button

- [ ] Task 4: Create Daily Summary Card Component (AC: E8.13.2)
  - [ ] Create DailySummaryCard.kt composable
  - [ ] Display "Today's Activity" title
  - [ ] Display trip count
  - [ ] Display total moving time
  - [ ] Display total distance
  - [ ] Add "View History" link

- [ ] Task 5: Implement Duration Timer (AC: E8.13.6)
  - [ ] Create updateable duration calculation
  - [ ] Format as "X min" or "X hr Y min"
  - [ ] Update every second using LaunchedEffect

- [ ] Task 6: Add Card to Home Screen (AC: E8.13.1, E8.13.2)
  - [ ] Add conditional display in HomeScreen
  - [ ] Show ActiveTripCard when trip active
  - [ ] Show DailySummaryCard when no active trip
  - [ ] Position appropriately in layout

- [ ] Task 7: Implement Card Tap Navigation (AC: E8.13.3)
  - [ ] Add onClick handler to both cards
  - [ ] Navigate to Screen.TripHistory
  - [ ] Pass navigation controller

- [ ] Task 8: Implement End Trip Button (AC: E8.13.5)
  - [ ] Add button to ActiveTripCard
  - [ ] Connect to ViewModel action
  - [ ] Show confirmation snackbar

- [ ] Task 9: Testing (All ACs)
  - [ ] Unit test ViewModel trip observation
  - [ ] Unit test duration formatting
  - [ ] UI test active trip card display
  - [ ] UI test daily summary card display
  - [ ] UI test end trip action

## Dev Notes

### Active Trip Card Wireframe

```
┌─────────────────────────────────────────┐
│ 🚗 Active Trip                          │
│ Started 8:15 AM • 23 min                │
│ 8.2 km • 15 locations                   │
│ ════════════════════════════════        │  ← Progress indicator
│            [End Trip]                   │
└─────────────────────────────────────────┘
```

### Daily Summary Card Wireframe

```
┌─────────────────────────────────────────┐
│ 📊 Today's Activity                     │
│ 3 trips • 2.5 hrs moving                │
│ 45.2 km total distance                  │
│            [View History →]             │
└─────────────────────────────────────────┘
```

### HomeViewModel Additions

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    // ... existing ...
    private val tripManager: TripManager,
    private val tripRepository: TripRepository,
) : ViewModel() {
    // ... existing ...

    val activeTrip: StateFlow<Trip?> = tripManager.activeTrip
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val todayStats: StateFlow<TodayTripStats> = tripRepository
        .observeTodayStats()
        .stateIn(viewModelScope, SharingStarted.Lazily, TodayTripStats.EMPTY)

    fun endActiveTrip() {
        viewModelScope.launch {
            tripManager.forceEndTrip()
        }
    }
}

data class TodayTripStats(
    val tripCount: Int,
    val totalMovingTimeSeconds: Long,
    val totalDistanceMeters: Double,
) {
    companion object {
        val EMPTY = TodayTripStats(0, 0, 0.0)
    }
}
```

### Duration Formatting

```kotlin
fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}min"
        else -> "${minutes} min"
    }
}
```

### Files to Create/Modify

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/ui/home/components/ActiveTripCard.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/home/components/DailySummaryCard.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/ui/home/HomeScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/home/HomeViewModel.kt`

### Dependencies

- Story E8.3 (TripRepository)
- Story E8.4 (TripManager)
- Story E8.9 (Trip History navigation target)

### References

- [Source: ANDROID_APP_SPEC.md - Section 6.5: HomeScreen Enhancement]
- [Source: Epic-E8-Movement-Tracking.md - Story E8.13]

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
**Dependencies**: E8.3, E8.4, E8.9
