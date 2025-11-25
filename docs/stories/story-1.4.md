# Story 1.4: Service State Persistence and Synchronization

**Story ID**: 1.4
**Epic**: 1 - Location Tracking Core
**Priority**: Must-Have
**Estimate**: 5 story points (3-4 days)
**Status**: Ready for Development
**Created**: 2025-11-25
**Context Document**: [story-context-1.1.4.xml](../story-context-1.1.4.xml)

---

## User Story

```
AS A user
I WANT the location tracking service state to persist correctly across app restarts and device reboots
SO THAT tracking automatically resumes without manual intervention and the UI always reflects the actual service state
```

---

## Business Value

- Ensures reliable tracking restoration after device reboots
- Prevents user confusion from UI/service state desynchronization
- Improves user trust by displaying accurate service status
- Reduces support requests about "tracking stopped unexpectedly"
- Enables proper functioning of the boot receiver and watchdog
- Supports the "set and forget" user experience for location tracking

---

## Background / Problem Statement

Based on code review analysis, the current implementation has critical issues:

1. **Service state is only stored in-memory** in `LocationRepositoryImpl._serviceHealth` as a `MutableStateFlow`. After a device reboot or process kill, this resets to `{ isRunning = false }`.

2. **BootReceiver checks in-memory state** via `locationRepository.getServiceHealth().first()`, which always returns "not running" after reboot, so tracking is **never restored**.

3. **`LocationServiceController.isServiceRunning()`** just returns the in-memory `_serviceState.value.isRunning`, not the actual OS service state. The ViewModel uses this for reconciliation, leading to UI/service desynchronization.

4. **Unit tests are out of sync** with current production models (e.g., `LocationQueueEntity` fields, `NetworkManager` return types), reducing CI value.

---

## Acceptance Criteria

### AC 1.4.1: Persisted Service Running State
**Given** the user has enabled location tracking
**When** the device reboots or the app process is killed
**Then** the "service should be running" preference is persisted in DataStore/SharedPreferences
**And** the value survives process death and device reboots
**And** `LocationRepositoryImpl.getServiceHealth()` reads from persisted storage on initialization

**Verification**: Enable tracking → Reboot device → Check persisted preference shows "running" before BootReceiver executes

---

### AC 1.4.2: Boot Receiver Correctly Restores Tracking
**Given** tracking was enabled and persisted before device reboot
**When** the device restarts and `BootReceiver.onReceive()` is triggered
**Then** `locationRepository.getServiceHealth().first().isRunning` returns `true`
**And** the service is started via `serviceController.startTracking()`
**And** the watchdog is scheduled via `watchdogManager.startWatchdog()`
**And** Timber logs confirm "Restoring location tracking service after boot"

**Verification**: Enable tracking → Reboot → Check logs → Verify tracking active after boot

---

### AC 1.4.3: Accurate Service Running Check
**Given** the app needs to determine if the location service is actually running
**When** `LocationServiceController.isServiceRunning()` is called
**Then** it returns the actual OS-level service state (not just in-memory state)
**And** the check uses `ActivityManager.getRunningServices()` or equivalent
**And** the result is accurate even if the service was killed by the OS

**Verification**: Start tracking → Force-kill service via adb → Call `isServiceRunning()` → Returns false

---

### AC 1.4.4: ViewModel State Reconciliation
**Given** the app is opened after being killed or backgrounded
**When** `LocationTrackingViewModel.init` executes
**Then** the toggle state is reconciled with actual service state
**And** if persisted state says "enabled" but service is not running, the service is restarted
**And** if service is running but persisted state is "disabled", the discrepancy is logged and state corrected

**Verification**: Enable tracking → Kill app → Reopen → Toggle shows correct state → Service is running

---

### AC 1.4.5: ServiceHealth Persistence on State Changes
**Given** the service state changes (start/stop/error)
**When** `LocationRepositoryImpl.updateServiceHealth()` is called
**Then** the `isRunning` state is persisted to DataStore
**And** the `lastLocationUpdate` timestamp is persisted
**And** persistence is atomic (no partial writes)
**And** persistence does not block the calling coroutine excessively (<100ms)

**Verification**: Start/stop tracking multiple times → Kill app → Reopen → Persisted state matches last action

---

### AC 1.4.6: Unit Tests Aligned with Production Models
**Given** the test suite in `app/src/test/`
**When** tests are compiled and run
**Then** all tests use current production model signatures:
  - `LocationQueueEntity` uses correct constructor and fields
  - `NetworkManager.uploadLocation()` returns `Result<LocationUploadResponse>`
  - `LocationRepository` method signatures match implementation
**And** all tests pass without modification to production code

**Verification**: `./gradlew testDebugUnitTest` passes with 0 failures

---

## Technical Details

### Architecture

**Pattern**: Extend existing MVVM + Repository pattern with persistent state backing

**Data Flow for State Persistence**:
```
┌─────────────────────────────────────────────────────────────────┐
│                      SERVICE STATE FLOW                          │
│                                                                  │
│  LocationTrackingService                                         │
│         │                                                        │
│         ▼ (state changes)                                        │
│  LocationRepositoryImpl.updateServiceHealth()                    │
│         │                                                        │
│         ├──► MutableStateFlow (in-memory for UI)                │
│         │                                                        │
│         └──► PreferencesRepository/DataStore (persisted)        │
│                    │                                             │
│                    ▼                                             │
│  ┌─────────────────────────────────────────┐                    │
│  │  DataStore                              │                    │
│  │  - service_running: Boolean             │                    │
│  │  - last_location_update: Long           │                    │
│  │  - last_error_message: String?          │                    │
│  └─────────────────────────────────────────┘                    │
│                                                                  │
│  On App/Boot Start:                                              │
│  PreferencesRepository.getServiceRunningState()                  │
│         │                                                        │
│         ▼                                                        │
│  LocationRepositoryImpl._serviceHealth.value = persisted state  │
│         │                                                        │
│         ▼                                                        │
│  BootReceiver / ViewModel reads correct initial state            │
└─────────────────────────────────────────────────────────────────┘
```

### Implementation Files

#### PreferencesRepository Extensions
**File**: `app/src/main/java/com/phonemanager/data/preferences/PreferencesRepository.kt`

```kotlin
// Add to existing PreferencesRepository interface
interface PreferencesRepository {
    // ... existing methods ...

    // NEW: Service state persistence
    val serviceRunningState: Flow<Boolean>
    suspend fun setServiceRunningState(isRunning: Boolean)

    val lastLocationUpdateTime: Flow<Long?>
    suspend fun setLastLocationUpdateTime(timestamp: Long)
}

// Implementation additions
private object PreferencesKeys {
    // ... existing keys ...
    val SERVICE_RUNNING = booleanPreferencesKey("service_running")
    val LAST_LOCATION_UPDATE = longPreferencesKey("last_location_update")
}
```

---

#### LocationRepositoryImpl Modifications
**File**: `app/src/main/java/com/phonemanager/data/repository/LocationRepositoryImpl.kt`

```kotlin
@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
    private val preferencesRepository: PreferencesRepository // NEW: inject
) : LocationRepository {

    // Initialize from persisted state
    private val _serviceHealth = MutableStateFlow(
        ServiceHealth(
            isRunning = false, // Will be updated from persistence
            healthStatus = HealthStatus.HEALTHY
        )
    )

    init {
        // Restore persisted state on initialization
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            combine(
                preferencesRepository.serviceRunningState,
                preferencesRepository.lastLocationUpdateTime
            ) { isRunning, lastUpdate ->
                _serviceHealth.value = _serviceHealth.value.copy(
                    isRunning = isRunning,
                    lastLocationUpdate = lastUpdate
                )
            }.collect()
        }
    }

    fun updateServiceHealth(health: ServiceHealth) {
        _serviceHealth.value = health

        // Persist to DataStore
        CoroutineScope(Dispatchers.IO).launch {
            preferencesRepository.setServiceRunningState(health.isRunning)
            health.lastLocationUpdate?.let {
                preferencesRepository.setLastLocationUpdateTime(it)
            }
        }

        Timber.d("Service health updated and persisted: $health")
    }
}
```

---

#### LocationServiceControllerImpl Fix
**File**: `app/src/main/java/com/phonemanager/service/LocationServiceController.kt`

```kotlin
@Singleton
class LocationServiceControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val preferencesRepository: PreferencesRepository
) : LocationServiceController {

    // ... existing code ...

    override fun isServiceRunning(): Boolean {
        // Use ActivityManager to check actual service state
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        @Suppress("DEPRECATION") // Still works, alternative is more complex
        val runningServices = manager.getRunningServices(Int.MAX_VALUE)

        return runningServices.any { serviceInfo ->
            serviceInfo.service.className == LocationTrackingService::class.java.name
        }
    }
}
```

---

#### ViewModel Reconciliation Enhancement
**File**: `app/src/main/java/com/phonemanager/ui/main/LocationTrackingViewModel.kt`

```kotlin
init {
    viewModelScope.launch {
        // Load persisted toggle state and reconcile with actual service
        preferencesRepository.isTrackingEnabled.collect { persistedEnabled ->
            val isServiceActuallyRunning = serviceController.isServiceRunning()

            when {
                persistedEnabled && !isServiceActuallyRunning -> {
                    // State desync: should be running but isn't - restart service
                    Timber.w("State desync: restarting service (persisted=ON, actual=OFF)")
                    serviceController.startTracking()
                    _trackingState.value = TrackingState.Starting
                }
                !persistedEnabled && isServiceActuallyRunning -> {
                    // State desync: shouldn't be running but is - log and correct
                    Timber.w("State desync: stopping service (persisted=OFF, actual=ON)")
                    serviceController.stopTracking()
                    _trackingState.value = TrackingState.Stopping
                }
                persistedEnabled && isServiceActuallyRunning -> {
                    _trackingState.value = TrackingState.Active()
                }
                else -> {
                    _trackingState.value = TrackingState.Stopped
                }
            }
        }
    }
}
```

---

### Test Fixes Required

#### QueueManagerTest.kt
- Update `LocationQueueEntity` constructor calls to match current signature
- Update `NetworkManager.uploadLocation()` mock to return `Result<LocationUploadResponse>`

#### LocationServiceControllerTest.kt
- Add test for `isServiceRunning()` using mocked `ActivityManager`
- Add test for state persistence on start/stop

#### BootReceiverTest.kt
- Mock `PreferencesRepository` to return persisted state
- Verify service starts when persisted state is `isRunning = true`

---

### Dependencies

**Internal Dependencies**:
- **Story 1.1**: LocationTrackingViewModel (enhanced here)
- **Story 1.3**: LocationServiceController and ServiceHealth (enhanced here)
- **Epic 0.2.3**: LocationRepository interface (extended here)

**External Dependencies**: None new

---

## Testing Strategy

### Unit Tests

**New Tests Required**:

```kotlin
// PreferencesRepositoryTest.kt
@Test
fun `setServiceRunningState persists value to DataStore`()

@Test
fun `serviceRunningState emits persisted value on init`()

// LocationRepositoryImplTest.kt
@Test
fun `updateServiceHealth persists isRunning to preferences`()

@Test
fun `getServiceHealth returns persisted state on initialization`()

// LocationServiceControllerTest.kt
@Test
fun `isServiceRunning returns true when service is running`()

@Test
fun `isServiceRunning returns false when service is not running`()

// LocationTrackingViewModelTest.kt
@Test
fun `init reconciles state and restarts service if desync detected`()
```

---

### Integration Tests

**File**: `app/src/androidTest/java/com/phonemanager/service/ServiceStatePersistenceTest.kt`

```kotlin
@Test
fun serviceStatePersistedAcrossProcessDeath() {
    // Given - Start tracking
    enableTracking()

    // When - Kill and restart app process
    killAppProcess()
    restartApp()

    // Then - Service state restored
    assertServiceIsRunning()
    assertToggleShowsEnabled()
}
```

---

### Manual Testing Checklist

**State Persistence**:
- [ ] Enable tracking → Kill app → Reopen → Toggle shows ON
- [ ] Enable tracking → Reboot device → Tracking auto-restarts
- [ ] Disable tracking → Reboot device → Tracking stays OFF
- [ ] Enable tracking → Force-stop app → Reopen → Service restarts

**State Synchronization**:
- [ ] Kill service via adb → UI updates to show OFF
- [ ] Enable via UI → Check `adb shell dumpsys activity services` shows service
- [ ] No "state desync" Timber warnings during normal operation

**Test Suite**:
- [ ] `./gradlew testDebugUnitTest` passes with 0 failures
- [ ] All QueueManagerTest tests pass
- [ ] All LocationServiceControllerTest tests pass
- [ ] All BootReceiverTest tests pass

---

## Definition of Done

- [x] `PreferencesRepository` extended with service state persistence methods
- [x] `LocationRepositoryImpl` initializes from persisted state
- [x] `LocationRepositoryImpl.updateServiceHealth()` persists state changes
- [x] `LocationServiceController.isServiceRunning()` checks actual OS service state
- [x] `LocationTrackingViewModel` reconciles persisted vs actual state on init
- [x] `BootReceiver` correctly reads persisted state and restores service
- [x] Unit tests updated to match production model signatures
- [ ] All unit tests pass
- [ ] Integration tests for state persistence pass
- [ ] Manual testing completed on Android 10, 13, 14
- [ ] Code review approved
- [ ] No regression in existing functionality

---

## Risks & Mitigations

**RISK**: DataStore write latency causes state loss
- **Impact**: Service state not persisted before process kill
- **Probability**: Low
- **Mitigation**: Use synchronous commit for critical state changes; add fallback SharedPreferences for immediate writes
- **Contingency**: Accept occasional state loss as edge case

**RISK**: ActivityManager.getRunningServices() deprecated
- **Impact**: May not work on future Android versions
- **Probability**: Medium (deprecated since API 26)
- **Mitigation**: Method still works; add fallback using service binding or WorkManager query
- **Contingency**: Use `ServiceCompat.startForeground()` return value or foreground service info

**RISK**: State reconciliation loops
- **Impact**: Service starts/stops repeatedly
- **Probability**: Low
- **Mitigation**: Add debounce/cooldown between reconciliation actions; log and alert on repeated cycles
- **Contingency**: Disable auto-reconciliation, require manual toggle

---

## Related Stories

- **Story 1.1**: Tracking Toggle (base implementation)
- **Story 1.3**: UI-Service Integration (ServiceHealth model)
- **Epic 0.2.4**: LocationTrackingService and BootReceiver

---

## File List

### Modified Files
- `app/src/main/java/com/phonemanager/data/preferences/PreferencesRepository.kt` - Extended interface with service state persistence methods
- `app/src/main/java/com/phonemanager/data/repository/LocationRepositoryImpl.kt` - Added persistence integration and state restoration from DataStore
- `app/src/main/java/com/phonemanager/service/LocationServiceController.kt` - Implemented actual OS service state check via ActivityManager
- `app/src/main/java/com/phonemanager/ui/main/LocationTrackingViewModel.kt` - Enhanced state reconciliation logic with service restart capability
- `app/src/main/java/com/phonemanager/receiver/BootReceiver.kt` - Updated documentation (now uses persisted state via repository)
- `app/src/test/java/com/phonemanager/queue/QueueManagerTest.kt` - Fixed LocationQueueEntity and NetworkManager signatures
- `app/src/test/java/com/phonemanager/service/LocationServiceControllerTest.kt` - Fixed constructor, added isServiceRunning tests
- `app/src/test/java/com/phonemanager/receiver/BootReceiverTest.kt` - Fixed interface reference to LocationServiceController
- `app/src/test/java/com/phonemanager/ui/main/LocationTrackingViewModelTest.kt` - Added state reconciliation tests
- `app/src/test/java/com/phonemanager/data/preferences/PreferencesRepositoryTest.kt` - Added service state persistence test stubs

---

## Dev Agent Record

### Debug Log
**2025-11-25**: Implementation Plan
1. Extended PreferencesRepository interface with serviceRunningState and lastLocationUpdateTime flows/setters
2. Implemented persistence methods in PreferencesRepositoryImpl using DataStore
3. Modified LocationRepositoryImpl to inject PreferencesRepository and restore state on init
4. Updated updateServiceHealth() to persist state changes asynchronously
5. Implemented actual OS service state check in LocationServiceController.isServiceRunning() using ActivityManager
6. Enhanced ViewModel reconciliation to restart service on desync when permissions granted
7. Fixed unit tests to match production model signatures

### Completion Notes
All code changes for Story 1.4 have been implemented. The implementation follows the existing DataStore/Repository patterns in the codebase. Key architectural decisions:
- Used async persistence (non-blocking) to avoid UI lag
- Implemented graceful fallbacks for ActivityManager unavailability
- Added state correction logic in ViewModel when permissions are missing

**Note**: Gradle wrapper JAR is missing from the repository, preventing local test execution. Test validation requires running `./gradlew testDebugUnitTest` after restoring the gradle wrapper.

---

## Dev Notes

### Source References

- [Source: CODE_REVIEW.md - High-priority issues: Service state persistence is purely in-memory]
- [Source: LocationRepositoryImpl.kt - `_serviceHealth` MutableStateFlow without persistence]
- [Source: BootReceiver.kt:50 - `locationRepository.getServiceHealth().first()` reads in-memory state]
- [Source: LocationServiceController.kt:115-118 - `isServiceRunning()` stub implementation]

### Project Structure Notes

- Alignment with existing `PreferencesRepository` pattern for DataStore access
- No new modules required; extends existing data layer
- Test fixes confined to `app/src/test/` directory

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-25 | Claude | Initial story creation based on code review analysis |
| 2025-11-25 | Claude | Implemented all code changes: PreferencesRepository extensions, LocationRepositoryImpl persistence, isServiceRunning OS check, ViewModel reconciliation, test fixes |

---

**Last Updated**: 2025-11-25
**Status**: Ready for Review
**Dependencies**: Stories 1.1, 1.3 (implemented), Epic 0.2.4 (implemented)
