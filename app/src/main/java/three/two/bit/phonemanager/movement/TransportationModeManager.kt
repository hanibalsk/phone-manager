package three.two.bit.phonemanager.movement

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing the current transportation state.
 */
data class TransportationState(
    val mode: TransportationMode = TransportationMode.UNKNOWN,
    val isInVehicle: Boolean = false,
    val source: DetectionSource = DetectionSource.NONE,
    val intervalMultiplier: Float = TransportationMode.DEFAULT_INTERVAL_MULTIPLIER,
) {
    /**
     * Calculate the adjusted tracking interval based on the transportation mode.
     *
     * @param baseIntervalMinutes the base tracking interval in minutes
     * @return adjusted interval in minutes
     */
    fun calculateAdjustedInterval(baseIntervalMinutes: Int): Int {
        val adjusted = (baseIntervalMinutes * intervalMultiplier).toInt()
        // Ensure minimum of 1 minute
        return adjusted.coerceAtLeast(1)
    }
}

/**
 * Source of the transportation mode detection.
 */
enum class DetectionSource {
    /** No detection source active */
    NONE,

    /** Detected via Google Activity Recognition API */
    ACTIVITY_RECOGNITION,

    /** Detected via Bluetooth car connection */
    BLUETOOTH_CAR,

    /** Detected via Android Auto / car mode */
    ANDROID_AUTO,

    /** Multiple sources confirming vehicle mode */
    MULTIPLE,
}

/**
 * Aggregates all transportation detection sources and provides a unified
 * transportation mode for the location tracking service.
 *
 * Detection priority (highest to lowest):
 * 1. Android Auto (most reliable indicator of being in a car)
 * 2. Bluetooth car connection
 * 3. Activity Recognition API
 *
 * When multiple sources detect vehicle mode, confidence is higher.
 */
@Singleton
class TransportationModeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityRecognitionManager: ActivityRecognitionManager,
    private val bluetoothCarDetector: BluetoothCarDetector,
    private val androidAutoDetector: AndroidAutoDetector,
    private val preferencesRepository: PreferencesRepository,
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    /**
     * Combined transportation state from all detection sources.
     * Uses configurable interval multipliers from preferences.
     */
    val transportationState: StateFlow<TransportationState> = combine(
        activityRecognitionManager.currentActivity,
        bluetoothCarDetector.isConnectedToCar,
        androidAutoDetector.isInCarMode,
        preferencesRepository.vehicleIntervalMultiplier,
        preferencesRepository.defaultIntervalMultiplier,
    ) { activityMode, isBluetoothCar, isAndroidAuto, vehicleMultiplier, defaultMultiplier ->
        computeTransportationState(
            activityMode,
            isBluetoothCar,
            isAndroidAuto,
            vehicleMultiplier,
            defaultMultiplier,
        )
    }.stateIn(
        scope = managerScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransportationState(),
    )

    /**
     * Convenience flow for just checking if in vehicle mode.
     */
    val isInVehicle: StateFlow<Boolean> = combine(
        transportationState,
    ) { states ->
        states[0].isInVehicle
    }.stateIn(
        scope = managerScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    /**
     * Start all transportation detection services.
     *
     * @param enableActivityRecognition whether to use Activity Recognition API
     * @param enableBluetoothDetection whether to use Bluetooth car detection
     * @param enableAndroidAutoDetection whether to use Android Auto detection
     */
    fun startMonitoring(
        enableActivityRecognition: Boolean = true,
        enableBluetoothDetection: Boolean = true,
        enableAndroidAutoDetection: Boolean = true,
    ) {
        if (_isMonitoring.value) {
            Timber.d("Transportation mode monitoring already started")
            return
        }

        Timber.i(
            "Starting transportation mode monitoring (activity=$enableActivityRecognition, " +
                "bluetooth=$enableBluetoothDetection, androidAuto=$enableAndroidAutoDetection)",
        )

        if (enableActivityRecognition) {
            activityRecognitionManager.startMonitoring()
        }

        if (enableBluetoothDetection) {
            bluetoothCarDetector.startMonitoring()
        }

        if (enableAndroidAutoDetection) {
            androidAutoDetector.startMonitoring()
        }

        _isMonitoring.value = true

        // Log state changes for debugging
        managerScope.launch {
            transportationState.collect { state ->
                Timber.d(
                    "Transportation state changed: mode=${state.mode}, inVehicle=${state.isInVehicle}, " +
                        "source=${state.source}, multiplier=${state.intervalMultiplier}",
                )
            }
        }
    }

    /**
     * Stop all transportation detection services.
     */
    fun stopMonitoring() {
        if (!_isMonitoring.value) {
            return
        }

        Timber.i("Stopping transportation mode monitoring")

        activityRecognitionManager.stopMonitoring()
        bluetoothCarDetector.stopMonitoring()
        androidAutoDetector.stopMonitoring()

        _isMonitoring.value = false
    }

    /**
     * Clean up resources when the manager is destroyed.
     */
    fun destroy() {
        stopMonitoring()
        managerScope.cancel()
    }

    /**
     * Compute the transportation state from all detection sources.
     *
     * @param activityMode the detected activity mode
     * @param isBluetoothCar whether connected to a car Bluetooth device
     * @param isAndroidAuto whether in Android Auto/car mode
     * @param vehicleMultiplier configurable multiplier for vehicle mode
     * @param defaultMultiplier configurable multiplier for other modes
     */
    private fun computeTransportationState(
        activityMode: TransportationMode,
        isBluetoothCar: Boolean,
        isAndroidAuto: Boolean,
        vehicleMultiplier: Float,
        defaultMultiplier: Float,
    ): TransportationState {
        // Count how many sources indicate vehicle mode
        val vehicleSources = mutableListOf<DetectionSource>()

        if (isAndroidAuto) {
            vehicleSources.add(DetectionSource.ANDROID_AUTO)
        }

        if (isBluetoothCar) {
            vehicleSources.add(DetectionSource.BLUETOOTH_CAR)
        }

        if (activityMode == TransportationMode.IN_VEHICLE) {
            vehicleSources.add(DetectionSource.ACTIVITY_RECOGNITION)
        }

        // Determine final state based on priority
        return when {
            // If any source indicates vehicle, we're in vehicle mode
            vehicleSources.isNotEmpty() -> {
                val source = when {
                    vehicleSources.size > 1 -> DetectionSource.MULTIPLE
                    else -> vehicleSources.first()
                }

                TransportationState(
                    mode = TransportationMode.IN_VEHICLE,
                    isInVehicle = true,
                    source = source,
                    intervalMultiplier = vehicleMultiplier,
                )
            }

            // Otherwise, use activity recognition result with default multiplier
            else -> {
                val source = if (activityMode != TransportationMode.UNKNOWN) {
                    DetectionSource.ACTIVITY_RECOGNITION
                } else {
                    DetectionSource.NONE
                }

                TransportationState(
                    mode = activityMode,
                    isInVehicle = false,
                    source = source,
                    intervalMultiplier = defaultMultiplier,
                )
            }
        }
    }

    /**
     * Check if required permissions are granted for each detection method.
     */
    fun getPermissionStatus(): Map<String, Boolean> = mapOf(
        "activity_recognition" to activityRecognitionManager.hasPermission(),
        "bluetooth" to bluetoothCarDetector.hasPermission(),
    )

    /**
     * Get current monitoring status for each detection source.
     */
    fun getMonitoringStatus(): Map<String, Boolean> = mapOf(
        "activity_recognition" to activityRecognitionManager.isMonitoring.value,
        "bluetooth" to bluetoothCarDetector.isMonitoring.value,
        "android_auto" to androidAutoDetector.isMonitoring.value,
    )
}
