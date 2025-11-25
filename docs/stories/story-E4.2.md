# Story E4.2: History Performance & Server Sync

**Story ID**: E4.2
**Epic**: 4 - Location History
**Priority**: Should-Have
**Estimate**: 2 story points (1-2 days)
**Status**: ContextReadyDraft
**Created**: 2025-11-25
**PRD Reference**: Feature 4 (FR-4.2, FR-4.4)

---

## Story

As a user,
I want to view other group members' history,
so that I can see where they've been.

## Acceptance Criteria

### AC E4.2.1: Fetch History from Server
**Given** I want to view another group member's history
**When** I select that member
**Then** the app should call `GET /api/devices/{deviceId}/locations`
**And** display their path as a polyline

### AC E4.2.2: Downsampling for Performance
**Given** a date range returns many location points (1000+)
**When** rendering the polyline
**Then** points should be downsampled to 200-500 visible points
**And** path accuracy should be preserved at current zoom level

### AC E4.2.3: Device Selector
**Given** I am on the History screen
**When** I want to view another member's history
**Then** I should see a device selector (dropdown or list)
**And** I can switch between my history and group members' history

### AC E4.2.4: Sync Tracking Extension
**Given** location records are uploaded to server
**When** sync completes
**Then** LocationEntity should be marked with isSynced=true and syncedAt timestamp
**And** duplicate uploads should be avoided

### AC E4.2.5: Server-Side Downsampling (Optional)
**Given** fetching large history from server
**When** the request includes `simplify=true` parameter
**Then** the server should return pre-downsampled data

## Tasks / Subtasks

- [ ] Task 1: Add Device Selector to History (AC: E4.2.3)
  - [ ] Add dropdown/chips for device selection
  - [ ] Include "My Device" and group member options
  - [ ] Update HistoryViewModel with selected device
- [ ] Task 2: Implement Server History Fetch (AC: E4.2.1)
  - [ ] Add getDeviceHistory() to DeviceApiService
  - [ ] Parse server response into LocationRecord list
  - [ ] Add to DeviceRepository
- [ ] Task 3: Implement Client-Side Downsampling (AC: E4.2.2)
  - [ ] Implement Douglas-Peucker or similar algorithm
  - [ ] Target 200-500 points output
  - [ ] Apply before rendering polyline
- [ ] Task 4: Extend LocationEntity for Sync Tracking (AC: E4.2.4)
  - [ ] Add isSynced: Boolean and syncedAt: Long? fields
  - [ ] Create Room migration
  - [ ] Update batch upload to mark synced
- [ ] Task 5: Add simplify Parameter Support (AC: E4.2.5)
  - [ ] Add simplify query param to API call
  - [ ] Handle server-downsampled response
- [ ] Task 6: Testing (All ACs)
  - [ ] Test viewing other member's history
  - [ ] Test downsampling with large datasets
  - [ ] Test sync tracking fields

## Dev Notes

### Architecture
- Extend DeviceApiService with location history endpoint
- Add downsampling utility function
- Extend LocationEntity with sync tracking

### API Call
```kotlin
interface DeviceApiService {
    suspend fun getDeviceHistory(
        deviceId: String,
        from: Instant,
        to: Instant,
        simplify: Boolean = false
    ): Result<List<LocationRecord>>
}
```

### Downsampling Algorithm
```kotlin
// Douglas-Peucker Algorithm (simplified)
fun downsample(points: List<LatLng>, targetCount: Int): List<LatLng> {
    if (points.size <= targetCount) return points

    // Implement Douglas-Peucker or Ramer-Douglas-Peucker
    // Or use simple distance-based filtering
    val step = points.size / targetCount
    return points.filterIndexed { index, _ -> index % step == 0 }
}
```

### LocationEntity Extension
```kotlin
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val speed: Float? = null,
    val provider: String? = null,
    // New fields for sync tracking
    val isSynced: Boolean = false,
    val syncedAt: Long? = null
)
```

### Files to Create/Modify
- `ui/history/HistoryScreen.kt` (MODIFY - add device selector)
- `ui/history/HistoryViewModel.kt` (MODIFY - add server fetch)
- `network/DeviceApiService.kt` (MODIFY - add getDeviceHistory)
- `data/model/LocationEntity.kt` (MODIFY - add sync fields)
- `data/database/AppDatabase.kt` (MODIFY - migration)
- `util/DownsamplingUtils.kt` (NEW)

### References
- [Source: PRD FR-4.2.1-4.2.3 - Server Synchronization requirements]
- [Source: PRD FR-4.4.1-4.4.3 - Performance Optimization requirements]
- [Source: PRD Section 6.3 - GET /api/devices/{deviceId}/locations spec]
- [Source: epics.md - Story 4.2 description]

## Dev Agent Record

### Context Reference
- `docs/story-context-E4.2.xml` - Generated 2025-11-25

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
**Dependencies**: Story E4.1 (Location History UI), Story E1.2 (Group Member Discovery)
