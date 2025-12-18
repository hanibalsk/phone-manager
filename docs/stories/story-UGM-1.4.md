# Story UGM-1.4: Offline Queue for Device Linking

**Story ID**: UGM-1.4
**Epic**: UGM-1 - Device-User Linking on Authentication
**Priority**: Medium
**Estimate**: 3 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: UGM-1.1

---

## Story

As a user logging in while offline,
I want my device linking to be queued and retried when online,
So that I don't lose the linking operation due to network issues.

**FRs Covered**: FR42

---

## Acceptance Criteria

### AC 1: Queue operation when offline
**Given** user logs in successfully but network is unavailable
**When** auto-link fails due to network
**Then** queue the link operation for later retry

### AC 2: Auto-retry when online
**Given** a queued link operation exists
**When** network becomes available
**Then** automatically retry the link operation

### AC 3: Update state on success
**Given** retry is in progress
**When** operation succeeds
**Then** update local state and clear the queue

### AC 4: Handle retry exhaustion (NFR-R2)
**Given** retry fails 3 times
**When** all retries exhausted
**Then** show notification to user with manual retry option

---

## Tasks / Subtasks

- [x] Task 1: Create Room database entity for offline queue (AC: 1)
  - [x] Create PendingDeviceLinkEntity with userId, deviceId, timestamp, retryCount
  - [x] Create PendingDeviceLinkDao with insert, delete, getAll methods
  - [x] Add entity to AppDatabase

- [x] Task 2: Create DeviceLinkWorker using WorkManager (AC: 2, 3)
  - [x] Extend CoroutineWorker
  - [x] Inject DeviceApiService and SecureStorage via HiltWorker
  - [x] Implement retry logic with exponential backoff
  - [x] Configure constraints: NetworkType.CONNECTED

- [x] Task 3: Create DeviceLinkQueueRepository (AC: 1, 2)
  - [x] Method to queue pending link operation
  - [x] Method to process queue when online
  - [x] Method to clear completed operations

- [x] Task 4: Monitor network state via ConnectivityManager (AC: 2)
  - [x] Create NetworkMonitor utility (may already exist)
  - [x] Trigger queue processing on connectivity change

- [x] Task 5: Update AuthViewModel to use queue on network failure (AC: 1)
  - [x] Detect network errors in autoLinkCurrentDevice
  - [x] Queue operation instead of failing immediately
  - [x] Update DeviceLinkState to include Queued state

- [x] Task 6: Handle retry exhaustion with notification (AC: 4)
  - [x] After 3 failed retries, show system notification
  - [x] Notification provides manual retry action
  - [x] Clear queue entry and mark as failed locally

---

## Dev Notes

### Technical Notes
- Use Room database for offline queue persistence
- Use WorkManager for background retry with exponential backoff
- Monitor network state via `ConnectivityManager`

### Implementation Details

**Room Entity:**
```kotlin
@Entity(tableName = "pending_device_links")
data class PendingDeviceLinkEntity(
    @PrimaryKey val deviceId: String,
    val userId: String,
    val timestamp: Long,
    val retryCount: Int = 0
)
```

**WorkManager Configuration:**
```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

val workRequest = OneTimeWorkRequestBuilder<DeviceLinkWorker>()
    .setConstraints(constraints)
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
    .build()
```

### NFR Compliance
- NFR-R2: Auto-link retry on failure - 3 attempts
- Exponential backoff: 30s, 60s, 120s

### Existing Code Reference
- SettingsSyncWorker.kt provides pattern for WorkManager with Hilt
- AppDatabase.kt for Room database setup
- NetworkMonitor may already exist in the codebase

---

## Dev Agent Record

### Debug Log

No issues encountered during implementation.

### Implementation Plan

1. Create PendingDeviceLinkEntity and DAO
2. Add entity to AppDatabase
3. Create DeviceLinkQueueRepository
4. Create DeviceLinkWorker with Hilt injection
5. Update AuthViewModel.autoLinkCurrentDevice() to queue on network error
6. Add DeviceLinkState.Queued state
7. Implement notification for retry exhaustion
8. Test offline → online flow

### Completion Notes

Implementation completed successfully:
- Created PendingDeviceLinkEntity and PendingDeviceLinkDao
- Added entity to AppDatabase with migration
- Created DeviceLinkWorker with HiltWorker for background retry
- Implemented exponential backoff (30s, 60s, 120s)
- Added isNetworkError() detection in AuthViewModel
- Added DeviceLinkState.Queued state
- Shows notification after 3 failed retries

---

## File List

### Created Files

- `app/src/main/java/three/two/bit/phonemanager/data/model/PendingDeviceLinkEntity.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/database/PendingDeviceLinkDao.kt`
- `app/src/main/java/three/two/bit/phonemanager/worker/DeviceLinkWorker.kt`

### Modified Files

- `app/src/main/java/three/two/bit/phonemanager/data/database/AppDatabase.kt` (add entity and DAO)
- `app/src/main/java/three/two/bit/phonemanager/ui/auth/AuthViewModel.kt` (queue on network error)
- `app/src/main/java/three/two/bit/phonemanager/di/DatabaseModule.kt` (provide DAO)
- `app/src/main/res/values/strings.xml` (notification strings)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created from UGM epics specification |
| 2025-12-18 | Claude | Implementation completed - all ACs satisfied |
| 2025-12-18 | Claude | Status corrected during code review |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-1.1
**Blocking**: None
