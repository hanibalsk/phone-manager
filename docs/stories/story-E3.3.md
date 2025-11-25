# Story E3.3: Real-Time Location Polling

**Story ID**: E3.3
**Epic**: 3 - Real-Time Map & Group Display
**Priority**: Must-Have
**Estimate**: 2 story points (1-2 days)
**Status**: ContextReadyDraft
**Created**: 2025-11-25
**PRD Reference**: Feature 2 (FR-2.3)

---

## Story

As a user,
I want location markers to update periodically,
so that I see near real-time positions.

## Acceptance Criteria

### AC E3.3.1: Periodic Polling
**Given** the Map screen is open
**When** the configured polling interval elapses (10-30 seconds)
**Then** the app should fetch updated locations for all group members
**And** this should happen automatically without manual refresh

### AC E3.3.2: Marker Updates
**Given** new location data is received
**When** the poll completes
**Then** markers on the map should update to reflect new positions
**And** animation should smooth the transition (optional)

### AC E3.3.3: Last Update Time Indicator
**Given** markers are displayed
**When** I view a group member's marker
**Then** I should see the last update time for that member
**And** time should update after each poll

### AC E3.3.4: Network Failure Handling
**Given** network is unavailable
**When** a poll cycle occurs
**Then** the system should fail gracefully without crashing
**And** retry on next interval
**And** optionally show a subtle connectivity indicator

### AC E3.3.5: Configurable Polling Interval
**Given** I am in Settings
**When** I configure the polling interval
**Then** I should be able to choose between 10, 15, 20, 30 seconds
**And** the Map screen should respect this setting

### AC E3.3.6: Stop Polling When Screen Hidden
**Given** the Map screen is open and polling
**When** I navigate away or minimize the app
**Then** polling should pause to save battery
**And** resume when I return to the Map screen

## Tasks / Subtasks

- [ ] Task 1: Implement Polling Timer (AC: E3.3.1, E3.3.6)
  - [ ] Create polling coroutine in MapViewModel
  - [ ] Use delay() or Timer with configurable interval
  - [ ] Start polling when screen visible (Lifecycle aware)
  - [ ] Stop polling when screen not visible
- [ ] Task 2: Fetch and Update Locations (AC: E3.3.2)
  - [ ] Call DeviceRepository.getGroupMembers() on each poll
  - [ ] Update MapUiState with new member locations
  - [ ] Trigger recomposition to move markers
- [ ] Task 3: Display Last Update Time (AC: E3.3.3)
  - [ ] Track lastPolledAt timestamp
  - [ ] Update marker snippets with relative time
  - [ ] Refresh display after each poll
- [ ] Task 4: Handle Network Errors (AC: E3.3.4)
  - [ ] Catch exceptions in polling coroutine
  - [ ] Log errors but don't crash
  - [ ] Continue polling on next interval
  - [ ] Optionally show "offline" indicator
- [ ] Task 5: Add Polling Interval Setting (AC: E3.3.5)
  - [ ] Add polling interval to PreferencesRepository
  - [ ] Add UI in Settings to configure (10, 15, 20, 30s)
  - [ ] MapViewModel reads interval from preferences
- [ ] Task 6: Testing (All ACs)
  - [ ] Manual test polling updates markers
  - [ ] Test network disconnection handling
  - [ ] Test lifecycle-aware polling

## Dev Notes

### Architecture
- Use lifecycle-aware coroutine scope
- Polling starts in onStart, stops in onStop
- Use repeatOnLifecycle for clean implementation

### Implementation Details
```kotlin
class MapViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchGroupMembers()
                delay(preferencesRepository.pollingIntervalMs.first())
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    private suspend fun fetchGroupMembers() {
        deviceRepository.getGroupMembers()
            .onSuccess { members ->
                _uiState.update { it.copy(groupMembers = members, lastPolledAt = Instant.now()) }
            }
            .onFailure { e ->
                Timber.e(e, "Failed to fetch group members")
                // Don't update state, keep showing last known positions
            }
    }
}

// In MapScreen
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose { viewModel.stopPolling() }
    }
    // ... rest of UI
}
```

### Files to Create/Modify
- `ui/map/MapViewModel.kt` (MODIFY - add polling logic)
- `ui/map/MapScreen.kt` (MODIFY - lifecycle management)
- `data/preferences/PreferencesRepository.kt` (MODIFY - add polling interval)
- `ui/settings/SettingsScreen.kt` (MODIFY - add polling interval selector)

### References
- [Source: PRD FR-2.3.1-2.3.4 - Real-Time Updates requirements]
- [Source: epics.md - Story 3.3 description]

## Dev Agent Record

### Context Reference
- `docs/story-context-E3.3.xml` - Generated 2025-11-25

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
**Dependencies**: Story E3.2 (Group Members on Map), Story E1.2 (Group Member Discovery)
