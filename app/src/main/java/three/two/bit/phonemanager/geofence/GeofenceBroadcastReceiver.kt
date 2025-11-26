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
import three.two.bit.phonemanager.MainActivity
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.data.repository.GeofenceRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E6.1: GeofenceBroadcastReceiver - Handles geofence transition events
 *
 * AC E6.1.3: Receives callbacks from Android Geofencing API
 * Sends notifications when geofence transitions occur
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var geofenceRepository: GeofenceRepository

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
            Geofence.GEOFENCE_TRANSITION_ENTER -> "entered"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "exited"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "dwelling in"
            else -> {
                Timber.w("Unknown geofence transition type: $geofenceTransition")
                return
            }
        }

        Timber.i("Geofence transition: $transitionType, count: ${triggeringGeofences.size}")

        // Process each triggered geofence
        triggeringGeofences.forEach { geofence ->
            val geofenceId = geofence.requestId
            Timber.d("Processing geofence: $geofenceId, transition: $transitionType")

            // Look up geofence name from repository
            scope.launch {
                val domainGeofence = geofenceRepository.getGeofence(geofenceId)
                val geofenceName = domainGeofence?.name ?: "Unknown location"

                // Send notification
                sendNotification(
                    context = context,
                    geofenceId = geofenceId,
                    geofenceName = geofenceName,
                    transitionType = transitionType,
                )
            }
        }
    }

    private fun sendNotification(
        context: Context,
        geofenceId: String,
        geofenceName: String,
        transitionType: String,
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Geofence Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications for geofence enter/exit events"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        // Build notification
        val title = when (transitionType) {
            "entered" -> "Entered: $geofenceName"
            "exited" -> "Left: $geofenceName"
            "dwelling in" -> "At: $geofenceName"
            else -> "Geofence: $geofenceName"
        }

        val text = when (transitionType) {
            "entered" -> "You have entered the $geofenceName area"
            "exited" -> "You have left the $geofenceName area"
            "dwelling in" -> "You are staying in the $geofenceName area"
            else -> "Geofence event at $geofenceName"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_service_neutral)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Use geofence ID hash as notification ID to allow multiple notifications
        val notificationId = geofenceId.hashCode()
        notificationManager.notify(notificationId, notification)

        Timber.i("Sent geofence notification: $title")
    }

    companion object {
        private const val CHANNEL_ID = "geofence_alerts_channel"
    }
}
