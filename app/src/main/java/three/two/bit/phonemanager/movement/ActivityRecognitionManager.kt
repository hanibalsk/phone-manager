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
import com.google.android.gms.location.ActivityRecognitionResult
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
 * Data class for debug information about detected activities.
 */
data class ActivityDebugInfo(
    val detectedMode: TransportationMode,
    val confidence: Int,
    val allActivities: List<Pair<String, Int>>,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String,
)

/**
 * Manages activity recognition using Google Play Services Activity Recognition API.
 * Detects user's transportation mode (walking, running, cycling, vehicle, etc.)
 * to enable adaptive location tracking intervals.
 *
 * Uses dual API approach:
 * 1. Activity Transition API - for reliable state change detection
 * 2. Regular Activity Recognition API - for periodic updates with confidence values
 */
@Singleton
class ActivityRecognitionManager @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val activityClient: ActivityRecognitionClient =
        ActivityRecognition.getClient(context)

    private val _currentActivity = MutableStateFlow(TransportationMode.UNKNOWN)
    val currentActivity: StateFlow<TransportationMode> = _currentActivity.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    // Debug information flows
    private val _lastConfidence = MutableStateFlow(0)
    val lastConfidence: StateFlow<Int> = _lastConfidence.asStateFlow()

    private val _lastDebugInfo = MutableStateFlow<ActivityDebugInfo?>(null)
    val lastDebugInfo: StateFlow<ActivityDebugInfo?> = _lastDebugInfo.asStateFlow()

    private var activityTransitionReceiver: ActivityTransitionReceiver? = null
    private var activityUpdateReceiver: ActivityUpdateReceiver? = null

    companion object {
        private const val ACTION_ACTIVITY_TRANSITION =
            "three.two.bit.phonemanager.action.ACTIVITY_TRANSITION"
        private const val ACTION_ACTIVITY_UPDATE =
            "three.two.bit.phonemanager.action.ACTIVITY_UPDATE"

        // Detection interval in milliseconds (30 seconds for responsive detection)
        private const val DETECTION_INTERVAL_MS = 30_000L

        // Faster interval for immediate detection requests (5 seconds)
        private const val IMMEDIATE_DETECTION_INTERVAL_MS = 5_000L

        // Minimum confidence threshold for activity detection (0-100)
        private const val CONFIDENCE_THRESHOLD = 75
    }

    /**
     * Check if activity recognition permission is granted.
     */
    fun hasPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        // Permission not required before Android Q
        true
    }

    /**
     * Start monitoring activity transitions and regular activity updates.
     * Requires ACTIVITY_RECOGNITION permission on Android Q+.
     *
     * Uses dual API approach:
     * 1. Activity Transition API - for reliable state change detection
     * 2. Regular Activity Recognition API - for periodic updates with confidence values
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
            // Register transition receiver (for state changes)
            activityTransitionReceiver = ActivityTransitionReceiver { mode ->
                updateActivity(mode, 100, "transition")
            }

            val transitionFilter = IntentFilter(ACTION_ACTIVITY_TRANSITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    activityTransitionReceiver,
                    transitionFilter,
                    Context.RECEIVER_EXPORTED,
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(activityTransitionReceiver, transitionFilter)
            }

            // Register activity update receiver (for periodic updates with confidence)
            activityUpdateReceiver = ActivityUpdateReceiver { mode, confidence, allActivities ->
                if (confidence >= CONFIDENCE_THRESHOLD) {
                    updateActivity(mode, confidence, "periodic")
                }
                // Always update debug info regardless of threshold
                _lastDebugInfo.value = ActivityDebugInfo(
                    detectedMode = mode,
                    confidence = confidence,
                    allActivities = allActivities,
                    source = "periodic",
                )
            }

            val updateFilter = IntentFilter(ACTION_ACTIVITY_UPDATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    activityUpdateReceiver,
                    updateFilter,
                    Context.RECEIVER_EXPORTED,
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(activityUpdateReceiver, updateFilter)
            }

            // Build activity transition request
            val transitions = buildActivityTransitions()
            val request = ActivityTransitionRequest(transitions)

            val transitionPendingIntent = createTransitionPendingIntent()
            val updatePendingIntent = createUpdatePendingIntent()

            // Request activity transition updates
            activityClient.requestActivityTransitionUpdates(request, transitionPendingIntent)
                .addOnSuccessListener {
                    Timber.i("Activity transition monitoring started successfully")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to start activity transition monitoring")
                }

            // Request regular activity updates for continuous monitoring
            activityClient.requestActivityUpdates(DETECTION_INTERVAL_MS, updatePendingIntent)
                .addOnSuccessListener {
                    _isMonitoring.value = true
                    Timber.i("Regular activity updates started successfully")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to start regular activity updates")
                    cleanupReceivers()
                }
        } catch (e: Exception) {
            Timber.e(e, "Exception starting activity monitoring")
            cleanupReceivers()
        }
    }

    /**
     * Request immediate activity detection.
     * Useful for getting the current state when monitoring starts.
     */
    @SuppressLint("MissingPermission")
    fun requestImmediateDetection() {
        if (!hasPermission()) {
            Timber.w("Activity recognition permission not granted")
            return
        }

        try {
            val updatePendingIntent = createUpdatePendingIntent()

            // Request a faster update for immediate detection
            activityClient.requestActivityUpdates(IMMEDIATE_DETECTION_INTERVAL_MS, updatePendingIntent)
                .addOnSuccessListener {
                    Timber.i("Immediate activity detection requested")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to request immediate activity detection")
                }
        } catch (e: Exception) {
            Timber.e(e, "Exception requesting immediate detection")
        }
    }

    private fun updateActivity(mode: TransportationMode, confidence: Int, source: String) {
        _currentActivity.value = mode
        _lastConfidence.value = confidence
        Timber.d("Activity detected ($source): $mode with confidence $confidence%")
    }

    /**
     * Stop monitoring activity transitions and regular updates.
     */
    @SuppressLint("MissingPermission")
    fun stopMonitoring() {
        if (!_isMonitoring.value) {
            return
        }

        try {
            val transitionPendingIntent = createTransitionPendingIntent()
            val updatePendingIntent = createUpdatePendingIntent()

            activityClient.removeActivityTransitionUpdates(transitionPendingIntent)
                .addOnSuccessListener {
                    Timber.i("Activity transition monitoring stopped")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to stop activity transition monitoring")
                }

            activityClient.removeActivityUpdates(updatePendingIntent)
                .addOnSuccessListener {
                    Timber.i("Regular activity updates stopped")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to stop regular activity updates")
                }

            cleanupReceivers()
            _isMonitoring.value = false
            _currentActivity.value = TransportationMode.UNKNOWN
            _lastConfidence.value = 0
            _lastDebugInfo.value = null
        } catch (e: Exception) {
            Timber.e(e, "Exception stopping activity monitoring")
        }
    }

    private fun cleanupReceivers() {
        activityTransitionReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                Timber.w("Transition receiver was not registered")
            }
        }
        activityTransitionReceiver = null

        activityUpdateReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                Timber.w("Update receiver was not registered")
            }
        }
        activityUpdateReceiver = null
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

    private fun createTransitionPendingIntent(): PendingIntent {
        val intent = Intent(ACTION_ACTIVITY_TRANSITION)
        intent.setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun createUpdatePendingIntent(): PendingIntent {
        val intent = Intent(ACTION_ACTIVITY_UPDATE)
        intent.setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    /**
     * Convert Google's DetectedActivity to our TransportationMode.
     */
    private fun detectedActivityToTransportationMode(activityType: Int): TransportationMode = when (activityType) {
        DetectedActivity.STILL -> TransportationMode.STATIONARY
        DetectedActivity.WALKING -> TransportationMode.WALKING
        DetectedActivity.RUNNING -> TransportationMode.RUNNING
        DetectedActivity.ON_BICYCLE -> TransportationMode.CYCLING
        DetectedActivity.IN_VEHICLE -> TransportationMode.IN_VEHICLE
        else -> TransportationMode.UNKNOWN
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

    /**
     * Internal broadcast receiver for activity transition events.
     */
    private inner class ActivityTransitionReceiver(private val onActivityDetected: (TransportationMode) -> Unit) :
        BroadcastReceiver() {

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
    }

    /**
     * Internal broadcast receiver for regular activity updates (with confidence values).
     */
    private inner class ActivityUpdateReceiver(
        private val onActivityDetected: (TransportationMode, Int, List<Pair<String, Int>>) -> Unit,
    ) : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_ACTIVITY_UPDATE) {
                return
            }

            if (ActivityRecognitionResult.hasResult(intent)) {
                val result = ActivityRecognitionResult.extractResult(intent) ?: return

                // Get the most probable activity
                val mostProbable = result.mostProbableActivity
                val mode = detectedActivityToTransportationMode(mostProbable.type)
                val confidence = mostProbable.confidence

                // Collect all activities for debugging
                val allActivities = result.probableActivities.map { activity ->
                    getActivityName(activity.type) to activity.confidence
                }

                Timber.d(
                    "Activity update: ${getActivityName(mostProbable.type)} " +
                        "($confidence%) - All: $allActivities",
                )

                onActivityDetected(mode, confidence, allActivities)
            }
        }
    }
}
