# Story 1.1: Location Tracking Toggle

**Story ID**: 1.1
**Epic**: 1 - Location Tracking Core (UI Layer)
**Priority**: Must-Have
**Estimate**: 5 story points (3-5 days)
**Status**: Ready for Development
**Created**: 2025-01-11

---

## User Story

```
AS A user
I WANT to easily enable or disable location tracking with a single toggle
SO THAT I have clear control over when my location is being collected
```

---

## Business Value

- Provides intuitive control over location tracking
- Ensures user consent and privacy control
- Improves user trust through transparent control
- Reduces support requests about "how to stop tracking"

---

## Acceptance Criteria

### AC 1.1.1: Toggle Component Display
**Given** the user opens the main screen
**When** the app loads
**Then** a Material 3 Switch component is displayed prominently
**And** the toggle label reads "Location Tracking"
**And** the toggle follows Material 3 design guidelines

**Verification**: Visual inspection; UI matches design specifications

### AC 1.1.2: State Persistence
**Given** the user enables the toggle
**When** the user force-closes the app and relaunches
**Then** the toggle remains in the ON state
**And** the state loads within 100ms

**Verification**: Enable toggle → Force close app → Relaunch → Toggle state preserved

### AC 1.1.3: Service Start on Enable
**Given** the user has granted location permissions
**When** the user toggles tracking ON
**Then** LocationTrackingService starts within 500ms
**And** a foreground notification appears
**And** the service begins collecting locations

**Verification**: Toggle ON → Service starts → Notification displayed → adb logs show service running

### AC 1.1.4: Service Stop on Disable
**Given** location tracking is currently active
**When** the user toggles tracking OFF
**Then** LocationTrackingService stops immediately
**And** the foreground notification disappears
**And** no new locations are collected

**Verification**: Toggle OFF → Service stops → Notification removed → adb logs show service stopped

### AC 1.1.5: Disabled State Without Permissions
**Given** the user has not granted location permissions
**When** the main screen loads
**Then** the toggle is displayed in a disabled state
**And** explanatory text shows "Location permissions required"
**And** tapping the toggle shows a permission prompt guidance

**Verification**: Deny permissions → Toggle disabled with explanation text

### AC 1.1.6: Toggle Response Performance
**Given** the user interacts with the toggle
**When** the toggle state changes
**Then** visual feedback occurs within 200ms
**And** the UI remains responsive during state transition

**Verification**: Performance profiling confirms < 200ms latency

---

## Technical Details

### Architecture

**Pattern**: MVVM with Repository

**Components**:
```
LocationTrackingViewModel ──► PreferencesRepository
           │                           │
           │                     DataStore
           │
           └──► LocationServiceController ──► LocationTrackingService (Epic 0.2.4)
```

### Implementation Files

#### ViewModel
**File**: `app/src/main/java/com/phonemanager/ui/main/LocationTrackingViewModel.kt`

```kotlin
@HiltViewModel
class LocationTrackingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val serviceController: LocationServiceController,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _trackingState = MutableStateFlow(TrackingState.Stopped)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Checking)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    init {
        viewModelScope.launch {
            // Load persisted toggle state
            preferencesRepository.isTrackingEnabled.collect { enabled ->
                _trackingState.value = if (enabled) TrackingState.Active else TrackingState.Stopped
            }
        }

        viewModelScope.launch {
            // Monitor permission state
            permissionManager.observePermissionState().collect { state ->
                _permissionState.value = state
            }
        }
    }

    fun toggleTracking() {
        viewModelScope.launch {
            when (_trackingState.value) {
                is TrackingState.Stopped -> startTracking()
                is TrackingState.Active -> stopTracking()
                is TrackingState.Starting, is TrackingState.Stopping -> {
                    // Ignore rapid taps during transition
                }
            }
        }
    }

    private suspend fun startTracking() {
        if (!permissionManager.hasAllPermissions()) {
            // Trigger permission request flow
            return
        }

        _trackingState.value = TrackingState.Starting

        serviceController.startTracking()
            .onSuccess {
                preferencesRepository.setTrackingEnabled(true)
                _trackingState.value = TrackingState.Active
            }
            .onFailure { error ->
                _trackingState.value = TrackingState.Error(error.message ?: "Failed to start")
            }
    }

    private suspend fun stopTracking() {
        _trackingState.value = TrackingState.Stopping

        serviceController.stopTracking()
            .onSuccess {
                preferencesRepository.setTrackingEnabled(false)
                _trackingState.value = TrackingState.Stopped
            }
            .onFailure { error ->
                _trackingState.value = TrackingState.Error(error.message ?: "Failed to stop")
            }
    }
}

sealed class TrackingState {
    object Stopped : TrackingState()
    object Starting : TrackingState()
    data class Active(val lastUpdate: Instant? = null) : TrackingState()
    object Stopping : TrackingState()
    data class Error(val message: String) : TrackingState()
}
```

#### Compose UI Component
**File**: `app/src/main/java/com/phonemanager/ui/main/LocationTrackingToggle.kt`

```kotlin
@Composable
fun LocationTrackingToggle(
    viewModel: LocationTrackingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val trackingState by viewModel.trackingState.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()

    val isChecked = trackingState is TrackingState.Active
    val isEnabled = permissionState is PermissionState.Granted &&
                    trackingState !is TrackingState.Starting &&
                    trackingState !is TrackingState.Stopping

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Location Tracking",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when (trackingState) {
                        is TrackingState.Stopped -> "Inactive"
                        is TrackingState.Starting -> "Starting..."
                        is TrackingState.Active -> "Active"
                        is TrackingState.Stopping -> "Stopping..."
                        is TrackingState.Error -> (trackingState as TrackingState.Error).message
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (trackingState) {
                        is TrackingState.Active -> MaterialTheme.colorScheme.primary
                        is TrackingState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (trackingState is TrackingState.Starting || trackingState is TrackingState.Stopping) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Switch(
                    checked = isChecked,
                    onCheckedChange = { viewModel.toggleTracking() },
                    enabled = isEnabled,
                    modifier = Modifier.semantics {
                        contentDescription = if (isChecked) {
                            "Location tracking is active. Toggle to stop."
                        } else {
                            "Location tracking is inactive. Toggle to start."
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocationTrackingTogglePreview() {
    PhoneManagerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            LocationTrackingToggle()
        }
    }
}
```

#### PreferencesRepository
**File**: `app/src/main/java/com/phonemanager/data/preferences/PreferencesRepository.kt`

```kotlin
interface PreferencesRepository {
    val isTrackingEnabled: Flow<Boolean>
    suspend fun setTrackingEnabled(enabled: Boolean)
    val trackingInterval: Flow<Int> // minutes
    suspend fun setTrackingInterval(minutes: Int)
}

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    private object PreferencesKeys {
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        val TRACKING_INTERVAL = intPreferencesKey("tracking_interval_minutes")
    }

    override val isTrackingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRACKING_ENABLED] ?: false
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading tracking enabled preference")
                emit(false)
            } else {
                throw exception
            }
        }

    override suspend fun setTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRACKING_ENABLED] = enabled
        }
    }

    override val trackingInterval: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRACKING_INTERVAL] ?: 5 // default 5 minutes
        }

    override suspend fun setTrackingInterval(minutes: Int) {
        require(minutes in 1..60) { "Interval must be between 1 and 60 minutes" }
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRACKING_INTERVAL] = minutes
        }
    }
}
```

#### ServiceController
**File**: `app/src/main/java/com/phonemanager/service/LocationServiceController.kt`

```kotlin
interface LocationServiceController {
    suspend fun startTracking(): Result<Unit>
    suspend fun stopTracking(): Result<Unit>
    fun observeServiceState(): Flow<ServiceState>
}

@Singleton
class LocationServiceControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository
) : LocationServiceController {

    override suspend fun startTracking(): Result<Unit> {
        return try {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START_TRACKING
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start tracking service")
            Result.failure(e)
        }
    }

    override suspend fun stopTracking(): Result<Unit> {
        return try {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_STOP_TRACKING
            }
            context.startService(intent)

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop tracking service")
            Result.failure(e)
        }
    }

    override fun observeServiceState(): Flow<ServiceState> {
        return locationRepository.observeServiceHealth()
            .map { health ->
                ServiceState(
                    isRunning = health.isRunning,
                    lastUpdate = health.lastLocationUpdate,
                    locationCount = health.locationCount
                )
            }
    }
}

data class ServiceState(
    val isRunning: Boolean,
    val lastUpdate: Instant?,
    val locationCount: Int
)
```

### Dependencies

**Internal Dependencies**:
- **Epic 0.2.3**: LocationRepository interface
- **Epic 0.2.4**: LocationTrackingService
- **Epic 0.1**: Hilt DI configuration

**External Dependencies**:
```kotlin
// app/build.gradle.kts
dependencies {
    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Material 3
    implementation("androidx.compose.material3:material3:1.2.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-compiler:2.48.1")
}
```

---

## Testing Strategy

### Unit Tests

**File**: `app/src/test/java/com/phonemanager/ui/main/LocationTrackingViewModelTest.kt`

```kotlin
@Test
fun `toggleTracking starts service when stopped`() = runTest {
    // Given
    val viewModel = createViewModel()
    coEvery { serviceController.startTracking() } returns Result.success(Unit)

    // When
    viewModel.toggleTracking()
    advanceUntilIdle()

    // Then
    coVerify { serviceController.startTracking() }
    verify { preferencesRepository.setTrackingEnabled(true) }
    assert(viewModel.trackingState.value is TrackingState.Active)
}

@Test
fun `toggle disabled when permissions not granted`() = runTest {
    // Given
    every { permissionManager.hasAllPermissions() } returns false
    val viewModel = createViewModel()

    // When
    viewModel.toggleTracking()
    advanceUntilIdle()

    // Then
    coVerify(exactly = 0) { serviceController.startTracking() }
}
```

### Integration Tests

**File**: `app/src/androidTest/java/com/phonemanager/ui/main/LocationTrackingToggleTest.kt`

```kotlin
@Test
fun toggle_startsAndStopsService() {
    // Given
    grantLocationPermissions()

    // When - Enable tracking
    composeTestRule.onNodeWithText("Location Tracking").performClick()

    // Then
    composeTestRule.waitUntil(timeoutMillis = 1000) {
        isServiceRunning(LocationTrackingService::class.java)
    }
    assertThat(isServiceRunning(LocationTrackingService::class.java)).isTrue()

    // When - Disable tracking
    composeTestRule.onNodeWithText("Location Tracking").performClick()

    // Then
    composeTestRule.waitUntil(timeoutMillis = 1000) {
        !isServiceRunning(LocationTrackingService::class.java)
    }
    assertThat(isServiceRunning(LocationTrackingService::class.java)).isFalse()
}
```

### Manual Testing Checklist

- [ ] Toggle visual states (on/off/disabled/loading)
- [ ] Toggle functionality with permissions granted
- [ ] Toggle disabled when permissions denied
- [ ] State persists after app restart
- [ ] State persists after device reboot
- [ ] Service starts within 500ms of toggle
- [ ] Notification appears when tracking active
- [ ] Service stops immediately on toggle off
- [ ] Toggle response feels instant (< 200ms)
- [ ] Accessibility: TalkBack reads toggle state correctly

---

## Definition of Done

- [ ] Toggle component implemented with Material 3 Switch
- [ ] ViewModel manages state with Kotlin StateFlow
- [ ] DataStore persists toggle state
- [ ] Service starts/stops correctly with toggle
- [ ] Loading states displayed during transitions
- [ ] Error states handled and displayed
- [ ] Unit tests achieve > 80% coverage
- [ ] Integration tests pass
- [ ] UI tests cover all user interactions
- [ ] Accessibility audit passes
- [ ] Performance profiling confirms < 200ms response
- [ ] Code review approved
- [ ] Documentation complete

---

## Risks & Mitigations

**RISK**: Service fails to start due to battery optimization
- **Mitigation**: Detect battery optimization status; guide user to whitelist app
- **Detection**: Check `PowerManager.isIgnoringBatteryOptimizations()`

**RISK**: State desynchronization between UI and service
- **Mitigation**: Implement health check on app resume; reconcile from service state
- **Detection**: Compare DataStore state with actual service running state

---

## Related Stories

- **Story 1.2**: Permission Request Flow (blocks this story - permissions needed)
- **Story 1.3**: UI-Service Integration (extends this story - adds real-time updates)
- **Story 0.2.4**: LocationTrackingService (dependency - service this toggle controls)

---

**Last Updated**: 2025-01-11
**Status**: ✅ Ready for Development
**Dependencies**: Epic 0.2 complete, Story 1.2 in progress
