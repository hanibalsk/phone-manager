package three.two.bit.phonemanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.MainActivity
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.data.repository.SettingsSyncRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Story E12.6: Firebase Messaging Service for settings updates.
 *
 * AC E12.6.5: Push notification handling
 * - Parse "settings_update" message type
 * - Extract setting changes from payload
 * - Update local settings state
 * - Show notification with admin name
 * - Trigger settings sync
 */
@AndroidEntryPoint
class SettingsMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var settingsSyncRepository: SettingsSyncRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val CHANNEL_ID = "settings_updates"
        private const val CHANNEL_NAME = "Settings Updates"
        private const val NOTIFICATION_ID = 2001

        // Message types
        const val TYPE_SETTINGS_UPDATE = "settings_update"
        const val TYPE_SETTING_LOCKED = "setting_locked"
        const val TYPE_SETTING_UNLOCKED = "setting_unlocked"

        // Payload keys
        const val KEY_TYPE = "type"
        const val KEY_ADMIN_NAME = "admin_name"
        const val KEY_ADMIN_EMAIL = "admin_email"
        const val KEY_SETTINGS = "settings"
        const val KEY_SETTING_KEY = "setting_key"
        const val KEY_SETTING_VALUE = "setting_value"
        const val KEY_GROUP_NAME = "group_name"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token refreshed: ${token.take(10)}...")
        // Send token to server for push registration
        serviceScope.launch {
            try {
                settingsSyncRepository.registerFcmToken(token)
            } catch (e: Exception) {
                Timber.e(e, "Failed to register FCM token")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("FCM message received from: ${message.from}")

        val data = message.data
        val messageType = data[KEY_TYPE]

        when (messageType) {
            TYPE_SETTINGS_UPDATE -> handleSettingsUpdate(data)
            TYPE_SETTING_LOCKED -> handleSettingLocked(data)
            TYPE_SETTING_UNLOCKED -> handleSettingUnlocked(data)
            else -> Timber.w("Unknown FCM message type: $messageType")
        }
    }

    /**
     * Handle settings update push notification.
     * AC E12.6.5: Parse settings changes and update local state.
     */
    private fun handleSettingsUpdate(data: Map<String, String>) {
        Timber.i("Handling settings update push")

        val adminName = data[KEY_ADMIN_NAME] ?: data[KEY_ADMIN_EMAIL] ?: "Admin"
        val settingsJson = data[KEY_SETTINGS]
        val groupName = data[KEY_GROUP_NAME]

        serviceScope.launch {
            try {
                // Parse settings changes
                val updatedSettings = parseSettingsJson(settingsJson)

                // Update local settings state via repository
                settingsSyncRepository.handleSettingsUpdatePush(updatedSettings, adminName)

                // Show notification to user
                showSettingsUpdateNotification(adminName, groupName, updatedSettings.keys.toList())

                Timber.i("Settings update processed: ${updatedSettings.size} settings changed by $adminName")
            } catch (e: Exception) {
                Timber.e(e, "Failed to process settings update push")
            }
        }
    }

    /**
     * Handle setting locked push notification.
     * AC E12.6.5: Update lock state for specific setting.
     */
    private fun handleSettingLocked(data: Map<String, String>) {
        Timber.i("Handling setting locked push")

        val settingKey = data[KEY_SETTING_KEY] ?: return
        val adminName = data[KEY_ADMIN_NAME] ?: data[KEY_ADMIN_EMAIL] ?: "Admin"
        val groupName = data[KEY_GROUP_NAME]

        serviceScope.launch {
            try {
                // Update lock state
                settingsSyncRepository.handleSettingLockPush(settingKey, isLocked = true, adminName)

                // Show notification
                showSettingLockedNotification(settingKey, adminName, groupName)

                Timber.i("Setting $settingKey locked by $adminName")
            } catch (e: Exception) {
                Timber.e(e, "Failed to process setting locked push")
            }
        }
    }

    /**
     * Handle setting unlocked push notification.
     * AC E12.6.5: Update lock state for specific setting.
     */
    private fun handleSettingUnlocked(data: Map<String, String>) {
        Timber.i("Handling setting unlocked push")

        val settingKey = data[KEY_SETTING_KEY] ?: return
        val adminName = data[KEY_ADMIN_NAME] ?: data[KEY_ADMIN_EMAIL] ?: "Admin"

        serviceScope.launch {
            try {
                // Update lock state
                settingsSyncRepository.handleSettingLockPush(settingKey, isLocked = false, adminName)

                // Show notification
                showSettingUnlockedNotification(settingKey)

                Timber.i("Setting $settingKey unlocked by $adminName")
            } catch (e: Exception) {
                Timber.e(e, "Failed to process setting unlocked push")
            }
        }
    }

    /**
     * Parse settings JSON from push payload.
     */
    private fun parseSettingsJson(settingsJson: String?): Map<String, Any> {
        if (settingsJson.isNullOrBlank()) return emptyMap()

        return try {
            // Simple JSON parsing for key-value pairs
            // Format expected: {"tracking_enabled": true, "tracking_interval": 300}
            val result = mutableMapOf<String, Any>()
            val cleanJson = settingsJson.trim().removeSurrounding("{", "}")

            cleanJson.split(",").forEach { pair ->
                val (key, value) = pair.split(":").map { it.trim().removeSurrounding("\"") }
                result[key] = when {
                    value == "true" -> true
                    value == "false" -> false
                    value.toIntOrNull() != null -> value.toInt()
                    else -> value
                }
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse settings JSON: $settingsJson")
            emptyMap()
        }
    }

    /**
     * Show notification for settings update.
     * AC E12.6.5: Show notification with admin name.
     */
    private fun showSettingsUpdateNotification(
        adminName: String,
        groupName: String?,
        changedSettings: List<String>,
    ) {
        val title = getString(R.string.notification_settings_updated_title)
        val message = if (groupName != null) {
            getString(R.string.notification_settings_updated_by_admin_group, adminName, groupName)
        } else {
            getString(R.string.notification_settings_updated_by_admin, adminName)
        }

        showNotification(title, message)
    }

    /**
     * Show notification for setting locked.
     */
    private fun showSettingLockedNotification(
        settingKey: String,
        adminName: String,
        groupName: String?,
    ) {
        val title = getString(R.string.notification_setting_locked_title)
        val settingName = getSettingDisplayName(settingKey)
        val message = getString(R.string.notification_setting_locked_by_admin, settingName, adminName)

        showNotification(title, message)
    }

    /**
     * Show notification for setting unlocked.
     */
    private fun showSettingUnlockedNotification(settingKey: String) {
        val title = getString(R.string.notification_setting_unlocked_title)
        val settingName = getSettingDisplayName(settingKey)
        val message = getString(R.string.notification_setting_unlocked, settingName)

        showNotification(title, message)
    }

    /**
     * Get display name for a setting key.
     */
    private fun getSettingDisplayName(settingKey: String): String {
        return when (settingKey) {
            "tracking_enabled" -> getString(R.string.setting_tracking_enabled)
            "tracking_interval_seconds" -> getString(R.string.setting_tracking_interval)
            "secret_mode_enabled" -> getString(R.string.setting_secret_mode)
            "show_weather_in_notification" -> getString(R.string.setting_weather_notification)
            "trip_detection_enabled" -> getString(R.string.setting_trip_detection)
            else -> settingKey.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Show a notification with the given title and message.
     */
    private fun showNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "settings")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Create notification channel for settings updates.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.notification_channel_settings_description)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
