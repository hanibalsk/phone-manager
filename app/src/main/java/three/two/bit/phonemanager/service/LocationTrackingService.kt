package three.two.bit.phonemanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import three.two.bit.phonemanager.MainActivity
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.data.model.HealthStatus
import three.two.bit.phonemanager.data.model.ServiceHealth
import three.two.bit.phonemanager.data.preferences.PreferencesRepositoryImpl
import three.two.bit.phonemanager.data.repository.LocationRepositoryImpl
import three.two.bit.phonemanager.location.LocationManager
import three.two.bit.phonemanager.queue.QueueManager
import three.two.bit.phonemanager.queue.WorkManagerScheduler
import three.two.bit.phonemanager.util.toNotificationText
import three.two.bit.phonemanager.util.toNotificationTitle
import three.two.bit.phonemanager.watchdog.WatchdogManager
import timber.log.Timber
import javax.inject.Inject

/**
 * Story 0.2.1/0.2.3/0.2.4/E2.2/E7.2: LocationTrackingService - Foreground service for location tracking
 *
 * Story 0.2.1: Implements periodic location capture
 * Story 0.2.3: Integrates upload queue and WorkManager
 * Story 0.2.4: Integrates service health watchdog
 * Story E2.2: Discreet notifications in secret mode
 * Story E7.2: Weather display in notifications
 * Epic 1: Provides service infrastructure for UI layer
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var locationRepository: LocationRepositoryImpl

    @Inject
    lateinit var locationManager: LocationManager

    @Inject
    lateinit var queueManager: QueueManager

    @Inject
    lateinit var workManagerScheduler: WorkManagerScheduler

    @Inject
    lateinit var watchdogManager: WatchdogManager

    @Inject
    lateinit var preferencesRepository: three.two.bit.phonemanager.data.preferences.PreferencesRepository

    @Inject
    lateinit var weatherRepository: three.two.bit.phonemanager.data.repository.WeatherRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentLocationCount = 0
    private var currentInterval = PreferencesRepositoryImpl.DEFAULT_TRACKING_INTERVAL_MINUTES
    private var trackingJob: Job? = null
    private var consecutiveFailures = 0

    // Story E7.2: Cached weather state for notification (avoids runBlocking)
    @Volatile
    private var cachedWeatherForNotification: three.two.bit.phonemanager.domain.model.Weather? = null

    companion object {
        const val ACTION_START_TRACKING = "three.two.bit.phonemanager.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "three.two.bit.phonemanager.action.STOP_TRACKING"
        const val ACTION_UPDATE_INTERVAL = "three.two.bit.phonemanager.action.UPDATE_INTERVAL"
        const val EXTRA_INTERVAL_MINUTES = "interval_minutes"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID_NORMAL = "location_tracking_channel"
        private const val CHANNEL_ID_SECRET = "background_service_channel"

        // Error recovery constants
        private const val MAX_CONSECUTIVE_FAILURES = 5
        private const val FAILURE_BACKOFF_MULTIPLIER = 2
        private const val MAX_BACKOFF_MINUTES = 30
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("LocationTrackingService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_START_TRACKING -> {
                startForegroundTracking()
            }

            ACTION_STOP_TRACKING -> {
                stopTracking()
            }

            ACTION_UPDATE_INTERVAL -> {
                val newInterval = intent.getIntExtra(
                    EXTRA_INTERVAL_MINUTES,
                    PreferencesRepositoryImpl.DEFAULT_TRACKING_INTERVAL_MINUTES,
                )
                updateTrackingInterval(newInterval)
            }
        }

        return START_STICKY
    }

    private fun startForegroundTracking() {
        Timber.d("Starting foreground tracking")

        // Story E2.2: Create notification channels (normal and secret)
        createNotificationChannels()

        // Start foreground with notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Update service health
        locationRepository.updateServiceHealth(
            ServiceHealth(
                isRunning = true,
                healthStatus = HealthStatus.HEALTHY,
                locationCount = 0,
            ),
        )

        // Observe location count for notification updates
        serviceScope.launch {
            locationRepository.observeLocationCount().collectLatest { count ->
                currentLocationCount = count
                updateNotification()
            }
        }

        // Story E2.2: Observe secret mode changes and update notification (AC E2.2.6)
        serviceScope.launch {
            preferencesRepository.isSecretModeEnabled.collectLatest { _ ->
                updateNotification()
            }
        }

        // Story E7.4: Observe weather notification toggle and update notification (AC E7.4.5)
        serviceScope.launch {
            preferencesRepository.showWeatherInNotification.collectLatest { _ ->
                updateNotification()
            }
        }

        // Story E7.2: Periodically fetch weather to keep notification fresh (AC E7.2.5)
        serviceScope.launch {
            locationRepository.observeLastLocation().collectLatest { lastLocation ->
                if (lastLocation != null) {
                    // Fetch weather for current location
                    val weather = weatherRepository.getWeather(lastLocation.latitude, lastLocation.longitude)
                    // Cache weather for notification (avoids runBlocking in createNotification)
                    cachedWeatherForNotification = weather
                    // Update notification with new weather data
                    updateNotification()
                }
            }
        }

        // Story 0.2.1: Start periodic location capture
        startLocationCapture()

        // Story 0.2.3: Schedule periodic queue processing
        workManagerScheduler.scheduleQueueProcessing(intervalMinutes = 15)

        // Story 0.2.4: Start service health watchdog
        watchdogManager.startWatchdog(intervalMinutes = 15)

        Timber.i("Foreground tracking started")
    }

    /**
     * Story 0.2.1: Periodically capture location and store to database
     * Enhanced with error recovery and backoff logic
     */
    private fun startLocationCapture() {
        trackingJob?.cancel()
        consecutiveFailures = 0

        trackingJob = serviceScope.launch {
            Timber.d("Starting location capture loop with interval $currentInterval minutes")

            while (isActive) {
                val captureSuccess = captureLocationWithRecovery()

                // Calculate next interval based on success/failure
                val nextIntervalMinutes = if (captureSuccess) {
                    consecutiveFailures = 0
                    currentInterval
                } else {
                    consecutiveFailures++
                    calculateBackoffInterval()
                }

                // Check if we've exceeded max failures
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    Timber.e("Max consecutive failures ($MAX_CONSECUTIVE_FAILURES) reached, entering recovery mode")
                    val errorMsg = "Location capture repeatedly failing. " +
                        "Will retry in $nextIntervalMinutes minutes."
                    locationRepository.updateServiceHealth(
                        ServiceHealth(
                            isRunning = true,
                            healthStatus = HealthStatus.ERROR,
                            locationCount = currentLocationCount,
                            errorMessage = errorMsg,
                        ),
                    )
                }

                // Wait for next interval
                delay(nextIntervalMinutes * 60 * 1000L)
            }
        }
    }

    /**
     * Attempt to capture location with error handling
     * @return true if capture was successful, false otherwise
     */
    private suspend fun captureLocationWithRecovery(): Boolean = try {
        // Update service health to GPS_ACQUIRING
        locationRepository.updateServiceHealth(
            ServiceHealth(
                isRunning = true,
                healthStatus = HealthStatus.GPS_ACQUIRING,
                locationCount = currentLocationCount,
            ),
        )

        // Get current location
        val result = locationManager.getCurrentLocation()

        result.fold(
            onSuccess = { locationEntity ->
                if (locationEntity != null) {
                    // Story 0.2.1: Store location to database
                    val id = locationRepository.insertLocation(locationEntity)
                    Timber.i(
                        "Location captured and stored: id=$id, lat=${locationEntity.latitude}, lon=${locationEntity.longitude}, accuracy=${locationEntity.accuracy}m",
                    )

                    // Story 0.2.3: Enqueue location for upload
                    queueManager.enqueueLocation(id)

                    // Update service health to HEALTHY
                    locationRepository.updateServiceHealth(
                        ServiceHealth(
                            isRunning = true,
                            lastLocationUpdate = locationEntity.timestamp,
                            locationCount = currentLocationCount + 1,
                            healthStatus = HealthStatus.HEALTHY,
                        ),
                    )
                    true
                } else {
                    Timber.w("Location is null - GPS may be unavailable")
                    locationRepository.updateServiceHealth(
                        ServiceHealth(
                            isRunning = true,
                            healthStatus = HealthStatus.NO_GPS_SIGNAL,
                            locationCount = currentLocationCount,
                            errorMessage = "No GPS signal",
                        ),
                    )
                    false
                }
            },
            onFailure = { error ->
                Timber.e(error, "Failed to capture location (failure $consecutiveFailures)")
                locationRepository.updateServiceHealth(
                    ServiceHealth(
                        isRunning = true,
                        healthStatus = HealthStatus.ERROR,
                        locationCount = currentLocationCount,
                        errorMessage = error.message ?: "Location capture failed",
                    ),
                )
                false
            },
        )
    } catch (e: Exception) {
        Timber.e(e, "Exception in location capture (failure $consecutiveFailures)")
        locationRepository.updateServiceHealth(
            ServiceHealth(
                isRunning = true,
                healthStatus = HealthStatus.ERROR,
                locationCount = currentLocationCount,
                errorMessage = "Unexpected error: ${e.message}",
            ),
        )
        false
    }

    /**
     * Calculate backoff interval based on consecutive failures
     * Uses exponential backoff with a maximum cap
     */
    private fun calculateBackoffInterval(): Int {
        val multiplier = Math.pow(FAILURE_BACKOFF_MULTIPLIER.toDouble(), consecutiveFailures.toDouble())
        val backoffMinutes = (currentInterval.toDouble() * multiplier).toInt()
        return kotlin.math.min(backoffMinutes, MAX_BACKOFF_MINUTES)
    }

    private fun stopTracking() {
        Timber.d("Stopping tracking")

        // Cancel location capture
        trackingJob?.cancel()
        trackingJob = null

        // Story 0.2.3: Cancel queue processing
        workManagerScheduler.cancelQueueProcessing()

        // Story 0.2.4: Stop service health watchdog
        watchdogManager.stopWatchdog()

        // Update service health
        locationRepository.updateServiceHealth(
            ServiceHealth(
                isRunning = false,
                healthStatus = HealthStatus.HEALTHY,
            ),
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateTrackingInterval(intervalMinutes: Int) {
        Timber.d("Updating tracking interval to $intervalMinutes minutes")
        currentInterval = intervalMinutes

        // Restart location capture with new interval if already tracking
        if (trackingJob?.isActive == true) {
            startLocationCapture()
        }

        updateNotification()
    }

    /**
     * Story E2.2/E7.2: Create dual notification channels for normal and secret modes
     * AC E2.2.3, E2.2.4, E7.2.3
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Normal mode channel (AC E2.2.5, E7.2.3)
            val normalChannel = NotificationChannel(
                CHANNEL_ID_NORMAL,
                "Location Tracking",
                NotificationManager.IMPORTANCE_MIN, // AC E7.2.3: Minimal prominence
            ).apply {
                description = "Ongoing location tracking service"
                setShowBadge(false)
                setSound(null, null) // AC E7.2.3: No sound
                enableVibration(false) // AC E7.2.3: No vibration
                lockscreenVisibility = Notification.VISIBILITY_SECRET // AC E7.2.3: Hide on lock screen
            }

            // Secret mode channel (AC E2.2.3, E2.2.4)
            val secretChannel = NotificationChannel(
                CHANNEL_ID_SECRET,
                "Background Service",
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = "Background service"
                setShowBadge(false)
                setSound(null, null) // AC E2.2.4: Silent
                enableVibration(false) // AC E2.2.4: No vibration
            }

            notificationManager.createNotificationChannels(listOf(normalChannel, secretChannel))
        }
    }

    /**
     * Story E2.2/E7.2/E7.4: Create notification based on secret mode, weather, and settings
     * AC E2.2.1-E2.2.5, E7.2.1-E7.2.7, E7.4.3, E7.4.4
     */
    private fun createNotification(): Notification {
        // Story E7.2: Main activity intent with deep link to weather screen (AC E7.2.4)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_NAVIGATE_TO, MainActivity.DESTINATION_WEATHER)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Stop tracking action
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Story E2.2: Check secret mode
        // Story E7.4: Check weather notification toggle
        // Story E7.2: Get weather data for notification
        // Note: We use runBlocking here as this is called from main thread context
        val isSecretMode = runCatching {
            runBlocking {
                preferencesRepository.isSecretModeEnabled.first()
            }
        }.getOrDefault(false)

        val showWeatherInNotification = runCatching {
            runBlocking {
                preferencesRepository.showWeatherInNotification.first()
            }
        }.getOrDefault(true)

        // AC E7.2.1, E7.2.2: Use pre-fetched weather (updated by observer, avoids blocking I/O)
        val weather = cachedWeatherForNotification

        // Priority: Weather (if enabled and available) > Secret Mode > Original
        return if (showWeatherInNotification && weather != null) {
            // AC E7.2.1, E7.2.2: Weather notification (shown even in secret mode for usefulness)
            val channelId = if (isSecretMode) CHANNEL_ID_SECRET else CHANNEL_ID_NORMAL
            NotificationCompat.Builder(this, channelId)
                .setContentTitle(weather.toNotificationTitle()) // AC E7.2.1: "{icon} {temp}°C"
                .setContentText(weather.toNotificationText()) // AC E7.2.2: Weather condition
                .setSmallIcon(R.drawable.ic_weather_notification) // Weather forecast icon
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN) // AC E7.2.3: Low importance
                .setSilent(true) // AC E7.2.3: No sound/vibration
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(contentIntent) // AC E7.2.4: Opens WeatherScreen
                .addAction(
                    android.R.drawable.ic_delete,
                    "Stop Tracking",
                    stopIntent,
                )
                .build()
        } else if (isSecretMode) {
            // AC E2.2.1, E2.2.2, E2.2.3, E2.2.4: Secret mode fallback when no weather
            NotificationCompat.Builder(this, CHANNEL_ID_SECRET)
                .setContentTitle("Service running") // AC E2.2.1: Generic title
                .setContentText("Active")
                .setSmallIcon(R.drawable.ic_service_neutral) // AC E2.2.2: Neutral icon
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN) // AC E2.2.3: Low importance
                .setSilent(true) // AC E2.2.4: Silent
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(contentIntent)
                .build()
        } else {
            // AC E7.2.7: Fallback to original notification
            // AC E7.4.4: Original notification when weather disabled or unavailable
            NotificationCompat.Builder(this, CHANNEL_ID_NORMAL)
                .setContentTitle("Location Tracking Active")
                .setContentText(getNotificationText()) // "{count} locations • Interval: {n} min"
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(contentIntent)
                .addAction(
                    android.R.drawable.ic_delete,
                    "Stop Tracking",
                    stopIntent,
                )
                .build()
        }
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getNotificationText(): String = "$currentLocationCount locations • Interval: $currentInterval min"

    override fun onDestroy() {
        Timber.d("LocationTrackingService destroyed")

        // Cancel location capture
        trackingJob?.cancel()
        trackingJob = null

        // Cancel service scope
        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't support binding
    }
}
