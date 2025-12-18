# Story UGM-1.4: Offline Queue for Device Linking

**Story ID**: UGM-1.4
**Epic**: UGM-1 - Device-User Linking on Authentication
**Priority**: Medium
**Estimate**: 3 story points
**Status**: Ready for Dev
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

- [ ] Task 1: Create Room database entity for offline queue (AC: 1)
  - [ ] Create PendingDeviceLinkEntity with userId, deviceId, timestamp, retryCount
  - [ ] Create PendingDeviceLinkDao with insert, delete, getAll methods
  - [ ] Add entity to AppDatabase

- [ ] Task 2: Create DeviceLinkWorker using WorkManager (AC: 2, 3)
  - [ ] Extend CoroutineWorker
  - [ ] Inject DeviceApiService and SecureStorage via HiltWorker
  - [ ] Implement retry logic with exponential backoff
  - [ ] Configure constraints: NetworkType.CONNECTED

- [ ] Task 3: Create DeviceLinkQueueRepository (AC: 1, 2)
  - [ ] Method to queue pending link operation
  - [ ] Method to process queue when online
  - [ ] Method to clear completed operations

- [ ] Task 4: Monitor network state via ConnectivityManager (AC: 2)
  - [ ] Create NetworkMonitor utility (may already exist)
  - [ ] Trigger queue processing on connectivity change

- [ ] Task 5: Update AuthViewModel to use queue on network failure (AC: 1)
  - [ ] Detect network errors in autoLinkCurrentDevice
  - [ ] Queue operation instead of failing immediately
  - [ ] Update DeviceLinkState to include Queued state

- [ ] Task 6: Handle retry exhaustion with notification (AC: 4)
  - [ ] After 3 failed retries, show system notification
  - [ ] Notification provides manual retry action
  - [ ] Clear queue entry and mark as failed locally

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

(To be filled during implementation)

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

(To be filled after completion)

---

## File List

### Files to Create

- `app/src/main/java/three/two/bit/phonemanager/data/model/PendingDeviceLinkEntity.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/database/PendingDeviceLinkDao.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/DeviceLinkQueueRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/worker/DeviceLinkWorker.kt`

### Files to Modify

- `app/src/main/java/three/two/bit/phonemanager/data/database/AppDatabase.kt` (add entity and DAO)
- `app/src/main/java/three/two/bit/phonemanager/ui/auth/AuthViewModel.kt` (queue on network error)
- `app/src/main/java/three/two/bit/phonemanager/di/DatabaseModule.kt` (provide DAO)
- `app/src/main/res/values/strings.xml` (notification strings)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created from UGM epics specification |

---

**Last Updated**: 2025-12-18
**Status**: Ready for Dev
**Dependencies**: UGM-1.1
**Blocking**: None
