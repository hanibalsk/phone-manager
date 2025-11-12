# Epic 0.2.3: Reliability & Offline Queue

**Epic ID**: 0.2.3
**Parent Epic**: [Epic 0.2: Background Location Tracking Service](./Epic-0.2-Location-Tracking-Service.md)
**Status**: Blocked by Epic 0.2.2
**Priority**: Critical (MVP Blocker)
**Estimated Duration**: 5-7 days
**Dependencies**: Epic 0.2.2 (Continuous Tracking & Network) must be complete

---

## Epic Goal

Ensure reliable location data delivery through local queuing, offline detection, and automatic retry mechanisms. This epic transforms the service from a basic tracker into a production-ready system that handles real-world network conditions including intermittent connectivity, server outages, and offline periods.

---

## Epic Scope

This epic focuses on:
- Local database implementation with Room
- Queue management system for failed/offline locations
- Network connectivity monitoring
- Retry logic with exponential backoff
- Automatic queue processing when network available
- Queue size limits and cleanup

What's NOT in scope:
- Auto-start on boot (Epic 0.2.4)
- Battery optimization (Epic 0.2.5)
- Configuration UI (Epic 0.2.6)
- Remote server implementation

---

## Stories

### Story 0.2.3.1: Setup Room Database

**Story ID**: 0.2.3.1
**Priority**: Critical
**Estimate**: 1 day
**Assigned To**: TBD

#### User Story
```
AS A location tracking service
I WANT to persist location data locally using Room database
SO THAT I can store locations when offline and ensure no data loss
```

#### Acceptance Criteria
- [ ] Room dependencies added to project
- [ ] Database class created with proper configuration
- [ ] LocationQueue entity defined with all required fields
- [ ] DAO interface created with necessary operations
- [ ] **CRITICAL**: Database migration strategy defined and documented
- [ ] **CRITICAL**: Schema export enabled (`exportSchema = true`)
- [ ] **CRITICAL**: Example migrations provided (1→2, 2→3)
- [ ] **CRITICAL**: Production build crashes if migration missing (no fallbackToDestructiveMigration)
- [ ] **CRITICAL**: Schema files exported to `app/schemas/` and committed to VCS
- [ ] Database singleton properly implemented
- [ ] Database operations tested
- [ ] Migration testing helper method provided

#### Technical Details

**Dependencies**: `app/build.gradle.kts`

```kotlin
dependencies {
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Kotlin Symbol Processing (KSP) - if not already added
    // Add to plugins block at top:
    // id("com.google.devtools.ksp") version "2.0.21-1.0.25"
}
```

**Location Queue Entity**: `app/src/main/java/com/phonemanager/data/db/entity/LocationQueueEntity.kt`

```kotlin
@Entity(tableName = "location_queue")
data class LocationQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long, // Location capture time (Unix timestamp)

    @ColumnInfo(name = "accuracy")
    val accuracy: Float,

    @ColumnInfo(name = "altitude")
    val altitude: Double? = null,

    @ColumnInfo(name = "bearing")
    val bearing: Float? = null,

    @ColumnInfo(name = "speed")
    val speed: Float? = null,

    @ColumnInfo(name = "provider")
    val provider: String? = null,

    @ColumnInfo(name = "queued_at")
    val queuedAt: Long = System.currentTimeMillis(), // When added to queue

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "last_retry_at")
    val lastRetryAt: Long? = null,

    @ColumnInfo(name = "status")
    val status: String = QueueStatus.PENDING.name // PENDING, PROCESSING, FAILED
) {
    companion object {
        fun fromLocationData(locationData: LocationData, deviceId: String): LocationQueueEntity {
            return LocationQueueEntity(
                deviceId = deviceId,
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                timestamp = locationData.timestamp,
                accuracy = locationData.accuracy,
                altitude = locationData.altitude,
                bearing = locationData.bearing,
                speed = locationData.speed,
                provider = locationData.provider
            )
        }
    }

    fun toLocationData(): LocationData {
        return LocationData(
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            accuracy = accuracy,
            altitude = altitude,
            bearing = bearing,
            speed = speed,
            provider = provider
        )
    }
}

enum class QueueStatus {
    PENDING,    // Waiting to be sent
    PROCESSING, // Currently being sent
    FAILED      // Exceeded max retries
}
```

**DAO Interface**: `app/src/main/java/com/phonemanager/data/db/dao/LocationQueueDao.kt`

```kotlin
@Dao
interface LocationQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: LocationQueueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<LocationQueueEntity>): List<Long>

    @Update
    suspend fun update(location: LocationQueueEntity)

    @Delete
    suspend fun delete(location: LocationQueueEntity)

    @Query("SELECT * FROM location_queue WHERE status = :status ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingLocations(status: String = QueueStatus.PENDING.name, limit: Int = 100): List<LocationQueueEntity>

    @Query("SELECT * FROM location_queue WHERE id = :id")
    suspend fun getById(id: Long): LocationQueueEntity?

    @Query("SELECT COUNT(*) FROM location_queue WHERE status = :status")
    suspend fun getQueueSize(status: String = QueueStatus.PENDING.name): Int

    @Query("SELECT COUNT(*) FROM location_queue")
    suspend fun getTotalCount(): Int

    @Query("DELETE FROM location_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM location_queue WHERE status = :status")
    suspend fun deleteByStatus(status: String)

    @Query("DELETE FROM location_queue WHERE timestamp < :timestampBefore")
    suspend fun deleteOldLocations(timestampBefore: Long): Int

    @Query("SELECT * FROM location_queue ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): LocationQueueEntity?

    @Query("UPDATE location_queue SET retry_count = retry_count + 1, last_retry_at = :retryTime WHERE id = :id")
    suspend fun incrementRetryCount(id: Long, retryTime: Long = System.currentTimeMillis())

    @Query("UPDATE location_queue SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: Long, newStatus: String)
}
```

**Database Class**: `app/src/main/java/com/phonemanager/data/db/AppDatabase.kt`

```kotlin
@Database(
    entities = [LocationQueueEntity::class],
    version = 1,
    exportSchema = true  // CRITICAL: Export schema for migration testing
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun locationQueueDao(): LocationQueueDao

    companion object {
        private const val DATABASE_NAME = "phone_manager.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * CRITICAL: Database Migration Strategy
         *
         * Room requires explicit migration paths for schema changes.
         * Without migrations, all data is lost on app updates.
         *
         * Migration Path for Future Versions:
         * - Version 1: Initial schema (location_queue table)
         * - Version 2: (Example) Add index on timestamp
         * - Version 3: (Example) Add new column for sync status
         */

        // Example Migration 1→2: Add index for performance
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add index on timestamp for faster queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_location_queue_timestamp " +
                    "ON location_queue(timestamp)"
                )
            }
        }

        // Example Migration 2→3: Add new column
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new column with default value
                database.execSQL(
                    "ALTER TABLE location_queue " +
                    "ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'pending'"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // DEVELOPMENT: Use fallbackToDestructiveMigration() for rapid iteration
                    // PRODUCTION: Remove this and add proper migrations
                    .apply {
                        if (BuildConfig.DEBUG) {
                            fallbackToDestructiveMigration() // Development only - loses data!
                        } else {
                            // Production: Add migration paths as they are defined
                            // addMigrations(MIGRATION_1_2, MIGRATION_2_3, ...)
                            // If no migration exists, app will crash (better than data loss)
                        }
                    }
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * For testing migrations, use this method
         */
        fun getTestInstance(context: Context, vararg migrations: Migration): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .addMigrations(*migrations)
                .build()
        }
    }
}

/**
 * Schema Export Configuration
 *
 * Add to app/build.gradle.kts:
 *
 * android {
 *     defaultConfig {
 *         // Export schema for migration testing
 *         javaCompileOptions {
 *             annotationProcessorOptions {
 *                 arguments["room.schemaLocation"] = "$projectDir/schemas"
 *             }
 *         }
 *     }
 * }
 *
 * This exports JSON schema files to app/schemas/ for each database version.
 * These files should be committed to version control for migration testing.
 */
```

#### Testing Strategy
- Unit tests for entity conversion methods
- DAO tests using in-memory database
- Test all CRUD operations
- Test query methods
- Test concurrent access

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Room dependencies added
- [ ] Database and DAO created
- [ ] Unit tests passing (>80% coverage)
- [ ] DAO tests passing
- [ ] Database operations work correctly
- [ ] No data corruption issues
- [ ] Migration strategy documented

---

### Story 0.2.3.2: Implement Queue Management

**Story ID**: 0.2.3.2
**Priority**: Critical
**Estimate**: 1.5 days
**Assigned To**: TBD
**Depends On**: 0.2.3.1

#### User Story
```
AS A location tracking service
I WANT to manage a queue of locations that failed to transmit
SO THAT no location data is lost during network outages
```

#### Acceptance Criteria
- [ ] QueueManager class created with enqueue/dequeue operations
- [ ] Locations automatically queued when transmission fails
- [ ] Queue size monitored and limited (max 1000 entries)
- [ ] Oldest entries pruned when queue full
- [ ] Queue persistence across app restarts
- [ ] Failed items marked appropriately
- [ ] Queue statistics available

#### Technical Details

**Queue Manager**: `app/src/main/java/com/phonemanager/data/queue/QueueManager.kt`

```kotlin
class QueueManager(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val dao = database.locationQueueDao()
    private val deviceId = DeviceUtil.getDeviceId(context)

    companion object {
        private const val MAX_QUEUE_SIZE = 1000
        private const val MAX_RETRY_COUNT = 5
        private const val MAX_AGE_DAYS = 7
        private const val TAG = "QueueManager"
    }

    /**
     * Add a location to the queue
     */
    suspend fun enqueue(locationData: LocationData): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                // Check queue size and cleanup if needed
                val currentSize = dao.getQueueSize()
                if (currentSize >= MAX_QUEUE_SIZE) {
                    Timber.w("Queue full ($currentSize items), removing oldest entries")
                    pruneOldestEntries(100) // Remove oldest 100 entries
                }

                // Insert location into queue
                val entity = LocationQueueEntity.fromLocationData(locationData, deviceId)
                val id = dao.insert(entity)

                Timber.d("Location queued with ID: $id (queue size: ${currentSize + 1})")
                Result.success(id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to enqueue location")
                Result.failure(e)
            }
        }
    }

    /**
     * Get pending locations for processing
     */
    suspend fun getPendingLocations(limit: Int = 50): List<LocationQueueEntity> {
        return withContext(Dispatchers.IO) {
            try {
                dao.getPendingLocations(QueueStatus.PENDING.name, limit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to get pending locations")
                emptyList()
            }
        }
    }

    /**
     * Mark location as successfully sent and remove from queue
     */
    suspend fun markAsSent(id: Long): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                dao.deleteById(id)
                Timber.d("Location $id marked as sent and removed from queue")
                Result.success(true)
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark location as sent")
                Result.failure(e)
            }
        }
    }

    /**
     * Mark location as failed after retry
     */
    suspend fun markAsFailed(id: Long): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val location = dao.getById(id)
                if (location != null) {
                    if (location.retryCount >= MAX_RETRY_COUNT) {
                        // Exceeded max retries, mark as failed
                        dao.updateStatus(id, QueueStatus.FAILED.name)
                        Timber.w("Location $id exceeded max retries, marked as failed")
                    } else {
                        // Increment retry count
                        dao.incrementRetryCount(id)
                        Timber.d("Location $id retry count incremented to ${location.retryCount + 1}")
                    }
                    Result.success(true)
                } else {
                    Result.failure(Exception("Location not found"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark location as failed")
                Result.failure(e)
            }
        }
    }

    /**
     * Get queue statistics
     */
    suspend fun getQueueStats(): QueueStats {
        return withContext(Dispatchers.IO) {
            try {
                val totalCount = dao.getTotalCount()
                val pendingCount = dao.getQueueSize(QueueStatus.PENDING.name)
                val failedCount = dao.getQueueSize(QueueStatus.FAILED.name)
                val latestEntry = dao.getLatest()

                QueueStats(
                    totalCount = totalCount,
                    pendingCount = pendingCount,
                    failedCount = failedCount,
                    latestTimestamp = latestEntry?.timestamp
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to get queue stats")
                QueueStats()
            }
        }
    }

    /**
     * Clean up old entries (older than MAX_AGE_DAYS)
     */
    suspend fun cleanupOldEntries(): Int {
        return withContext(Dispatchers.IO) {
            try {
                val cutoffTime = System.currentTimeMillis() - (MAX_AGE_DAYS * 24 * 60 * 60 * 1000)
                val deletedCount = dao.deleteOldLocations(cutoffTime)
                if (deletedCount > 0) {
                    Timber.i("Cleaned up $deletedCount old location entries")
                }
                deletedCount
            } catch (e: Exception) {
                Timber.e(e, "Failed to cleanup old entries")
                0
            }
        }
    }

    /**
     * Prune oldest entries to make room
     */
    private suspend fun pruneOldestEntries(count: Int) {
        try {
            val pendingLocations = dao.getPendingLocations(limit = count)
            pendingLocations.forEach { location ->
                dao.delete(location)
            }
            Timber.i("Pruned $count oldest entries from queue")
        } catch (e: Exception) {
            Timber.e(e, "Failed to prune oldest entries")
        }
    }

    /**
     * Clear all failed entries
     */
    suspend fun clearFailedEntries(): Int {
        return withContext(Dispatchers.IO) {
            try {
                val failedCount = dao.getQueueSize(QueueStatus.FAILED.name)
                dao.deleteByStatus(QueueStatus.FAILED.name)
                Timber.i("Cleared $failedCount failed entries")
                failedCount
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear failed entries")
                0
            }
        }
    }
}

data class QueueStats(
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val latestTimestamp: Long? = null
) {
    override fun toString(): String {
        return "Queue: $pendingCount pending, $failedCount failed, $totalCount total"
    }
}
```

**Service Integration**:

```kotlin
class LocationTrackingService : Service() {

    private lateinit var queueManager: QueueManager

    override fun onCreate() {
        super.onCreate()
        locationManager = LocationManager(this)
        networkManager = NetworkManager(this)
        queueManager = QueueManager(this)

        // Periodic cleanup of old entries
        serviceScope.launch {
            while (isActive) {
                delay(24 * 60 * 60 * 1000) // Once per day
                queueManager.cleanupOldEntries()
            }
        }
    }

    private suspend fun handleLocationUpdate(locationData: LocationData) {
        Timber.i("Received location: ${locationData.latitude}, ${locationData.longitude}")

        if (!isNetworkAvailable()) {
            Timber.w("No network available, queueing location")
            queueManager.enqueue(locationData)
            return
        }

        // Attempt to send location
        val result = networkManager.sendLocation(locationData)

        result.onSuccess {
            Timber.i("Location transmitted successfully")
        }.onFailure { exception ->
            Timber.e(exception, "Failed to transmit location, queueing for retry")
            queueManager.enqueue(locationData)
        }
    }
}
```

#### Testing Strategy
- Unit tests for QueueManager methods
- Test queue size limits
- Test pruning logic
- Test queue statistics
- Test concurrent enqueue/dequeue
- Integration test with database

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing (>80% coverage)
- [ ] Queue management works correctly
- [ ] Size limits enforced
- [ ] Pruning works as expected
- [ ] Statistics accurate
- [ ] Handles concurrent access safely

---

### Story 0.2.3.3: Add Network Connectivity Monitoring

**Story ID**: 0.2.3.3
**Priority**: Critical
**Estimate**: 1 day
**Assigned To**: TBD
**Depends On**: None (can be parallel with 0.2.3.1)

#### User Story
```
AS A location tracking service
I WANT to monitor network connectivity changes in real-time
SO THAT I can queue locations when offline and process queue when online
```

#### Acceptance Criteria
- [ ] ConnectivityMonitor class created
- [ ] Network state changes detected in real-time
- [ ] Callbacks triggered on connectivity changes
- [ ] Online/offline state tracked accurately
- [ ] Network type identified (WiFi vs cellular)
- [ ] Works across Android versions (API 21+)
- [ ] Proper cleanup on service destroy

#### Technical Details

**Connectivity Monitor**: `app/src/main/java/com/phonemanager/network/ConnectivityMonitor.kt`

```kotlin
class ConnectivityMonitor(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var callback: ConnectivityCallback? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _isConnected = MutableStateFlow(isNetworkAvailable())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    interface ConnectivityCallback {
        fun onConnectivityChanged(isConnected: Boolean, networkType: NetworkType)
    }

    enum class NetworkType {
        WIFI,
        CELLULAR,
        NONE
    }

    fun startMonitoring(callback: ConnectivityCallback) {
        this.callback = callback

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerNetworkCallback()
        } else {
            // Fallback for older Android versions
            registerLegacyReceiver()
        }

        Timber.d("Started network connectivity monitoring")
    }

    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            unregisterNetworkCallback()
        }

        callback = null
        Timber.d("Stopped network connectivity monitoring")
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                handleConnectivityChange(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                handleConnectivityChange(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val isConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                handleConnectivityChange(isConnected)
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }

    private fun registerLegacyReceiver() {
        // For older Android versions, use BroadcastReceiver
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                handleConnectivityChange(isNetworkAvailable())
            }
        }, filter)
    }

    private fun handleConnectivityChange(isConnected: Boolean) {
        _isConnected.value = isConnected

        val networkType = if (isConnected) {
            getCurrentNetworkType()
        } else {
            NetworkType.NONE
        }

        Timber.d("Network connectivity changed: $isConnected, type: $networkType")
        callback?.onConnectivityChanged(isConnected, networkType)
    }

    private fun isNetworkAvailable(): Boolean {
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

    private fun getCurrentNetworkType(): NetworkType {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                else -> NetworkType.NONE
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return NetworkType.NONE

            return when (networkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR
                else -> NetworkType.NONE
            }
        }
    }
}
```

**Service Integration**:

```kotlin
class LocationTrackingService : Service() {

    private lateinit var connectivityMonitor: ConnectivityMonitor
    private var isOnline = false

    override fun onCreate() {
        super.onCreate()
        locationManager = LocationManager(this)
        networkManager = NetworkManager(this)
        queueManager = QueueManager(this)
        connectivityMonitor = ConnectivityMonitor(this)

        // Start monitoring connectivity
        connectivityMonitor.startMonitoring(object : ConnectivityMonitor.ConnectivityCallback {
            override fun onConnectivityChanged(isConnected: Boolean, networkType: ConnectivityMonitor.NetworkType) {
                isOnline = isConnected
                Timber.i("Connectivity changed: $isConnected ($networkType)")

                if (isConnected) {
                    // Network is back, process queue
                    serviceScope.launch {
                        processQueue()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        connectivityMonitor.stopMonitoring()
        locationManager.stopLocationUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun handleLocationUpdate(locationData: LocationData) {
        if (!isOnline) {
            Timber.w("Offline, queueing location")
            queueManager.enqueue(locationData)
            return
        }

        // Try to send
        val result = networkManager.sendLocation(locationData)
        result.onFailure {
            queueManager.enqueue(locationData)
        }
    }

    private suspend fun processQueue() {
        // Will be implemented in Story 0.2.3.5
    }
}
```

#### Testing Strategy
- Unit tests for connectivity detection
- Manual test: Enable/disable airplane mode
- Manual test: Switch WiFi on/off
- Manual test: Switch mobile data on/off
- Test on multiple Android versions (API 21, 26, 29, 33)

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing
- [ ] Connectivity changes detected accurately
- [ ] Callbacks triggered correctly
- [ ] Works on Android 8-14
- [ ] Proper cleanup on destroy
- [ ] No memory leaks

---

### Story 0.2.3.4: Implement Retry Logic

**Story ID**: 0.2.3.4
**Priority**: Critical
**Estimate**: 1 day
**Assigned To**: TBD
**Depends On**: 0.2.3.2

#### User Story
```
AS A location tracking service
I WANT to retry failed transmissions with exponential backoff
SO THAT temporary network issues don't cause permanent data loss
```

#### Acceptance Criteria
- [ ] Exponential backoff algorithm implemented
- [ ] Maximum retry attempts configured (5 retries)
- [ ] Backoff delays: 1s, 2s, 4s, 8s, 16s, max 60s
- [ ] Failed items marked after max retries
- [ ] Retry does not block other operations
- [ ] Jitter added to prevent thundering herd
- [ ] Retry statistics tracked

#### Technical Details

**Retry Manager**: `app/src/main/java/com/phonemanager/network/RetryManager.kt`

```kotlin
class RetryManager {

    companion object {
        private const val MAX_RETRIES = 5
        private const val INITIAL_BACKOFF_MS = 1000L // 1 second
        private const val MAX_BACKOFF_MS = 60000L // 60 seconds
        private const val JITTER_FACTOR = 0.1 // 10% jitter
    }

    /**
     * Calculate backoff delay with exponential backoff and jitter
     */
    fun calculateBackoffDelay(retryCount: Int): Long {
        val exponentialDelay = INITIAL_BACKOFF_MS * (1 shl retryCount.coerceIn(0, 6))
        val cappedDelay = exponentialDelay.coerceAtMost(MAX_BACKOFF_MS)

        // Add jitter to prevent thundering herd
        val jitter = (Random.nextDouble(-JITTER_FACTOR, JITTER_FACTOR) * cappedDelay).toLong()

        return cappedDelay + jitter
    }

    /**
     * Check if should retry based on retry count
     */
    fun shouldRetry(retryCount: Int): Boolean {
        return retryCount < MAX_RETRIES
    }

    /**
     * Check if should retry based on exception type
     */
    fun isRetryableException(exception: Throwable): Boolean {
        return when (exception) {
            is IOException -> true // Network errors are retryable
            is HttpException -> {
                // Retry on 5xx server errors, not on 4xx client errors
                exception.code() in 500..599
            }
            is NetworkException -> true
            else -> false
        }
    }
}

class HttpException(val code: Int, message: String) : Exception(message)
```

**Network Manager Enhancement**:

```kotlin
class NetworkManager(private val context: Context) {

    private val retryManager = RetryManager()

    suspend fun sendLocationWithRetry(
        locationData: LocationData,
        retryCount: Int = 0
    ): Result<Boolean> {
        val result = sendLocation(locationData)

        return result.getOrElse { exception ->
            if (retryManager.shouldRetry(retryCount) && retryManager.isRetryableException(exception)) {
                val delay = retryManager.calculateBackoffDelay(retryCount)
                Timber.w("Retry ${retryCount + 1} after ${delay}ms for location transmission")

                delay(delay)

                // Recursive retry
                return sendLocationWithRetry(locationData, retryCount + 1)
            } else {
                Timber.e("Max retries reached or non-retryable error for location transmission")
                return Result.failure(exception)
            }
        }
    }
}
```

#### Testing Strategy
- Unit tests for backoff calculation
- Unit tests for retry logic
- Test max retries enforcement
- Test exponential backoff timing
- Test jitter application
- Integration test with mock network failures

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing (>90% coverage)
- [ ] Exponential backoff works correctly
- [ ] Max retries enforced
- [ ] Jitter prevents thundering herd
- [ ] Non-retryable errors handled correctly
- [ ] Integration test successful

---

### Story 0.2.3.5: Create Queue Processing Worker

**Story ID**: 0.2.3.5
**Priority**: Critical
**Estimate**: 1.5 days
**Assigned To**: TBD
**Depends On**:
- **Story 0.2.3.2** - Network Monitoring & Connectivity
  - File: `docs/product/Story-0.2.3-Reliability-Queue.md#story-0232`
  - Provides: Network state change detection for automatic sync
- **Story 0.2.3.3** - Queue Manager Implementation
  - File: `docs/product/Story-0.2.3-Reliability-Queue.md#story-0233`
  - Provides: Queue read/write operations for processing
- **Story 0.2.3.4** - Exponential Backoff Retry Logic
  - File: `docs/product/Story-0.2.3-Reliability-Queue.md#story-0234`
  - Provides: Retry mechanism for failed transmissions

#### User Story
```
AS A location tracking service
I WANT to automatically process the queue when network becomes available
SO THAT queued locations are transmitted without user intervention
```

#### Acceptance Criteria
- [ ] Queue processor implemented
- [ ] Automatic processing triggered when network available
- [ ] Batch sending for efficiency (up to 50 locations per batch)
- [ ] Processing doesn't block location capture
- [ ] Throttling to prevent server overload
- [ ] Successful items removed from queue
- [ ] Failed items marked for retry
- [ ] Processing statistics logged
- [ ] WorkManager worker implemented with proper constraints
- [ ] Network connectivity constraint configured
- [ ] Battery not low constraint configured
- [ ] Maximum retry attempts limited (3 attempts)
- [ ] Exponential backoff configured for retries
- [ ] Periodic queue processing scheduled (hourly)

#### Technical Details

**Queue Processor**: `app/src/main/java/com/phonemanager/data/queue/QueueProcessor.kt`

```kotlin
class QueueProcessor(
    private val queueManager: QueueManager,
    private val networkManager: NetworkManager
) {

    companion object {
        private const val BATCH_SIZE = 50
        private const val PROCESSING_DELAY_MS = 1000L // 1 second between batches
        private const val TAG = "QueueProcessor"
    }

    suspend fun processQueue(): QueueProcessingResult {
        Timber.i("Starting queue processing")

        val stats = queueManager.getQueueStats()
        if (stats.pendingCount == 0) {
            Timber.d("Queue is empty, nothing to process")
            return QueueProcessingResult(0, 0, 0)
        }

        var totalProcessed = 0
        var successCount = 0
        var failureCount = 0

        while (true) {
            val pending = queueManager.getPendingLocations(BATCH_SIZE)
            if (pending.isEmpty()) {
                break
            }

            Timber.d("Processing batch of ${pending.size} locations")

            pending.forEach { queuedLocation ->
                totalProcessed++

                val locationData = queuedLocation.toLocationData()
                val result = networkManager.sendLocationWithRetry(locationData, queuedLocation.retryCount)

                result.onSuccess {
                    queueManager.markAsSent(queuedLocation.id)
                    successCount++
                    Timber.d("Location ${queuedLocation.id} sent successfully")
                }.onFailure { exception ->
                    queueManager.markAsFailed(queuedLocation.id)
                    failureCount++
                    Timber.e(exception, "Location ${queuedLocation.id} failed to send")
                }
            }

            // Delay between batches to avoid overwhelming server
            delay(PROCESSING_DELAY_MS)
        }

        val result = QueueProcessingResult(totalProcessed, successCount, failureCount)
        Timber.i("Queue processing complete: $result")

        return result
    }

    suspend fun processSingleBatch(): Int {
        val pending = queueManager.getPendingLocations(BATCH_SIZE)
        if (pending.isEmpty()) {
            return 0
        }

        Timber.d("Processing single batch of ${pending.size} locations")

        var sentCount = 0

        pending.forEach { queuedLocation ->
            val locationData = queuedLocation.toLocationData()
            val result = networkManager.sendLocation(locationData)

            result.onSuccess {
                queueManager.markAsSent(queuedLocation.id)
                sentCount++
            }.onFailure {
                queueManager.markAsFailed(queuedLocation.id)
            }
        }

        return sentCount
    }
}

data class QueueProcessingResult(
    val totalProcessed: Int,
    val successCount: Int,
    val failureCount: Int
) {
    override fun toString(): String {
        return "Processed: $totalProcessed, Success: $successCount, Failed: $failureCount"
    }
}
```

**Service Integration**:

```kotlin
class LocationTrackingService : Service() {

    private lateinit var queueProcessor: QueueProcessor
    private var isProcessingQueue = false

    override fun onCreate() {
        super.onCreate()
        locationManager = LocationManager(this)
        networkManager = NetworkManager(this)
        queueManager = QueueManager(this)
        queueProcessor = QueueProcessor(queueManager, networkManager)
        connectivityMonitor = ConnectivityMonitor(this)

        connectivityMonitor.startMonitoring(object : ConnectivityMonitor.ConnectivityCallback {
            override fun onConnectivityChanged(isConnected: Boolean, networkType: ConnectivityMonitor.NetworkType) {
                isOnline = isConnected

                if (isConnected) {
                    serviceScope.launch {
                        processQueue()
                    }
                }
            }
        })
    }

    private suspend fun processQueue() {
        if (isProcessingQueue) {
            Timber.d("Queue processing already in progress")
            return
        }

        isProcessingQueue = true

        try {
            val result = queueProcessor.processQueue()
            Timber.i("Queue processing result: $result")

            updateNotification("Queue processed: ${result.successCount} sent")
        } catch (e: Exception) {
            Timber.e(e, "Error during queue processing")
        } finally {
            isProcessingQueue = false
        }
    }
}
```

**WorkManager Backup**: `app/src/main/java/com/phonemanager/worker/QueueProcessingWorker.kt`

For reliability, implement a WorkManager worker as a backup mechanism to process the queue even when the service isn't running or as a periodic health check.

```kotlin
import androidx.work.*
import java.util.concurrent.TimeUnit

class QueueProcessingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "queue_processing_worker"
        const val TAG = "QueueProcessingWorker"

        private const val MAX_RETRY_ATTEMPTS = 3
        private const val PERIODIC_INTERVAL_HOURS = 1L
    }

    override suspend fun doWork(): Result {
        Timber.d("WorkManager: Starting queue processing")

        // Check if we have network connectivity (constraint should handle this, but double-check)
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        if (network == null) {
            Timber.w("WorkManager: No network available, will retry")
            return Result.retry()
        }

        return try {
            val queueManager = QueueManager.getInstance(applicationContext)
            val networkManager = NetworkManager(applicationContext)
            val queueProcessor = QueueProcessor(queueManager, networkManager)

            val result = queueProcessor.processQueue()

            Timber.i("WorkManager: Queue processed - $result")

            // If there are failures and we haven't exceeded max retries, retry
            if (result.failureCount > 0 && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Timber.w("WorkManager: ${result.failureCount} failures, retrying (attempt ${runAttemptCount + 1}/$MAX_RETRY_ATTEMPTS)")
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "WorkManager: Error processing queue")

            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
```

**WorkManager Enqueue with Constraints**:

```kotlin
object QueueWorkManager {

    fun schedulePeriodicQueueProcessing(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Only run when network available
            .setRequiresBatteryNotLow(true) // Don't drain battery
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<QueueProcessingWorker>(
            QueueProcessingWorker.PERIODIC_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(QueueProcessingWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            QueueProcessingWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Don't replace if already scheduled
            periodicWorkRequest
        )

        Timber.i("Scheduled periodic queue processing with WorkManager")
    }

    fun scheduleOneTimeQueueProcessing(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<QueueProcessingWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(QueueProcessingWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${QueueProcessingWorker.WORK_NAME}_onetime",
            ExistingWorkPolicy.KEEP,
            oneTimeWorkRequest
        )

        Timber.d("Scheduled one-time queue processing with WorkManager")
    }

    fun cancelQueueProcessing(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(QueueProcessingWorker.WORK_NAME)
        Timber.i("Cancelled queue processing WorkManager tasks")
    }
}
```

**Scheduling in Application**:

```kotlin
class PhoneManagerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize WorkManager for queue processing as backup
        QueueWorkManager.schedulePeriodicQueueProcessing(this)
    }
}
```

**WorkManager Dependencies** (add to `app/build.gradle.kts`):

```kotlin
dependencies {
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

**Key WorkManager Features**:
- **Network Constraint**: Only runs when device has network connectivity
- **Battery Constraint**: Won't run when battery is low (< 15%)
- **Exponential Backoff**: Built-in retry with exponential delays
- **Max Retry Attempts**: Limits retries to 3 attempts to avoid infinite loops
- **Periodic Scheduling**: Runs every hour as a health check
- **One-Time Scheduling**: Can be triggered on-demand (e.g., after adding items to queue)
- **Unique Work**: Prevents duplicate workers from running

**Why Both Service and WorkManager?**
- **Service**: Immediate processing when connectivity changes while service is running
- **WorkManager**: Backup mechanism for when service isn't running, and periodic health checks
- This dual approach ensures queue is processed reliably under all conditions

#### Testing Strategy
- Unit tests for QueueProcessor
- Unit tests for QueueProcessingWorker
- Test WorkManager constraints (network, battery)
- Test WorkManager retry logic and backoff
- Integration test: queue multiple locations, go offline, go online
- Test batch processing
- Test throttling delays
- Test concurrent processing prevention
- Stress test: 500+ queued locations
- Test WorkManager periodic execution

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing
- [ ] Integration tests passing
- [ ] Queue processing works correctly
- [ ] Batch sending efficient
- [ ] Throttling prevents overload
- [ ] Handles large queues (500+ items)
- [ ] No blocking of location capture
- [ ] WorkManager constraints properly configured
- [ ] WorkManager retry logic tested
- [ ] Periodic queue processing verified
- [ ] Dual approach (Service + WorkManager) working reliably

---

### Story 0.2.3.6: Integration & Stress Testing

**Story ID**: 0.2.3.6
**Priority**: High
**Estimate**: 1 day
**Assigned To**: TBD
**Depends On**: All previous stories in Epic 0.2.3

#### User Story
```
AS A development team
I WANT to verify the offline queue system works reliably under stress
SO THAT I can confidently deploy to production
```

#### Acceptance Criteria
- [ ] Offline/online cycle testing completed
- [ ] Large queue processing tested (1000+ items)
- [ ] Extended offline period tested (24+ hours)
- [ ] Rapid connectivity changes handled
- [ ] Database performance acceptable under load
- [ ] Memory usage stable during queue processing
- [ ] No data loss during stress tests
- [ ] Battery impact measured

#### Technical Details

**Stress Test Plan**:

```markdown
# Offline Queue Stress Test Plan

## Test Scenarios

### Scenario 1: Basic Offline/Online Cycle
1. Start service with network available
2. Verify locations transmitting successfully
3. Enable airplane mode
4. Wait for 10 location captures (50 minutes)
5. Verify all locations queued
6. Disable airplane mode
7. Verify all queued locations transmitted
8. Verify queue is empty

**Success Criteria**: 100% of queued locations transmitted

### Scenario 2: Extended Offline Period
1. Start service
2. Enable airplane mode
3. Run for 6 hours (72 location captures)
4. Verify queue size = 72
5. Disable airplane mode
6. Measure time to clear queue
7. Verify all locations transmitted

**Success Criteria**:
- Queue processing time <10 minutes
- 100% transmission success
- Memory usage <100MB

### Scenario 3: Large Queue Processing
1. Start service offline
2. Generate 500 locations (mock or wait 42 hours)
3. Verify queue size = 500
4. Enable network
5. Monitor queue processing
6. Measure processing time and success rate

**Success Criteria**:
- Processing time <30 minutes
- Success rate >98%
- No crashes or ANRs
- Memory usage stable

### Scenario 4: Rapid Connectivity Changes
1. Start service
2. Toggle airplane mode every 2 minutes for 30 minutes
3. Verify queue management handles rapid changes
4. Verify no duplicate transmissions
5. Verify no data loss

**Success Criteria**:
- No crashes
- Queue size accurate
- No duplicates
- All locations eventually transmitted

### Scenario 5: Queue Overflow
1. Start service offline
2. Generate 1200 locations (exceeds max queue size)
3. Verify oldest 200 locations pruned
4. Verify queue size = 1000
5. Enable network
6. Verify 1000 locations transmitted

**Success Criteria**:
- Queue size never exceeds 1000
- Pruning works correctly
- Remaining 1000 transmitted successfully

### Scenario 6: Server Errors
1. Start service
2. Configure mock server to return 500 errors
3. Capture 10 locations
4. Verify retry logic engaged
5. Configure mock server to return 200
6. Verify queued locations transmitted

**Success Criteria**:
- Retry logic works
- Exponential backoff applied
- Eventually successful transmission

## Performance Metrics

### Memory Usage
- Monitor during queue processing
- Target: <100MB peak

### Database Performance
- Measure insert time: <10ms average
- Measure query time: <50ms for 1000 records
- No database locks >100ms

### Battery Impact
- Measure during 6-hour offline test
- Target: <2% battery drain (queue only, not GPS)

### Processing Throughput
- Target: >10 locations/second during queue processing
- Target: Clear 500-item queue in <1 minute
```

**Test Implementation**:

```kotlin
// Mock data generator for testing
class LocationDataGenerator {
    fun generateLocations(count: Int, deviceId: String): List<LocationQueueEntity> {
        return (1..count).map { index ->
            LocationQueueEntity(
                deviceId = deviceId,
                latitude = 37.422 + (index * 0.0001), // Incrementing locations
                longitude = -122.084 + (index * 0.0001),
                timestamp = System.currentTimeMillis() - (count - index) * 5 * 60 * 1000,
                accuracy = 10f + (index % 20),
                queuedAt = System.currentTimeMillis() - (count - index) * 5 * 60 * 1000
            )
        }
    }
}

// Stress test helper
@RunWith(AndroidJUnit4::class)
class QueueStressTest {

    @Test
    fun testLargeQueueProcessing() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = AppDatabase.getInstance(context)
        val dao = database.locationQueueDao()

        // Generate 500 test locations
        val generator = LocationDataGenerator()
        val locations = generator.generateLocations(500, "test-device")

        // Insert all at once
        val start = System.currentTimeMillis()
        dao.insertAll(locations)
        val insertTime = System.currentTimeMillis() - start

        Timber.d("Inserted 500 locations in ${insertTime}ms")
        assert(insertTime < 5000) // Should take less than 5 seconds

        // Verify queue size
        val queueSize = dao.getQueueSize()
        assertEquals(500, queueSize)

        // Query performance test
        val queryStart = System.currentTimeMillis()
        val pending = dao.getPendingLocations(limit = 500)
        val queryTime = System.currentTimeMillis() - queryStart

        Timber.d("Queried 500 locations in ${queryTime}ms")
        assert(queryTime < 100) // Should take less than 100ms

        assertEquals(500, pending.size)
    }
}
```

#### Testing Strategy
- Execute all 6 stress test scenarios
- Run on real devices (not emulators)
- Test on multiple Android versions
- Measure and log all performance metrics
- Generate comprehensive test report

#### Definition of Done
- [ ] All test scenarios completed successfully
- [ ] All success criteria met
- [ ] Performance metrics within targets
- [ ] Test report generated and reviewed
- [ ] No critical or high bugs found
- [ ] Memory leaks addressed
- [ ] Ready for Epic 0.2.4

---

## Epic Completion Criteria

This epic is considered complete when:

### Functional Criteria
- [ ] All 6 stories completed and closed
- [ ] Room database implemented and stable
- [ ] Queue management system working
- [ ] Network monitoring detecting changes accurately
- [ ] Retry logic with exponential backoff functional
- [ ] Queue processor clearing queue when online
- [ ] Stress tests passing

### Quality Criteria
- [ ] Code coverage >70%
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Stress tests successful
- [ ] No data loss in offline scenarios
- [ ] No critical or high priority bugs

### Performance Criteria
- [ ] Queue insert time <10ms average
- [ ] Queue query time <50ms for 1000 records
- [ ] Queue processing >10 locations/second
- [ ] Memory usage <100MB peak
- [ ] No database locks >100ms

### Documentation Criteria
- [ ] Queue system architecture documented
- [ ] Database schema documented
- [ ] Retry strategy documented
- [ ] Known limitations documented

---

## Risks & Mitigations

### Risk: Database Corruption
**Severity**: High
**Mitigation**: Regular backups, migration strategy, comprehensive testing
**Status**: Mitigated with Room best practices

### Risk: Queue Overflow
**Severity**: Medium
**Mitigation**: Size limits (1000 entries), automatic pruning, old entry cleanup
**Status**: Mitigated in Story 0.2.3.2

### Risk: Battery Drain During Queue Processing
**Severity**: Medium
**Mitigation**: Batch processing, throttling delays, will optimize in Epic 0.2.5
**Status**: Accepted for MVP, monitor during testing

### Risk: Network State False Positives
**Severity**: Low
**Mitigation**: Verify connectivity before processing, retry logic handles failures
**Status**: Mitigated

---

## Dependencies

### External Dependencies
- Network connectivity (for transmission)
- Server availability (for queue processing)

### Internal Dependencies
- Epic 0.2.2 (Continuous Tracking & Network) must be complete

### Blocks
- Epic 0.2.4 (Auto-start & Service Persistence) cannot start until this epic is complete

---

## Notes for Next Epic

After completing this epic, the service has:
- ✅ Continuous location tracking
- ✅ Network transmission
- ✅ Offline resilience with queue
- ✅ Automatic retry and recovery

Epic 0.2.4 will add:
- Boot receiver for auto-start
- Service persistence mechanisms
- WorkManager watchdog
- Doze mode handling

The service is now functionally complete for manual operation. Epic 0.2.4 makes it truly hands-free.

---

**Last Updated**: 2025-11-11
**Approved By**: TBD
**Ready for Sprint Planning**: Yes (after Epic 0.2.2 complete)
