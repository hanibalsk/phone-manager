# Story E4.1: Location History UI

**Story ID**: E4.1
**Epic**: 4 - Location History
**Priority**: Must-Have
**Estimate**: 2 story points (1-2 days)
**Status**: ContextReadyDraft
**Created**: 2025-11-25
**PRD Reference**: Feature 4 (FR-4.1, FR-4.3)

---

## Story

As a user,
I want to view my location history on the map,
so that I can see where I've been.

## Acceptance Criteria

### AC E4.1.1: History Screen
**Given** I am in the app
**When** I navigate to History
**Then** I should see a History screen with a map view
**And** date/time filter options

### AC E4.1.2: Polyline Display
**Given** I have recorded location history
**When** I view the History screen
**Then** my path should be displayed as a polyline on the map
**And** the polyline should connect location points chronologically

### AC E4.1.3: Local Data Source
**Given** I want to view my own history
**When** I select a date range
**Then** the app should fetch data from local Room database
**And** display without requiring network connectivity

### AC E4.1.4: Date Filter - Presets
**Given** I am on the History screen
**When** I tap the date filter
**Then** I should see preset options:
  - Today
  - Yesterday
  - Last 7 days

### AC E4.1.5: Date Filter - Custom Range
**Given** I am on the History screen
**When** I select "Custom range"
**Then** I should be able to pick start and end dates
**And** the polyline should update to show only that range

### AC E4.1.6: Empty History State
**Given** no location data exists for selected range
**When** I view History
**Then** I should see a message: "No location history for this period"

## Tasks / Subtasks

- [ ] Task 1: Create HistoryScreen (AC: E4.1.1)
  - [ ] Create HistoryScreen composable with GoogleMap
  - [ ] Add date filter UI (chips/dropdown)
  - [ ] Add navigation entry point
- [ ] Task 2: Create HistoryViewModel (AC: E4.1.3)
  - [ ] Create HistoryUiState data class
  - [ ] Implement fetchHistoryFromLocal(startTime, endTime)
  - [ ] Query LocationDao for date range
- [ ] Task 3: Implement Polyline Rendering (AC: E4.1.2)
  - [ ] Convert LocationEntity list to LatLng list
  - [ ] Use Polyline composable on GoogleMap
  - [ ] Style polyline (color, width)
- [ ] Task 4: Implement Date Presets (AC: E4.1.4)
  - [ ] Add Today, Yesterday, Last 7 Days chips
  - [ ] Calculate date ranges for each preset
  - [ ] Update query on selection
- [ ] Task 5: Implement Custom Date Range (AC: E4.1.5)
  - [ ] Add date picker dialogs
  - [ ] Allow custom start/end selection
  - [ ] Validate range (end > start)
- [ ] Task 6: Handle Empty State (AC: E4.1.6)
  - [ ] Detect empty result set
  - [ ] Show empty state message
- [ ] Task 7: Testing (All ACs)
  - [ ] Manual test polyline display
  - [ ] Test date filters
  - [ ] Test offline functionality

## Dev Notes

### Architecture
- Use existing Room database and LocationDao
- Add date range query to LocationDao
- Follow MVVM pattern with HistoryViewModel

### LocationDao Query
```kotlin
@Dao
interface LocationDao {
    @Query("""
        SELECT * FROM locations
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        ORDER BY timestamp ASC
    """)
    suspend fun getLocationsBetween(startTime: Long, endTime: Long): List<LocationEntity>
}
```

### Implementation Details
```kotlin
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column {
        // Date filter chips
        DateFilterRow(
            selected = uiState.selectedFilter,
            onFilterSelected = viewModel::setDateFilter
        )

        // Map with polyline
        GoogleMap(...) {
            if (uiState.locations.isNotEmpty()) {
                Polyline(
                    points = uiState.locations.map { LatLng(it.latitude, it.longitude) },
                    color = Color.Blue,
                    width = 5f
                )
            }
        }

        // Empty state
        if (uiState.locations.isEmpty() && !uiState.isLoading) {
            EmptyHistoryMessage()
        }
    }
}
```

### Files to Create/Modify
- `ui/history/HistoryScreen.kt` (NEW)
- `ui/history/HistoryViewModel.kt` (NEW)
- `ui/history/components/DateFilterRow.kt` (NEW)
- `data/database/dao/LocationDao.kt` (MODIFY - add date range query)
- `ui/navigation/PhoneManagerNavHost.kt` (MODIFY)

### References
- [Source: PRD FR-4.1.1-4.1.3 - Local Storage requirements]
- [Source: PRD FR-4.3.1-4.3.3 - History Viewing requirements]
- [Source: epics.md - Story 4.1 description]

## Dev Agent Record

### Context Reference
- `docs/story-context-E4.1.xml` - Generated 2025-11-25

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
**Dependencies**: Story E3.1 (Google Maps Integration), Epic 0 (Room database infrastructure)
