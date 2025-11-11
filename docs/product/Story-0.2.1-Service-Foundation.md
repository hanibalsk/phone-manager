# Epic 0.2.1: Service Foundation & Location Permissions

**Epic ID**: 0.2.1
**Parent Epic**: [Epic 0.2: Background Location Tracking Service](./Epic-0.2-Location-Tracking-Service.md)
**Status**: Ready for Development
**Priority**: Critical (MVP Blocker)
**Estimated Duration**: 3-5 days
**Dependencies**: None (First epic in sequence)

---

## Epic Goal

Establish the foundational Android Service infrastructure and obtain device location through proper permission handling. This epic proves we can run a background service and access location data - the core technical prerequisites for the entire location tracking system.

---

## Epic Scope

This epic focuses exclusively on:
- Creating a stable foreground service
- Displaying a persistent notification
- Handling location permissions properly
- Successfully retrieving a single device location

What's NOT in scope:
- Continuous location updates (Epic 0.2.2)
- Network transmission (Epic 0.2.2)
- Auto-start on boot (Epic 0.2.4)
- Offline queue (Epic 0.2.3)

---

## Stories

### Story 0.2.1.1: Create Android Foreground Service

**Story ID**: 0.2.1.1
**Priority**: Critical
**Estimate**: 1 day
**Assigned To**: TBD

#### User Story
```
AS A system developer
I WANT to create a basic Android foreground service
SO THAT I have the foundation for continuous background location tracking
```

#### Acceptance Criteria
- [ ] Service class `LocationTrackingService` created extending Android `Service`
- [ ] Service registered in `AndroidManifest.xml` with required permissions
- [ ] `onCreate()`, `onStartCommand()`, and `onDestroy()` lifecycle methods implemented
- [ ] Service can be started via `startService()` intent
- [ ] Service runs in foreground mode (not background)
- [ ] Service logs lifecycle events for debugging
- [ ] Unit tests for service lifecycle methods
- [ ] **Android 14+**: `FOREGROUND_SERVICE_LOCATION` permission declared in manifest
- [ ] **Android 14+**: `foregroundServiceType="location"` attribute set on service
- [ ] **Android 13+**: `POST_NOTIFICATIONS` permission declared in manifest
- [ ] **Android 13+**: Runtime notification permission request implemented
- [ ] Service tested on Android 8, 10, 12, 13, and 14

#### Technical Details

**File**: `app/src/main/java/com/phonemanager/service/LocationTrackingService.kt`

```kotlin
class LocationTrackingService : Service() {

    companion object {
        const val SERVICE_ID = 1001
        private const val TAG = "LocationTrackingService"
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("$TAG: Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("$TAG: Service started")

        // Will be implemented in Story 0.2.1.2
        // startForeground(SERVICE_ID, createNotification())

        return START_STICKY // Restart service if killed
    }

    override fun onDestroy() {
        Timber.d("$TAG: Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't support binding
    }
}
```

**Manifest Entry**: `app/src/main/AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Foreground service permissions -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <!-- Android 14+ (API 34+) requires explicit foreground service type permission -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

    <!-- Android 13+ (API 33+) requires notification permission -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application>
        <service
            android:name=".service.LocationTrackingService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="location" />
            <!-- CRITICAL for Android 14+: foregroundServiceType MUST be declared -->
            <!-- This matches the FOREGROUND_SERVICE_LOCATION permission above -->
    </application>
</manifest>
```

**Android Version Specific Requirements**:

| Android Version | API Level | Required Permissions | Notes |
|----------------|-----------|----------------------|-------|
| Android 8.0+ | 26+ | FOREGROUND_SERVICE | Service must call startForeground() within 5 seconds |
| Android 13+ | 33+ | POST_NOTIFICATIONS | Must request at runtime for notification display |
| Android 14+ | 34+ | FOREGROUND_SERVICE_LOCATION | Required for location-based foreground services |

**Runtime Permission Handling for POST_NOTIFICATIONS** (Android 13+):

```kotlin
// Add to TestActivity or permission handler
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun requestNotificationPermission() {
    if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE
        )
    }
}

companion object {
    private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002
}
```

#### Dependencies
- Timber logging library (add to `build.gradle.kts`)

#### Testing Strategy
- Unit test for `onStartCommand()` return value (START_STICKY)
- Unit test for service creation
- Manual test: Start service via ADB: `adb shell am start-foreground-service com.phonemanager/.service.LocationTrackingService`

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing
- [ ] Service can be started manually via ADB
- [ ] Service appears in running services list
- [ ] Logs show proper lifecycle events
- [ ] No memory leaks detected

---

### Story 0.2.1.2: Implement Foreground Notification

**Story ID**: 0.2.1.2
**Priority**: Critical
**Estimate**: 0.5 days
**Assigned To**: TBD
**Depends On**: 0.2.1.1

#### User Story
```
AS AN Android system
I REQUIRE a persistent notification when a foreground service is running
SO THAT users are aware of background location tracking
```

#### Acceptance Criteria
- [ ] Notification channel created for location tracking
- [ ] Notification builder creates appropriate notification
- [ ] Notification displays app icon, title, and description
- [ ] Notification cannot be dismissed by user
- [ ] Service promoted to foreground with notification
- [ ] Notification appears in system tray when service starts
- [ ] Notification follows Material Design guidelines

#### Technical Details

**Implementation**:

```kotlin
class LocationTrackingService : Service() {

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW // Low importance = no sound
            ).apply {
                description = "Ongoing location tracking service"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText("Phone Manager is tracking your location")
            .setSmallIcon(R.drawable.ic_location_notification)
            .setOngoing(true) // Cannot be dismissed
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(SERVICE_ID, createNotification())

        return START_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val SERVICE_ID = 1001
    }
}
```

**Resources Needed**:
- `res/drawable/ic_location_notification.xml` - Notification icon (24dp)
- Update `res/values/strings.xml` with notification strings

#### Testing Strategy
- Manual test: Verify notification appears in tray
- Manual test: Verify notification cannot be dismissed
- Manual test: Verify notification icon displays correctly
- Device test: Test on Android 8, 10, 12, 14

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Notification icon designed and added
- [ ] Notification appears when service starts
- [ ] Notification matches design specifications
- [ ] Works on Android 8-14
- [ ] Strings externalized for localization

---

### Story 0.2.1.3: Add Location Permission Handling

**Story ID**: 0.2.1.3
**Priority**: Critical
**Estimate**: 1 day
**Assigned To**: TBD
**Depends On**: 0.2.1.1

#### User Story
```
AS A location tracking service
I WANT to properly request and check location permissions
SO THAT I can legally and securely access device location data
```

#### Acceptance Criteria
- [ ] Location permissions declared in manifest
- [ ] Permission checking utility created
- [ ] Runtime permission request mechanism implemented (for testing)
- [ ] Permission state properly handled (granted/denied)
- [ ] Background location permission handled for Android 10+
- [ ] Service handles permission denial gracefully
- [ ] Appropriate permission rationale provided

#### Technical Details

**Manifest Permissions**: `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

**Permission Utility**: `app/src/main/java/com/phonemanager/util/PermissionUtil.kt`

```kotlin
object PermissionUtil {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true // Not required on Android 9 and below
    }

    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasLocationPermission(context) && hasBackgroundLocationPermission(context)
    }
}
```

**Test Activity** (for MVP testing): `app/src/main/java/com/phonemanager/TestActivity.kt`

```kotlin
class TestActivity : ComponentActivity() {

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // Location permission granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestBackgroundLocationPermission()
                } else {
                    startLocationService()
                }
            }
            else -> {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Button(onClick = { requestPermissionsAndStartService() }) {
                Text("Start Location Tracking")
            }
        }
    }

    private fun requestPermissionsAndStartService() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundRequest = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    startLocationService()
                }
            }
            backgroundRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
```

#### Testing Strategy
- Unit tests for `PermissionUtil` methods
- Manual test: Grant permissions and verify service starts
- Manual test: Deny permissions and verify graceful handling
- Device test: Test permission flow on Android 9, 10, 12, 14

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing
- [ ] Permission requests work on all Android versions
- [ ] Service handles denied permissions gracefully
- [ ] Test activity allows manual permission granting
- [ ] ADB command documented for granting permissions programmatically

---

### Story 0.2.1.4: Integrate Location Provider

**Story ID**: 0.2.1.4
**Priority**: Critical
**Estimate**: 1 day
**Assigned To**: TBD
**Depends On**: 0.2.1.3

#### User Story
```
AS A location tracking service
I WANT to integrate with Google Play Services Location API
SO THAT I can access the device's location efficiently and accurately
```

#### Acceptance Criteria
- [ ] Google Play Services Location dependency added
- [ ] FusedLocationProviderClient initialized in service
- [ ] Location settings verified (GPS enabled)
- [ ] Location provider availability checked
- [ ] Last known location retrieved successfully
- [ ] Appropriate error handling for unavailable location
- [ ] Service checks for location provider state

#### Technical Details

**Dependencies**: `app/build.gradle.kts`

```kotlin
dependencies {
    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
}
```

**Location Manager**: `app/src/main/java/com/phonemanager/location/LocationManager.kt`

```kotlin
class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    suspend fun getLastKnownLocation(): Location? {
        if (!PermissionUtil.hasLocationPermission(context)) {
            Timber.w("Location permission not granted")
            return null
        }

        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get last known location")
            null
        }
    }

    companion object {
        private const val TAG = "LocationManager"
    }
}

// Extension function for Tasks to suspend functions
suspend fun <T> Task<T>.await(): T? {
    return suspendCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            Timber.e(exception, "Task failed")
            continuation.resume(null)
        }
    }
}
```

**Service Integration**:

```kotlin
class LocationTrackingService : Service() {

    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()
        locationManager = LocationManager(this)

        if (!locationManager.isLocationEnabled()) {
            Timber.w("Location services are disabled")
        }
    }
}
```

#### Testing Strategy
- Unit tests for `LocationManager` methods (with mocked location client)
- Integration test with real location provider
- Manual test: Verify GPS on/off handling
- Manual test: Test with location mocked via ADB

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing
- [ ] Integration tests passing
- [ ] GPS enabled/disabled states handled
- [ ] Last known location retrieved successfully
- [ ] Works on devices with and without GPS

---

### Story 0.2.1.5: Implement Single Location Capture

**Story ID**: 0.2.1.5
**Priority**: Critical
**Estimate**: 1 day
**Assigned To**: TBD
**Depends On**: 0.2.1.4

#### User Story
```
AS A location tracking service
I WANT to capture a single device location with timestamp and accuracy
SO THAT I can prove the location capture mechanism works
```

#### Acceptance Criteria
- [ ] Location data class defined with all required fields
- [ ] One-time location request implemented
- [ ] Location captured with timestamp in UTC
- [ ] Location includes accuracy and provider information
- [ ] Location data logged for verification
- [ ] Location capture triggered when service starts
- [ ] Timeout handling if location not available

#### Technical Details

**Location Data Model**: `app/src/main/java/com/phonemanager/data/model/LocationData.kt`

```kotlin
@Parcelize
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long, // Unix timestamp in milliseconds (UTC)
    val accuracy: Float, // Accuracy in meters
    val altitude: Double? = null,
    val bearing: Float? = null,
    val speed: Float? = null,
    val provider: String? = null,
    val capturedAt: String = Instant.ofEpochMilli(timestamp).toString() // ISO 8601 format
) : Parcelable {

    companion object {
        fun fromLocation(location: Location): LocationData {
            return LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = location.time,
                accuracy = location.accuracy,
                altitude = if (location.hasAltitude()) location.altitude else null,
                bearing = if (location.hasBearing()) location.bearing else null,
                speed = if (location.hasSpeed()) location.speed else null,
                provider = location.provider
            )
        }
    }

    fun isAccurate(threshold: Float = 50f): Boolean {
        return accuracy <= threshold
    }
}
```

**Location Manager Enhancement**:

```kotlin
class LocationManager(private val context: Context) {

    suspend fun getCurrentLocation(timeoutMillis: Long = 30000): LocationData? {
        if (!PermissionUtil.hasLocationPermission(context)) {
            Timber.w("Location permission not granted")
            return null
        }

        if (!isLocationEnabled()) {
            Timber.w("Location services disabled")
            return null
        }

        val locationRequest = CurrentLocationRequest.Builder()
            .setDurationMillis(timeoutMillis)
            .setMaxUpdateAgeMillis(60000) // Accept locations up to 1 minute old
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        return try {
            val location = fusedLocationClient.getCurrentLocation(
                locationRequest,
                CancellationTokenSource().token
            ).await()

            location?.let {
                val locationData = LocationData.fromLocation(it)
                Timber.d("Location captured: $locationData")
                locationData
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current location")
            null
        }
    }
}
```

**Service Integration**:

```kotlin
class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(SERVICE_ID, createNotification())

        // Capture initial location
        serviceScope.launch {
            val location = locationManager.getCurrentLocation()
            if (location != null) {
                Timber.i("Initial location captured: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")
            } else {
                Timber.w("Failed to capture initial location")
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
```

#### Testing Strategy
- Unit tests for `LocationData.fromLocation()`
- Unit tests for `LocationData.isAccurate()`
- Integration test for `getCurrentLocation()`
- Manual test: Verify location logged when service starts
- Manual test: Test with mock location via ADB
- Manual test: Test timeout handling (airplane mode)

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing (>80% coverage)
- [ ] Integration tests passing
- [ ] Location captured and logged on service start
- [ ] Timeout properly handled
- [ ] Accuracy threshold validation works
- [ ] Works indoors and outdoors

---

### Story 0.2.1.6: Service Testing & Validation

**Story ID**: 0.2.1.6
**Priority**: High
**Estimate**: 0.5 days
**Assigned To**: TBD
**Depends On**: 0.2.1.5

#### User Story
```
AS A development team
I WANT comprehensive tests for the location tracking service
SO THAT I can verify it works correctly before building on this foundation
```

#### Acceptance Criteria
- [ ] Unit test suite covers all service lifecycle methods
- [ ] Integration tests verify location capture flow
- [ ] Manual testing procedure documented
- [ ] ADB commands documented for testing
- [ ] Test report generated and reviewed
- [ ] Code coverage >70%
- [ ] All edge cases tested (no permissions, no GPS, etc.)

#### Technical Details

**Unit Tests**: `app/src/test/java/com/phonemanager/service/LocationTrackingServiceTest.kt`

```kotlin
@RunWith(RobolectricTestRunner::class)
class LocationTrackingServiceTest {

    @Test
    fun `service returns START_STICKY on start command`() {
        val service = LocationTrackingService()
        val result = service.onStartCommand(null, 0, 0)

        assertEquals(Service.START_STICKY, result)
    }

    @Test
    fun `service creates notification on start`() {
        // Test notification creation
    }

    @Test
    fun `service properly cleans up on destroy`() {
        // Test cleanup logic
    }
}
```

**Integration Tests**: `app/src/androidTest/java/com/phonemanager/service/LocationServiceIntegrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class LocationServiceIntegrationTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun testServiceStartsAndCapturesLocation() {
        // Grant permissions
        InstrumentationRegistry.getInstrumentation().uiAutomation.apply {
            grantRuntimePermission(
                InstrumentationRegistry.getInstrumentation().targetContext.packageName,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        // Start service
        val intent = Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            LocationTrackingService::class.java
        )
        serviceRule.startService(intent)

        // Wait and verify location captured (check logs)
        Thread.sleep(5000)
    }
}
```

**Manual Testing Procedure** (document in README or wiki):

```markdown
# Manual Testing Procedure

## Prerequisites
- Android device or emulator with Google Play Services
- USB debugging enabled
- ADB installed and device connected

## Test Scenarios

### Scenario 1: Start Service with Permissions
1. Grant permissions:
   ```bash
   adb shell pm grant com.phonemanager android.permission.ACCESS_FINE_LOCATION
   adb shell pm grant com.phonemanager android.permission.ACCESS_BACKGROUND_LOCATION
   ```
2. Start service:
   ```bash
   adb shell am start-foreground-service com.phonemanager/.service.LocationTrackingService
   ```
3. Verify notification appears in system tray
4. Check logs for location capture:
   ```bash
   adb logcat | grep LocationTrackingService
   ```

### Scenario 2: Start Service without Permissions
1. Revoke permissions:
   ```bash
   adb shell pm revoke com.phonemanager android.permission.ACCESS_FINE_LOCATION
   ```
2. Start service
3. Verify service handles gracefully (no crash)

### Scenario 3: GPS Disabled
1. Disable location services on device
2. Start service
3. Verify appropriate warning logged

### Scenario 4: Mock Location
1. Enable mock location:
   ```bash
   adb shell appops set com.phonemanager android:mock_location allow
   ```
2. Set mock location:
   ```bash
   adb shell am startservice -e command location -e provider gps --ef lat 37.422 --ef lon -122.084
   ```
3. Verify service captures mock location
```

#### Testing Strategy
- Run all unit tests: `./gradlew test`
- Run all instrumentation tests: `./gradlew connectedAndroidTest`
- Execute all manual test scenarios
- Generate code coverage report: `./gradlew jacocoTestReport`
- Review coverage report (target >70%)

#### Definition of Done
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Manual test scenarios documented
- [ ] All manual tests executed successfully
- [ ] Code coverage >70%
- [ ] Test report reviewed by team
- [ ] Edge cases tested and passing
- [ ] No critical or high bugs found

---

## Epic Completion Criteria

This epic is considered complete when:

### Functional Criteria
- [ ] All 6 stories completed and closed
- [ ] Service runs as foreground service
- [ ] Notification displays correctly
- [ ] Location permissions properly handled
- [ ] Single location successfully captured and logged
- [ ] Service survives for at least 1 hour without issues

### Quality Criteria
- [ ] Code coverage >70%
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Manual tests executed successfully
- [ ] Code reviewed and approved
- [ ] No critical or high priority bugs

### Documentation Criteria
- [ ] Service architecture documented
- [ ] API documentation complete
- [ ] Testing procedures documented
- [ ] Known issues documented

### Technical Criteria
- [ ] Works on Android 8-14
- [ ] Tested on Google Pixel, Samsung, Xiaomi
- [ ] No memory leaks detected
- [ ] Proper error handling implemented

---

## Risks & Mitigations

### Risk: Permission Handling Without UI
**Severity**: Medium
**Mitigation**: Use test activity for MVP, document ADB commands
**Status**: Mitigated in Story 0.2.1.3

### Risk: Foreground Service Killed by System
**Severity**: Low (this epic only)
**Mitigation**: Using START_STICKY, foreground service, proper notification
**Status**: Mitigated

### Risk: Location Provider Unavailable
**Severity**: Medium
**Mitigation**: Proper error handling, graceful degradation
**Status**: Mitigated in Story 0.2.1.4

---

## Dependencies

### External Dependencies
- Google Play Services Location API
- Android location services
- Device GPS hardware

### Internal Dependencies
- None (first epic in sequence)

### Blocks
- Epic 0.2.2 (Continuous Tracking) cannot start until this epic is complete

---

## Notes for Next Epic

After completing this epic, Epic 0.2.2 should focus on:
- Continuous location updates (not just single capture)
- Network layer implementation for data transmission
- Background location permission handling
- Extended service runtime testing

---

**Last Updated**: 2025-11-11
**Approved By**: TBD
**Ready for Sprint Planning**: Yes
