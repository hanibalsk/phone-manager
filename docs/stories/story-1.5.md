# Story 1.5: Offline Queue & Reliable Upload with WorkManager

**Status:** Ready for Implementation
**Epic:** 1 - Background Location Tracking Service  
**Priority:** MVP - Critical Path
**Complexity:** High
**Estimated Effort:** 5-7 days

## Story

As a user,
I want location data to be reliably uploaded even when offline,
so that no location data is lost regardless of network conditions.

## Acceptance Criteria

1. **WorkManager Integration:**
   - [ ] Periodic WorkManager job for location upload (every 15 minutes)
   - [ ] Job only runs when network available (network constraint)
   - [ ] Job persists across device restarts
   - [ ] Job runs in Doze mode maintenance windows

2. **Upload Logic:**
   - [ ] Query unsynced locations from database (max 100 per batch)
   - [ ] Upload in batches to avoid overwhelming server
   - [ ] Mark successfully uploaded locations as synced
   - [ ] Handle partial upload failures gracefully
   - [ ] Upload oldest locations first (FIFO)

3. **Retry Mechanism:**
   - [ ] Exponential backoff for retries (1min, 5min, 15min, 1hr)
   - [ ] Maximum 5 retry attempts before giving up
   - [ ] Failed locations remain in queue for next attempt
   - [ ] Track retry count per location

4. **Network Awareness:**
   - [ ] Prefer WiFi for uploads (configurable)
   - [ ] Fall back to cellular if WiFi unavailable
   - [ ] Monitor network type changes
   - [ ] Pause uploads if metered network and configured to wait

5. **Idempotency:**
   - [ ] Prevent duplicate uploads
   - [ ] Handle server-side duplicate detection
   - [ ] Transaction management for database updates

6. **Testing:**
   - [ ] Unit tests for upload logic
   - [ ] Integration tests with WorkManager TestDriver
   - [ ] Test various network conditions
   - [ ] Test with large backlogs (1000+ records)

## Tasks / Subtasks

### Task 1: Add Dependencies
```kotlin
implementation("androidx.work:work-runtime-ktx:2.9.0")
testImplementation("androidx.work:work-testing:2.9.0")
```

### Task 2: Create Upload Worker
```kotlin
@HiltWorker
class LocationUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val locationRepository: LocationRepository,
    private val networkManager: NetworkConnectivityManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!networkManager.isNetworkAvailable()) {
            return Result.retry()
        }

        return try {
            val batchSize = inputData.getInt(KEY_BATCH_SIZE, 100)
            val locations = locationRepository.getUnsyncedLocations(batchSize)

            if (locations.isEmpty()) {
                return Result.success()
            }

            when (val result = locationRepository.uploadLocations(locations)) {
                is three.two.bit.phone.manager.core.Result.Success -> {
                    val locationIds = locations.map { /* extract IDs */ }
                    locationRepository.markLocationsSynced(locationIds)

                    val output = workDataOf(
                        KEY_UPLOADED_COUNT to result.data
                    )
                    Result.success(output)
                }
                is three.two.bit.phone.manager.core.Result.Error -> {
                    if (runAttemptCount < MAX_RETRIES) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_BATCH_SIZE = "batch_size"
        const val KEY_UPLOADED_COUNT = "uploaded_count"
        const val MAX_RETRIES = 5
    }
}
```

### Task 3: Create WorkManager Scheduler
```kotlin
class LocationUploadScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun schedulePeriodicUpload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWork = PeriodicWorkRequestBuilder<LocationUploadWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1,
                TimeUnit.MINUTES
            )
            .setInputData(workDataOf(
                LocationUploadWorker.KEY_BATCH_SIZE to 100
            ))
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            uploadWork
        )
    }

    fun cancelUploadWork() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "location_upload_work"
    }
}
```

### Task 4: Update Repository for Batch Operations
```kotlin
// Add to LocationRepository
suspend fun uploadAndMarkSynced(locations: List<Location>): Result<Int>
```

### Task 5: Implement Retry Logic
```kotlin
class RetryPolicy {
    fun calculateBackoff(attemptCount: Int): Long {
        return when (attemptCount) {
            0 -> 1 * 60 * 1000L       // 1 minute
            1 -> 5 * 60 * 1000L       // 5 minutes
            2 -> 15 * 60 * 1000L      // 15 minutes
            3 -> 30 * 60 * 1000L      // 30 minutes
            else -> 60 * 60 * 1000L   // 1 hour
        }
    }
}
```

### Task 6: Testing
- Unit tests for worker logic
- Integration tests with WorkManager TestDriver
- Test exponential backoff
- Test network constraint behavior

## Definition of Done

- [ ] All acceptance criteria met
- [ ] WorkManager job scheduled correctly
- [ ] Offline queue working reliably
- [ ] No duplicate uploads
- [ ] Tested with large backlogs
- [ ] Battery efficient

## Dependencies

**Blocks:** Story 1.6 (upload mechanism needed)

**Blocked By:**
- Story 1.3 (database) ✅
- Story 1.4 (network) ✅

## References

- [WorkManager Documentation](https://developer.android.com/topic/libraries/architecture/workmanager)
- BMAD Technical Evaluation

---

**Epic:** [Epic 1](../epics/epic-1-location-tracking.md)
**Previous:** [Story 1.4](./story-1.4.md) | **Next:** [Story 1.6](./story-1.6.md)
