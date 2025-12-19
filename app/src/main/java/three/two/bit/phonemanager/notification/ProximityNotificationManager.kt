package three.two.bit.phonemanager.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import three.two.bit.phonemanager.MainActivity
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.domain.model.AlertDirection
import three.two.bit.phonemanager.domain.model.ProximityAlert
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E5.2: Proximity Notification Manager
 *
 * AC E5.2.5: Show local notification when proximity alert triggers
 * - Notification includes target display name
 * - Notification is actionable (tap to open map)
 */
@Singleton
class ProximityNotificationManager @Inject constructor(@param:ApplicationContext private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "proximity_alerts_channel"
        private const val CHANNEL_NAME = "Proximity Alerts"
        private const val CHANNEL_DESCRIPTION = "Notifications when devices are nearby"
    }

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel for Android O+ (AC E5.2.5)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            Timber.d("Proximity notification channel created")
        }
    }

    /**
     * Show proximity alert notification (AC E5.2.5)
     *
     * @param alert The triggered proximity alert
     * @param targetName Display name of the target device
     * @param distance Distance in meters to target
     */
    fun showProximityAlert(alert: ProximityAlert, targetName: String, distance: Float) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.w("Notification permission not granted, skipping proximity alert notification")
                return
            }
        }

        val title = when (alert.direction) {
            AlertDirection.ENTER -> "Proximity Alert: Entered"
            AlertDirection.EXIT -> "Proximity Alert: Exited"
            AlertDirection.BOTH -> "Proximity Alert"
        }

        val distanceText = if (distance < 1000) {
            "${distance.toInt()}m away"
        } else {
            String.format(Locale.getDefault(), "%.1fkm away", distance / 1000)
        }

        val text = when (alert.direction) {
            AlertDirection.ENTER -> "$targetName is now nearby ($distanceText)"
            AlertDirection.EXIT -> "$targetName has left the area"
            AlertDirection.BOTH -> "Proximity change: $targetName ($distanceText)"
        }

        // Create intent to open map when notification tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "map")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_service_neutral)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(alert.id.hashCode(), notification)
            Timber.i("Proximity notification shown for alert ${alert.id}: $text")
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to show notification due to missing permission")
        }
    }
}
