# Story E8.9: Create Trip History Screen with day grouping, filtering, and pagination

**Story ID**: E8.9
**Epic**: 8 - Movement Tracking & Intelligent Path Detection
**Priority**: Must-Have
**Estimate**: 4 story points (2 days)
**Status**: Planned
**Created**: 2025-11-30
**PRD Reference**: PRD-movement-tracking.md, ANDROID_APP_SPEC.md

---

## Story

As a user,
I want to view my trip history,
so that I can review my past travels.

## Acceptance Criteria

### AC E8.9.1: Trip History Screen Created
**Given** the user navigates to Trip History
**Then** TripHistoryScreen should be created with:
  - TripHistoryViewModel for state management
  - Navigation integration (Screen.TripHistory route)
  - Material3 design following app patterns

### AC E8.9.2: Trip Cards with Day Grouping
**Given** trips are displayed
**Then** they should be:
  - Grouped by day (Today, Yesterday, specific dates)
  - Sorted by start time (newest first)
  - Displayed in expandable/collapsible day sections

### AC E8.9.3: Trip Card Information
**Given** a trip card is displayed
**Then** it should show:
  - Mode icon (🚗 🚶 🏃 🚲) based on dominant mode
  - Trip name (auto: "Start → End" or user-named)
  - Duration formatted (e.g., "45 min")
  - Distance formatted (e.g., "12.5 km")
  - Start and end times
  - Mini route preview polyline (optional)

### AC E8.9.4: Date Range Filter
**Given** user wants to filter by date
**Then** the screen should provide:
  - Date range picker (calendar UI)
  - Quick filters (Today, This Week, This Month)
  - Clear filter option

### AC E8.9.5: Transportation Mode Filter
**Given** user wants to filter by mode
**Then** the screen should provide:
  - Filter chips for each mode
  - Multi-select capability
  - Visual indicator of active filters

### AC E8.9.6: Pagination
**Given** there are many trips
**Then** the screen should:
  - Load 20 trips initially
  - Show "Load More" button at bottom
  - Implement infinite scroll (optional)
  - Show loading indicator during fetch

### AC E8.9.7: Pull to Refresh
**Given** user wants fresh data
**Then** pull-to-refresh should:
  - Trigger data reload
  - Show refresh indicator
  - Reset pagination to first page

### AC E8.9.8: Swipe to Delete
**Given** user wants to delete a trip
**Then** swipe gesture should:
  - Reveal delete action
  - Show confirmation dialog
  - Remove trip on confirmation
  - Show undo snackbar

### AC E8.9.9: Navigation to Detail
**Given** user taps a trip card
**Then** it should navigate to Trip Detail Screen with tripId

### AC E8.9.10: Empty State
**Given** there are no trips
**Then** show empty state with:
  - Appropriate illustration/icon
  - Message "No trips recorded yet"
  - Suggestion to start tracking

## Tasks / Subtasks

- [ ] Task 1: Create TripHistoryViewModel (AC: E8.9.1, E8.9.6)
  - [ ] Create TripHistoryViewModel class with @HiltViewModel
  - [ ] Inject TripRepository
  - [ ] Create trips StateFlow for trip list
  - [ ] Create isLoading StateFlow
  - [ ] Create error StateFlow
  - [ ] Implement pagination with currentPage and pageSize
  - [ ] Implement loadTrips() function
  - [ ] Implement loadMoreTrips() function
  - [ ] Implement refreshTrips() function

- [ ] Task 2: Add Filter State to ViewModel (AC: E8.9.4, E8.9.5)
  - [ ] Create selectedDateRange StateFlow
  - [ ] Create selectedModeFilter StateFlow
  - [ ] Implement setDateRangeFilter() function
  - [ ] Implement setModeFilter() function
  - [ ] Implement clearFilters() function
  - [ ] Trigger reload when filters change

- [ ] Task 3: Add Delete Functionality (AC: E8.9.8)
  - [ ] Implement deleteTrip(tripId: String) function
  - [ ] Handle repository call
  - [ ] Emit success/error state

- [ ] Task 4: Create TripHistoryScreen Composable (AC: E8.9.1, E8.9.2)
  - [ ] Create TripHistoryScreen.kt file
  - [ ] Add Scaffold with TopAppBar
  - [ ] Add back navigation
  - [ ] Add filter icons in top bar
  - [ ] Implement LazyColumn for trip list

- [ ] Task 5: Implement Day Grouping (AC: E8.9.2)
  - [ ] Group trips by date
  - [ ] Create day header composable
  - [ ] Show "Today", "Yesterday", or formatted date
  - [ ] Use sticky headers (optional)

- [ ] Task 6: Create TripCard Component (AC: E8.9.3)
  - [ ] Create TripCard.kt composable
  - [ ] Display mode icon based on dominantMode
  - [ ] Format duration (minutes, hours)
  - [ ] Format distance (km with 1 decimal)
  - [ ] Format start/end times
  - [ ] Add click handler for navigation

- [ ] Task 7: Implement Filter Bar (AC: E8.9.4, E8.9.5)
  - [ ] Create TripFilterBar.kt composable
  - [ ] Add date range button with calendar picker
  - [ ] Add mode filter chips
  - [ ] Show clear filters option when active

- [ ] Task 8: Create DateRangePicker (AC: E8.9.4)
  - [ ] Create DateRangePicker.kt composable
  - [ ] Use Material3 DateRangePicker
  - [ ] Add quick filter options
  - [ ] Handle date selection callback

- [ ] Task 9: Implement Pagination UI (AC: E8.9.6)
  - [ ] Add "Load More" button at list bottom
  - [ ] Show loading indicator during fetch
  - [ ] Disable button when loading
  - [ ] Hide when no more data

- [ ] Task 10: Implement Pull to Refresh (AC: E8.9.7)
  - [ ] Add SwipeRefresh wrapper
  - [ ] Connect to refreshTrips()
  - [ ] Show refresh indicator

- [ ] Task 11: Implement Swipe to Delete (AC: E8.9.8)
  - [ ] Add SwipeToDismiss to trip cards
  - [ ] Show delete background
  - [ ] Show confirmation AlertDialog
  - [ ] Show undo Snackbar after delete

- [ ] Task 12: Create Empty State (AC: E8.9.10)
  - [ ] Create EmptyTripHistory composable
  - [ ] Add icon/illustration
  - [ ] Add descriptive message

- [ ] Task 13: Add Navigation Route (AC: E8.9.9)
  - [ ] Add Screen.TripHistory to navigation
  - [ ] Add navGraphBuilder entry
  - [ ] Handle navigation from card tap

- [ ] Task 14: Testing (All ACs)
  - [ ] Unit test ViewModel pagination
  - [ ] Unit test filter logic
  - [ ] Unit test day grouping
  - [ ] UI test trip card display
  - [ ] UI test filter interactions

## Dev Notes

### Screen Wireframe

```
┌─────────────────────────────────────────┐
│ ← Trip History                    🔍 📅 │
├─────────────────────────────────────────┤
│ [Today]                                 │
│ ┌─────────────────────────────────────┐ │
│ │ 🚗 Home → Work           45 min    │ │
│ │    8:15 AM - 9:00 AM    12.5 km    │ │
│ └─────────────────────────────────────┘ │
│ ┌─────────────────────────────────────┐ │
│ │ 🚶 Lunch Walk             15 min   │ │
│ │    12:30 PM - 12:45 PM   0.8 km    │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ [Yesterday]                             │
│ ┌─────────────────────────────────────┐ │
│ │ 🚗 Work → Home           52 min    │ │
│ │    5:30 PM - 6:22 PM    13.1 km    │ │
│ └─────────────────────────────────────┘ │
│                                         │
│           ▼ Load More                   │
└─────────────────────────────────────────┘
```

### Mode Icons

| Mode | Icon |
|------|------|
| WALKING | 🚶 |
| RUNNING | 🏃 |
| CYCLING | 🚲 |
| IN_VEHICLE | 🚗 |

### Files to Create

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/ui/triphistory/TripHistoryScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/triphistory/TripHistoryViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/triphistory/components/TripCard.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/triphistory/components/TripFilterBar.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/triphistory/components/DateRangePicker.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/navigation/AppNavigation.kt`

### Dependencies

- Story E8.3 (TripRepository)
- Story E8.4 (Trip domain model)

### References

- [Source: ANDROID_APP_SPEC.md - Section 6.1: Trip History Screen]
- [Source: ANDROID_APP_SPEC.md - Section 7.1: TripHistoryViewModel]
- [Source: Epic-E8-Movement-Tracking.md - Story E8.9]

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
