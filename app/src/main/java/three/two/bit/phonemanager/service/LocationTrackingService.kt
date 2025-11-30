package three.two.bit.phonemanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import three.two.bit.phonemanager.MainActivity
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.data.model.HealthStatus
import three.two.bit.phonemanager.data.model.LocationEntity
import three.two.bit.phonemanager.data.model.ServiceHealth
import three.two.bit.phonemanager.data.preferences.PreferencesRepositoryImpl
import three.two.bit.phonemanager.data.repository.LocationRepositoryImpl
import three.two.bit.phonemanager.data.repository.TripRepository
import three.two.bit.phonemanager.location.LocationManager
import three.two.bit.phonemanager.queue.QueueManager
import three.two.bit.phonemanager.queue.WorkManagerScheduler
import three.two.bit.phonemanager.util.toNotificationText
import three.two.bit.phonemanager.util.toNotificationTitle
import three.two.bit.phonemanager.movement.TransportationModeManager
import three.two.bit.phonemanager.movement.TransportationState
import three.two.bit.phonemanager.trip.TripManager
import three.two.bit.phonemanager.domain.model.Trip
import three.two.bit.phonemanager.watchdog.WatchdogManager
import kotlinx.datetime.Clock
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

    @Inject
    lateinit var transportationModeManager: TransportationModeManager

    // Story E8.7: TripManager and TripRepository integration (AC E8.7.1)
    @Inject
    lateinit var tripManager: TripManager

    @Inject
    lateinit var tripRepository: TripRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Story E8.7: Last location for distance calculation (AC E8.7.3)
    @Volatile
    private var lastCapturedLocation: LocationEntity? = null

    private var currentLocationCount = 0
    private var currentInterval = PreferencesRepositoryImpl.DEFAULT_TRACKING_INTERVAL_MINUTES
    private var trackingJob: Job? = null
    private var consecutiveFailures = 0

    // Movement detection: Current transportation state for adaptive intervals
    @Volatile
    private var currentTransportationState: TransportationState = TransportationState()

    @Volatile
    private var isMovementDetectionEnabled = false

    // Story E7.2: Cached weather state for notification (avoids runBlocking)
    @Volatile
    private var cachedWeatherForNotification: three.two.bit.phonemanager.domain.model.Weather? = null

    // Story E8.14: Cached active trip for notification (AC E8.14.1)
    @Volatile
    private var cachedActiveTripForNotification: Trip? = null

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

        // Story E8.14: Observe active trip changes for notification (AC E8.14.1, E8.14.6)
        serviceScope.launch {
            tripManager.activeTrip.collectLatest { trip ->
                cachedActiveTripForNotification = trip
                updateNotification()
                Timber.d("Active trip updated in notification: ${trip?.id}")
            }
        }

        // Movement detection: Observe movement detection settings and start/stop monitoring
        serviceScope.launch {
            preferencesRepository.isMovementDetectionEnabled.collectLatest { enabled ->
                isMovementDetectionEnabled = enabled
                if (enabled) {
                    startMovementDetection()
                } else {
                    stopMovementDetection()
                    // Reset to default interval when disabled
                    currentTransportationState = TransportationState()
                }
                Timber.d("Movement detection enabled: $enabled")
            }
        }

        // Movement detection: Observe transportation state changes for adaptive intervals
        serviceScope.launch {
            transportationModeManager.transportationState.collectLatest { state ->
                if (isMovementDetectionEnabled) {
                    val previousState = currentTransportationState
                    currentTransportationState = state

                    // Log state change
                    if (previousState.isInVehicle != state.isInVehicle) {
                        Timber.i(
                            "Transportation state changed: inVehicle=${state.isInVehicle}, " +
                                "mode=${state.mode}, source=${state.source}, " +
                                "multiplier=${state.intervalMultiplier}",
                        )

                        // Restart location capture to apply new interval
                        if (trackingJob?.isActive == true) {
                            Timber.d("Restarting location capture with new interval multiplier")
                            startLocationCapture()
                        }
                    }
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
     * Movement detection: Start movement detection monitoring based on preferences.
     * Story E8.7: Also starts TripManager monitoring (AC E8.7.4)
     */
    private suspend fun startMovementDetection() {
        val activityEnabled = preferencesRepository.isActivityRecognitionEnabled.first()
        val bluetoothEnabled = preferencesRepository.isBluetoothCarDetectionEnabled.first()
        val androidAutoEnabled = preferencesRepository.isAndroidAutoDetectionEnabled.first()

        Timber.i(
            "Starting movement detection (activity=$activityEnabled, " +
                "bluetooth=$bluetoothEnabled, androidAuto=$androidAutoEnabled)",
        )

        transportationModeManager.startMonitoring(
            enableActivityRecognition = activityEnabled,
            enableBluetoothDetection = bluetoothEnabled,
            enableAndroidAutoDetection = androidAutoEnabled,
        )

        // Story E8.7: Start TripManager monitoring (AC E8.7.4)
        try {
            tripManager.startMonitoring()
            Timber.i("TripManager monitoring started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start TripManager monitoring")
        }
    }

    /**
     * Movement detection: Stop movement detection monitoring.
     * Story E8.7: Also stops TripManager monitoring (AC E8.7.4)
     */
    private fun stopMovementDetection() {
        Timber.i("Stopping movement detection")
        transportationModeManager.stopMonitoring()

        // Story E8.7: Stop TripManager monitoring (AC E8.7.4)
        try {
            tripManager.stopMonitoring()
            Timber.i("TripManager monitoring stopped")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop TripManager monitoring")
        }
    }

    /**
     * Calculate the effective tracking interval considering transportation mode.
     *
     * @return adjusted interval in minutes
     */
    private fun calculateEffectiveInterval(): Int {
        return if (isMovementDetectionEnabled) {
            currentTransportationState.calculateAdjustedInterval(currentInterval)
        } else {
            currentInterval
        }
    }

    /**
     * Story 0.2.1: Periodically capture location and store to database
     * Enhanced with error recovery, backoff logic, and adaptive intervals
     */
    private fun startLocationCapture() {
        trackingJob?.cancel()
        consecutiveFailures = 0

        trackingJob = serviceScope.launch {
            val effectiveInterval = calculateEffectiveInterval()
            Timber.d(
                "Starting location capture loop with base interval $currentInterval minutes, " +
                    "effective interval $effectiveInterval minutes " +
                    "(inVehicle=${currentTransportationState.isInVehicle})",
            )

            while (isActive) {
                val captureSuccess = captureLocationWithRecovery()

                // Calculate next interval based on success/failure and transportation mode
                val nextIntervalMinutes = if (captureSuccess) {
                    consecutiveFailures = 0
                    calculateEffectiveInterval()
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
                    // Story E8.7: Enrich location with transportation mode and trip context (AC E8.7.2)
                    val transportState = currentTransportationState
                    val activeTrip = tripManager.activeTrip.value

                    // Calculate distance from last location (AC E8.7.3)
                    val distance = lastCapturedLocation?.let { last ->
                        calculateDistance(
                            last.latitude, last.longitude,
                            locationEntity.latitude, locationEntity.longitude,
                        )
                    } ?: 0f

                    // Derive confidence from detection source
                    val modeConfidence = when (transportState.source) {
                        three.two.bit.phonemanager.movement.DetectionSource.MULTIPLE -> 0.95f
                        three.two.bit.phonemanager.movement.DetectionSource.ANDROID_AUTO -> 0.9f
                        three.two.bit.phonemanager.movement.DetectionSource.BLUETOOTH_CAR -> 0.85f
                        three.two.bit.phonemanager.movement.DetectionSource.ACTIVITY_RECOGNITION -> 0.8f
                        three.two.bit.phonemanager.movement.DetectionSource.NONE -> null
                    }

                    // Create enriched location entity
                    val enrichedLocation = locationEntity.copy(
                        transportationMode = transportState.mode.name,
                        detectionSource = transportState.source.name,
                        modeConfidence = modeConfidence,
                        tripId = activeTrip?.id,
                    )

                    // Story 0.2.1: Store enriched location to database
                    val id = locationRepository.insertLocation(enrichedLocation)
                    Timber.i(
                        "Location captured and stored: id=$id, lat=${enrichedLocation.latitude}, lon=${enrichedLocation.longitude}, " +
                            "accuracy=${enrichedLocation.accuracy}m, mode=${enrichedLocation.transportationMode}, tripId=${enrichedLocation.tripId}",
                    )

                    // Update last captured location for distance calculation
                    lastCapturedLocation = enrichedLocation

                    // Story E8.7: Update trip statistics if active trip exists (AC E8.7.3)
                    activeTrip?.let { trip ->
                        updateTripStatistics(trip.id, distance, enrichedLocation)
                    }

                    // Story 0.2.3: Enqueue location for upload
                    queueManager.enqueueLocation(id)

                    // Update service health to HEALTHY
                    locationRepository.updateServiceHealth(
                        ServiceHealth(
                            isRunning = true,
                            lastLocationUpdate = enrichedLocation.timestamp,
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

        // Stop movement detection
        stopMovementDetection()

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
        // minSdk is 26 (O), so notification channels are always required
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Normal mode channel (AC E2.2.5, E7.2.3)
        val normalChannel = NotificationChannel(
            CHANNEL_ID_NORMAL,
            "Location Tracking",
            NotificationManager.IMPORTANCE_MIN, // AC E7.2.3: Minimal prominence
        ).apply {
            description = getString(R.string.channel_tracking_description)
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
            description = getString(R.string.channel_background_description)
            setShowBadge(false)
            setSound(null, null) // AC E2.2.4: Silent
            enableVibration(false) // AC E2.2.4: No vibration
        }

        notificationManager.createNotificationChannels(listOf(normalChannel, secretChannel))
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

        // Story E8.14: Get active trip for notification content (AC E8.14.1)
        val activeTrip = cachedActiveTripForNotification

        // Priority: Weather (if enabled and available) > Secret Mode > Original
        return if (showWeatherInNotification && weather != null) {
            // AC E7.2.1, E7.2.2: Weather notification (shown even in secret mode for usefulness)
            // Purpose: Tap to view detailed forecast; back to close (hide notification)
            val channelId = if (isSecretMode) CHANNEL_ID_SECRET else CHANNEL_ID_NORMAL
            NotificationCompat.Builder(this, channelId)
                .setContentTitle(weather.toNotificationTitle()) // AC E7.2.1: "{icon} {temp}°C"
                .setContentText(weather.toNotificationText()) // AC E7.2.2: Weather condition
                .setSmallIcon(R.drawable.ic_foreground_service) // Custom foreground service icon
                .setColor(getColor(R.color.notification_icon_color)) // Help system display icon
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN) // AC E7.2.3: Low importance
                .setSilent(true) // AC E7.2.3: No sound/vibration
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(contentIntent) // AC E7.2.4: Opens WeatherScreen
                // No stop action - weather screen is for viewing, not control
                .build()
        } else if (isSecretMode) {
            // AC E2.2.1, E2.2.2, E2.2.3, E2.2.4: Secret mode fallback when no weather
            NotificationCompat.Builder(this, CHANNEL_ID_SECRET)
                .setContentTitle("Service running") // AC E2.2.1: Generic title
                .setContentText("Active")
                .setSmallIcon(R.drawable.ic_foreground_service) // AC E2.2.2: Neutral location icon
                .setColor(getColor(R.color.notification_icon_color)) // Help system display icon
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN) // AC E2.2.3: Low importance
                .setSilent(true) // AC E2.2.4: Silent
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(contentIntent)
                .build()
        } else {
            // AC E7.2.7: Fallback to original notification
            // AC E7.4.4: Original notification when weather disabled or unavailable
            // Story E8.14: Show trip status when active (AC E8.14.1, E8.14.5, E8.14.6)
            val contentText = if (activeTrip != null) {
                buildTripNotificationContent(activeTrip)
            } else {
                buildStandardNotificationContent()
            }

            NotificationCompat.Builder(this, CHANNEL_ID_NORMAL)
                .setContentTitle("Location Tracking Active")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_foreground_service)
                .setColor(getColor(R.color.notification_icon_color)) // Help system display icon
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

    /**
     * Story E8.14: Build notification content when trip is active (AC E8.14.1, E8.14.6)
     * Format: "🚗 Trip in progress • 23 min • 8.2 km"
     */
    private fun buildTripNotificationContent(trip: Trip): String {
        val emoji = getModeEmoji(trip.dominantMode)
        val duration = formatTripDuration(trip.startTime.epochSeconds)
        val distance = formatTripDistance(trip.totalDistanceMeters)
        return "$emoji Trip in progress • $duration • $distance"
    }

    /**
     * Story E8.14: Build notification content when no trip active (AC E8.14.5)
     * Format: "Walking • 95% confidence" or original "{count} locations • Interval: {n} min"
     */
    private fun buildStandardNotificationContent(): String {
        val transportState = currentTransportationState
        return if (isMovementDetectionEnabled && transportState.source != three.two.bit.phonemanager.movement.DetectionSource.NONE) {
            val modeName = when (transportState.mode) {
                three.two.bit.phonemanager.movement.TransportationMode.WALKING -> "Walking"
                three.two.bit.phonemanager.movement.TransportationMode.RUNNING -> "Running"
                three.two.bit.phonemanager.movement.TransportationMode.CYCLING -> "Cycling"
                three.two.bit.phonemanager.movement.TransportationMode.IN_VEHICLE -> "Driving"
                three.two.bit.phonemanager.movement.TransportationMode.STATIONARY -> "Stationary"
                three.two.bit.phonemanager.movement.TransportationMode.UNKNOWN -> "Unknown"
            }
            // Derive confidence from detection source
            val confidence = when (transportState.source) {
                three.two.bit.phonemanager.movement.DetectionSource.MULTIPLE -> 95
                three.two.bit.phonemanager.movement.DetectionSource.ANDROID_AUTO -> 90
                three.two.bit.phonemanager.movement.DetectionSource.BLUETOOTH_CAR -> 85
                three.two.bit.phonemanager.movement.DetectionSource.ACTIVITY_RECOGNITION -> 80
                three.two.bit.phonemanager.movement.DetectionSource.NONE -> 0
            }
            "$modeName • $confidence% confidence"
        } else {
            getNotificationText()
        }
    }

    /**
     * Story E8.14: Get mode emoji for notification (AC E8.14.2)
     */
    private fun getModeEmoji(mode: three.two.bit.phonemanager.movement.TransportationMode): String = when (mode) {
        three.two.bit.phonemanager.movement.TransportationMode.WALKING -> "🚶"
        three.two.bit.phonemanager.movement.TransportationMode.RUNNING -> "🏃"
        three.two.bit.phonemanager.movement.TransportationMode.CYCLING -> "🚲"
        three.two.bit.phonemanager.movement.TransportationMode.IN_VEHICLE -> "🚗"
        three.two.bit.phonemanager.movement.TransportationMode.STATIONARY -> "📍"
        three.two.bit.phonemanager.movement.TransportationMode.UNKNOWN -> "❓"
    }

    /**
     * Story E8.14: Format trip duration (AC E8.14.3)
     * Format: "X min" or "X hr Y min"
     */
    private fun formatTripDuration(startEpochSeconds: Long): String {
        val now = Clock.System.now().epochSeconds
        val durationSeconds = now - startEpochSeconds

        return when {
            durationSeconds >= 3600 -> {
                val hours = durationSeconds / 3600
                val minutes = (durationSeconds % 3600) / 60
                "${hours}h ${minutes}m"
            }
            durationSeconds >= 60 -> {
                val minutes = durationSeconds / 60
                "${minutes} min"
            }
            else -> "<1 min"
        }
    }

    /**
     * Story E8.14: Format trip distance (AC E8.14.4)
     * Format: "X.X km"
     */
    private fun formatTripDistance(distanceMeters: Double): String {
        return if (distanceMeters >= 1000) {
            String.format("%.1f km", distanceMeters / 1000.0)
        } else {
            String.format("%.0f m", distanceMeters)
        }
    }

    private fun getNotificationText(): String = "$currentLocationCount locations • Interval: $currentInterval min"

    override fun onDestroy() {
        Timber.d("LocationTrackingService destroyed")

        // Cancel location capture
        trackingJob?.cancel()
        trackingJob = null

        // Stop movement detection
        stopMovementDetection()

        // Cancel service scope
        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't support binding
    }

    /**
     * Story E8.7: Calculate distance between two points in meters (AC E8.7.3)
     * Uses Android's Location.distanceBetween for accurate results.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Story E8.7: Update trip statistics with new location (AC E8.7.3)
     * AC E8.7.6: Handles errors without crashing the service
     */
    private fun updateTripStatistics(tripId: String, distance: Float, location: LocationEntity) {
        serviceScope.launch {
            try {
                // Update TripManager with new location
                tripManager.updateLocation(location.latitude, location.longitude)

                // Increment location count and add distance
                if (distance > 0) {
                    tripManager.addDistance(tripId, distance.toDouble())
                    Timber.d("Trip $tripId: Added distance ${distance}m, total locations updated")
                }
            } catch (e: Exception) {
                // AC E8.7.6: Log errors but don't crash
                Timber.e(e, "Failed to update trip statistics for trip $tripId")
            }
        }
    }
}
