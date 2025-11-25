# Story E5.1: Proximity Alert Definition

**Story ID**: E5.1
**Epic**: 5 - Proximity Alerts
**Priority**: Must-Have
**Estimate**: 2 story points (1-2 days)
**Status**: ContextReadyDraft
**Created**: 2025-11-25
**PRD Reference**: Feature 3 (FR-3.1, FR-3.4)

---

## Story

As a user,
I want to create a proximity alert for another user,
so that I'm notified when they're nearby.

## Acceptance Criteria

### AC E5.1.1: ProximityAlert Entity
**Given** the data model is defined
**Then** ProximityAlert entity should include:
  - id (UUID)
  - ownerDeviceId (my device)
  - targetDeviceId (watched device)
  - radiusMeters (50-10,000)
  - direction (ENTER, EXIT, BOTH)
  - active (Boolean)
  - lastState (INSIDE, OUTSIDE)
  - createdAt, updatedAt, lastTriggeredAt

### AC E5.1.2: Radius Configuration
**Given** I am creating a proximity alert
**When** I set the radius
**Then** I should be able to choose values from 50 to 10,000 meters
**And** invalid values should be rejected

### AC E5.1.3: Direction Selection
**Given** I am creating a proximity alert
**When** I select the direction type
**Then** I should be able to choose:
  - "Enter" (notify when target enters radius)
  - "Exit" (notify when target leaves radius)
  - "Both" (notify on both transitions)

### AC E5.1.4: Server Sync
**Given** I create a proximity alert
**When** I save the alert
**Then** it should be sent to server via `POST /api/proximity-alerts`
**And** synced alerts should persist across app reinstalls

### AC E5.1.5: Alert Management UI
**Given** I am on the Alerts screen
**Then** I should be able to:
  - View list of my proximity alerts
  - Create new alert
  - Edit existing alert
  - Delete alert
  - Enable/disable alert without deletion

### AC E5.1.6: Sync on Startup
**Given** I launch the app
**When** network is available
**Then** alerts should sync from server
**And** local state should match server state

## Tasks / Subtasks

- [ ] Task 1: Create ProximityAlert Domain Model (AC: E5.1.1)
  - [ ] Create ProximityAlert data class
  - [ ] Create AlertDirection enum (ENTER, EXIT, BOTH)
  - [ ] Create ProximityState enum (INSIDE, OUTSIDE)
- [ ] Task 2: Create ProximityAlert Room Entity (AC: E5.1.1)
  - [ ] Create ProximityAlertEntity with Room annotations
  - [ ] Create ProximityAlertDao
  - [ ] Add to AppDatabase
- [ ] Task 3: Create Network Models (AC: E5.1.4)
  - [ ] Create ProximityAlertDto for API
  - [ ] Add CRUD endpoints to AlertApiService
  - [ ] Implement create, update, delete, list
- [ ] Task 4: Create AlertRepository (AC: E5.1.4, E5.1.6)
  - [ ] Implement local + remote sync logic
  - [ ] Add createAlert, updateAlert, deleteAlert
  - [ ] Add syncAlerts() for startup sync
- [ ] Task 5: Create AlertsScreen UI (AC: E5.1.5)
  - [ ] Create AlertsScreen composable
  - [ ] Show list of alerts
  - [ ] Add create/edit dialog or screen
- [ ] Task 6: Create CreateAlertScreen (AC: E5.1.2, E5.1.3)
  - [ ] Add target device selector (from group members)
  - [ ] Add radius slider/input (50-10,000m)
  - [ ] Add direction selector (chips or dropdown)
- [ ] Task 7: Create AlertsViewModel (AC: E5.1.5, E5.1.6)
  - [ ] Load alerts from repository
  - [ ] Implement CRUD operations
  - [ ] Handle sync on init
- [ ] Task 8: Testing (All ACs)
  - [ ] Unit test AlertRepository
  - [ ] Unit test AlertsViewModel
  - [ ] Manual test CRUD flow

## Dev Notes

### Architecture
- Follow Repository pattern with local + remote sync
- Room database for offline support
- Sync strategy: remote-first on startup, optimistic local updates

### Data Models
```kotlin
// Domain
data class ProximityAlert(
    val id: String,
    val ownerDeviceId: String,
    val targetDeviceId: String,
    val targetDisplayName: String? = null,
    val radiusMeters: Int,
    val direction: AlertDirection,
    val active: Boolean,
    val lastState: ProximityState,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastTriggeredAt: Instant?
)

enum class AlertDirection { ENTER, EXIT, BOTH }
enum class ProximityState { INSIDE, OUTSIDE }

// Room Entity
@Entity(tableName = "proximity_alerts")
data class ProximityAlertEntity(
    @PrimaryKey val id: String,
    val ownerDeviceId: String,
    val targetDeviceId: String,
    val radiusMeters: Int,
    val direction: String,
    val active: Boolean,
    val lastState: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastTriggeredAt: Long?
)
```

### Files to Create
- `domain/model/ProximityAlert.kt` (NEW)
- `data/model/ProximityAlertEntity.kt` (NEW)
- `data/database/dao/ProximityAlertDao.kt` (NEW)
- `network/AlertApiService.kt` (NEW)
- `data/repository/AlertRepository.kt` (NEW)
- `ui/alerts/AlertsScreen.kt` (NEW)
- `ui/alerts/AlertsViewModel.kt` (NEW)
- `ui/alerts/CreateAlertScreen.kt` (NEW)

### References
- [Source: PRD FR-3.1.1-3.1.4 - Proximity Alert Definition requirements]
- [Source: PRD FR-3.4.1-3.4.3 - Alert Management requirements]
- [Source: PRD Section 6.2 - POST /api/proximity-alerts spec]
- [Source: PRD Data Model: ProximityAlert]
- [Source: epics.md - Story 5.1 description]

## Dev Agent Record

### Context Reference
- `/Users/martinjanci/cursor/phone-manager/docs/story-context-E5.1.xml` (Generated: 2025-11-25)

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
**Dependencies**: Story E1.2 (Group Member Discovery - for target device selection)
