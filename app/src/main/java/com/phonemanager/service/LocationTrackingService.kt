package com.phonemanager.service

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
import com.phonemanager.MainActivity
import com.phonemanager.R
import com.phonemanager.data.model.HealthStatus
import com.phonemanager.data.model.ServiceHealth
import com.phonemanager.data.repository.LocationRepositoryImpl
import com.phonemanager.location.LocationManager
import com.phonemanager.queue.QueueManager
import com.phonemanager.queue.WorkManagerScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Story 0.2.1/0.2.3: LocationTrackingService - Foreground service for location tracking
 *
 * Story 0.2.1: Implements periodic location capture
 * Story 0.2.3: Integrates upload queue and WorkManager
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentLocationCount = 0
    private var currentInterval = 5 // minutes
    private var trackingJob: Job? = null

    companion object {
        const val ACTION_START_TRACKING = "com.phonemanager.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.phonemanager.action.STOP_TRACKING"
        const val ACTION_UPDATE_INTERVAL = "com.phonemanager.action.UPDATE_INTERVAL"
        const val EXTRA_INTERVAL_MINUTES = "interval_minutes"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking_channel"
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
                val newInterval = intent.getIntExtra(EXTRA_INTERVAL_MINUTES, 5)
                updateTrackingInterval(newInterval)
            }
        }

        return START_STICKY
    }

    private fun startForegroundTracking() {
        Timber.d("Starting foreground tracking")

        // Create notification channel
        createNotificationChannel()

        // Start foreground with notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Update service health
        locationRepository.updateServiceHealth(
            ServiceHealth(
                isRunning = true,
                healthStatus = HealthStatus.HEALTHY,
                locationCount = 0
            )
        )

        // Observe location count for notification updates
        serviceScope.launch {
            locationRepository.observeLocationCount().collectLatest { count ->
                currentLocationCount = count
                updateNotification()
            }
        }

        // Story 0.2.1: Start periodic location capture
        startLocationCapture()

        // Story 0.2.3: Schedule periodic queue processing
        workManagerScheduler.scheduleQueueProcessing(intervalMinutes = 15)

        Timber.i("Foreground tracking started")
    }

    /**
     * Story 0.2.1: Periodically capture location and store to database
     */
    private fun startLocationCapture() {
        trackingJob?.cancel()

        trackingJob = serviceScope.launch {
            Timber.d("Starting location capture loop with interval $currentInterval minutes")

            while (isActive) {
                try {
                    // Update service health to GPS_ACQUIRING
                    locationRepository.updateServiceHealth(
                        ServiceHealth(
                            isRunning = true,
                            healthStatus = HealthStatus.GPS_ACQUIRING,
                            locationCount = currentLocationCount
                        )
                    )

                    // Get current location
                    val result = locationManager.getCurrentLocation()

                    result.onSuccess { locationEntity ->
                        if (locationEntity != null) {
                            // Story 0.2.1: Store location to database
                            val id = locationRepository.insertLocation(locationEntity)
                            Timber.i("Location captured and stored: id=$id, lat=${locationEntity.latitude}, lon=${locationEntity.longitude}, accuracy=${locationEntity.accuracy}m")

                            // Story 0.2.3: Enqueue location for upload
                            queueManager.enqueueLocation(id)

                            // Update service health to HEALTHY
                            locationRepository.updateServiceHealth(
                                ServiceHealth(
                                    isRunning = true,
                                    lastLocationUpdate = locationEntity.timestamp,
                                    locationCount = currentLocationCount + 1,
                                    healthStatus = HealthStatus.HEALTHY
                                )
                            )
                        } else {
                            Timber.w("Location is null - GPS may be unavailable")
                            locationRepository.updateServiceHealth(
                                ServiceHealth(
                                    isRunning = true,
                                    healthStatus = HealthStatus.NO_GPS_SIGNAL,
                                    locationCount = currentLocationCount,
                                    errorMessage = "No GPS signal"
                                )
                            )
                        }
                    }.onFailure { error ->
                        Timber.e(error, "Failed to capture location")
                        locationRepository.updateServiceHealth(
                            ServiceHealth(
                                isRunning = true,
                                healthStatus = HealthStatus.ERROR,
                                locationCount = currentLocationCount,
                                errorMessage = error.message ?: "Location capture failed"
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Exception in location capture loop")
                }

                // Wait for next interval
                delay(currentInterval * 60 * 1000L)
            }
        }
    }

    private fun stopTracking() {
        Timber.d("Stopping tracking")

        // Cancel location capture
        trackingJob?.cancel()
        trackingJob = null

        // Story 0.2.3: Cancel queue processing
        workManagerScheduler.cancelQueueProcessing()

        // Update service health
        locationRepository.updateServiceHealth(
            ServiceHealth(
                isRunning = false,
                healthStatus = HealthStatus.HEALTHY
            )
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing location tracking service"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // Main activity intent
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Using system icon for now
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_delete,
                "Stop Tracking",
                stopIntent
            )
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getNotificationText(): String {
        return "$currentLocationCount locations • Interval: $currentInterval min"
    }

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
