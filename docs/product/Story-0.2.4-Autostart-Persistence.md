# Epic 0.2.4: Auto-start & Service Persistence

**Epic ID**: 0.2.4
**Parent Epic**: [Epic 0.2: Background Location Tracking Service](./Epic-0.2-Location-Tracking-Service.md)
**Status**: Blocked by Epic 0.2.3
**Priority**: Critical (MVP Blocker - Final MVP Epic)
**Estimated Duration**: 3-5 days
**Dependencies**: Epic 0.2.3 (Reliability & Offline Queue) must be complete

---

## Epic Goal

Enable the service to start automatically on device boot and persist across all system events without user intervention. This epic delivers on the critical requirement of "hands-off operation," transforming the service from a manually-started application into a truly autonomous background tracking system.

---

## Epic Scope

This epic focuses on:
- Boot completed broadcast receiver
- Automatic service restart on boot
- Service auto-restart after crashes/kills
- WorkManager integration for health monitoring
- Doze mode and App Standby handling
- Service state persistence
- Battery optimization exemption requests

What's NOT in scope:
- Battery optimization implementation (Epic 0.2.5)
- Configuration UI (Epic 0.2.6)
- Advanced monitoring/analytics (Epic 0.2.6)
- OEM-specific battery workarounds (Epic 0.2.5)

---

## Stories

### Story 0.2.4.1: Implement Boot Completed Receiver

**Story ID**: 0.2.4.1
**Priority**: Critical
**Estimate**: 1 day
**Assigned To**: TBD

#### User Story
```
AS A location tracking system
I WANT to automatically start the service when the device boots
SO THAT tracking resumes without user intervention after device restart
```

#### Acceptance Criteria
- [ ] BroadcastReceiver created for BOOT_COMPLETED
- [ ] Receiver registered in AndroidManifest.xml
- [ ] RECEIVE_BOOT_COMPLETED permission declared
- [ ] Service started from receiver
- [ ] Permission state validated before starting service
- [ ] Boot receiver works on Android 8-14
- [ ] Proper logging for debugging
- [ ] **CRITICAL**: Battery optimization status checked on boot
- [ ] **CRITICAL**: User notified if battery optimization is enabled
- [ ] **CRITICAL**: User guided to disable battery optimization
- [ ] REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission declared
- [ ] Battery exemption guidance activity created

#### Technical Details

**Boot Receiver**: `app/src/main/java/com/phonemanager/receiver/BootReceiver.kt`

```kotlin
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            Timber.w("Received unexpected intent: ${intent.action}")
            return
        }

        Timber.i("Device boot completed, attempting to start LocationTrackingService")

        // Check if we have required permissions
        if (!PermissionUtil.hasAllRequiredPermissions(context)) {
            Timber.w("Cannot start service after boot: missing required permissions")
            return
        }

        // Check if service should be running (from saved state)
        val prefs = context.getSharedPreferences("service_state", Context.MODE_PRIVATE)
        val shouldBeRunning = prefs.getBoolean("service_running", false)

        if (!shouldBeRunning) {
            Timber.d("Service was not running before reboot, not starting")
            return
        }

        // CRITICAL: Check battery optimization status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                Timber.w("Battery optimization is enabled - service may be killed by system")

                // Show notification to user about battery optimization
                showBatteryOptimizationNotification(context)

                // Service can still start, but with reduced reliability
                // User should be prompted to disable optimization
            }
        }

        // Start the service
        try {
            val serviceIntent = Intent(context, LocationTrackingService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
                Timber.i("Started foreground service after boot")
            } else {
                context.startService(serviceIntent)
                Timber.i("Started service after boot")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start service after boot")
        }
    }

    private fun showBatteryOptimizationNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for warnings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "battery_warning",
                "Battery Optimization Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when battery optimization may affect location tracking"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open battery optimization settings
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            null
        }

        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Build notification
        val notification = NotificationCompat.Builder(context, "battery_warning")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Location Tracking Reliability")
            .setContentText("Battery optimization may stop location tracking. Tap to fix.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Battery optimization is enabled for this app. " +
                        "This may cause the location tracking service to stop unexpectedly. " +
                        "Tap here to disable battery optimization for reliable tracking."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(9001, notification)
        Timber.i("Showed battery optimization warning notification")
    }
}
```

**Manifest Registration**: `AndroidManifest.xml`

```xml
<manifest>
    <!-- Permission to receive boot completed broadcast -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <!-- CRITICAL: Permission to request battery optimization exemption -->
    <!-- This allows the app to direct users to disable battery optimization -->
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <application>
        <!-- Boot receiver -->
        <receiver
            android:name=".receiver.BootReceiver"
            android:enabled="true"
            android:exported="true"
            android:directBootAware="false">
            <!-- IMPORTANT: exported="true" required for system to call on boot -->
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.QUICKBOOT_POWERON" />
                <!-- For HTC devices -->
                <action android:name="com.htc.intent.action.QUICKBOOT_POWERON" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

**CRITICAL NOTE ON BATTERY OPTIMIZATION**:

Google Play has strict policies about REQUEST_IGNORE_BATTERY_OPTIMIZATIONS:
- Can ONLY be used if core functionality breaks without exemption
- Location tracking qualifies as valid use case
- Must show clear explanation to users why exemption is needed
- Cannot automatically enable - must direct user to settings

**Manufacturer-Specific Battery Management** (Post-MVP - Epic 0.2.5):

Some manufacturers have additional battery management:
- **Xiaomi**: MIUI Power Management
- **Huawei**: Protected Apps
- **OnePlus**: Battery Optimization
- **Samsung**: App Power Management
- **Oppo**: Battery Optimization

These require OEM-specific workarounds beyond standard Android APIs.

**Service State Management**: Enhance `LocationTrackingService`

```kotlin
class LocationTrackingService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ... existing startup code ...

        // Save that service is running
        saveServiceState(true)

        return START_STICKY
    }

    override fun onDestroy() {
        // Save that service is not running
        saveServiceState(false)

        // ... existing cleanup code ...

        super.onDestroy()
    }

    private fun saveServiceState(isRunning: Boolean) {
        val prefs = getSharedPreferences("service_state", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("service_running", isRunning).apply()
        Timber.d("Service state saved: $isRunning")
    }
}
```

#### Testing Strategy
- Manual test: Start service, reboot device, verify service starts
- Manual test: Service not running, reboot, verify service doesn't start
- Test on Android 8, 10, 12, 14
- Test with missing permissions (verify graceful handling)
- Test on Samsung, Xiaomi devices (boot sequence variations)

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Boot receiver implemented
- [ ] Service starts automatically after boot
- [ ] Service state persisted correctly
- [ ] Works on Android 8-14
- [ ] Tested on multiple device manufacturers
- [ ] Proper error handling
- [ ] Logs provide debugging information

---

### Story 0.2.4.2: Configure Service Auto-restart

**Story ID**: 0.2.4.2
**Priority**: Critical
**Estimate**: 0.5 days
**Assigned To**: TBD
**Depends On**: None (enhances existing service)

#### User Story
```
AS A location tracking service
I WANT to automatically restart if killed by the system
SO THAT tracking continues even if the system terminates the service
```

#### Acceptance Criteria
- [ ] Service returns START_STICKY from onStartCommand
- [ ] Service restart intent properly configured
- [ ] Service state restored after restart
- [ ] Service recreates resources after restart
- [ ] Restart behavior tested and verified
- [ ] Service survives low memory kills
- [ ] Logging indicates restart events

#### Technical Details

**Service Enhancement**: `LocationTrackingService.kt`

```kotlin
class LocationTrackingService : Service() {

    companion object {
        private const val EXTRA_RESTART_FLAG = "restart_flag"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isRestart = intent?.getBooleanExtra(EXTRA_RESTART_FLAG, false) ?: false

        if (isRestart) {
            Timber.i("Service restarted by system")
        } else {
            Timber.i("Service started normally")
        }

        // Validate permissions
        if (!PermissionUtil.hasAllRequiredPermissions(this)) {
            Timber.e("Cannot start service: missing required permissions")
            stopSelf()
            return START_NOT_STICKY // Don't restart if we don't have permissions
        }

        // Start foreground service
        createNotificationChannel()
        startForeground(SERVICE_ID, createNotification())

        // Restore or initialize state
        if (isRestart) {
            restoreServiceState()
        } else {
            initializeService()
        }

        // Save that service is running
        saveServiceState(true)

        // Return START_STICKY to ensure system restarts service
        return START_STICKY
    }

    private fun initializeService() {
        Timber.d("Initializing service")

        // Start location tracking
        locationManager.startLocationUpdates { locationData ->
            serviceScope.launch {
                handleLocationUpdate(locationData)
            }
        }

        // Start connectivity monitoring
        connectivityMonitor.startMonitoring(object : ConnectivityMonitor.ConnectivityCallback {
            override fun onConnectivityChanged(isConnected: Boolean, networkType: ConnectivityMonitor.NetworkType) {
                isOnline = isConnected
                if (isConnected) {
                    serviceScope.launch { processQueue() }
                }
            }
        })
    }

    private fun restoreServiceState() {
        Timber.d("Restoring service state after restart")

        // Re-initialize all components
        initializeService()

        // Check if there's a queue to process
        serviceScope.launch {
            val stats = queueManager.getQueueStats()
            if (stats.pendingCount > 0) {
                Timber.i("Found ${stats.pendingCount} queued locations after restart")
                if (isOnline) {
                    processQueue()
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Timber.w("Task removed, service may be killed soon")

        // Service will be restarted by START_STICKY
        // Ensure state is saved
        saveServiceState(true)
    }
}
```

**Restart Intent Configuration**:

The system automatically handles restart with START_STICKY, but we can configure the restart intent:

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // ... validation and initialization ...

    // Configure restart intent with flag
    val restartIntent = Intent(this, LocationTrackingService::class.java).apply {
        putExtra(EXTRA_RESTART_FLAG, true)
    }

    // This intent will be used when system restarts the service
    // (Note: This is informational - system handles restart automatically with START_STICKY)

    return START_STICKY
}
```

#### Testing Strategy
- Manual test: Force stop service, verify restart
- Manual test: Simulate low memory (fill memory), verify service survives
- Test on Android 8, 10, 12, 14
- Monitor restart frequency over 24 hours
- Verify state restoration after restart

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] START_STICKY configured correctly
- [ ] Service restarts after force stop
- [ ] Service survives low memory kills
- [ ] State properly restored
- [ ] Tested on multiple Android versions
- [ ] Restart events logged

---

### Story 0.2.4.3: Integrate WorkManager Watchdog

**Story ID**: 0.2.4.3
**Priority**: High
**Estimate**: 1.5 days
**Assigned To**: TBD
**Depends On**: 0.2.4.1, 0.2.4.2

#### User Story
```
AS A location tracking system
I WANT a watchdog process to monitor and restart the service if needed
SO THAT the service stays running even if auto-restart fails
```

#### Acceptance Criteria
- [ ] WorkManager dependency added
- [ ] Periodic health check worker created
- [ ] Worker checks if service is running
- [ ] Worker restarts service if not running
- [ ] Health check runs every 15 minutes
- [ ] Worker survives Doze mode
- [ ] Worker provides status reporting

#### Technical Details

**Dependencies**: `app/build.gradle.kts`

```kotlin
dependencies {
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

**Health Check Worker**: `app/src/main/java/com/phonemanager/worker/ServiceHealthCheckWorker.kt`

```kotlin
class ServiceHealthCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("Running service health check")

        return try {
            if (!isServiceRunning()) {
                Timber.w("Service is not running, attempting to start")
                startService()
            } else {
                Timber.d("Service is running normally")
            }

            // Optionally, check queue health
            checkQueueHealth()

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error during health check")
            Result.retry()
        }
    }

    private fun isServiceRunning(): Boolean {
        val manager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        @Suppress("DEPRECATION")
        return manager.getRunningServices(Integer.MAX_VALUE).any { service ->
            service.service.className == LocationTrackingService::class.java.name
        }
    }

    private fun startService() {
        // Check if we should be running
        val prefs = applicationContext.getSharedPreferences("service_state", Context.MODE_PRIVATE)
        val shouldBeRunning = prefs.getBoolean("service_running", false)

        if (!shouldBeRunning) {
            Timber.d("Service is not supposed to be running")
            return
        }

        // Check permissions
        if (!PermissionUtil.hasAllRequiredPermissions(applicationContext)) {
            Timber.w("Cannot start service: missing permissions")
            return
        }

        // Start service
        try {
            val intent = Intent(applicationContext, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
            Timber.i("Service started by watchdog")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start service from watchdog")
        }
    }

    private suspend fun checkQueueHealth() {
        // Optional: Check if queue is growing too large
        val database = AppDatabase.getInstance(applicationContext)
        val queueSize = database.locationQueueDao().getQueueSize()

        if (queueSize > 800) {
            Timber.w("Queue is very large: $queueSize items")
            // Could trigger alert or cleanup
        }
    }
}
```

**Watchdog Manager**: `app/src/main/java/com/phonemanager/worker/WatchdogManager.kt`

```kotlin
object WatchdogManager {

    private const val HEALTH_CHECK_WORK_NAME = "service_health_check"
    private const val HEALTH_CHECK_INTERVAL_MINUTES = 15L

    fun scheduleHealthChecks(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false) // Run even on low battery
            .build()

        val healthCheckRequest = PeriodicWorkRequestBuilder<ServiceHealthCheckWorker>(
            HEALTH_CHECK_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(5, TimeUnit.MINUTES) // First check after 5 minutes
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                HEALTH_CHECK_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Don't replace if already scheduled
                healthCheckRequest
            )

        Timber.i("Scheduled service health checks every $HEALTH_CHECK_INTERVAL_MINUTES minutes")
    }

    fun cancelHealthChecks(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(HEALTH_CHECK_WORK_NAME)

        Timber.i("Cancelled service health checks")
    }

    fun getHealthCheckStatus(context: Context): WorkInfo.State? {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(HEALTH_CHECK_WORK_NAME)
            .get()

        return workInfos.firstOrNull()?.state
    }
}
```

**Application Integration**: `PhoneManagerApplication.kt`

```kotlin
class PhoneManagerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Schedule watchdog health checks
        WatchdogManager.scheduleHealthChecks(this)

        Timber.d("Application initialized")
    }
}
```

**Boot Receiver Enhancement**:

```kotlin
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Timber.i("Device boot completed")

        // Re-schedule watchdog (WorkManager persists, but this ensures it's active)
        WatchdogManager.scheduleHealthChecks(context)

        // ... existing service start logic ...
    }
}
```

#### Testing Strategy
- Unit tests for watchdog logic
- Manual test: Kill service, verify watchdog restarts it within 15 minutes
- Test health check execution during Doze mode
- Monitor watchdog over 24-hour period
- Verify WorkManager persistence across reboots

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] WorkManager integrated
- [ ] Health check worker implemented
- [ ] Watchdog restarts service when down
- [ ] Runs reliably every 15 minutes
- [ ] Survives Doze mode
- [ ] Works after device reboot
- [ ] Status query works correctly

---

### Story 0.2.4.4: Add Doze & Standby Handling

**Story ID**: 0.2.4.4
**Priority**: High
**Estimate**: 1 day
**Assigned To**: TBD
**Depends On**: 0.2.4.3

#### User Story
```
AS A location tracking service
I WANT to handle Doze mode and App Standby gracefully
SO THAT tracking continues reliably even with Android battery optimizations
```

#### Acceptance Criteria
- [ ] Doze mode detection implemented
- [ ] Battery optimization exemption requested (optional)
- [ ] Service behavior documented in Doze mode
- [ ] App Standby bucket monitoring
- [ ] Foreground service provides protection from Doze
- [ ] User guidance for whitelist (if needed)
- [ ] Doze behavior tested

#### Technical Details

**Doze Mode Utility**: `app/src/main/java/com/phonemanager/util/PowerUtil.kt`

```kotlin
object PowerUtil {

    /**
     * Check if app is ignoring battery optimizations
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true // Not applicable on older versions
    }

    /**
     * Check if device is in Doze mode
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun isDeviceInDozeMode(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isDeviceIdleMode
    }

    /**
     * Get app standby bucket
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun getAppStandbyBucket(context: Context): Int {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return usageStatsManager.appStandbyBucket
    }

    /**
     * Get human-readable standby bucket name
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun getStandbyBucketName(bucket: Int): String {
        return when (bucket) {
            UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "Active"
            UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "Working Set"
            UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "Frequent"
            UsageStatsManager.STANDBY_BUCKET_RARE -> "Rare"
            UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "Restricted"
            else -> "Unknown"
        }
    }

    /**
     * Request battery optimization exemption (opens system settings)
     * Should only be called when absolutely necessary and with user education
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestBatteryOptimizationExemption(activity: Activity) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
    }

    /**
     * Open battery optimization settings
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        context.startActivity(intent)
    }
}
```

**Service Monitoring**: Enhance `LocationTrackingService`

```kotlin
class LocationTrackingService : Service() {

    private var lastDozeCheck = 0L
    private val dozeCheckInterval = 5 * 60 * 1000L // 5 minutes

    override fun onCreate() {
        super.onCreate()

        // ... existing initialization ...

        // Monitor Doze mode periodically
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            serviceScope.launch {
                while (isActive) {
                    delay(dozeCheckInterval)
                    checkDozeMode()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkDozeMode() {
        val isInDoze = PowerUtil.isDeviceInDozeMode(this)
        val isExempt = PowerUtil.isIgnoringBatteryOptimizations(this)

        if (isInDoze) {
            Timber.w("Device is in Doze mode, service behavior may be restricted")
        }

        if (!isExempt) {
            Timber.d("App is not exempt from battery optimizations")
        }

        // Log app standby bucket on Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val bucket = PowerUtil.getAppStandbyBucket(this)
            val bucketName = PowerUtil.getStandbyBucketName(bucket)
            Timber.d("App standby bucket: $bucketName")
        }
    }

    private fun createNotification(): Notification {
        // ... existing notification code ...

        // Add action to exempt from battery optimization (optional)
        val exemptIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
        } else {
            null
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText("Tracking your location")
            .setSmallIcon(R.drawable.ic_location_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Add battery optimization action if not exempt
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!PowerUtil.isIgnoringBatteryOptimizations(this)) {
                exemptIntent?.let {
                    val pendingIntent = PendingIntent.getActivity(
                        this,
                        0,
                        it,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(
                        R.drawable.ic_battery,
                        "Optimize Battery",
                        pendingIntent
                    )
                }
            }
        }

        return builder.build()
    }
}
```

**Manifest Permission**:

```xml
<!-- Request permission to ignore battery optimizations -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

**Documentation**: Create `docs/user/DOZE_MODE_HANDLING.md`

```markdown
# Doze Mode and Battery Optimization Handling

## What is Doze Mode?

Android's Doze mode restricts app behavior when the device is stationary and screen off for extended periods. This can affect background location tracking.

## Our Approach

### Foreground Service Protection
The app runs as a foreground service, which provides significant protection from Doze mode:
- Service continues running during Doze
- Location updates continue (though may be delayed)
- Network access maintained

### Limitations During Doze
- Location update frequency may be reduced
- Network requests may be batched
- WorkManager tasks delayed until maintenance windows

### Battery Optimization Exemption
The app can request exemption from battery optimizations, but this is optional:
- Improves reliability on some devices (especially Chinese OEMs)
- User must manually grant via system settings
- Not guaranteed on all devices/manufacturers

## User Guidance

If experiencing issues with location tracking:
1. Open Settings → Apps → Phone Manager
2. Select "Battery"
3. Choose "Unrestricted" or "Not optimized"

## Testing Doze Mode

Enable Doze manually for testing:
```bash
adb shell dumpsys deviceidle force-idle
```

Exit Doze mode:
```bash
adb shell dumpsys deviceidle unforce
```
```

#### Testing Strategy
- Manual Doze mode testing using ADB commands
- Monitor service during Doze mode (6+ hours)
- Test on devices known for aggressive optimization (Xiaomi, Huawei)
- Verify foreground service protection
- Test battery exemption request flow

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Doze mode detection works
- [ ] Service survives Doze mode
- [ ] Standby bucket monitoring works
- [ ] Battery exemption request functional
- [ ] Documentation complete
- [ ] Tested in real Doze conditions
- [ ] Works on aggressive optimization devices

---

### Story 0.2.4.5: Implement Service State Persistence

**Story ID**: 0.2.4.5
**Priority**: Medium-High
**Estimate**: 0.5 days
**Assigned To**: TBD
**Depends On**: None (enhances existing service)

#### User Story
```
AS A location tracking service
I WANT to persist critical state across restarts
SO THAT service behavior is consistent after crashes or reboots
```

#### Acceptance Criteria
- [ ] Service running state persisted
- [ ] Last known location persisted
- [ ] Configuration state persisted
- [ ] Queue metadata persisted (already in database)
- [ ] State restored on service restart
- [ ] Corrupted state handled gracefully
- [ ] State cleanup on service stop

#### Technical Details

**State Manager**: `app/src/main/java/com/phonemanager/state/ServiceStateManager.kt`

```kotlin
class ServiceStateManager(context: Context) {

    private val prefs = context.getSharedPreferences("service_state", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVICE_RUNNING = "service_running"
        private const val KEY_LAST_LOCATION_LAT = "last_location_lat"
        private const val KEY_LAST_LOCATION_LON = "last_location_lon"
        private const val KEY_LAST_LOCATION_TIME = "last_location_time"
        private const val KEY_LAST_LOCATION_ACCURACY = "last_location_accuracy"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_TOTAL_LOCATIONS_CAPTURED = "total_locations_captured"
        private const val KEY_TOTAL_LOCATIONS_SENT = "total_locations_sent"
    }

    fun saveServiceRunning(isRunning: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_RUNNING, isRunning).apply()
    }

    fun isServiceRunning(): Boolean {
        return prefs.getBoolean(KEY_SERVICE_RUNNING, false)
    }

    fun saveLastLocation(locationData: LocationData) {
        prefs.edit().apply {
            putFloat(KEY_LAST_LOCATION_LAT, locationData.latitude.toFloat())
            putFloat(KEY_LAST_LOCATION_LON, locationData.longitude.toFloat())
            putLong(KEY_LAST_LOCATION_TIME, locationData.timestamp)
            putFloat(KEY_LAST_LOCATION_ACCURACY, locationData.accuracy)
        }.apply()
    }

    fun getLastLocation(): LocationData? {
        if (!prefs.contains(KEY_LAST_LOCATION_LAT)) {
            return null
        }

        return try {
            LocationData(
                latitude = prefs.getFloat(KEY_LAST_LOCATION_LAT, 0f).toDouble(),
                longitude = prefs.getFloat(KEY_LAST_LOCATION_LON, 0f).toDouble(),
                timestamp = prefs.getLong(KEY_LAST_LOCATION_TIME, 0),
                accuracy = prefs.getFloat(KEY_LAST_LOCATION_ACCURACY, 0f)
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore last location")
            null
        }
    }

    fun saveServiceStartTime(startTime: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_START_TIME, startTime).apply()
    }

    fun getServiceStartTime(): Long {
        return prefs.getLong(KEY_START_TIME, 0)
    }

    fun getServiceUptime(): Long {
        val startTime = getServiceStartTime()
        if (startTime == 0L) return 0
        return System.currentTimeMillis() - startTime
    }

    fun incrementLocationsCaptured() {
        val current = prefs.getInt(KEY_TOTAL_LOCATIONS_CAPTURED, 0)
        prefs.edit().putInt(KEY_TOTAL_LOCATIONS_CAPTURED, current + 1).apply()
    }

    fun incrementLocationsSent() {
        val current = prefs.getInt(KEY_TOTAL_LOCATIONS_SENT, 0)
        prefs.edit().putInt(KEY_TOTAL_LOCATIONS_SENT, current + 1).apply()
    }

    fun getTotalLocationsCaptured(): Int {
        return prefs.getInt(KEY_TOTAL_LOCATIONS_CAPTURED, 0)
    }

    fun getTotalLocationsSent(): Int {
        return prefs.getInt(KEY_TOTAL_LOCATIONS_SENT, 0)
    }

    fun getServiceStats(): ServiceStats {
        return ServiceStats(
            isRunning = isServiceRunning(),
            startTime = getServiceStartTime(),
            uptime = getServiceUptime(),
            totalLocationsCaptured = getTotalLocationsCaptured(),
            totalLocationsSent = getTotalLocationsSent(),
            lastLocation = getLastLocation()
        )
    }

    fun clearState() {
        prefs.edit().clear().apply()
        Timber.d("Service state cleared")
    }
}

data class ServiceStats(
    val isRunning: Boolean,
    val startTime: Long,
    val uptime: Long,
    val totalLocationsCaptured: Int,
    val totalLocationsSent: Int,
    val lastLocation: LocationData?
) {
    override fun toString(): String {
        return "Service Stats: Running=$isRunning, Uptime=${uptime / 1000 / 60}min, " +
               "Captured=$totalLocationsCaptured, Sent=$totalLocationsSent"
    }
}
```

**Service Integration**:

```kotlin
class LocationTrackingService : Service() {

    private lateinit var stateManager: ServiceStateManager

    override fun onCreate() {
        super.onCreate()

        stateManager = ServiceStateManager(this)

        // ... other initialization ...
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ... existing code ...

        stateManager.saveServiceRunning(true)
        stateManager.saveServiceStartTime()

        return START_STICKY
    }

    private suspend fun handleLocationUpdate(locationData: LocationData) {
        Timber.i("Received location: ${locationData.latitude}, ${locationData.longitude}")

        // Save last location
        stateManager.saveLastLocation(locationData)
        stateManager.incrementLocationsCaptured()

        // Attempt transmission
        if (!isOnline) {
            queueManager.enqueue(locationData)
            return
        }

        val result = networkManager.sendLocation(locationData)
        result.onSuccess {
            stateManager.incrementLocationsSent()
        }.onFailure {
            queueManager.enqueue(locationData)
        }
    }

    override fun onDestroy() {
        stateManager.saveServiceRunning(false)

        // Log final statistics
        val stats = stateManager.getServiceStats()
        Timber.i("Service stopping. Final stats: $stats")

        // ... existing cleanup ...

        super.onDestroy()
    }
}
```

#### Testing Strategy
- Unit tests for ServiceStateManager
- Test state persistence across service restarts
- Test state restoration after reboot
- Test corrupted state handling
- Verify statistics accuracy

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing (>85% coverage)
- [ ] State persists across restarts
- [ ] State restored correctly
- [ ] Corrupted state handled
- [ ] Statistics accurate
- [ ] Cleanup works properly

---

### Story 0.2.4.6: Multi-reboot & Reliability Testing

**Story ID**: 0.2.4.6
**Priority**: High
**Estimate**: 1 day
**Assigned To**: TBD
**Depends On**: All previous stories in Epic 0.2.4

#### User Story
```
AS A development team
I WANT to verify the service reliably auto-starts and persists across multiple reboots and lifecycle events
SO THAT I can confidently release the MVP
```

#### Acceptance Criteria
- [ ] Service survives 10+ consecutive reboots
- [ ] Service runs continuously for 48+ hours
- [ ] Force stop and recovery tested
- [ ] Low memory kill and recovery tested
- [ ] Watchdog effectiveness verified
- [ ] State persistence verified across all events
- [ ] Performance metrics within acceptable ranges
- [ ] No critical or high bugs found

#### Technical Details

**Multi-Reboot Test Plan**:

```markdown
# Multi-Reboot and Reliability Test Plan

## Test Environment
- Device: Real Android device (not emulator)
- Android versions: 10, 12, 14
- Manufacturers: Google Pixel, Samsung, Xiaomi
- Test duration: 48+ hours

## Test Scenarios

### Scenario 1: Ten Consecutive Reboots
1. Start service and verify it's running
2. Reboot device
3. Verify service starts automatically within 60 seconds
4. Repeat 10 times
5. Check logs for any failures

**Success Criteria**:
- Service starts after every reboot
- No crashes or errors
- All state persisted correctly
- Queue integrity maintained

### Scenario 2: 48-Hour Continuous Operation
1. Start service
2. Let run for 48 hours without intervention
3. Monitor every 6 hours:
   - Service still running
   - Location updates continuing
   - Queue processing working
   - Memory usage stable
   - Battery drain acceptable

**Success Criteria**:
- Service uptime: 100%
- Location capture rate: >95%
- Memory usage: <60MB average
- Battery drain: <10% per 24 hours
- No memory leaks

### Scenario 3: Force Stop Recovery
1. Start service
2. Force stop via Settings
3. Wait 15 minutes for watchdog
4. Verify service restarted
5. Repeat 5 times

**Success Criteria**:
- Watchdog restarts service within 15 minutes
- Service resumes normal operation
- Queue processing continues
- No data loss

### Scenario 4: Low Memory Kill
1. Start service
2. Fill device memory (open many apps)
3. Monitor for service kill
4. Verify service restarts automatically
5. Check state restoration

**Success Criteria**:
- Service restarts via START_STICKY
- State restored correctly
- Queue integrity maintained
- Continues operation normally

### Scenario 5: Mixed Stress Test
1. Start service
2. Random events over 12 hours:
   - 3 reboots at random times
   - 2 force stops
   - Toggle airplane mode 10 times
   - Fill memory 2 times
   - Doze mode simulation

**Success Criteria**:
- Service recovers from all events
- No data loss
- Queue processed correctly
- No crashes

### Scenario 6: Watchdog Effectiveness
1. Start service
2. Manually kill service process (adb shell kill)
3. Monitor watchdog reaction
4. Measure time to recovery
5. Repeat 10 times

**Success Criteria**:
- Watchdog detects service down
- Service restarted within 15 minutes
- 100% recovery success rate
- No false positives

## Performance Metrics to Collect

### Service Reliability
- Auto-start success rate after boot
- Watchdog restart success rate
- Service uptime percentage
- Recovery time from failures

### Performance
- Average memory usage
- Peak memory usage
- Battery drain per 24 hours
- CPU usage (average and peak)

### Data Integrity
- Location capture success rate
- Transmission success rate
- Queue integrity (no corruption)
- State persistence accuracy

## Monitoring Commands

### Check if service is running
```bash
adb shell dumpsys activity services | grep LocationTrackingService
```

### Check memory usage
```bash
adb shell dumpsys meminfo com.phonemanager
```

### Check battery stats
```bash
adb shell dumpsys batterystats --charged com.phonemanager
```

### Monitor logs continuously
```bash
adb logcat -s LocationTrackingService:D QueueManager:D WatchdogManager:D
```

### Simulate low memory
```bash
adb shell am send-trim-memory com.phonemanager RUNNING_CRITICAL
```

### Kill service process
```bash
adb shell ps | grep com.phonemanager | awk '{print $2}' | xargs adb shell kill
```

### Simulate Doze mode
```bash
adb shell dumpsys deviceidle force-idle
```

## Test Report Template

```
# MVP Reliability Test Report

## Test Summary
- Test Duration: XX hours
- Devices Tested: XX
- Android Versions: XX
- Test Date: YYYY-MM-DD

## Results

### Auto-start Success Rate
- Reboots Tested: XX
- Successful Starts: XX
- Success Rate: XX%

### Service Uptime
- Total Runtime: XX hours
- Downtime: XX minutes
- Uptime Percentage: XX%

### Watchdog Effectiveness
- Service Kills: XX
- Successful Recoveries: XX
- Average Recovery Time: XX minutes

### Performance Metrics
- Average Memory: XX MB
- Peak Memory: XX MB
- Battery Drain: XX% per 24h
- Location Capture Rate: XX%
- Transmission Success Rate: XX%

### Issues Found
1. [Issue description]
2. [Issue description]

### Recommendations
1. [Recommendation]
2. [Recommendation]

## Conclusion
[Pass/Fail] - [Reasoning]
```
```

**Automated Test Script**: `scripts/reliability_test.sh`

```bash
#!/bin/bash

echo "=== Location Tracking Service Reliability Test ==="
echo "This script will run automated reliability tests"
echo ""

# Configuration
PACKAGE_NAME="com.phonemanager"
SERVICE_NAME="com.phonemanager.service.LocationTrackingService"
NUM_REBOOTS=10

# Function to check if service is running
check_service() {
    adb shell dumpsys activity services | grep -q "$SERVICE_NAME"
    return $?
}

# Function to reboot and wait
reboot_and_wait() {
    local reboot_num=$1
    echo "[$reboot_num/$NUM_REBOOTS] Rebooting device..."
    adb reboot
    echo "Waiting for device to come back online..."
    adb wait-for-device
    sleep 30 # Wait for boot to complete
}

# Test 1: Multi-Reboot Test
echo "=== Test 1: Multi-Reboot Test ==="
echo "Will reboot device $NUM_REBOOTS times and verify service starts"
echo ""

successful_starts=0

for i in $(seq 1 $NUM_REBOOTS); do
    reboot_and_wait $i

    # Wait up to 60 seconds for service to start
    echo "Checking if service started..."
    for j in $(seq 1 12); do
        if check_service; then
            echo "✓ Service started successfully"
            ((successful_starts++))
            break
        fi
        echo "  Waiting... ($j/12)"
        sleep 5
    done

    if ! check_service; then
        echo "✗ Service failed to start after reboot $i"
    fi

    echo ""
done

echo "=== Test Results ==="
echo "Total reboots: $NUM_REBOOTS"
echo "Successful starts: $successful_starts"
echo "Success rate: $(echo "scale=2; $successful_starts * 100 / $NUM_REBOOTS" | bc)%"

if [ $successful_starts -eq $NUM_REBOOTS ]; then
    echo "✓ TEST PASSED"
    exit 0
else
    echo "✗ TEST FAILED"
    exit 1
fi
```

#### Testing Strategy
- Execute all test scenarios
- Run on multiple devices and Android versions
- Collect comprehensive performance data
- Generate detailed test report
- Review with team before MVP release

#### Definition of Done
- [ ] All test scenarios completed
- [ ] All success criteria met
- [ ] Performance metrics within acceptable ranges
- [ ] Test report generated and reviewed
- [ ] No critical or high severity bugs
- [ ] Service ready for MVP release
- [ ] Known limitations documented

---

## Epic Completion Criteria

This epic (and the entire MVP) is considered complete when:

### Functional Criteria
- [ ] All 6 stories completed and closed
- [ ] Service starts automatically on boot
- [ ] Service auto-restarts after crashes/kills
- [ ] Watchdog monitors and recovers service
- [ ] Doze mode handled appropriately
- [ ] Service state persists across restarts
- [ ] Multi-reboot testing successful (10+ reboots)
- [ ] 48-hour continuous operation test passed

### Quality Criteria
- [ ] Code coverage >70%
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Reliability tests passing
- [ ] No critical or high priority bugs
- [ ] Code reviewed and approved

### Performance Criteria
- [ ] Auto-start success rate >98%
- [ ] Watchdog recovery time <15 minutes
- [ ] Service uptime >99% over 48 hours
- [ ] Memory usage <60MB average
- [ ] Battery drain <10% per 24 hours

### Documentation Criteria
- [ ] Auto-start mechanism documented
- [ ] Watchdog behavior documented
- [ ] Doze mode handling documented
- [ ] Known limitations documented
- [ ] User guidance for battery optimization

---

## Risks & Mitigations

### Risk: OEM Battery Optimization Interference
**Severity**: High
**Mitigation**: Foreground service, watchdog, battery exemption guidance
**Status**: Partially mitigated, OEM variations may still cause issues

### Risk: Watchdog May Not Run in Extreme Doze
**Severity**: Medium
**Mitigation**: WorkManager designed for Doze, foreground service provides primary protection
**Status**: Accepted limitation, documented

### Risk: User Can Disable Auto-start
**Severity**: Low-Medium
**Mitigation**: Cannot prevent, provide user guidance, monitor state
**Status**: Accepted limitation

### Risk: Permissions Revoked After Boot
**Severity**: Low
**Mitigation**: Validate permissions before starting service, handle gracefully
**Status**: Mitigated in code

---

## Dependencies

### External Dependencies
- Android system boot sequence
- WorkManager reliability
- Device manufacturer boot policies

### Internal Dependencies
- Epic 0.2.1 (Service Foundation) - Complete
- Epic 0.2.2 (Continuous Tracking) - Complete
- Epic 0.2.3 (Reliability & Queue) - Complete

### Blocks
- No epics blocked (MVP complete after this epic)
- Epic 0.2.5 and 0.2.6 are post-MVP enhancements

---

## MVP COMPLETION MILESTONE

🎉 **Completion of Epic 0.2.4 marks the MVP release milestone!**

After this epic, the service provides:
- ✅ Continuous location tracking
- ✅ Network transmission to remote server
- ✅ Offline queue with automatic retry
- ✅ Auto-start on boot
- ✅ Self-healing via watchdog
- ✅ Persistence across all lifecycle events

**Post-MVP Enhancements** (Optional):
- Epic 0.2.5: Battery Optimization & Performance
- Epic 0.2.6: Configuration & Operational Support

---

## Notes for Post-MVP

While the MVP is functionally complete, consider these enhancements:

**Epic 0.2.5 (Battery Optimization)**:
- Adaptive location strategy based on movement
- Geofencing for stationary detection
- Battery-aware update frequency
- Advanced Doze optimization
- OEM-specific workarounds

**Epic 0.2.6 (Configuration)**:
- Server endpoint configuration UI
- Update interval configuration
- Build variants for environments
- Advanced logging and monitoring
- Health check dashboard

---

**Last Updated**: 2025-11-11
**Approved By**: TBD
**Ready for Sprint Planning**: Yes (after Epic 0.2.3 complete)
**MVP Status**: Final MVP Epic ✨
