# Story 1.2: Background Location Service with Foreground Notification

**Status:** Ready for Implementation
**Epic:** 1 - Background Location Tracking Service
**Priority:** MVP - Critical Path
**Complexity:** High
**Estimated Effort:** 5-8 days

## Story

As a user,
I want the app to continuously track my location in the background,
so that my location data is captured even when I'm not actively using the app.

## Acceptance Criteria

1. **Foreground Service:**
   - [ ] Service starts as foreground service with persistent notification
   - [ ] Service type declared as "location" in manifest (Android 14+)
   - [ ] Service calls `startForeground()` within 10 seconds of creation
   - [ ] Service uses `START_STICKY` to restart if killed
   - [ ] Service stops cleanly when tracking disabled

2. **Location Tracking:**
   - [ ] FusedLocationProviderClient integrated and configured
   - [ ] Location updates received at configurable interval (default: 5 minutes)
   - [ ] Location data includes: latitude, longitude, accuracy, timestamp, altitude, bearing, speed, provider
   - [ ] Service continues running when app is closed/in background
   - [ ] Service handles location provider availability changes
   - [ ] Service handles permission revocation gracefully

3. **Notification:**
   - [ ] Notification channel created (API 26+)
   - [ ] Persistent notification displays service status
   - [ ] Notification shows last update time
   - [ ] Notification cannot be dismissed while service running
   - [ ] Notification includes action to stop tracking
   - [ ] Tap notification opens app

4. **Location Configuration:**
   - [ ] Support three accuracy modes: High (GPS), Balanced (GPS+Network), Low Power (Network)
   - [ ] Configurable update interval (1 minute to 1 hour, default: 5 minutes)
   - [ ] Configurable minimum displacement (default: 10 meters)
   - [ ] Handle location provider errors gracefully

5. **Battery Optimization:**
   - [ ] Use balanced power accuracy mode by default
   - [ ] Implement location request batching where possible
   - [ ] Reduce update frequency when device stationary (if Activity Recognition available)
   - [ ] Battery usage monitored and logged

6. **Testing:**
   - [ ] Unit tests for service lifecycle
   - [ ] Integration tests with FusedLocationProviderClient
   - [ ] Manual testing: background operation for 24+ hours
   - [ ] Battery monitoring over 24-hour period
   - [ ] Tested on Android 10, 12, 14

## Tasks / Subtasks

### Task 1: Add Dependencies
- [ ] Add Google Play Services Location dependency:
  ```kotlin
  implementation("com.google.android.gms:play-services-location:21.1.0")
  ```
- [ ] Add coroutines support for location API:
  ```kotlin
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
  ```

### Task 2: Create Domain Models
- [ ] Create `Location` domain model in `domain/model/Location.kt`:
  ```kotlin
  data class Location(
      val latitude: Double,
      val longitude: Double,
      val accuracy: Float,
      val timestamp: Long,
      val altitude: Double?,
      val bearing: Float?,
      val speed: Float?,
      val provider: String
  )
  ```
- [ ] Create `LocationTrackingConfig` model:
  ```kotlin
  data class LocationTrackingConfig(
      val updateInterval: Long = 5 * 60 * 1000L, // 5 minutes
      val fastestInterval: Long = 1 * 60 * 1000L, // 1 minute
      val minDisplacement: Float = 10f, // meters
      val priority: LocationPriority = LocationPriority.BALANCED
  )

  enum class LocationPriority {
      HIGH_ACCURACY,
      BALANCED_POWER_ACCURACY,
      LOW_POWER
  }
  ```

### Task 3: Create Location Data Source
- [ ] Create `LocationDataSource` interface in `data/source/`:
  ```kotlin
  interface LocationDataSource {
      fun startLocationUpdates(config: LocationTrackingConfig): Flow<Location>
      fun stopLocationUpdates()
      fun getLastLocation(): Location?
      fun isLocationAvailable(): Boolean
  }
  ```
- [ ] Implement `FusedLocationDataSource`:
  - Initialize FusedLocationProviderClient
  - Create LocationRequest from config
  - Implement LocationCallback
  - Handle permission checks
  - Convert Android Location to domain Location
  - Manage location update lifecycle

### Task 4: Create Location Repository
- [ ] Create `LocationRepository` interface in `domain/repository/`:
  ```kotlin
  interface LocationRepository {
      fun startTracking(config: LocationTrackingConfig): Flow<Result<Location>>
      fun stopTracking()
      suspend fun getLastKnownLocation(): Result<Location>
      fun isTrackingActive(): Flow<Boolean>
  }
  ```
- [ ] Implement `LocationRepositoryImpl` in `data/repository/`:
  - Delegate to LocationDataSource
  - Wrap results in Result type
  - Handle errors and exceptions
  - Manage tracking state

### Task 5: Create Use Cases
- [ ] Create `StartLocationTrackingUseCase`:
  ```kotlin
  class StartLocationTrackingUseCase @Inject constructor(
      private val locationRepository: LocationRepository,
      private val configRepository: ConfigRepository
  ) {
      operator fun invoke(): Flow<Result<Location>> {
          val config = configRepository.getTrackingConfig()
          return locationRepository.startTracking(config)
      }
  }
  ```
- [ ] Create `StopLocationTrackingUseCase`
- [ ] Create `GetLastKnownLocationUseCase`

### Task 6: Implement Foreground Service
- [ ] Create `LocationTrackingService` in `data/service/`:
  - Extend `Service` class
  - Inject dependencies with Hilt
  - Create notification channel in onCreate
  - Implement onStartCommand with START_STICKY
  - Call startForeground() with notification
  - Initialize location tracking on start
  - Stop location tracking on destroy
  - Handle ACTION_START_SERVICE and ACTION_STOP_SERVICE intents

- [ ] Create notification helper:
  ```kotlin
  private fun createNotification(): Notification {
      return NotificationCompat.Builder(this, CHANNEL_ID)
          .setContentTitle("Phone Manager")
          .setContentText("Tracking location...")
          .setSmallIcon(R.drawable.ic_location)
          .setOngoing(true)
          .setPriority(NotificationCompat.PRIORITY_LOW)
          .setCategory(NotificationCompat.CATEGORY_SERVICE)
          .setContentIntent(createPendingIntent())
          .addAction(createStopAction())
          .build()
  }
  ```

- [ ] Update notification with last location time:
  ```kotlin
  private fun updateNotification(location: Location) {
      val notification = createNotification().apply {
          setContentText("Last update: ${formatTime(location.timestamp)}")
      }
      notificationManager.notify(NOTIFICATION_ID, notification)
  }
  ```

### Task 7: Update AndroidManifest.xml
- [ ] Add permissions:
  ```xml
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  ```
- [ ] Declare service:
  ```xml
  <service
      android:name=".feature.location.data.service.LocationTrackingService"
      android:enabled="true"
      android:exported="false"
      android:foregroundServiceType="location" />
  ```

### Task 8: Create Service Manager
- [ ] Create `LocationServiceManager` to control service:
  ```kotlin
  class LocationServiceManager @Inject constructor(
      @ApplicationContext private val context: Context
  ) {
      fun startService() {
          val intent = Intent(context, LocationTrackingService::class.java).apply {
              action = ACTION_START_SERVICE
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              context.startForegroundService(intent)
          } else {
              context.startService(intent)
          }
      }

      fun stopService() {
          val intent = Intent(context, LocationTrackingService::class.java).apply {
              action = ACTION_STOP_SERVICE
          }
          context.startService(intent)
      }
  }
  ```

### Task 9: Create ViewModel (if UI needed)
- [ ] Create `LocationTrackingViewModel`:
  ```kotlin
  @HiltViewModel
  class LocationTrackingViewModel @Inject constructor(
      private val startTrackingUseCase: StartLocationTrackingUseCase,
      private val stopTrackingUseCase: StopLocationTrackingUseCase,
      private val locationRepository: LocationRepository
  ) : ViewModel() {

      val isTracking = locationRepository.isTrackingActive()
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

      private val _lastLocation = MutableStateFlow<Location?>(null)
      val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

      fun startTracking() {
          viewModelScope.launch {
              startTrackingUseCase()
                  .collect { result ->
                      when (result) {
                          is Result.Success -> _lastLocation.value = result.data
                          is Result.Error -> handleError(result.message)
                      }
                  }
          }
      }

      fun stopTracking() {
          stopTrackingUseCase()
      }
  }
  ```

### Task 10: Implement Dependency Injection
- [ ] Create `LocationDataModule`:
  ```kotlin
  @Module
  @InstallIn(SingletonComponent::class)
  object LocationDataModule {

      @Provides
      @Singleton
      fun provideFusedLocationProviderClient(
          @ApplicationContext context: Context
      ): FusedLocationProviderClient {
          return LocationServices.getFusedLocationProviderClient(context)
      }

      @Provides
      @Singleton
      fun provideLocationDataSource(
          fusedLocationClient: FusedLocationProviderClient
      ): LocationDataSource {
          return FusedLocationDataSource(fusedLocationClient)
      }

      @Provides
      @Singleton
      fun provideLocationRepository(
          locationDataSource: LocationDataSource
      ): LocationRepository {
          return LocationRepositoryImpl(locationDataSource)
      }
  }
  ```

- [ ] Create `LocationServiceModule` for service injection:
  ```kotlin
  @Module
  @InstallIn(ServiceComponent::class)
  object LocationServiceModule {
      // Service-scoped dependencies
  }
  ```

### Task 11: Testing
- [ ] Write unit tests:
  - `LocationTrackingServiceTest`: Test lifecycle, notification, foreground behavior
  - `FusedLocationDataSourceTest`: Test location updates with mock FusedLocationProviderClient
  - `LocationRepositoryImplTest`: Test repository with mocked data source
  - `StartLocationTrackingUseCaseTest`: Test use case logic

- [ ] Write integration tests:
  - `LocationServiceIntegrationTest`: Test service starts, receives locations, stops correctly
  - Test permission scenarios
  - Test service survival after app kill

- [ ] Manual testing checklist:
  - [ ] Service runs in background for 24+ hours
  - [ ] Battery usage measured (<5% per day target)
  - [ ] Location updates received at correct intervals
  - [ ] Notification displays and updates correctly
  - [ ] Service survives app being swiped from recents
  - [ ] Service handles airplane mode
  - [ ] Service handles GPS off/on
  - [ ] Test on multiple devices (Samsung, Pixel, Xiaomi)
  - [ ] Test on Android 10, 12, 14

### Task 12: Battery Optimization
- [ ] Implement intelligent update intervals based on battery level:
  ```kotlin
  private fun getAdaptiveInterval(batteryLevel: Int): Long {
      return when {
          batteryLevel > 50 -> 5 * 60 * 1000L  // 5 minutes
          batteryLevel > 20 -> 10 * 60 * 1000L // 10 minutes
          else -> 15 * 60 * 1000L               // 15 minutes
      }
  }
  ```

- [ ] Implement location batching:
  ```kotlin
  val locationRequest = LocationRequest.Builder(
      Priority.PRIORITY_BALANCED_POWER_ACCURACY,
      updateInterval
  ).apply {
      setMinUpdateIntervalMillis(fastestInterval)
      setMaxUpdateDelayMillis(updateInterval * 2) // Batch up to 2 intervals
      setWaitForAccurateLocation(true)
  }.build()
  ```

- [ ] Monitor battery usage and log metrics

### Task 13: Error Handling
- [ ] Handle location unavailable (GPS off, no permission)
- [ ] Handle provider changes (GPS to Network)
- [ ] Handle service killed by system (START_STICKY ensures restart)
- [ ] Handle low memory conditions (onTrimMemory)
- [ ] Log all errors with proper context

### Task 14: Documentation
- [ ] Add KDoc to all public APIs
- [ ] Document service lifecycle in comments
- [ ] Create sequence diagram for location tracking flow
- [ ] Update architecture documentation

## Technical Details

### FusedLocationProviderClient Configuration

```kotlin
class FusedLocationDataSource @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationDataSource {

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates(
        config: LocationTrackingConfig
    ): Flow<Location> = callbackFlow {

        val locationRequest = LocationRequest.Builder(
            config.priority.toPriority(),
            config.updateInterval
        ).apply {
            setMinUpdateIntervalMillis(config.fastestInterval)
            setMinUpdateDistanceMeters(config.minDisplacement)
            setMaxUpdateDelayMillis(config.updateInterval * 2)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { androidLocation ->
                    trySend(androidLocation.toDomainModel())
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    // Handle location unavailable
                    Log.w(TAG, "Location unavailable")
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )

        awaitClose {
            stopLocationUpdates()
        }
    }

    override fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }
}
```

### Priority Mapping

```kotlin
private fun LocationPriority.toPriority(): Int {
    return when (this) {
        LocationPriority.HIGH_ACCURACY -> Priority.PRIORITY_HIGH_ACCURACY
        LocationPriority.BALANCED_POWER_ACCURACY -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
        LocationPriority.LOW_POWER -> Priority.PRIORITY_LOW_POWER
    }
}
```

### Android Location to Domain Model

```kotlin
private fun android.location.Location.toDomainModel(): Location {
    return Location(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        timestamp = time,
        altitude = if (hasAltitude()) altitude else null,
        bearing = if (hasBearing()) bearing else null,
        speed = if (hasSpeed()) speed else null,
        provider = provider ?: "unknown"
    )
}
```

## Definition of Done

- [ ] All acceptance criteria met
- [ ] Code review completed
- [ ] Unit tests written and passing (>80% coverage)
- [ ] Integration tests passing
- [ ] Manual 24-hour background test completed
- [ ] Battery usage measured and acceptable (<5% per day)
- [ ] Tested on Android 10, 12, 14
- [ ] Tested on multiple OEM devices
- [ ] Documentation updated (KDoc, architecture docs)
- [ ] No lint warnings
- [ ] Service meets Android 14 foreground service requirements
- [ ] Performance profiled with Android Studio

## Dependencies

**Blocks:**
- Story 1.3 (location data model needed for persistence)
- Story 1.6 (service needed for boot receiver)

**Blocked By:**
- Story 1.1 (requires permission handling) ✅ Must Complete First

## Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Excessive battery drain | High | Critical | Use balanced power mode, implement adaptive intervals, extensive testing |
| Service killed by system | Medium | High | Use START_STICKY, foreground service, handle restart gracefully |
| Location accuracy issues | Medium | Medium | Multiple priority modes, handle provider changes |
| Android 14 restrictions | Medium | High | Declare foreground service type, follow guidelines, test thoroughly |
| OEM battery optimization | High | High | User education, test on multiple OEMs, provide whitelist instructions |

## Testing Strategy

### Unit Tests
- `LocationTrackingServiceTest`: Lifecycle, notification creation, intent handling
- `FusedLocationDataSourceTest`: Location update flow, error handling
- `LocationRepositoryImplTest`: Repository logic with mocked data source

### Integration Tests
- `LocationServiceIntegrationTest`: Full service lifecycle with real dependencies
- Test with TestLocationProviderClient for deterministic location data

### Performance Tests
- Battery monitoring over 24+ hours using Battery Historian
- Memory profiling with Android Studio Profiler
- Measure wakelock usage

### Manual Tests
- Background operation (app not visible for hours)
- Airplane mode transitions
- GPS on/off scenarios
- Different accuracy modes
- Service restart after force kill
- Multiple device manufacturers
- Android 10, 12, 14 versions

## Notes

### Android 14 Foreground Service Requirements
- Must declare `android:foregroundServiceType="location"`
- Must call `startForeground()` within 10 seconds
- Must show user-visible notification
- Cannot be dismissed by user while service running

### Battery Optimization Best Practices
1. Use `PRIORITY_BALANCED_POWER_ACCURACY` by default
2. Enable location batching with `setMaxUpdateDelayMillis()`
3. Set appropriate `minUpdateDistanceMeters` to avoid updates when stationary
4. Consider Activity Recognition API to detect stationary state
5. Reduce frequency when battery low
6. Monitor wakelock usage

### Service Lifecycle
```
START_SERVICE → onCreate() → onStartCommand() → startForeground()
              → Location updates begin
              → Service runs indefinitely (START_STICKY)
STOP_SERVICE  → onDestroy() → Remove location updates → stopForeground()
```

## References

- [FusedLocationProviderClient](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient)
- [Foreground Services](https://developer.android.com/develop/background-work/services/foreground-services)
- [Request Location Updates](https://developer.android.com/training/location/request-updates)
- [Battery Optimization](https://developer.android.com/topic/performance/vitals/bg-power)
- BMAD Technical Evaluation Report
- `/home/user/phone-manager/ARCHITECTURE.md`

---

**Story Created:** 2025-10-30
**Created By:** BMAD Epic Optimizer
**Epic:** [Epic 1: Background Location Tracking Service](../epics/epic-1-location-tracking.md)
**Previous Story:** [Story 1.1: Permission Management](./story-1.1.md)
**Next Story:** [Story 1.3: Local Data Persistence](./story-1.3.md)
