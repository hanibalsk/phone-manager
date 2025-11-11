# Epic 0.2.2: Continuous Tracking & Network Integration

**Epic ID**: 0.2.2
**Parent Epic**: [Epic 0.2: Background Location Tracking Service](./Epic-0.2-Location-Tracking-Service.md)
**Status**: Blocked by Epic 0.2.1
**Priority**: Critical (MVP Blocker)
**Estimated Duration**: 5-7 days
**Dependencies**: Epic 0.2.1 (Service Foundation) must be complete

---

## Epic Goal

Implement continuous location tracking with configurable update intervals and establish network integration to transmit location data to a remote server via HTTP/HTTPS. This epic completes the end-to-end value chain: service captures location continuously and successfully transmits it to the remote server.

---

## Epic Scope

This epic focuses on:
- Continuous location updates using LocationCallback
- Background location permission handling (Android 10+)
- HTTP client setup with Retrofit and OkHttp
- Location data transmission to remote server
- Configurable update intervals
- Basic error handling for network failures

What's NOT in scope:
- Offline queueing (Epic 0.2.3)
- Retry logic with backoff (Epic 0.2.3)
- Auto-start on boot (Epic 0.2.4)
- Battery optimization (Epic 0.2.5)
- Configuration management (Epic 0.2.6)

---

## Stories

### Story 0.2.2.1: Implement Continuous Location Callback

**Story ID**: 0.2.2.1
**Priority**: Critical
**Estimate**: 1.5 days
**Assigned To**: TBD

#### User Story
```
AS A location tracking service
I WANT to receive continuous location updates at regular intervals
SO THAT I can track device movement over time
```

#### Acceptance Criteria
- [ ] LocationCallback implemented for continuous updates
- [ ] LocationRequest configured with update interval
- [ ] Location updates received at specified interval (5 minutes default)
- [ ] Callback properly registered with FusedLocationProviderClient
- [ ] Location updates continue with screen off
- [ ] Callback properly unregistered on service destroy
- [ ] Memory leaks prevented (callback lifecycle managed)

#### Technical Details

**Location Callback Implementation**: `app/src/main/java/com/phonemanager/location/LocationManager.kt`

```kotlin
class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var isTracking = false

    fun startLocationUpdates(
        intervalMillis: Long = 5 * 60 * 1000, // 5 minutes
        onLocationUpdate: (LocationData) -> Unit
    ) {
        if (!PermissionUtil.hasLocationPermission(context)) {
            Timber.w("Cannot start location updates: permission not granted")
            return
        }

        if (isTracking) {
            Timber.w("Location updates already running")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis
        ).apply {
            setMinUpdateIntervalMillis(intervalMillis / 2) // Fastest update: half the interval
            setMaxUpdateDelayMillis(intervalMillis * 2) // Batch updates if needed
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val locationData = LocationData.fromLocation(location)

                    // Only process locations meeting accuracy threshold
                    if (locationData.isAccurate(threshold = 50f)) {
                        Timber.d("Location update received: ${locationData.latitude}, ${locationData.longitude}")
                        onLocationUpdate(locationData)
                    } else {
                        Timber.w("Location rejected due to low accuracy: ${locationData.accuracy}m")
                    }
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    Timber.w("Location is not available")
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            isTracking = true
            Timber.i("Started location updates with interval: ${intervalMillis}ms")
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception when requesting location updates")
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
            isTracking = false
            Timber.i("Stopped location updates")
        }
    }

    fun isTracking(): Boolean = isTracking
}
```

**Service Integration**:

```kotlin
class LocationTrackingService : Service() {

    private lateinit var locationManager: LocationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        locationManager = LocationManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(SERVICE_ID, createNotification())

        // Start continuous location tracking
        locationManager.startLocationUpdates { locationData ->
            serviceScope.launch {
                handleLocationUpdate(locationData)
            }
        }

        return START_STICKY
    }

    private fun handleLocationUpdate(locationData: LocationData) {
        Timber.i("Received location: ${locationData.latitude}, ${locationData.longitude}, accuracy: ${locationData.accuracy}m")
        // Will be enhanced in Story 0.2.2.5 to transmit to server
    }

    override fun onDestroy() {
        locationManager.stopLocationUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }
}
```

**Memory Management Strategy**:

For long-running location tracking services, proper memory management is critical to prevent out-of-memory errors and ensure stable 24/7 operation.

```kotlin
class LocationManager(private val context: Context) {

    companion object {
        private const val MAX_PENDING_LOCATIONS = 100
        private const val CLEANUP_THRESHOLD = 80 // Clean when 80% full
        private const val MAX_CACHE_AGE_HOURS = 24L
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var isTracking = false

    /**
     * In-memory buffer for pending locations (before transmission/persistence)
     * Uses ConcurrentLinkedQueue for thread-safe operations
     */
    private val pendingLocations = ConcurrentLinkedQueue<LocationData>()

    /**
     * Track memory usage and trigger cleanup when needed
     */
    private fun handleLocationUpdate(locationData: LocationData) {
        // Add to pending queue
        pendingLocations.offer(locationData)

        // Check if cleanup needed
        if (pendingLocations.size >= MAX_PENDING_LOCATIONS * CLEANUP_THRESHOLD / 100) {
            Timber.w("Pending locations buffer at ${pendingLocations.size}/${MAX_PENDING_LOCATIONS}, triggering cleanup")
            cleanupOldLocations()
        }

        // If queue is full, remove oldest to prevent memory overflow
        while (pendingLocations.size > MAX_PENDING_LOCATIONS) {
            val removed = pendingLocations.poll()
            Timber.w("Buffer overflow: Removed oldest location from ${removed?.timestamp}")
        }
    }

    /**
     * Clean up old cached locations based on age
     */
    private fun cleanupOldLocations() {
        val cutoffTime = System.currentTimeMillis() - (MAX_CACHE_AGE_HOURS * 60 * 60 * 1000)
        val iterator = pendingLocations.iterator()
        var removedCount = 0

        while (iterator.hasNext()) {
            val location = iterator.next()
            if (location.timestamp < cutoffTime) {
                iterator.remove()
                removedCount++
            }
        }

        if (removedCount > 0) {
            Timber.i("Cleaned up $removedCount old locations from memory buffer")
        }
    }

    /**
     * Clear all in-memory buffers (called on service destroy or memory pressure)
     */
    fun clearMemoryBuffers() {
        pendingLocations.clear()
        Timber.i("Cleared all in-memory location buffers")
    }

    /**
     * Handle system memory pressure
     */
    fun onLowMemory() {
        Timber.w("Low memory detected - performing emergency cleanup")

        // Keep only most recent locations
        val recentCount = 10
        val recentLocations = pendingLocations.toList().takeLast(recentCount)

        pendingLocations.clear()
        pendingLocations.addAll(recentLocations)

        Timber.i("Reduced memory buffer from ${pendingLocations.size} to $recentCount locations")

        // Suggest garbage collection (hint only)
        System.gc()
    }
}
```

**Service Memory Management**:

```kotlin
class LocationTrackingService : Service() {

    private lateinit var locationManager: LocationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        locationManager = LocationManager(this)

        // Register for memory pressure callbacks
        registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                when (level) {
                    ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
                    ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                        Timber.w("Memory pressure detected (level: $level)")
                        locationManager.onLowMemory()
                    }
                }
            }

            override fun onConfigurationChanged(newConfig: Configuration) {}
            override fun onLowMemory() {
                Timber.w("System low memory callback")
                locationManager.onLowMemory()
            }
        })
    }

    override fun onDestroy() {
        // Ensure proper cleanup to prevent memory leaks
        locationManager.stopLocationUpdates()
        locationManager.clearMemoryBuffers()
        serviceScope.cancel()
        super.onDestroy()
    }
}
```

**Memory Management Best Practices**:

1. **Buffer Size Limits**: Never allow unbounded growth of in-memory collections
2. **Automatic Cleanup**: Trigger cleanup at 80% capacity to prevent overflow
3. **Oldest-First Eviction**: When buffer is full, remove oldest items first
4. **Age-Based Cleanup**: Remove items older than configured threshold (24 hours)
5. **Memory Pressure Handling**: Respond to system callbacks (onTrimMemory, onLowMemory)
6. **Proper Lifecycle Management**: Clear all buffers in onDestroy()
7. **Garbage Collection Hints**: Suggest GC after major cleanup operations
8. **Thread-Safe Collections**: Use ConcurrentLinkedQueue for multi-threaded access

**Memory Profiling Requirements**:

```kotlin
@Test
fun testLongRunningServiceMemoryUsage() {
    // Test requirements:
    // 1. Run service for 24+ hours
    // 2. Monitor memory usage every hour using Android Profiler
    // 3. Verify memory growth stays within acceptable bounds (<50MB increase over 24h)
    // 4. Verify no memory leaks detected by LeakCanary
    // 5. Verify buffer cleanup occurs at expected thresholds
    // 6. Test behavior under low memory conditions
}
```

**Monitoring and Alerts**:

- **Buffer Size**: Log warning when buffer exceeds 80% capacity
- **Cleanup Events**: Log all cleanup operations with count removed
- **Memory Pressure**: Log all memory pressure callbacks received
- **Leak Detection**: Use LeakCanary in debug builds to detect memory leaks

#### Testing Strategy
- Unit tests for LocationCallback registration/unregistration
- Integration test for continuous updates over 15 minutes
- Manual test: Verify updates continue with screen off
- Manual test: Verify updates at correct interval
- Memory leak test using LeakCanary
- **24-hour memory profiling test**
- **Test buffer cleanup at 80% threshold**
- **Test memory pressure callback handling**
- **Test oldest-first eviction when buffer full**
- **Verify memory growth stays under 50MB over 24 hours**

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing
- [ ] Integration tests passing
- [ ] Location updates received at configured interval
- [ ] Works with screen off
- [ ] No memory leaks detected
- [ ] Proper cleanup on service destroy
- [ ] **Memory buffer limits enforced (max 100 locations)**
- [ ] **Automatic cleanup triggers at 80% capacity**
- [ ] **Memory pressure callbacks properly handled**
- [ ] **24-hour profiling shows stable memory usage**

---

### Story 0.2.2.2: Add Background Location Permission

**Story ID**: 0.2.2.2
**Priority**: Critical
**Estimate**: 1 day
**Assigned To**: TBD
**Depends On**: 0.2.2.1

#### User Story
```
AS A location tracking service
I WANT to request background location permission on Android 10+
SO THAT continuous location tracking works when the app is not visible
```

#### Acceptance Criteria
- [ ] Background location permission declared in manifest
- [ ] Permission request flow implemented for Android 10+ (API 29+)
- [ ] Two-step permission request (foreground first, then background)
- [ ] Educational rationale shown before requesting background permission
- [ ] Service handles permission denial gracefully
- [ ] Works correctly on Android 9 (no background permission needed)
- [ ] Works correctly on Android 10-14 (background permission required)

#### Technical Details

**Manifest Update**: `AndroidManifest.xml`

```xml
<!-- Already added in Epic 0.2.1.3, but ensure it's present -->
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

**Permission Utility Enhancement**: `PermissionUtil.kt`

```kotlin
object PermissionUtil {

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 9 and below don't need explicit background permission
            true
        }
    }

    fun shouldShowBackgroundLocationRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            false
        }
    }

    fun canRequestBackgroundLocation(context: Context): Boolean {
        // Background location can only be requested after foreground permission is granted
        return hasLocationPermission(context) && !hasBackgroundLocationPermission(context)
    }
}
```

**Test Activity Enhancement**:

```kotlin
class TestActivity : ComponentActivity() {

    private val backgroundLocationRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Timber.i("Background location permission granted")
            startLocationService()
        } else {
            Timber.w("Background location permission denied")
            showBackgroundPermissionDeniedDialog()
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (PermissionUtil.shouldShowBackgroundLocationRationale(this)) {
                showBackgroundLocationRationale()
            } else {
                backgroundLocationRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            // Android 9 and below - no background permission needed
            startLocationService()
        }
    }

    private fun showBackgroundLocationRationale() {
        AlertDialog.Builder(this)
            .setTitle("Background Location Access")
            .setMessage(
                "This app needs to access your location even when the app is closed or not in use. " +
                "This is required for continuous location tracking.\n\n" +
                "Please select 'Allow all the time' in the next screen."
            )
            .setPositiveButton("Continue") { _, _ ->
                backgroundLocationRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBackgroundPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(
                "Background location permission is required for this app to function. " +
                "Please grant 'Allow all the time' in Settings."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
```

**Service Validation**:

```kotlin
class LocationTrackingService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PermissionUtil.hasAllRequiredPermissions(this)) {
            Timber.e("Cannot start service: missing required permissions")
            stopSelf()
            return START_NOT_STICKY
        }

        // Proceed with service startup
        createNotificationChannel()
        startForeground(SERVICE_ID, createNotification())

        locationManager.startLocationUpdates { locationData ->
            serviceScope.launch {
                handleLocationUpdate(locationData)
            }
        }

        return START_STICKY
    }
}
```

#### Testing Strategy
- Manual test on Android 9 (no background permission needed)
- Manual test on Android 10, 11, 12, 13, 14 (background permission flow)
- Test permission denial scenario
- Test permission rationale display
- Test opening Settings from dialog

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Works on Android 9 (no background permission)
- [ ] Works on Android 10-14 (background permission required)
- [ ] Educational rationale shown
- [ ] Permission denial handled gracefully
- [ ] Settings link works correctly
- [ ] Service validates permissions before starting

---

### Story 0.2.2.3: Setup Network Layer

**Story ID**: 0.2.2.3
**Priority**: Critical
**Estimate**: 1.5 days
**Assigned To**: TBD
**Depends On**: None (can be parallel with 0.2.2.1)

#### User Story
```
AS A location tracking service
I WANT to establish HTTP network connectivity to the remote server
SO THAT I can transmit location data
```

#### Acceptance Criteria
- [ ] Retrofit and OkHttp dependencies added
- [ ] HTTP client configured with timeouts and interceptors
- [ ] API service interface defined
- [ ] HTTPS enforced for security
- [ ] Network error handling implemented
- [ ] Request/response logging for debugging
- [ ] Connection pooling configured
- [ ] **SECURITY**: API key retrieved from secure storage (BuildConfig or EncryptedSharedPreferences)
- [ ] **SECURITY**: API key NOT hardcoded in source code
- [ ] **SECURITY**: gradle.properties added to .gitignore
- [ ] Security crypto dependency added for EncryptedSharedPreferences

#### Technical Details

**Dependencies**: `app/build.gradle.kts`

```kotlin
dependencies {
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Secure credential storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

**Security Configuration**: Add to `gradle.properties` (DO NOT commit to VCS, add to .gitignore)

```properties
# API Key for development (NEVER commit this file)
API_KEY=your-development-api-key-here
```

**Build Config**: Add to `app/build.gradle.kts`

```kotlin
android {
    // ... existing config ...

    buildFeatures {
        buildConfig = true  // Enable BuildConfig generation
    }

    defaultConfig {
        // ... existing config ...

        // Read API key from gradle.properties (not committed to VCS)
        buildConfigField("String", "API_KEY", "\"${project.findProperty("API_KEY") ?: ""}\"")
    }
}
```

**Network Module**: `app/src/main/java/com/phonemanager/network/NetworkModule.kt`

```kotlin
object NetworkModule {

    private const val BASE_URL = "https://api.example.com/" // TODO: Make configurable
    private const val CONNECT_TIMEOUT = 30L // seconds
    private const val READ_TIMEOUT = 30L // seconds
    private const val WRITE_TIMEOUT = 30L // seconds

    fun provideOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(createLoggingInterceptor())
            .addInterceptor(createAuthInterceptor(context))
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(false) // We'll handle retries manually in Epic 0.2.3
            .build()
    }

    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
    }

    private fun createAuthInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()

            // SECURITY: NEVER hardcode API keys!
            // Get API key from secure storage
            val apiKey = getApiKeySecurely(context)

            val request = original.newBuilder()
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()

            chain.proceed(request)
        }
    }

    /**
     * Retrieve API key from secure storage
     *
     * Option 1: Use BuildConfig for compile-time injection (recommended for development)
     * Add to gradle.properties (NOT committed to VCS):
     *   API_KEY=your-actual-api-key
     *
     * Add to app/build.gradle.kts:
     *   buildConfigField("String", "API_KEY", "\"${project.findProperty("API_KEY")}\"")
     *
     * Option 2: Use EncryptedSharedPreferences for runtime storage (recommended for production)
     */
    private fun getApiKeySecurely(context: Context): String {
        // For MVP: Use BuildConfig (set via gradle.properties, NOT committed)
        // WARNING: Never commit API keys to version control!
        return if (BuildConfig.DEBUG) {
            // Development: Load from BuildConfig
            BuildConfig.API_KEY
        } else {
            // Production: Load from EncryptedSharedPreferences
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            encryptedPrefs.getString("api_key", "") ?: ""
        }
    }

    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(provideGson()))
            .build()
    }

    private fun provideGson(): Gson {
        return GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    }

    fun provideLocationApiService(retrofit: Retrofit): LocationApiService {
        return retrofit.create(LocationApiService::class.java)
    }
}
```

**API Service Interface**: `app/src/main/java/com/phonemanager/network/LocationApiService.kt`

```kotlin
interface LocationApiService {

    @POST("location")
    suspend fun sendLocation(@Body locationPayload: LocationPayload): Response<LocationResponse>

    @POST("location/batch")
    suspend fun sendLocationBatch(@Body locations: List<LocationPayload>): Response<BatchLocationResponse>
}
```

**Network Manager**: `app/src/main/java/com/phonemanager/network/NetworkManager.kt`

```kotlin
class NetworkManager(context: Context) {

    private val okHttpClient = NetworkModule.provideOkHttpClient()
    private val retrofit = NetworkModule.provideRetrofit(okHttpClient)
    private val apiService = NetworkModule.provideLocationApiService(retrofit)

    suspend fun sendLocation(locationData: LocationData): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = LocationPayload.fromLocationData(locationData)
                val response = apiService.sendLocation(payload)

                if (response.isSuccessful) {
                    Timber.d("Location sent successfully: ${response.body()}")
                    Result.success(true)
                } else {
                    Timber.e("Failed to send location: ${response.code()} - ${response.message()}")
                    Result.failure(NetworkException("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while sending location")
                Result.failure(e)
            }
        }
    }

    companion object {
        private const val TAG = "NetworkManager"
    }
}

class NetworkException(message: String) : Exception(message)
```

**Network Security Configuration**: `app/src/main/res/xml/network_security_config.xml`

To enhance API security with certificate pinning, create a Network Security Configuration file:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Base configuration for all connections -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>

    <!-- Domain-specific configuration with certificate pinning -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">your-api-domain.com</domain>

        <!-- Certificate pinning with backup pins -->
        <pin-set expiration="2026-12-31">
            <!-- Primary certificate SHA-256 hash -->
            <pin digest="SHA-256">base64EncodedPrimaryPublicKeyHash==</pin>
            <!-- Backup certificate SHA-256 hash -->
            <pin digest="SHA-256">base64EncodedBackupPublicKeyHash==</pin>
        </pin-set>

        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </domain-config>

    <!-- Debug configuration (for local testing) -->
    <debug-overrides>
        <trust-anchors>
            <!-- Trust user-added certificates for debugging -->
            <certificates src="user" />
            <certificates src="system" />
        </trust-anchors>
    </debug-overrides>
</network-security-config>
```

**Generating Certificate Hashes**:

To generate the SHA-256 public key hash for certificate pinning:

```bash
# Option 1: From server certificate
echo | openssl s_client -servername your-api-domain.com -connect your-api-domain.com:443 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64

# Option 2: From PEM certificate file
openssl x509 -in certificate.pem -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

**Add to AndroidManifest.xml**:

```xml
<application
    android:name=".PhoneManagerApplication"
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
    ...
</application>
```

**Certificate Pinning Best Practices**:

1. **Always include backup pins**: Include at least 2 pins (primary + backup) to prevent app lockout during certificate rotation
2. **Set appropriate expiration**: Set expiration date to match your certificate rotation schedule
3. **Monitor expiration**: Set up alerts 30 days before pin expiration
4. **Test thoroughly**: Test certificate pinning in debug builds before production
5. **Plan for rotation**: Document certificate rotation procedures
6. **Emergency bypass**: Have a plan for updating app if certificates change unexpectedly

**Certificate Rotation Strategy**:

```kotlin
/**
 * Certificate rotation checklist:
 *
 * 1. Generate new certificate on server
 * 2. Add new certificate hash as backup pin in app
 * 3. Release app update with both old (primary) and new (backup) pins
 * 4. Wait for majority of users to update (monitor analytics)
 * 5. Rotate certificate on server
 * 6. In next app release, promote backup pin to primary
 * 7. Add new backup pin for future rotation
 *
 * Timeline: Minimum 4-6 weeks between adding backup pin and server rotation
 */
```

**Testing Certificate Pinning**:

```kotlin
// Test that certificate pinning is working
@Test
fun testCertificatePinningRejectsInvalidCertificate() {
    // Use OkHttp MockWebServer with self-signed certificate
    val mockServer = MockWebServer()
    mockServer.useHttps(selfSignedCertificate, false)

    // Attempt connection should fail due to pinning
    assertThrows<SSLPeerUnverifiedException> {
        networkManager.sendLocation(testLocationData)
    }
}
```

#### Testing Strategy
- Unit tests for network module configuration
- Mock server tests for API service
- Integration test with real HTTP endpoint (test server)
- Test timeout handling
- Test HTTPS enforcement
- Test authentication header injection
- **Test certificate pinning with invalid certificates**
- **Verify NetworkSecurityConfig.xml is properly configured**
- **Test certificate rotation procedure**

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing
- [ ] HTTP client configured correctly
- [ ] HTTPS enforced
- [ ] Timeouts configured appropriately
- [ ] Logging works in debug mode
- [ ] Authentication header added
- [ ] **NetworkSecurityConfig.xml created with certificate pinning**
- [ ] **Certificate hashes generated and configured**
- [ ] **Backup certificate pins included**
- [ ] **Certificate pinning tested with invalid certificates**
- [ ] Integration test with test server successful

---

### Story 0.2.2.4: Define Location Payload Model

**Story ID**: 0.2.2.4
**Priority**: Critical
**Estimate**: 0.5 days
**Assigned To**: TBD
**Depends On**: 0.2.2.3

#### User Story
```
AS A location tracking service
I WANT to format location data as a JSON payload
SO THAT it can be transmitted to the server in the expected format
```

#### Acceptance Criteria
- [ ] LocationPayload data class defined with JSON serialization
- [ ] Conversion from LocationData to LocationPayload implemented
- [ ] Device ID included in payload
- [ ] Timestamp in ISO 8601 format
- [ ] All required fields included
- [ ] Optional fields handled correctly
- [ ] Payload validation implemented

#### Technical Details

**Location Payload**: `app/src/main/java/com/phonemanager/network/model/LocationPayload.kt`

```kotlin
data class LocationPayload(
    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("timestamp")
    val timestamp: String, // ISO 8601 format

    @SerializedName("accuracy")
    val accuracy: Float,

    @SerializedName("altitude")
    val altitude: Double? = null,

    @SerializedName("bearing")
    val bearing: Float? = null,

    @SerializedName("speed")
    val speed: Float? = null,

    @SerializedName("provider")
    val provider: String? = null
) {
    companion object {
        fun fromLocationData(locationData: LocationData, deviceId: String): LocationPayload {
            return LocationPayload(
                deviceId = deviceId,
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                timestamp = locationData.capturedAt, // Already in ISO 8601 format
                accuracy = locationData.accuracy,
                altitude = locationData.altitude,
                bearing = locationData.bearing,
                speed = locationData.speed,
                provider = locationData.provider
            )
        }
    }

    fun validate(): Boolean {
        return latitude in -90.0..90.0 &&
               longitude in -180.0..180.0 &&
               accuracy >= 0 &&
               deviceId.isNotBlank()
    }
}

data class LocationResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("location_id")
    val locationId: String?
)

data class BatchLocationResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("received_count")
    val receivedCount: Int,

    @SerializedName("processed_count")
    val processedCount: Int
)
```

**Device ID Utility**: `app/src/main/java/com/phonemanager/util/DeviceUtil.kt`

```kotlin
object DeviceUtil {

    private const val PREF_DEVICE_ID = "device_id"
    private const val PREFS_NAME = "phone_manager_prefs"

    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        var deviceId = prefs.getString(PREF_DEVICE_ID, null)

        if (deviceId == null) {
            // Generate new UUID for this device
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(PREF_DEVICE_ID, deviceId).apply()
            Timber.d("Generated new device ID: $deviceId")
        }

        return deviceId
    }
}
```

**NetworkManager Enhancement**:

```kotlin
class NetworkManager(private val context: Context) {

    private val deviceId = DeviceUtil.getDeviceId(context)

    suspend fun sendLocation(locationData: LocationData): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = LocationPayload.fromLocationData(locationData, deviceId)

                if (!payload.validate()) {
                    Timber.e("Invalid location payload: $payload")
                    return@withContext Result.failure(IllegalArgumentException("Invalid payload"))
                }

                val response = apiService.sendLocation(payload)

                if (response.isSuccessful) {
                    Timber.d("Location sent successfully: ${response.body()}")
                    Result.success(true)
                } else {
                    Timber.e("Failed to send location: ${response.code()} - ${response.message()}")
                    Result.failure(NetworkException("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while sending location")
                Result.failure(e)
            }
        }
    }
}
```

#### Testing Strategy
- Unit tests for LocationPayload.fromLocationData()
- Unit tests for payload validation
- Unit tests for DeviceUtil.getDeviceId()
- Test JSON serialization/deserialization
- Test device ID persistence across app restarts

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing (>80% coverage)
- [ ] JSON serialization works correctly
- [ ] Payload validation works
- [ ] Device ID generated and persisted
- [ ] All fields properly mapped
- [ ] Handles null optional fields

---

### Story 0.2.2.5: Implement Location Transmission

**Story ID**: 0.2.2.5
**Priority**: Critical
**Estimate**: 1 day
**Assigned To**: TBD
**Depends On**:
- **Story 0.2.2.1** - Continuous Location Callback
  - File: `docs/product/Story-0.2.2-Continuous-Tracking.md#story-0221`
  - Provides: Location update callback mechanism
- **Story 0.2.2.3** - Network Layer Setup
  - File: `docs/product/Story-0.2.2-Continuous-Tracking.md#story-0223`
  - Provides: HTTP client and API infrastructure
- **Story 0.2.2.4** - Location Payload Model
  - File: `docs/product/Story-0.2.2-Continuous-Tracking.md#story-0224`
  - Provides: Data model for transmission

#### User Story
```
AS A location tracking service
I WANT to transmit captured location data to the remote server
SO THAT location information is available on the backend
```

#### Acceptance Criteria
- [ ] Location data transmitted via HTTP POST after each capture
- [ ] Success responses handled correctly
- [ ] Error responses logged appropriately
- [ ] Service continues operating after transmission failures
- [ ] Network connectivity checked before transmission
- [ ] Failed transmissions logged for future queueing (Epic 0.2.3)
- [ ] No blocking of location capture thread

#### Technical Details

**Service Integration**:

```kotlin
class LocationTrackingService : Service() {

    private lateinit var locationManager: LocationManager
    private lateinit var networkManager: NetworkManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        locationManager = LocationManager(this)
        networkManager = NetworkManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PermissionUtil.hasAllRequiredPermissions(this)) {
            Timber.e("Cannot start service: missing required permissions")
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(SERVICE_ID, createNotification())

        locationManager.startLocationUpdates { locationData ->
            serviceScope.launch {
                handleLocationUpdate(locationData)
            }
        }

        return START_STICKY
    }

    private suspend fun handleLocationUpdate(locationData: LocationData) {
        Timber.i("Received location: ${locationData.latitude}, ${locationData.longitude}")

        // Check network connectivity
        if (!isNetworkAvailable()) {
            Timber.w("No network available, location will need to be queued")
            // TODO: Queue for later in Epic 0.2.3
            return
        }

        // Attempt to send location
        val result = networkManager.sendLocation(locationData)

        result.onSuccess {
            Timber.i("Location transmitted successfully")
            updateNotification("Last update: ${System.currentTimeMillis()}")
        }.onFailure { exception ->
            Timber.e(exception, "Failed to transmit location")
            // TODO: Queue for retry in Epic 0.2.3
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }

    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_location_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(SERVICE_ID, notification)
    }

    override fun onDestroy() {
        locationManager.stopLocationUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }
}
```

**Network Connectivity Permission**: Add to `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

#### Testing Strategy
- Integration test: Full flow from capture to transmission
- Manual test with real server endpoint
- Test with mock server (simulate success/failure)
- Test network unavailable scenario
- Test transmission during poor connectivity
- Monitor logcat for successful transmissions

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Integration tests passing
- [ ] Location transmitted to server successfully
- [ ] Success responses handled correctly
- [ ] Failure logged appropriately
- [ ] Service continues after transmission failures
- [ ] Network connectivity checked
- [ ] Notification updated with last transmission time

---

### Story 0.2.2.6: Integration & Extended Runtime Testing

**Story ID**: 0.2.2.6
**Priority**: High
**Estimate**: 1 day
**Assigned To**: TBD
**Depends On**:
- **All previous stories in Epic 0.2.2**
  - Story 0.2.2.1 through 0.2.2.5
  - File: `docs/product/Story-0.2.2-Continuous-Tracking.md`
  - Requires: Complete end-to-end location tracking flow

#### User Story
```
AS A development team
I WANT to verify the location tracking service works reliably over extended periods
SO THAT I can confidently move to the next epic
```

#### Acceptance Criteria
- [ ] End-to-end flow tested (capture → transmit → server receives)
- [ ] Service runs continuously for at least 6 hours without issues
- [ ] Location updates received at correct intervals
- [ ] All transmissions successful when network available
- [ ] Service survives screen off/on cycles
- [ ] Service handles network disconnection gracefully
- [ ] Memory usage stable over time
- [ ] No ANRs or crashes
- [ ] Battery impact measured and acceptable

#### Technical Details

**Test Plan**:

```markdown
# Extended Runtime Test Plan

## Test Environment
- Device: Real Android device (not emulator)
- Android version: 10, 12, 14 (minimum coverage)
- Network: WiFi or mobile data
- Test duration: 6 hours minimum

## Pre-Test Setup
1. Install latest build
2. Grant all required permissions
3. Ensure server endpoint is accessible
4. Clear app data to start fresh
5. Charge device to 100%
6. Install LeakCanary for memory leak detection

## Test Procedure

### Phase 1: Initial Verification (30 minutes)
1. Start service via test activity
2. Verify notification appears
3. Verify first location captured within 60 seconds
4. Verify first transmission successful
5. Monitor logs for any errors
6. Check server logs for received data

### Phase 2: Normal Operation (3 hours)
1. Let service run with screen on
2. Monitor location updates every 5 minutes
3. Verify all transmissions successful
4. Check memory usage every 30 minutes
5. Screen off for 30 minutes, verify updates continue
6. Screen on, verify service still running

### Phase 3: Stress Testing (2 hours)
1. Enable airplane mode for 15 minutes
2. Disable airplane mode, verify recovery
3. Switch between WiFi and mobile data
4. Lock/unlock device repeatedly
5. Open/close multiple apps
6. Put device in pocket and move around

### Phase 4: Endurance (30 minutes)
1. Let service continue running
2. Final memory check
3. Final battery check
4. Review all logs for errors
5. Check server for data continuity

## Success Criteria
- ✅ Service uptime: 100% (no crashes)
- ✅ Location capture rate: >95%
- ✅ Transmission success rate: >95% (when online)
- ✅ Memory usage: <50MB average
- ✅ Battery drain: <5% per hour
- ✅ No memory leaks detected
- ✅ No ANRs
```

**Monitoring Script**: `scripts/monitor_service.sh`

```bash
#!/bin/bash

# Monitor location tracking service
echo "Monitoring LocationTrackingService..."
echo "Press Ctrl+C to stop"
echo ""

while true; do
    # Check if service is running
    SERVICE_STATUS=$(adb shell dumpsys activity services com.phonemanager | grep LocationTrackingService | wc -l)

    # Get memory usage
    MEMORY=$(adb shell dumpsys meminfo com.phonemanager | grep "TOTAL" | awk '{print $2}')

    # Get battery level
    BATTERY=$(adb shell dumpsys battery | grep level | awk '{print $2}')

    # Check for location updates in logs
    RECENT_LOCATION=$(adb logcat -d -s LocationTrackingService:D | tail -1)

    echo "$(date) - Service: $SERVICE_STATUS | Memory: ${MEMORY}KB | Battery: ${BATTERY}% | Last: $RECENT_LOCATION"

    sleep 60  # Check every minute
done
```

**Performance Metrics Collection**:

```kotlin
class PerformanceMetrics {
    private var startTime: Long = 0
    private var locationCaptureCount: Int = 0
    private var transmissionSuccessCount: Int = 0
    private var transmissionFailureCount: Int = 0

    fun start() {
        startTime = System.currentTimeMillis()
    }

    fun recordLocationCapture() {
        locationCaptureCount++
    }

    fun recordTransmissionSuccess() {
        transmissionSuccessCount++
    }

    fun recordTransmissionFailure() {
        transmissionFailureCount++
    }

    fun generateReport(): String {
        val uptimeMinutes = (System.currentTimeMillis() - startTime) / 60000
        val captureRate = locationCaptureCount.toFloat() / (uptimeMinutes / 5) * 100
        val successRate = if (locationCaptureCount > 0) {
            transmissionSuccessCount.toFloat() / locationCaptureCount * 100
        } else 0f

        return """
            Performance Report:
            - Uptime: $uptimeMinutes minutes
            - Location captures: $locationCaptureCount
            - Transmission successes: $transmissionSuccessCount
            - Transmission failures: $transmissionFailureCount
            - Capture rate: ${captureRate.roundToInt()}%
            - Success rate: ${successRate.roundToInt()}%
        """.trimIndent()
    }
}
```

**Edge Case Testing Requirements**:

In addition to the standard extended runtime testing, the following edge cases must be explicitly tested to ensure robust service operation:

## Network Transition Tests

### Airplane Mode Scenarios
```
Test 1: Airplane Mode Toggle During Active Tracking
- Start location tracking with network available
- Enable airplane mode
- Verify service continues tracking (locations queued)
- Wait 5 minutes with airplane mode ON
- Disable airplane mode
- Verify service reconnects and transmits queued locations
- Success: All locations transmitted after reconnection
```

### Network Type Transitions
```
Test 2: WiFi to Cellular Handoff
- Start tracking on WiFi network
- Move out of WiFi range to trigger cellular fallback
- Verify service continues operating on cellular
- Verify no data loss during transition
- Success: Continuous operation without service restart

Test 3: Complete Network Loss for Extended Period
- Start tracking with network available
- Disable all network connectivity (airplane mode or no signal)
- Wait 60+ minutes with no connectivity
- Re-enable network connectivity
- Verify service resumes transmission
- Verify queued locations transmitted
- Success: Service recovers without manual intervention
```

### Roaming Scenarios
```
Test 4: Network Roaming
- Simulate roaming scenario (use roaming SIM or test tool)
- Verify service respects roaming preferences
- Verify data transmission over roaming network
- Success: Service adapts to roaming state appropriately
```

## Device State Tests

### SIM Card Operations
```
Test 5: SIM Card Removal During Tracking
- Start location tracking
- Remove SIM card while service running
- Verify service continues (using WiFi if available)
- Verify device ID remains consistent
- Insert SIM card
- Verify service continues normally
- Success: Service resilient to SIM changes

Test 6: Dual SIM Switching
- Start tracking on device with dual SIM
- Switch active data SIM
- Verify service continues without interruption
- Success: No service restart required
```

### Time and Timezone Tests
```
Test 7: Timezone Change During Tracking
- Start location tracking in timezone A (e.g., PST)
- Change device timezone to timezone B (e.g., EST)
- Verify timestamps use UTC or correctly adjusted
- Verify location tracking continues
- Success: Timestamps remain consistent and accurate

Test 8: System Clock Manual Adjustment
- Start location tracking
- Manually adjust system clock forward 1 hour
- Verify service handles time jump gracefully
- Manually adjust system clock backward 1 hour
- Verify no duplicate timestamps or errors
- Success: Service tolerates clock changes

Test 9: Automatic Time Toggle
- Disable "Automatic date & time" setting
- Set manual time (slightly different from actual)
- Start location tracking
- Enable "Automatic date & time" (time jumps to correct value)
- Verify service handles time correction
- Success: No crashes or timestamp inconsistencies
```

### Storage Tests
```
Test 10: Low Storage Conditions
- Fill device storage to <100MB free
- Start location tracking
- Verify service handles storage errors gracefully
- Verify appropriate error logging
- Verify service doesn't crash
- Free up storage
- Verify service resumes normal operation
- Success: Degraded but stable operation under low storage
```

### Thermal Tests
```
Test 11: Thermal Throttling Simulation
- Run intensive app to heat up device (gaming, video processing)
- Monitor location tracking service behavior
- Verify service continues under thermal throttling
- Verify location accuracy impacts documented
- Success: Service remains stable under thermal stress
```

## Security and Permission Tests

### Mock Location Detection
```
Test 12: Mock Location App Detection
- Enable Developer Options → "Allow mock locations"
- Install mock location app (e.g., Fake GPS)
- Start location tracking
- Attempt to send mock locations
- Verify service detects mock locations (if required)
- Document behavior with mock locations
- Success: Service behavior with mock locations is defined and tested
```

### Developer Options Spoofing
```
Test 13: Location Spoofing via Developer Tools
- Enable Developer Options
- Use "Select mock location app" setting
- Test various mock locations
- Verify service handles spoofed data appropriately
- Success: Service behavior documented and tested
```

### GPS Jamming Simulation
```
Test 14: GPS Signal Loss
- Start tracking outdoors (strong GPS signal)
- Move indoors to location with no GPS (deep basement, tunnel)
- Verify service handles signal loss gracefully
- Verify fallback to network/WiFi location
- Move back outdoors
- Verify service resumes GPS location
- Success: Graceful degradation and recovery
```

## Permission Revocation Tests

```
Test 15: Runtime Permission Revocation
- Start location tracking
- Revoke location permission while service running (via Settings)
- Verify service stops gracefully or requests permission
- Verify no crashes
- Re-grant permission
- Verify service can resume tracking
- Success: Handles permission changes without crashes

Test 16: Background Location Permission Revocation
- Start tracking with background permission granted
- Revoke background location permission (Settings → App → Permissions)
- Keep foreground permission granted
- Verify service behavior (may continue with limitations)
- Success: Defined behavior when background permission revoked
```

## Concurrency and Race Condition Tests

```
Test 17: Rapid Service Start/Stop Cycles
- Start service
- Immediately stop service (<1 second)
- Repeat 10 times rapidly
- Verify no race conditions
- Verify proper cleanup each time
- Success: No crashes, no leaked resources

Test 18: Multiple Simultaneous Location Callbacks
- Configure very short update interval (e.g., 1 second)
- Simulate GPS updates coming faster than processing
- Verify service handles concurrent callbacks
- Verify no location data loss
- Success: Thread-safe operation confirmed
```

## Power Management Edge Cases

```
Test 19: Battery Saver Mode Activation
- Start location tracking
- Enable Battery Saver mode
- Verify service continues (possibly with reduced accuracy)
- Verify behavior is acceptable and documented
- Disable Battery Saver mode
- Verify service returns to normal operation
- Success: Service adapts to battery saver constraints

Test 20: Extreme Battery Saver (Some OEMs)
- On devices with aggressive battery management (Xiaomi, Huawei, etc.)
- Enable manufacturer's "Ultra Battery Saver" mode
- Verify service behavior
- Document limitations
- Success: Behavior under OEM battery modes documented
```

## System Resource Pressure

```
Test 21: High CPU Load
- Run CPU-intensive task in background
- Monitor location tracking service performance
- Verify service continues receiving updates
- Verify update intervals remain consistent
- Success: Service maintains performance under CPU pressure

Test 22: Memory Pressure from Other Apps
- Open multiple memory-intensive apps
- Monitor location service memory usage
- Verify service handles memory pressure callbacks
- Verify service not killed by system
- Success: Service remains running under memory pressure
```

## Edge Case Test Execution Checklist

- [ ] All 22 edge case scenarios tested
- [ ] Results documented for each test
- [ ] Failures analyzed and bugs filed
- [ ] Known limitations documented
- [ ] Workarounds identified where applicable
- [ ] Edge case test report generated
- [ ] Product team informed of any limitations

## Expected Outcomes

For each edge case test, document:
1. **Expected Behavior**: What should happen
2. **Actual Behavior**: What actually happened
3. **Pass/Fail Status**: Did it meet expectations
4. **Issues Found**: Any bugs discovered
5. **Risk Level**: Low/Medium/High if edge case fails
6. **Mitigation**: How to handle the edge case in production

## Edge Case Priority Levels

**CRITICAL (Must Pass)**:
- Tests 1, 3, 7, 12, 15, 17, 19

**HIGH (Should Pass)**:
- Tests 2, 5, 8, 10, 11, 16, 18, 21

**MEDIUM (Nice to Pass)**:
- Tests 4, 6, 9, 13, 14, 20, 22

#### Testing Strategy
- Execute complete 6-hour test
- Run on multiple devices (Google Pixel, Samsung, Xiaomi)
- Test on multiple Android versions (10, 12, 14)
- Collect performance metrics
- Generate test report
- **Execute all CRITICAL edge case tests**
- **Execute HIGH priority edge case tests**
- **Document results of all edge case tests**

#### Definition of Done
- [ ] 6-hour test completed successfully
- [ ] All success criteria met
- [ ] Test report generated and reviewed
- [ ] Performance metrics within acceptable ranges
- [ ] No critical or high bugs found
- [ ] Service ready for Epic 0.2.3 (offline queue)
- [ ] Known issues documented
- [ ] **All CRITICAL edge case tests passed (Tests 1, 3, 7, 12, 15, 17, 19)**
- [ ] **All HIGH priority edge case tests executed and documented**
- [ ] **Edge case test report completed with pass/fail status for all tests**

---

## Epic Completion Criteria

This epic is considered complete when:

### Functional Criteria
- [ ] All 6 stories completed and closed
- [ ] Continuous location updates working
- [ ] Background location permission properly handled
- [ ] Network layer established and functional
- [ ] Location data successfully transmitted to server
- [ ] Service runs for 6+ hours without issues

### Quality Criteria
- [ ] Code coverage >70%
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Extended runtime test successful
- [ ] Code reviewed and approved
- [ ] No critical or high priority bugs

### Documentation Criteria
- [ ] Network integration documented
- [ ] API payload format documented
- [ ] Testing procedures documented
- [ ] Known limitations documented

### Technical Criteria
- [ ] Works on Android 9-14
- [ ] Background location permission flow works
- [ ] HTTPS enforced for security
- [ ] Proper error handling implemented
- [ ] Memory leaks addressed

---

## Risks & Mitigations

### Risk: Background Location Permission Rejection
**Severity**: High
**Mitigation**: Educational rationale, clear explanation, graceful degradation
**Status**: Mitigated in Story 0.2.2.2

### Risk: Network Failures Cause Data Loss
**Severity**: High
**Mitigation**: Will be addressed in Epic 0.2.3 with offline queue
**Status**: Accepted for this epic, deferred to 0.2.3

### Risk: Server Endpoint Not Ready
**Severity**: Medium
**Mitigation**: Use mock server or test endpoint for development
**Status**: Plan to use test endpoint

### Risk: High Battery Consumption
**Severity**: Medium
**Mitigation**: Will be addressed in Epic 0.2.5
**Status**: Accepted for MVP, monitor during testing

---

## Dependencies

### External Dependencies
- Remote server endpoint (or mock server for testing)
- Server API contract defined
- Network connectivity required

### Internal Dependencies
- Epic 0.2.1 (Service Foundation) must be complete

### Blocks
- Epic 0.2.3 (Reliability & Offline Queue) cannot start until this epic is complete

---

## Notes for Next Epic

After completing this epic, Epic 0.2.3 should focus on:
- Local database setup with Room
- Offline queue management
- Network connectivity monitoring
- Retry logic with exponential backoff
- Queue processing when network restored

The foundation established in this epic (continuous tracking + network layer) will be enhanced with reliability features in Epic 0.2.3.

---

**Last Updated**: 2025-11-11
**Approved By**: TBD
**Ready for Sprint Planning**: Yes (after Epic 0.2.1 complete)
