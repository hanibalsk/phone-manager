package three.two.bit.phonemanager.movement

import android.content.Context
import android.location.LocationManager
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.repository.MovementEventRepository
import three.two.bit.phonemanager.domain.model.DeviceState
import three.two.bit.phonemanager.domain.model.EventLocation
import three.two.bit.phonemanager.domain.model.NetworkType
import three.two.bit.phonemanager.domain.model.SensorTelemetry
import three.two.bit.phonemanager.trip.SensorTelemetryCollector
import three.two.bit.phonemanager.trip.TripManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy
import three.two.bit.phonemanager.domain.model.DetectionSource as DomainDetectionSource

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
    private val movementEventRepository: MovementEventRepository,
    private val sensorTelemetryCollector: SensorTelemetryCollector,
    private val tripManagerLazy: Lazy<TripManager>,
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    // State tracking for movement event recording (AC E8.6.1)
    @Volatile
    private var lastState: TransportationState? = null

    @Volatile
    private var lastStateTimestamp: Long = 0L

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

        // Subscribe to state changes for logging and movement event recording (AC E8.6.1)
        transportationState
            .distinctUntilChanged { old, new -> old.mode == new.mode }
            .onEach { newState ->
                val previousState = lastState
                val previousTimestamp = lastStateTimestamp

                // Update tracking variables
                lastState = newState
                lastStateTimestamp = System.currentTimeMillis()

                Timber.d(
                    "Transportation state changed: mode=${newState.mode}, inVehicle=${newState.isInVehicle}, " +
                        "source=${newState.source}, multiplier=${newState.intervalMultiplier}",
                )

                // Record movement event if mode actually changed (AC E8.6.2)
                if (previousState != null && previousState.mode != newState.mode) {
                    recordMovementEvent(previousState, newState, previousTimestamp)
                }
            }
            .launchIn(managerScope)
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

    /**
     * Record a movement event when transportation mode changes.
     *
     * AC E8.6.2: Records event with telemetry when mode changes
     * AC E8.6.3: Populates all event fields
     * AC E8.6.4: Calculates detection latency
     * AC E8.6.5: Includes tripId from active trip
     * AC E8.6.6: Handles errors asynchronously
     */
    private fun recordMovementEvent(
        previousState: TransportationState,
        newState: TransportationState,
        previousTimestamp: Long,
    ) {
        managerScope.launch {
            try {
                // Collect sensor telemetry (AC E8.6.3)
                val telemetrySnapshot = sensorTelemetryCollector.collect()

                // Get location (AC E8.6.3)
                val eventLocation = getLastKnownLocation()

                // Calculate detection latency (AC E8.6.4)
                val detectionLatencyMs = System.currentTimeMillis() - previousTimestamp
                Timber.d("Movement event detection latency: ${detectionLatencyMs}ms")

                // Map detection source to domain model
                val domainSource = mapDetectionSource(newState.source)

                // Derive confidence from source (multiple sources = higher confidence)
                val confidence = when (newState.source) {
                    DetectionSource.MULTIPLE -> 0.95f
                    DetectionSource.ANDROID_AUTO -> 0.9f
                    DetectionSource.BLUETOOTH_CAR -> 0.85f
                    DetectionSource.ACTIVITY_RECOGNITION -> 0.8f
                    DetectionSource.NONE -> 0.5f
                }

                // Build device state from telemetry
                val deviceState = DeviceState(
                    batteryLevel = telemetrySnapshot.batteryLevel,
                    batteryCharging = telemetrySnapshot.batteryCharging,
                    networkType = mapNetworkType(telemetrySnapshot.networkType),
                    networkStrength = telemetrySnapshot.networkStrength,
                )

                // Build sensor telemetry domain object
                val sensorTelemetry = SensorTelemetry(
                    accelerometerMagnitude = telemetrySnapshot.accelerometerMagnitude,
                    accelerometerVariance = telemetrySnapshot.accelerometerVariance,
                    accelerometerPeakFrequency = telemetrySnapshot.accelerometerPeakFrequency,
                    gyroscopeMagnitude = telemetrySnapshot.gyroscopeMagnitude,
                    stepCount = telemetrySnapshot.stepCount,
                    significantMotion = telemetrySnapshot.significantMotion,
                    activityType = newState.mode.name,
                    activityConfidence = (confidence * 100).toInt(),
                )

                // Get active trip ID (AC E8.6.5)
                val tripId = tripManagerLazy.get().activeTrip.value?.id

                // Record event via repository (AC E8.6.2)
                val result = movementEventRepository.recordEvent(
                    tripId = tripId,
                    previousMode = previousState.mode,
                    newMode = newState.mode,
                    detectionSource = domainSource,
                    confidence = confidence,
                    detectionLatencyMs = detectionLatencyMs,
                    location = eventLocation,
                    deviceState = deviceState,
                    sensorTelemetry = sensorTelemetry,
                    movementContext = null, // Context can be added in future enhancement
                )

                result.onSuccess { eventId ->
                    Timber.i(
                        "Recorded movement event #$eventId: ${previousState.mode} → ${newState.mode} " +
                            "(latency=${detectionLatencyMs}ms, tripId=$tripId)",
                    )
                }.onFailure { e ->
                    Timber.e(e, "Failed to record movement event")
                }
            } catch (e: Exception) {
                // AC E8.6.6: Log errors but don't crash
                Timber.e(e, "Error recording movement event")
            }
        }
    }

    /**
     * Get the last known location for event recording.
     */
    @Suppress("MissingPermission")
    private fun getLastKnownLocation(): EventLocation? {
        return try {
            val location = locationManager?.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            location?.let {
                EventLocation(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    speed = if (it.hasSpeed()) it.speed else null,
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to get last known location for movement event")
            null
        }
    }

    /**
     * Map movement DetectionSource to domain model DetectionSource.
     */
    private fun mapDetectionSource(source: DetectionSource): DomainDetectionSource {
        return when (source) {
            DetectionSource.ACTIVITY_RECOGNITION -> DomainDetectionSource.ACTIVITY_RECOGNITION
            DetectionSource.BLUETOOTH_CAR -> DomainDetectionSource.SENSOR_FUSION
            DetectionSource.ANDROID_AUTO -> DomainDetectionSource.SENSOR_FUSION
            DetectionSource.MULTIPLE -> DomainDetectionSource.SENSOR_FUSION
            DetectionSource.NONE -> DomainDetectionSource.UNKNOWN
        }
    }

    /**
     * Map network type string to domain model NetworkType.
     */
    private fun mapNetworkType(networkType: String?): NetworkType? {
        return when (networkType) {
            "WIFI" -> NetworkType.WIFI
            "MOBILE" -> NetworkType.CELLULAR
            "NONE" -> NetworkType.NONE
            "UNKNOWN" -> NetworkType.UNKNOWN
            else -> null
        }
    }
}
