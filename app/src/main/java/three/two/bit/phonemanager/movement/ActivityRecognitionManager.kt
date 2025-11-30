package three.two.bit.phonemanager.movement

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages activity recognition using Google Play Services Activity Recognition API.
 * Detects user's transportation mode (walking, running, cycling, vehicle, etc.)
 * to enable adaptive location tracking intervals.
 */
@Singleton
class ActivityRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val activityClient: ActivityRecognitionClient =
        ActivityRecognition.getClient(context)

    private val _currentActivity = MutableStateFlow(TransportationMode.UNKNOWN)
    val currentActivity: StateFlow<TransportationMode> = _currentActivity.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var activityReceiver: ActivityTransitionReceiver? = null

    companion object {
        private const val ACTION_ACTIVITY_TRANSITION =
            "three.two.bit.phonemanager.action.ACTIVITY_TRANSITION"

        // Detection interval in milliseconds (30 seconds for responsive detection)
        private const val DETECTION_INTERVAL_MS = 30_000L

        // Minimum confidence threshold for activity detection (0-100)
        private const val CONFIDENCE_THRESHOLD = 75
    }

    /**
     * Check if activity recognition permission is granted.
     */
    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Permission not required before Android Q
            true
        }
    }

    /**
     * Start monitoring activity transitions.
     * Requires ACTIVITY_RECOGNITION permission on Android Q+.
     */
    @SuppressLint("MissingPermission")
    fun startMonitoring() {
        if (!hasPermission()) {
            Timber.w("Activity recognition permission not granted, cannot start monitoring")
            return
        }

        if (_isMonitoring.value) {
            Timber.d("Activity monitoring already started")
            return
        }

        try {
            // Register broadcast receiver
            activityReceiver = ActivityTransitionReceiver { mode ->
                _currentActivity.value = mode
                Timber.d("Activity detected: $mode")
            }

            val intentFilter = IntentFilter(ACTION_ACTIVITY_TRANSITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    activityReceiver,
                    intentFilter,
                    Context.RECEIVER_NOT_EXPORTED,
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(activityReceiver, intentFilter)
            }

            // Build activity transition request
            val transitions = buildActivityTransitions()
            val request = ActivityTransitionRequest(transitions)

            val pendingIntent = createPendingIntent()

            activityClient.requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    _isMonitoring.value = true
                    Timber.i("Activity transition monitoring started successfully")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to start activity transition monitoring")
                    cleanupReceiver()
                }
        } catch (e: Exception) {
            Timber.e(e, "Exception starting activity monitoring")
            cleanupReceiver()
        }
    }

    /**
     * Stop monitoring activity transitions.
     */
    @SuppressLint("MissingPermission")
    fun stopMonitoring() {
        if (!_isMonitoring.value) {
            return
        }

        try {
            val pendingIntent = createPendingIntent()
            activityClient.removeActivityTransitionUpdates(pendingIntent)
                .addOnSuccessListener {
                    Timber.i("Activity transition monitoring stopped")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to stop activity transition monitoring")
                }

            cleanupReceiver()
            _isMonitoring.value = false
            _currentActivity.value = TransportationMode.UNKNOWN
        } catch (e: Exception) {
            Timber.e(e, "Exception stopping activity monitoring")
        }
    }

    private fun cleanupReceiver() {
        activityReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                Timber.w("Receiver was not registered")
            }
        }
        activityReceiver = null
    }

    private fun buildActivityTransitions(): List<ActivityTransition> {
        val activities = listOf(
            DetectedActivity.STILL,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.IN_VEHICLE,
        )

        return activities.flatMap { activityType ->
            listOf(
                ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build(),
            )
        }
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(ACTION_ACTIVITY_TRANSITION)
        intent.setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    /**
     * Convert Google's DetectedActivity to our TransportationMode.
     */
    private fun detectedActivityToTransportationMode(activityType: Int): TransportationMode =
        when (activityType) {
            DetectedActivity.STILL -> TransportationMode.STATIONARY
            DetectedActivity.WALKING -> TransportationMode.WALKING
            DetectedActivity.RUNNING -> TransportationMode.RUNNING
            DetectedActivity.ON_BICYCLE -> TransportationMode.CYCLING
            DetectedActivity.IN_VEHICLE -> TransportationMode.IN_VEHICLE
            else -> TransportationMode.UNKNOWN
        }

    /**
     * Internal broadcast receiver for activity transition events.
     */
    private inner class ActivityTransitionReceiver(
        private val onActivityDetected: (TransportationMode) -> Unit,
    ) : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_ACTIVITY_TRANSITION) {
                return
            }

            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent) ?: return

                for (event in result.transitionEvents) {
                    // Only handle ENTER transitions (when user starts an activity)
                    if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        val mode = detectedActivityToTransportationMode(event.activityType)
                        Timber.d(
                            "Activity transition: ${getActivityName(event.activityType)} → $mode",
                        )
                        onActivityDetected(mode)
                    }
                }
            }
        }

        private fun getActivityName(activityType: Int): String = when (activityType) {
            DetectedActivity.STILL -> "STILL"
            DetectedActivity.WALKING -> "WALKING"
            DetectedActivity.RUNNING -> "RUNNING"
            DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
            DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
            DetectedActivity.TILTING -> "TILTING"
            DetectedActivity.UNKNOWN -> "UNKNOWN"
            else -> "OTHER($activityType)"
        }
    }
}
