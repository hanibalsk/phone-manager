package three.two.bit.phonemanager.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story 1.1/1.4/E2.1/E3.3/E7.4: PreferencesRepository - Manages app preferences using DataStore
 *
 * Story 1.4 additions: Service state persistence for boot restoration
 * Story E2.1 additions: Secret mode setting
 * Story E3.3 additions: Map polling interval
 * Story E7.4 additions: Weather notification toggle
 * Movement detection: Adaptive tracking interval based on transportation mode
 */
interface PreferencesRepository {
    val isTrackingEnabled: Flow<Boolean>
    suspend fun setTrackingEnabled(enabled: Boolean)
    val trackingInterval: Flow<Int>
    suspend fun setTrackingInterval(minutes: Int)

    // Story 1.4: Service state persistence
    val serviceRunningState: Flow<Boolean>
    suspend fun setServiceRunningState(isRunning: Boolean)
    val lastLocationUpdateTime: Flow<Long?>
    suspend fun setLastLocationUpdateTime(timestamp: Long)
    suspend fun clearLastLocationUpdateTime()

    // Story E2.1: Secret mode
    val isSecretModeEnabled: Flow<Boolean>
    suspend fun setSecretModeEnabled(enabled: Boolean)

    // Story E3.3: Map polling interval
    val mapPollingIntervalSeconds: Flow<Int>
    suspend fun setMapPollingIntervalSeconds(seconds: Int)

    // Story E7.4: Weather notification toggle
    val showWeatherInNotification: Flow<Boolean>
    suspend fun setShowWeatherInNotification(enabled: Boolean)

    // Movement detection: Adaptive tracking based on transportation mode
    val isMovementDetectionEnabled: Flow<Boolean>
    suspend fun setMovementDetectionEnabled(enabled: Boolean)
    val isActivityRecognitionEnabled: Flow<Boolean>
    suspend fun setActivityRecognitionEnabled(enabled: Boolean)
    val isBluetoothCarDetectionEnabled: Flow<Boolean>
    suspend fun setBluetoothCarDetectionEnabled(enabled: Boolean)
    val isAndroidAutoDetectionEnabled: Flow<Boolean>
    suspend fun setAndroidAutoDetectionEnabled(enabled: Boolean)

    // Movement detection: Configurable interval multipliers
    // Vehicle mode multiplier (0.1 to 1.0, lower = more frequent updates)
    val vehicleIntervalMultiplier: Flow<Float>
    suspend fun setVehicleIntervalMultiplier(multiplier: Float)

    // Default mode multiplier for walking/cycling/stationary (0.1 to 2.0)
    val defaultIntervalMultiplier: Flow<Float>
    suspend fun setDefaultIntervalMultiplier(multiplier: Float)

    // Story E8.8: Trip detection preferences
    val isTripDetectionEnabled: Flow<Boolean>
    suspend fun setTripDetectionEnabled(enabled: Boolean)

    val tripStationaryThresholdMinutes: Flow<Int>
    suspend fun setTripStationaryThresholdMinutes(minutes: Int)

    val tripMinimumDurationMinutes: Flow<Int>
    suspend fun setTripMinimumDurationMinutes(minutes: Int)

    val tripMinimumDistanceMeters: Flow<Int>
    suspend fun setTripMinimumDistanceMeters(meters: Int)

    val isTripAutoMergeEnabled: Flow<Boolean>
    suspend fun setTripAutoMergeEnabled(enabled: Boolean)

    val tripVehicleGraceSeconds: Flow<Int>
    suspend fun setTripVehicleGraceSeconds(seconds: Int)

    val tripWalkingGraceSeconds: Flow<Int>
    suspend fun setTripWalkingGraceSeconds(seconds: Int)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesRepositoryImpl @Inject constructor(@param:ApplicationContext private val context: Context) :
    PreferencesRepository {

    private object PreferencesKeys {
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        val TRACKING_INTERVAL = intPreferencesKey("tracking_interval_minutes")

        // Story 1.4: Service state persistence keys
        val SERVICE_RUNNING = booleanPreferencesKey("service_running")
        val LAST_LOCATION_UPDATE = longPreferencesKey("last_location_update")

        // Story E2.1: Secret mode key
        val SECRET_MODE_ENABLED = booleanPreferencesKey("secret_mode_enabled")

        // Story E3.3: Map polling interval key
        val MAP_POLLING_INTERVAL_SECONDS = intPreferencesKey("map_polling_interval_seconds")

        // Story E7.4: Weather notification toggle key
        val SHOW_WEATHER_IN_NOTIFICATION = booleanPreferencesKey("show_weather_in_notification")

        // Movement detection keys
        val MOVEMENT_DETECTION_ENABLED = booleanPreferencesKey("movement_detection_enabled")
        val ACTIVITY_RECOGNITION_ENABLED = booleanPreferencesKey("activity_recognition_enabled")
        val BLUETOOTH_CAR_DETECTION_ENABLED = booleanPreferencesKey("bluetooth_car_detection_enabled")
        val ANDROID_AUTO_DETECTION_ENABLED = booleanPreferencesKey("android_auto_detection_enabled")

        // Movement detection interval multiplier keys
        val VEHICLE_INTERVAL_MULTIPLIER = floatPreferencesKey("vehicle_interval_multiplier")
        val DEFAULT_INTERVAL_MULTIPLIER = floatPreferencesKey("default_interval_multiplier")

        // Story E8.8: Trip detection preference keys
        val TRIP_DETECTION_ENABLED = booleanPreferencesKey("trip_detection_enabled")
        val TRIP_STATIONARY_THRESHOLD_MINUTES = intPreferencesKey("trip_stationary_threshold_minutes")
        val TRIP_MINIMUM_DURATION_MINUTES = intPreferencesKey("trip_minimum_duration_minutes")
        val TRIP_MINIMUM_DISTANCE_METERS = intPreferencesKey("trip_minimum_distance_meters")
        val TRIP_AUTO_MERGE_ENABLED = booleanPreferencesKey("trip_auto_merge_enabled")
        val TRIP_VEHICLE_GRACE_SECONDS = intPreferencesKey("trip_vehicle_grace_seconds")
        val TRIP_WALKING_GRACE_SECONDS = intPreferencesKey("trip_walking_grace_seconds")
    }

    override val isTrackingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRACKING_ENABLED] ?: false
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading tracking enabled preference")
                emit(false)
            } else {
                throw exception
            }
        }

    override suspend fun setTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRACKING_ENABLED] = enabled
        }
        Timber.d("Tracking enabled set to: $enabled")
    }

    override val trackingInterval: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRACKING_INTERVAL] ?: DEFAULT_TRACKING_INTERVAL_MINUTES
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading tracking interval preference")
                emit(DEFAULT_TRACKING_INTERVAL_MINUTES)
            } else {
                throw exception
            }
        }

    override suspend fun setTrackingInterval(minutes: Int) {
        require(minutes in 1..60) { "Interval must be between 1 and 60 minutes" }
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRACKING_INTERVAL] = minutes
        }
        Timber.d("Tracking interval set to: $minutes minutes")
    }

    // Story 1.4: Service state persistence implementation

    override val serviceRunningState: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SERVICE_RUNNING] ?: false
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading service running state preference")
                emit(false)
            } else {
                throw exception
            }
        }

    override suspend fun setServiceRunningState(isRunning: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVICE_RUNNING] = isRunning
        }
        Timber.d("Service running state persisted: $isRunning")
    }

    override val lastLocationUpdateTime: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_LOCATION_UPDATE]
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading last location update time preference")
                emit(null)
            } else {
                throw exception
            }
        }

    override suspend fun setLastLocationUpdateTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_LOCATION_UPDATE] = timestamp
        }
        Timber.d("Last location update time persisted: $timestamp")
    }

    override suspend fun clearLastLocationUpdateTime() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.LAST_LOCATION_UPDATE)
        }
        Timber.d("Last location update time cleared")
    }

    // Story E2.1: Secret mode implementation

    override val isSecretModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SECRET_MODE_ENABLED] ?: false
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading secret mode preference")
                emit(false)
            } else {
                throw exception
            }
        }

    override suspend fun setSecretModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SECRET_MODE_ENABLED] = enabled
        }
        // Note: No logging here to avoid revealing secret mode state in logs (AC E2.1.6)
    }

    // Story E3.3: Map polling interval implementation

    override val mapPollingIntervalSeconds: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.MAP_POLLING_INTERVAL_SECONDS]
                ?: DEFAULT_MAP_POLLING_INTERVAL_SECONDS
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading map polling interval preference")
                emit(DEFAULT_MAP_POLLING_INTERVAL_SECONDS)
            } else {
                throw exception
            }
        }

    override suspend fun setMapPollingIntervalSeconds(seconds: Int) {
        require(seconds in 10..30) { "Polling interval must be between 10 and 30 seconds" }
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAP_POLLING_INTERVAL_SECONDS] = seconds
        }
        Timber.d("Map polling interval set to: $seconds seconds")
    }

    // Story E7.4: Weather notification toggle implementation

    override val showWeatherInNotification: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_WEATHER_IN_NOTIFICATION] ?: true
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading weather notification preference")
                emit(true)
            } else {
                throw exception
            }
        }

    override suspend fun setShowWeatherInNotification(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_WEATHER_IN_NOTIFICATION] = enabled
        }
        Timber.d("Weather notification setting set to: $enabled")
    }

    // Movement detection implementation

    override val isMovementDetectionEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.MOVEMENT_DETECTION_ENABLED] ?: false
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading movement detection preference")
                emit(false)
            } else {
                throw exception
            }
        }

    override suspend fun setMovementDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MOVEMENT_DETECTION_ENABLED] = enabled
        }
        Timber.d("Movement detection enabled set to: $enabled")
    }

    override val isActivityRecognitionEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ACTIVITY_RECOGNITION_ENABLED] ?: true
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading activity recognition preference")
                emit(true)
            } else {
                throw exception
            }
        }

    override suspend fun setActivityRecognitionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVITY_RECOGNITION_ENABLED] = enabled
        }
        Timber.d("Activity recognition enabled set to: $enabled")
    }

    override val isBluetoothCarDetectionEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BLUETOOTH_CAR_DETECTION_ENABLED] ?: true
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading Bluetooth car detection preference")
                emit(true)
            } else {
                throw exception
            }
        }

    override suspend fun setBluetoothCarDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLUETOOTH_CAR_DETECTION_ENABLED] = enabled
        }
        Timber.d("Bluetooth car detection enabled set to: $enabled")
    }

    override val isAndroidAutoDetectionEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ANDROID_AUTO_DETECTION_ENABLED] ?: true
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading Android Auto detection preference")
                emit(true)
            } else {
                throw exception
            }
        }

    override suspend fun setAndroidAutoDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANDROID_AUTO_DETECTION_ENABLED] = enabled
        }
        Timber.d("Android Auto detection enabled set to: $enabled")
    }

    // Movement detection interval multipliers implementation

    override val vehicleIntervalMultiplier: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.VEHICLE_INTERVAL_MULTIPLIER] ?: DEFAULT_VEHICLE_INTERVAL_MULTIPLIER
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading vehicle interval multiplier preference")
                emit(DEFAULT_VEHICLE_INTERVAL_MULTIPLIER)
            } else {
                throw exception
            }
        }

    override suspend fun setVehicleIntervalMultiplier(multiplier: Float) {
        require(multiplier in 0.1f..1.0f) { "Vehicle interval multiplier must be between 0.1 and 1.0" }
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VEHICLE_INTERVAL_MULTIPLIER] = multiplier
        }
        Timber.d("Vehicle interval multiplier set to: $multiplier")
    }

    override val defaultIntervalMultiplier: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_INTERVAL_MULTIPLIER] ?: DEFAULT_DEFAULT_INTERVAL_MULTIPLIER
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading default interval multiplier preference")
                emit(DEFAULT_DEFAULT_INTERVAL_MULTIPLIER)
            } else {
                throw exception
            }
        }

    override suspend fun setDefaultIntervalMultiplier(multiplier: Float) {
        require(multiplier in 0.1f..2.0f) { "Default interval multiplier must be between 0.1 and 2.0" }
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_INTERVAL_MULTIPLIER] = multiplier
        }
        Timber.d("Default interval multiplier set to: $multiplier")
    }

    // Story E8.8: Trip detection preferences implementation

    override val isTripDetectionEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRIP_DETECTION_ENABLED] ?: true
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading trip detection enabled preference")
                emit(true)
            } else {
                throw exception
            }
        }

    override suspend fun setTripDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRIP_DETECTION_ENABLED] = enabled
        }
        Timber.d("Trip detection enabled set to: $enabled")
    }

    override val tripStationaryThresholdMinutes: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRIP_STATIONARY_THRESHOLD_MINUTES]
                ?: DEFAULT_TRIP_STATIONARY_THRESHOLD_MINUTES
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading trip stationary threshold preference")
                emit(DEFAULT_TRIP_STATIONARY_THRESHOLD_MINUTES)
            } else {
                throw exception
            }
        }

    override suspend fun setTripStationaryThresholdMinutes(minutes: Int) {
        val validatedMinutes = minutes.coerceIn(1, 30)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRIP_STATIONARY_THRESHOLD_MINUTES] = validatedMinutes
        }
        Timber.d("Trip stationary threshold set to: $validatedMinutes minutes")
    }

    override val tripMinimumDurationMinutes: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRIP_MINIMUM_DURATION_MINUTES]
                ?: DEFAULT_TRIP_MINIMUM_DURATION_MINUTES
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading trip minimum duration preference")
                emit(DEFAULT_TRIP_MINIMUM_DURATION_MINUTES)
            } else {
                throw exception
            }
        }

    override suspend fun setTripMinimumDurationMinutes(minutes: Int) {
        val validatedMinutes = minutes.coerceIn(1, 10)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRIP_MINIMUM_DURATION_MINUTES] = validatedMinutes
        }
        Timber.d("Trip minimum duration set to: $validatedMinutes minutes")
    }

    override val tripMinimumDistanceMeters: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRIP_MINIMUM_DISTANCE_METERS]
                ?: DEFAULT_TRIP_MINIMUM_DISTANCE_METERS
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading trip minimum distance preference")
                emit(DEFAULT_TRIP_MINIMUM_DISTANCE_METERS)
            } else {
                throw exception
            }
        }

    override suspend fun setTripMinimumDistanceMeters(meters: Int) {
        val validatedMeters = meters.coerceIn(50, 500)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRIP_MINIMUM_DISTANCE_METERS] = validatedMeters
        }
        Timber.d("Trip minimum distance set to: $validatedMeters meters")
    }

    override val isTripAutoMergeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRIP_AUTO_MERGE_ENABLED] ?: true
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading trip auto-merge preference")
                emit(true)
            } else {
                throw exception
            }
        }

    override suspend fun setTripAutoMergeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRIP_AUTO_MERGE_ENABLED] = enabled
        }
        Timber.d("Trip auto-merge enabled set to: $enabled")
    }

    override val tripVehicleGraceSeconds: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRIP_VEHICLE_GRACE_SECONDS]
                ?: DEFAULT_TRIP_VEHICLE_GRACE_SECONDS
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading trip vehicle grace period preference")
                emit(DEFAULT_TRIP_VEHICLE_GRACE_SECONDS)
            } else {
                throw exception
            }
        }

    override suspend fun setTripVehicleGraceSeconds(seconds: Int) {
        val validatedSeconds = seconds.coerceIn(30, 180)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRIP_VEHICLE_GRACE_SECONDS] = validatedSeconds
        }
        Timber.d("Trip vehicle grace period set to: $validatedSeconds seconds")
    }

    override val tripWalkingGraceSeconds: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TRIP_WALKING_GRACE_SECONDS]
                ?: DEFAULT_TRIP_WALKING_GRACE_SECONDS
        }
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading trip walking grace period preference")
                emit(DEFAULT_TRIP_WALKING_GRACE_SECONDS)
            } else {
                throw exception
            }
        }

    override suspend fun setTripWalkingGraceSeconds(seconds: Int) {
        val validatedSeconds = seconds.coerceIn(30, 120)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRIP_WALKING_GRACE_SECONDS] = validatedSeconds
        }
        Timber.d("Trip walking grace period set to: $validatedSeconds seconds")
    }

    companion object {
        /** Default tracking interval in minutes */
        const val DEFAULT_TRACKING_INTERVAL_MINUTES = 5

        /** Default map polling interval in seconds (Story E3.3) */
        const val DEFAULT_MAP_POLLING_INTERVAL_SECONDS = 15

        /** Default vehicle interval multiplier (0.55 = 45% more frequent updates) */
        const val DEFAULT_VEHICLE_INTERVAL_MULTIPLIER = 0.55f

        /** Default interval multiplier for other modes (1.0 = no change) */
        const val DEFAULT_DEFAULT_INTERVAL_MULTIPLIER = 1.0f

        // Story E8.8: Trip detection defaults

        /** Default stationary threshold in minutes before ending trip */
        const val DEFAULT_TRIP_STATIONARY_THRESHOLD_MINUTES = 5

        /** Default minimum trip duration in minutes */
        const val DEFAULT_TRIP_MINIMUM_DURATION_MINUTES = 2

        /** Default minimum trip distance in meters */
        const val DEFAULT_TRIP_MINIMUM_DISTANCE_METERS = 100

        /** Default vehicle grace period in seconds */
        const val DEFAULT_TRIP_VEHICLE_GRACE_SECONDS = 90

        /** Default walking grace period in seconds */
        const val DEFAULT_TRIP_WALKING_GRACE_SECONDS = 60
    }
}
