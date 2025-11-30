package three.two.bit.phonemanager.geofence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import three.two.bit.phonemanager.MainActivity
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.data.database.GeofenceEventDao
import three.two.bit.phonemanager.data.model.GeofenceEventEntity
import three.two.bit.phonemanager.data.repository.GeofenceRepository
import three.two.bit.phonemanager.domain.model.TransitionType
import three.two.bit.phonemanager.network.GeofenceEventApiService
import three.two.bit.phonemanager.network.models.CreateGeofenceEventRequest
import three.two.bit.phonemanager.network.models.GeofenceEventType
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Story E6.1/E6.2: GeofenceBroadcastReceiver - Handles geofence transition events
 *
 * AC E6.1.3: Receives callbacks from Android Geofencing API
 * AC E6.2.1: Extract geofence ID and transition type
 * AC E6.2.2: Local notification on event
 * AC E6.2.3: Send event to backend
 * AC E6.2.5: Event logging to database
 * AC E6.2.6: Handle multiple geofences
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var geofenceRepository: GeofenceRepository

    @Inject
    lateinit var geofenceEventDao: GeofenceEventDao

    @Inject
    lateinit var geofenceEventApiService: GeofenceEventApiService

    @Inject
    lateinit var secureStorage: SecureStorage

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Timber.e("GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Timber.e("Geofence error: $errorMessage (code: ${geofencingEvent.errorCode})")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()

        if (triggeringGeofences.isEmpty()) {
            Timber.w("No triggering geofences in event")
            return
        }

        val transitionType = when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> TransitionType.ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> TransitionType.EXIT
            Geofence.GEOFENCE_TRANSITION_DWELL -> TransitionType.DWELL
            else -> {
                Timber.w("Unknown geofence transition type: $geofenceTransition")
                return
            }
        }

        // Get triggering location (AC E6.2.4)
        val triggeringLocation = geofencingEvent.triggeringLocation
        val latitude = triggeringLocation?.latitude ?: 0.0
        val longitude = triggeringLocation?.longitude ?: 0.0

        Timber.i("Geofence transition: ${transitionType.name}, count: ${triggeringGeofences.size}")

        // Process each triggered geofence (AC E6.2.6)
        triggeringGeofences.forEach { geofence ->
            val geofenceId = geofence.requestId
            Timber.d("Processing geofence: $geofenceId, transition: ${transitionType.name}")

            // Look up geofence name and process event
            scope.launch {
                val domainGeofence = geofenceRepository.getGeofence(geofenceId)
                val geofenceName = domainGeofence?.name ?: "Unknown location"

                // Create and save event (AC E6.2.5)
                val eventId = UUID.randomUUID().toString()
                val now = Clock.System.now()
                val deviceId = secureStorage.getDeviceId()

                val eventEntity = GeofenceEventEntity(
                    id = eventId,
                    deviceId = deviceId,
                    geofenceId = geofenceId,
                    eventType = transitionType.name,
                    timestamp = now.toEpochMilliseconds(),
                    latitude = latitude,
                    longitude = longitude,
                    webhookDelivered = false,
                    webhookResponseCode = null,
                )

                // Save to local database
                geofenceEventDao.insert(eventEntity)
                Timber.d("Geofence event saved locally: $eventId")

                // Send to backend (AC E6.2.3)
                sendEventToBackend(eventId, deviceId, geofenceId, transitionType, now, latitude, longitude)

                // Send notification (AC E6.2.2)
                sendNotification(
                    context = context,
                    geofenceId = geofenceId,
                    geofenceName = geofenceName,
                    transitionType = transitionType,
                )
            }
        }
    }

    /**
     * Send geofence event to backend server (AC E6.2.3)
     */
    private suspend fun sendEventToBackend(
        eventId: String,
        deviceId: String,
        geofenceId: String,
        transitionType: TransitionType,
        timestamp: kotlinx.datetime.Instant,
        latitude: Double,
        longitude: Double,
    ) {
        val request = CreateGeofenceEventRequest(
            deviceId = deviceId,
            geofenceId = geofenceId,
            eventType = transitionType.toEventType(),
            timestamp = timestamp.toString(),
            latitude = latitude,
            longitude = longitude,
        )

        geofenceEventApiService.createEvent(request).fold(
            onSuccess = { response ->
                Timber.i("Geofence event sent to backend: ${response.eventId}")
                // Update local record with webhook status if returned
                if (response.webhookDelivered) {
                    val updated = geofenceEventDao.getById(eventId)?.copy(
                        webhookDelivered = response.webhookDelivered,
                        webhookResponseCode = response.webhookResponseCode,
                    )
                    updated?.let { geofenceEventDao.update(it) }
                }
            },
            onFailure = { error ->
                Timber.w(error, "Failed to send geofence event to backend: $eventId")
                // Event is saved locally for retry later
            },
        )
    }

    /**
     * Convert domain TransitionType to API GeofenceEventType
     */
    private fun TransitionType.toEventType(): GeofenceEventType = when (this) {
        TransitionType.ENTER -> GeofenceEventType.ENTER
        TransitionType.EXIT -> GeofenceEventType.EXIT
        TransitionType.DWELL -> GeofenceEventType.DWELL
    }

    private fun sendNotification(
        context: Context,
        geofenceId: String,
        geofenceName: String,
        transitionType: TransitionType,
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for Android O+, minSdk is 26)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Geofence Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_geofence_description)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        // Create intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        // Build notification (AC E6.2.2)
        val title = when (transitionType) {
            TransitionType.ENTER -> "Entered: $geofenceName"
            TransitionType.EXIT -> "Left: $geofenceName"
            TransitionType.DWELL -> "At: $geofenceName"
        }

        val text = when (transitionType) {
            TransitionType.ENTER -> "You have entered the $geofenceName area"
            TransitionType.EXIT -> "You have left the $geofenceName area"
            TransitionType.DWELL -> "You are staying in the $geofenceName area"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_service_neutral)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Use geofence ID hash as notification ID to allow multiple notifications (AC E6.2.6)
        val notificationId = geofenceId.hashCode()
        notificationManager.notify(notificationId, notification)

        Timber.i("Sent geofence notification: $title")
    }

    companion object {
        private const val CHANNEL_ID = "geofence_alerts_channel"
    }
}
