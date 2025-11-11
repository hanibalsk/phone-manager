# Story 1.3: UI-Service Integration

**Story ID**: 1.3
**Epic**: 1 - Location Tracking Core (UI Layer)
**Priority**: Must-Have
**Estimate**: 8 story points (5-7 days)
**Status**: Ready for Development
**Created**: 2025-01-11

---

## User Story

```
AS A user
I WANT to see real-time updates of my location tracking status and collected locations
SO THAT I know the feature is working correctly and have confidence in the system
```

---

## Business Value

- Provides transparency into location tracking activity
- Builds user confidence through real-time feedback
- Enables users to verify tracking is working correctly
- Helps troubleshoot location collection issues
- Reduces support requests about "is tracking working?"
- Improves user experience through responsive UI updates
- Demonstrates data collection in action

---

## Acceptance Criteria

### AC 1.3.1: Repository Pattern Enforcement
**Given** the UI layer needs to interact with the LocationTrackingService
**When** any UI component requests location data or service state
**Then** the request flows through the Repository pattern exclusively
**And** no UI component directly binds to or communicates with the Service
**And** the data flow is: UI → ViewModel → ServiceController → Service → Database → Repository → ViewModel → UI
**And** the Repository provides a clean abstraction over service and database operations

**Verification**: Code review confirms no direct service binding in UI code

---

### AC 1.3.2: Real-Time Location Count Updates
**Given** the LocationTrackingService is active and collecting locations
**When** a new location is captured and saved to the database
**Then** the UI displays the updated location count within 1 second
**And** the count increments by 1 for each new location
**And** the update happens automatically without user interaction
**And** the count is accurate (matches database query)

**Verification**: Enable tracking → Wait for location updates → Verify count increments within 1s

---

### AC 1.3.3: Last Update Timestamp Display
**Given** location tracking is active
**When** a new location is captured
**Then** the UI displays the timestamp of the last captured location
**And** the timestamp format is user-friendly (e.g., "2 minutes ago", "Just now")
**And** the timestamp auto-updates every minute (e.g., "2 min ago" → "3 min ago")
**And** the timestamp shows exact time when > 1 hour old
**And** the timestamp updates within 1 second of new location capture

**Verification**: Observe timestamp updating in real-time and format changing based on age

---

### AC 1.3.4: Service Health Indicator
**Given** the user is viewing the main screen
**When** the screen loads or service state changes
**Then** a ServiceStatusCard displays the current service health:
- 🟢 "Tracking Active" (green) - Service running, locations being collected
- 🟡 "GPS Acquiring..." (yellow) - Service running, waiting for GPS fix
- 🔴 "Tracking Stopped" (red) - Service not running
- ⚠️ "GPS Unavailable" (orange) - Location services disabled
- ❌ "Service Error" (red) - Service crashed or failed
**And** the indicator updates within 500ms of state change
**And** tapping the card shows detailed service information

**Verification**: Test all service states and verify correct indicators and timing

---

### AC 1.3.5: Foreground Notification Lifecycle
**Given** the user enables location tracking
**When** the LocationTrackingService starts
**Then** a foreground notification appears in the system tray within 1 second
**And** the notification displays:
- App icon
- Title: "Location Tracking Active"
- Body: Location count and last update time
- Action: "Stop Tracking" button
**And** tapping "Stop Tracking" stops the service and dismisses notification
**And** the notification cannot be dismissed by swiping
**And** when tracking is disabled, the notification disappears immediately

**Verification**: Toggle tracking → Verify notification lifecycle and content

---

### AC 1.3.6: State Synchronization After Process Death
**Given** location tracking is active
**When** the app is force-killed (swipe away from recents)
**Then** the service continues running in background
**And** when the app is reopened:
- The toggle shows the correct ON state within 100ms
- Location count displays current value from database
- Service health shows "Tracking Active"
- Last update timestamp is accurate
**And** no user interaction is required to restore UI state

**Verification**: Enable tracking → Force kill app → Verify service continues → Reopen app → Verify UI state restored

---

### AC 1.3.7: State Synchronization After Device Reboot
**Given** location tracking was active before device reboot
**When** the device restarts (if auto-start implemented)
**Then** the UI reflects the correct tracking state
**And** if service auto-started, toggle shows ON
**And** if service did not auto-start, toggle shows OFF (matches actual state)
**And** no desynchronization between UI state and service state occurs

**Verification**: Enable tracking → Reboot device → Verify UI state matches service state

---

### AC 1.3.8: Configuration Changes Without Service Restart
**Given** location tracking is active with a 5-minute interval
**When** the user changes the tracking interval to 10 minutes in settings
**Then** the new interval is applied to the running service within 500ms
**And** the service does NOT restart (no notification blink)
**And** the next location is collected at the new interval
**And** the UI shows "Configuration updated" confirmation
**And** the change persists across app restarts

**Verification**: Change interval while tracking → Verify no service restart → Verify new interval applied

---

### AC 1.3.9: Location Statistics Card Display
**Given** the user is on the main screen
**When** location tracking is or was active
**Then** a LocationStatsCard displays:
- Total locations collected today
- Total locations collected all-time
- Last location timestamp
- Current tracking interval
- Average accuracy (if available)
**And** statistics update in real-time as new locations are collected
**And** statistics persist and load correctly after app restart

**Verification**: Observe statistics card updating in real-time and persisting across restarts

---

### AC 1.3.10: Service Error Handling and Display
**Given** the LocationTrackingService encounters an error
**When** an error occurs (e.g., database write failure, location provider error)
**Then** the UI displays an error state within 1 second
**And** the error message is user-friendly and actionable:
- "GPS signal lost" → "Move to an area with better GPS signal"
- "Storage full" → "Free up device storage to continue tracking"
- "Location services disabled" → "Enable location services in Settings"
**And** the service health indicator shows error state (red/orange)
**And** errors are logged for debugging
**And** the service attempts recovery automatically when possible

**Verification**: Simulate errors (disable GPS, fill storage) → Verify error display and recovery

---

### AC 1.3.11: Real-Time UI Updates via Flow
**Given** multiple screens or components observe service state
**When** the service state changes (e.g., new location, state change, error)
**Then** all observing UI components receive updates via Kotlin Flow
**And** updates are received within 1 second of state change
**And** UI components automatically recompose with new state
**And** no polling or manual refresh is required
**And** Flow collectors are lifecycle-aware (stop when UI destroyed)

**Verification**: Code review confirms Flow usage; manual test shows real-time updates

---

### AC 1.3.12: Notification Content Updates
**Given** the foreground notification is displayed
**When** new locations are collected or service state changes
**Then** the notification content updates to reflect current state:
- Location count updates in real-time
- Last update timestamp refreshes
- Service status reflected (e.g., "GPS Acquiring...")
**And** notification updates do NOT trigger sound or vibration
**And** updates happen within 1 second of state change

**Verification**: Observe notification updating in real-time without sound/vibration

---

## Technical Details

### Architecture

**Pattern**: MVVM + Repository + Service Controller Abstraction

**Data Flow**:
```
┌─────────────────────────────────────────────────────────────────┐
│                            UI LAYER                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Compose UI Components                                   │  │
│  │  - LocationTrackingToggle                                │  │
│  │  - ServiceStatusCard                                     │  │
│  │  - LocationStatsCard                                     │  │
│  └──────────────────┬───────────────────────────────────────┘  │
│                     │ StateFlow                                 │
│  ┌──────────────────▼───────────────────────────────────────┐  │
│  │  LocationTrackingViewModel                              │  │
│  │  - observes: serviceState, locationStats               │  │
│  │  - commands: startTracking(), stopTracking()           │  │
│  └──────────────────┬───────────────────────────────────────┘  │
└─────────────────────┼─────────────────────────────────────────┘
                      │
┌─────────────────────▼─────────────────────────────────────────┐
│                     DOMAIN LAYER                               │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  LocationServiceController (Interface)                 │  │
│  │  - startTracking(): Result<Unit>                       │  │
│  │  - stopTracking(): Result<Unit>                        │  │
│  │  - updateInterval(minutes: Int)                        │  │
│  │  - observeServiceState(): Flow<ServiceState>           │  │
│  └──────────────────┬──────────────────────────────────────┘  │
└─────────────────────┼─────────────────────────────────────────┘
                      │
┌─────────────────────▼─────────────────────────────────────────┐
│                      DATA LAYER                                │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  LocationRepository (from Epic 0.2.3)                  │  │
│  │  - observeLocationCount(): Flow<Int>                   │  │
│  │  - observeLastLocation(): Flow<LocationEntity?>        │  │
│  │  - observeServiceHealth(): Flow<ServiceHealth>         │  │
│  └──────────────────┬──────────────────────────────────────┘  │
│                     │                                          │
│  ┌──────────────────▼──────────────────────────────────────┐  │
│  │  LocationTrackingService (from Epic 0.2.4)             │  │
│  │  - Collects locations                                  │  │
│  │  - Saves to database                                   │  │
│  │  - Broadcasts state updates                            │  │
│  └──────────────────┬──────────────────────────────────────┘  │
│                     │                                          │
│  ┌──────────────────▼──────────────────────────────────────┐  │
│  │  Room Database (from Epic 0.2.2)                       │  │
│  │  - LocationEntity table                                │  │
│  │  - LocationDao                                         │  │
│  └─────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

### Implementation Files

#### ServiceState Data Model
**File**: `app/src/main/java/com/phonemanager/domain/model/ServiceState.kt`

```kotlin
data class ServiceState(
    val isRunning: Boolean,
    val status: ServiceStatus,
    val lastUpdate: Instant?,
    val locationCount: Int,
    val currentInterval: Duration,
    val healthStatus: HealthStatus,
    val errorMessage: String? = null
)

enum class ServiceStatus {
    STOPPED,
    STARTING,
    RUNNING,
    GPS_ACQUIRING,
    STOPPING,
    ERROR
}

enum class HealthStatus {
    HEALTHY,           // Service running, locations being collected
    GPS_UNAVAILABLE,   // Location services disabled
    NO_GPS_SIGNAL,     // Service running but no GPS fix
    ERROR              // Service error state
}

data class LocationStats(
    val totalCount: Int,
    val todayCount: Int,
    val lastLocation: LocationEntity?,
    val averageAccuracy: Float?,
    val trackingInterval: Duration
)
```

---

#### LocationServiceController
**File**: `app/src/main/java/com/phonemanager/service/LocationServiceController.kt`

```kotlin
interface LocationServiceController {
    suspend fun startTracking(): Result<Unit>
    suspend fun stopTracking(): Result<Unit>
    suspend fun updateInterval(intervalMinutes: Int): Result<Unit>
    fun observeServiceState(): Flow<ServiceState>
    fun isServiceRunning(): Boolean
}

@Singleton
class LocationServiceControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val preferencesRepository: PreferencesRepository
) : LocationServiceController {

    private val _serviceState = MutableStateFlow(
        ServiceState(
            isRunning = false,
            status = ServiceStatus.STOPPED,
            lastUpdate = null,
            locationCount = 0,
            currentInterval = Duration.ofMinutes(5),
            healthStatus = HealthStatus.HEALTHY
        )
    )

    init {
        // Observe repository updates and update service state
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            combine(
                locationRepository.observeServiceHealth(),
                locationRepository.observeLocationCount(),
                locationRepository.observeLastLocation(),
                preferencesRepository.trackingInterval
            ) { health, count, lastLocation, interval ->
                ServiceState(
                    isRunning = health.isRunning,
                    status = mapHealthToStatus(health),
                    lastUpdate = lastLocation?.timestamp?.let { Instant.ofEpochMilli(it) },
                    locationCount = count,
                    currentInterval = Duration.ofMinutes(interval.toLong()),
                    healthStatus = health.healthStatus,
                    errorMessage = health.errorMessage
                )
            }.collect { state ->
                _serviceState.value = state
            }
        }
    }

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

            _serviceState.value = _serviceState.value.copy(
                status = ServiceStatus.STARTING
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start tracking service")
            _serviceState.value = _serviceState.value.copy(
                status = ServiceStatus.ERROR,
                healthStatus = HealthStatus.ERROR,
                errorMessage = e.message
            )
            Result.failure(e)
        }
    }

    override suspend fun stopTracking(): Result<Unit> {
        return try {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_STOP_TRACKING
            }
            context.startService(intent)

            _serviceState.value = _serviceState.value.copy(
                status = ServiceStatus.STOPPING
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop tracking service")
            Result.failure(e)
        }
    }

    override suspend fun updateInterval(intervalMinutes: Int): Result<Unit> {
        return try {
            preferencesRepository.setTrackingInterval(intervalMinutes)

            // Notify running service to update interval
            if (_serviceState.value.isRunning) {
                val intent = Intent(context, LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.ACTION_UPDATE_INTERVAL
                    putExtra(LocationTrackingService.EXTRA_INTERVAL_MINUTES, intervalMinutes)
                }
                context.startService(intent)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update tracking interval")
            Result.failure(e)
        }
    }

    override fun observeServiceState(): Flow<ServiceState> = _serviceState.asStateFlow()

    override fun isServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE).any { serviceInfo ->
            serviceInfo.service.className == LocationTrackingService::class.java.name
        }
    }

    private fun mapHealthToStatus(health: ServiceHealth): ServiceStatus {
        return when {
            !health.isRunning -> ServiceStatus.STOPPED
            health.healthStatus == HealthStatus.ERROR -> ServiceStatus.ERROR
            health.healthStatus == HealthStatus.GPS_ACQUIRING -> ServiceStatus.GPS_ACQUIRING
            health.healthStatus == HealthStatus.GPS_UNAVAILABLE -> ServiceStatus.ERROR
            else -> ServiceStatus.RUNNING
        }
    }
}
```

---

#### Enhanced LocationTrackingViewModel
**File**: `app/src/main/java/com/phonemanager/ui/main/LocationTrackingViewModel.kt` (enhanced from Story 1.1)

```kotlin
@HiltViewModel
class LocationTrackingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val serviceController: LocationServiceController,
    private val permissionManager: PermissionManager,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _trackingState = MutableStateFlow(TrackingState.Stopped)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Checking)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    // NEW: Service state from controller
    val serviceState: StateFlow<ServiceState> = serviceController.observeServiceState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ServiceState(
                isRunning = false,
                status = ServiceStatus.STOPPED,
                lastUpdate = null,
                locationCount = 0,
                currentInterval = Duration.ofMinutes(5),
                healthStatus = HealthStatus.HEALTHY
            )
        )

    // NEW: Location statistics
    val locationStats: StateFlow<LocationStats> = combine(
        locationRepository.observeLocationCount(),
        locationRepository.observeTodayLocationCount(),
        locationRepository.observeLastLocation(),
        locationRepository.observeAverageAccuracy(),
        preferencesRepository.trackingInterval
    ) { total, today, last, avgAccuracy, interval ->
        LocationStats(
            totalCount = total,
            todayCount = today,
            lastLocation = last,
            averageAccuracy = avgAccuracy,
            trackingInterval = Duration.ofMinutes(interval.toLong())
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LocationStats(
            totalCount = 0,
            todayCount = 0,
            lastLocation = null,
            averageAccuracy = null,
            trackingInterval = Duration.ofMinutes(5)
        )
    )

    init {
        viewModelScope.launch {
            // Load persisted toggle state
            preferencesRepository.isTrackingEnabled.collect { enabled ->
                // Reconcile with actual service state
                val isServiceActuallyRunning = serviceController.isServiceRunning()

                _trackingState.value = when {
                    enabled && isServiceActuallyRunning -> TrackingState.Active()
                    enabled && !isServiceActuallyRunning -> {
                        // State desync - service should be running but isn't
                        Timber.w("State desync detected: toggle ON but service not running")
                        TrackingState.Stopped
                    }
                    else -> TrackingState.Stopped
                }
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
                _trackingState.value = TrackingState.Active()
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

    // NEW: Update tracking interval
    suspend fun updateTrackingInterval(intervalMinutes: Int) {
        serviceController.updateInterval(intervalMinutes)
            .onFailure { error ->
                Timber.e(error, "Failed to update interval")
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

---

#### ServiceStatusCard
**File**: `app/src/main/java/com/phonemanager/ui/components/ServiceStatusCard.kt`

```kotlin
@Composable
fun ServiceStatusCard(
    serviceState: ServiceState,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (serviceState.status) {
                ServiceStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                ServiceStatus.GPS_ACQUIRING -> MaterialTheme.colorScheme.tertiaryContainer
                ServiceStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = when (serviceState.healthStatus) {
                                    HealthStatus.HEALTHY -> Color.Green
                                    HealthStatus.GPS_ACQUIRING -> Color.Yellow
                                    HealthStatus.GPS_UNAVAILABLE -> Color(0xFFFF9800) // Orange
                                    HealthStatus.ERROR -> Color.Red
                                },
                                shape = CircleShape
                            )
                    )

                    Text(
                        text = when (serviceState.status) {
                            ServiceStatus.STOPPED -> "Tracking Stopped"
                            ServiceStatus.STARTING -> "Starting..."
                            ServiceStatus.RUNNING -> "Tracking Active"
                            ServiceStatus.GPS_ACQUIRING -> "GPS Acquiring..."
                            ServiceStatus.STOPPING -> "Stopping..."
                            ServiceStatus.ERROR -> "Service Error"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when {
                        serviceState.errorMessage != null -> serviceState.errorMessage
                        serviceState.healthStatus == HealthStatus.GPS_UNAVAILABLE ->
                            "Location services are disabled"
                        serviceState.healthStatus == HealthStatus.GPS_ACQUIRING ->
                            "Waiting for GPS signal..."
                        serviceState.status == ServiceStatus.RUNNING ->
                            "Collecting locations every ${serviceState.currentInterval.toMinutes()} minutes"
                        else -> "Ready to start tracking"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Icon
            Icon(
                imageVector = when (serviceState.status) {
                    ServiceStatus.RUNNING -> Icons.Default.GpsFixed
                    ServiceStatus.GPS_ACQUIRING -> Icons.Default.GpsNotFixed
                    ServiceStatus.ERROR -> Icons.Default.Error
                    else -> Icons.Default.GpsOff
                },
                contentDescription = null,
                tint = when (serviceState.healthStatus) {
                    HealthStatus.HEALTHY -> MaterialTheme.colorScheme.primary
                    HealthStatus.ERROR -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ServiceStatusCardPreview() {
    PhoneManagerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ServiceStatusCard(
                serviceState = ServiceState(
                    isRunning = true,
                    status = ServiceStatus.RUNNING,
                    lastUpdate = Instant.now(),
                    locationCount = 42,
                    currentInterval = Duration.ofMinutes(5),
                    healthStatus = HealthStatus.HEALTHY
                )
            )

            ServiceStatusCard(
                serviceState = ServiceState(
                    isRunning = true,
                    status = ServiceStatus.GPS_ACQUIRING,
                    lastUpdate = null,
                    locationCount = 0,
                    currentInterval = Duration.ofMinutes(5),
                    healthStatus = HealthStatus.GPS_ACQUIRING
                )
            )

            ServiceStatusCard(
                serviceState = ServiceState(
                    isRunning = false,
                    status = ServiceStatus.ERROR,
                    lastUpdate = null,
                    locationCount = 0,
                    currentInterval = Duration.ofMinutes(5),
                    healthStatus = HealthStatus.ERROR,
                    errorMessage = "GPS signal lost"
                )
            )
        }
    }
}
```

---

#### LocationStatsCard
**File**: `app/src/main/java/com/phonemanager/ui/components/LocationStatsCard.kt`

```kotlin
@Composable
fun LocationStatsCard(
    locationStats: LocationStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Location Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Location count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = locationStats.todayCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = "All Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = locationStats.totalCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = "Interval",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${locationStats.trackingInterval.toMinutes()} min",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider()

            // Last update
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Last Update",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = locationStats.lastLocation?.let {
                            formatTimestamp(Instant.ofEpochMilli(it.timestamp))
                        } ?: "No locations yet",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                locationStats.averageAccuracy?.let { accuracy ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Avg Accuracy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "±${accuracy.toInt()}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                accuracy <= 10f -> Color.Green
                                accuracy <= 50f -> Color(0xFFFF9800)
                                else -> Color.Red
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun formatTimestamp(instant: Instant): String {
    val now = Instant.now()
    val duration = Duration.between(instant, now)

    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()} min ago"
        duration.toHours() < 24 -> "${duration.toHours()} hours ago"
        else -> {
            // Format as "Jan 11, 2:30 PM"
            val formatter = DateTimeFormatter.ofPattern("MMM dd, h:mm a")
            instant.atZone(ZoneId.systemDefault()).format(formatter)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocationStatsCardPreview() {
    PhoneManagerTheme {
        LocationStatsCard(
            locationStats = LocationStats(
                totalCount = 1234,
                todayCount = 42,
                lastLocation = LocationEntity(
                    id = 1,
                    latitude = 37.7749,
                    longitude = -122.4194,
                    accuracy = 12.5f,
                    timestamp = System.currentTimeMillis() - 300000, // 5 min ago
                    altitude = null,
                    bearing = null,
                    speed = null,
                    provider = "fused"
                ),
                averageAccuracy = 15.3f,
                trackingInterval = Duration.ofMinutes(5)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
```

---

#### Enhanced LocationTrackingService (Integration Points)
**File**: `app/src/main/java/com/phonemanager/service/LocationTrackingService.kt` (additions to Epic 0.2.4 service)

```kotlin
class LocationTrackingService : Service() {

    companion object {
        const val ACTION_START_TRACKING = "com.phonemanager.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.phonemanager.action.STOP_TRACKING"
        const val ACTION_UPDATE_INTERVAL = "com.phonemanager.action.UPDATE_INTERVAL"
        const val EXTRA_INTERVAL_MINUTES = "interval_minutes"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking_channel"
    }

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    private var currentLocationCount = 0
    private var lastUpdateTime: Instant? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                startForegroundTracking()
            }
            ACTION_STOP_TRACKING -> {
                stopTracking()
            }
            ACTION_UPDATE_INTERVAL -> {
                val newInterval = intent.getIntExtra(EXTRA_INTERVAL_MINUTES, 5)
                updateTrackingInterval(newInterval)
            }
        }

        return START_STICKY
    }

    private fun startForegroundTracking() {
        // Start foreground with notification
        startForeground(NOTIFICATION_ID, createNotification())

        // Begin location collection (Epic 0.2.4 logic)
        // ...

        // Observe location count for notification updates
        serviceScope.launch {
            locationRepository.observeLocationCount().collect { count ->
                currentLocationCount = count
                updateNotification()
            }
        }

        // Observe last location for notification updates
        serviceScope.launch {
            locationRepository.observeLastLocation().collect { location ->
                lastUpdateTime = location?.timestamp?.let { Instant.ofEpochMilli(it) }
                updateNotification()
            }
        }
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        // Stop tracking action
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText(getNotificationText())
            .setSmallIcon(R.drawable.ic_location_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_stop,
                "Stop Tracking",
                stopIntent
            )
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getNotificationText(): String {
        val lastUpdateText = lastUpdateTime?.let {
            val duration = Duration.between(it, Instant.now())
            when {
                duration.toMinutes() < 1 -> "Just now"
                duration.toMinutes() < 60 -> "${duration.toMinutes()} min ago"
                else -> "${duration.toHours()} hours ago"
            }
        } ?: "No locations yet"

        return "$currentLocationCount locations • Last update: $lastUpdateText"
    }

    private fun updateTrackingInterval(intervalMinutes: Int) {
        // Update location request interval without restarting service
        // Epic 0.2.4 implementation details
        Timber.d("Updating tracking interval to $intervalMinutes minutes")
    }

    private fun stopTracking() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
```

---

### Dependencies

**Internal Dependencies**:
- **Story 1.1**: LocationTrackingViewModel base (extended here)
- **Story 1.2**: PermissionManager (for permission checks)
- **Epic 0.2.2**: LocationEntity and LocationDao
- **Epic 0.2.3**: LocationRepository interface
- **Epic 0.2.4**: LocationTrackingService

**External Dependencies**:
```kotlin
// app/build.gradle.kts
dependencies {
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Jetpack Compose
    implementation("androidx.compose.runtime:runtime:1.6.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // Timber for logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-compiler:2.48.1")
}
```

---

## Testing Strategy

### Unit Tests

**File**: `app/src/test/java/com/phonemanager/service/LocationServiceControllerTest.kt`

```kotlin
@ExperimentalCoroutinesApi
class LocationServiceControllerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var locationRepository: LocationRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var serviceController: LocationServiceControllerImpl

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        locationRepository = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)

        every { locationRepository.observeServiceHealth() } returns flowOf(
            ServiceHealth(isRunning = false, healthStatus = HealthStatus.HEALTHY)
        )
        every { locationRepository.observeLocationCount() } returns flowOf(0)
        every { locationRepository.observeLastLocation() } returns flowOf(null)
        every { preferencesRepository.trackingInterval } returns flowOf(5)

        serviceController = LocationServiceControllerImpl(
            context,
            locationRepository,
            preferencesRepository
        )
    }

    @Test
    fun `startTracking starts foreground service and returns success`() = runTest {
        // When
        val result = serviceController.startTracking()

        // Then
        assertThat(result.isSuccess).isTrue()
        verify { context.startForegroundService(any()) }
    }

    @Test
    fun `observeServiceState emits updates when repository data changes`() = runTest {
        // Given
        val healthFlow = MutableStateFlow(
            ServiceHealth(isRunning = true, healthStatus = HealthStatus.HEALTHY)
        )
        every { locationRepository.observeServiceHealth() } returns healthFlow

        // When
        val states = mutableListOf<ServiceState>()
        val job = launch {
            serviceController.observeServiceState().take(2).collect { states.add(it) }
        }

        // Emit health update
        healthFlow.emit(
            ServiceHealth(isRunning = true, healthStatus = HealthStatus.GPS_ACQUIRING)
        )

        job.join()

        // Then
        assertThat(states).hasSize(2)
        assertThat(states[1].status).isEqualTo(ServiceStatus.GPS_ACQUIRING)
    }

    @Test
    fun `updateInterval updates preferences and notifies service`() = runTest {
        // Given
        coEvery { preferencesRepository.setTrackingInterval(10) } returns Unit

        // When
        val result = serviceController.updateInterval(10)

        // Then
        assertThat(result.isSuccess).isTrue()
        coVerify { preferencesRepository.setTrackingInterval(10) }
    }
}
```

---

### Integration Tests

**File**: `app/src/androidTest/java/com/phonemanager/ui/main/LocationTrackingIntegrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class LocationTrackingIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        grantLocationPermissions()
    }

    @Test
    fun toggleOnStartsService_statsCardUpdates() {
        // When - Toggle tracking ON
        composeTestRule.onNodeWithText("Location Tracking").performClick()

        // Then - Wait for service to start
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule
                .onAllNodesWithText("Tracking Active")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // And - Verify ServiceStatusCard shows running state
        composeTestRule.onNodeWithText("Tracking Active").assertIsDisplayed()

        // And - Verify notification appears
        val notificationManager = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        Thread.sleep(1000) // Give notification time to appear

        val activeNotifications = notificationManager.activeNotifications
        assertThat(activeNotifications).isNotEmpty()
        assertThat(activeNotifications[0].notification.extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo("Location Tracking Active")
    }

    @Test
    fun serviceStatePersistedAfterAppKill() {
        // Given - Start tracking
        composeTestRule.onNodeWithText("Location Tracking").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            isServiceRunning(LocationTrackingService::class.java)
        }

        // When - Simulate app kill and restart
        composeTestRule.activityRule.scenario.close()
        Thread.sleep(2000) // Service continues running
        composeTestRule.activityRule.scenario.recreate()

        // Then - Toggle shows correct ON state
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithText("Tracking Active")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun grantLocationPermissions() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.apply {
            grantRuntimePermission(
                InstrumentationRegistry.getInstrumentation().targetContext.packageName,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                grantRuntimePermission(
                    InstrumentationRegistry.getInstrumentation().targetContext.packageName,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            }
        }
    }
}
```

---

### Manual Testing Checklist

**Real-Time Updates**:
- [ ] Location count increments within 1 second of collection
- [ ] Last update timestamp updates within 1 second
- [ ] Timestamp auto-refreshes every minute (e.g., "2 min ago" → "3 min ago")
- [ ] Service health indicator updates within 500ms

**State Synchronization**:
- [ ] Toggle state restored after app force-kill
- [ ] Location stats persist after app restart
- [ ] Service continues running when app killed
- [ ] UI reflects correct state on app reopen
- [ ] No desynchronization between toggle and service

**Configuration Changes**:
- [ ] Interval change applied without service restart
- [ ] No notification blink when interval changes
- [ ] New interval takes effect for next location
- [ ] Configuration persists across app restarts

**Notification Lifecycle**:
- [ ] Notification appears within 1s of service start
- [ ] Notification displays location count
- [ ] Notification shows last update time
- [ ] Notification updates in real-time (no sound/vibration)
- [ ] "Stop Tracking" button works
- [ ] Notification dismissed when service stops

**Error Handling**:
- [ ] GPS disabled → Shows "GPS Unavailable" warning
- [ ] GPS weak signal → Shows "GPS Acquiring..."
- [ ] Service error → Shows error message with actionable guidance
- [ ] Errors displayed within 1 second

**Performance**:
- [ ] UI updates within 1 second for all state changes
- [ ] No UI lag when location count updates
- [ ] Smooth scrolling with real-time updates
- [ ] Memory usage < 10MB for UI state management

---

## Definition of Done

- [ ] LocationServiceController abstracts service lifecycle
- [ ] ServiceStatusCard displays all health states correctly
- [ ] LocationStatsCard shows real-time statistics
- [ ] Foreground notification managed by service
- [ ] Repository pattern enforced (no direct service binding)
- [ ] Real-time UI updates via Kotlin Flow
- [ ] Location count updates within 1 second
- [ ] Service health updates within 500ms
- [ ] Configuration changes applied without service restart
- [ ] State synchronized after app kill and reopen
- [ ] Notification content updates in real-time
- [ ] Error states displayed with actionable messages
- [ ] Unit tests achieve > 80% coverage
- [ ] Integration tests pass for complete flow
- [ ] Manual testing completed on Android 8, 10, 13, 14
- [ ] Performance targets met (< 1s updates)
- [ ] Memory usage < 10MB for state management
- [ ] Code review approved
- [ ] Documentation complete

---

## Risks & Mitigations

**RISK**: State Desynchronization Between UI and Service
- **Impact**: Toggle shows ON but service not running (confusing users)
- **Probability**: Medium
- **Mitigation**:
  - Health check on app resume reconciles state
  - Service broadcasts lifecycle events
  - UI queries actual service state, not just DataStore
- **Contingency**: Add manual "Refresh Status" button

**RISK**: Real-Time Updates Cause Performance Issues
- **Impact**: UI laggy, battery drain from frequent updates
- **Probability**: Low
- **Mitigation**:
  - Use StateFlow with replay=1 to prevent backpressure
  - Database queries optimized with indices
  - Updates throttled to 1s maximum frequency
- **Contingency**: Add user setting to reduce update frequency

**RISK**: Notification Updates Trigger Sound/Vibration
- **Impact**: Annoying notifications every time count updates
- **Probability**: Medium
- **Mitigation**:
  - Use IMPORTANCE_LOW notification channel
  - Set notification updates with setOnlyAlertOnce(true)
  - Test on multiple Android versions
- **Contingency**: Add user setting to disable notification updates

**RISK**: Service Killed by Battery Optimization
- **Impact**: Service stops but UI still shows "Active"
- **Probability**: Medium (manufacturer-specific)
- **Mitigation**:
  - Health check detects missing service
  - Guide user to whitelist app
  - WorkManager backup for critical tasks
- **Contingency**: Display warning when battery optimization detected

---

## Related Stories

- **Story 1.1**: Tracking Toggle (integrated with this story's components)
- **Story 1.2**: Permission Flow (permissions checked before starting service)
- **Epic 0.2.2**: LocationEntity and LocationDao (data source)
- **Epic 0.2.3**: LocationRepository interface (abstraction layer)
- **Epic 0.2.4**: LocationTrackingService (service lifecycle)

---

**Last Updated**: 2025-01-11
**Status**: ✅ Ready for Development
**Dependencies**: Epic 0.2 complete, Stories 1.1 and 1.2 in progress

