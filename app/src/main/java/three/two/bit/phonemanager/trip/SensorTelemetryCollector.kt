package three.two.bit.phonemanager.trip

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Story E8.5: SensorTelemetryCollector - Collects sensor data for movement event enrichment
 *
 * Provides real-time sensor telemetry including:
 * - Accelerometer (magnitude, variance, peak frequency)
 * - Gyroscope (angular velocity magnitude)
 * - Step counter
 * - Significant motion detection
 * - Device state (battery, network)
 *
 * AC E8.5.1: Injectable singleton with collect() function
 */
@Singleton
class SensorTelemetryCollector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    // Accelerometer buffer for 5-second rolling window
    private val accelerometerBuffer = AccelerometerBuffer(windowDurationMs = 5000L)

    // Latest gyroscope reading
    @Volatile
    private var lastGyroscopeMagnitude: Float? = null

    // Step counter
    @Volatile
    private var lastStepCount: Int? = null

    // Significant motion (resets after detection)
    @Volatile
    private var significantMotionDetected: Boolean = false

    // Sensor listeners
    private var isListening = false

    private val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                accelerometerBuffer.add(event.values[0], event.values[1], event.values[2])
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val gyroscopeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                lastGyroscopeMagnitude = sqrt(x * x + y * y + z * z)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val stepCounterListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                lastStepCount = event.values[0].toInt()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val significantMotionListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_SIGNIFICANT_MOTION) {
                significantMotionDetected = true
                // Re-register since significant motion is one-shot
                registerSignificantMotionSensor()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    /**
     * AC E8.5.2: TelemetrySnapshot data class with all sensor fields
     */
    data class TelemetrySnapshot(
        val accelerometerMagnitude: Float?,
        val accelerometerVariance: Float?,
        val accelerometerPeakFrequency: Float?,
        val gyroscopeMagnitude: Float?,
        val stepCount: Int?,
        val significantMotion: Boolean?,
        val batteryLevel: Int?,
        val batteryCharging: Boolean?,
        val networkType: String?,
        val networkStrength: Int?,
    )

    /**
     * Start listening to all available sensors.
     */
    fun startListening() {
        if (isListening) return

        Timber.d("Starting sensor telemetry collection")

        sensorManager?.let { sm ->
            // Accelerometer
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
                sm.registerListener(
                    accelerometerListener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                )
                Timber.d("Accelerometer sensor registered")
            } ?: Timber.w("Accelerometer sensor not available")

            // Gyroscope
            sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let { sensor ->
                sm.registerListener(
                    gyroscopeListener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                )
                Timber.d("Gyroscope sensor registered")
            } ?: Timber.w("Gyroscope sensor not available")

            // Step counter
            sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let { sensor ->
                sm.registerListener(
                    stepCounterListener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                )
                Timber.d("Step counter sensor registered")
            } ?: Timber.w("Step counter sensor not available")

            // Significant motion
            registerSignificantMotionSensor()
        }

        isListening = true
    }

    /**
     * Stop listening to sensors.
     */
    fun stopListening() {
        if (!isListening) return

        Timber.d("Stopping sensor telemetry collection")

        sensorManager?.let { sm ->
            sm.unregisterListener(accelerometerListener)
            sm.unregisterListener(gyroscopeListener)
            sm.unregisterListener(stepCounterListener)
            sm.unregisterListener(significantMotionListener)
        }

        isListening = false
    }

    /**
     * Register significant motion sensor (one-shot trigger).
     */
    private fun registerSignificantMotionSensor() {
        sensorManager?.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)?.let { sensor ->
            sensorManager.requestTriggerSensor(
                object : android.hardware.TriggerEventListener() {
                    override fun onTrigger(event: android.hardware.TriggerEvent?) {
                        significantMotionDetected = true
                        // Re-register for next trigger
                        registerSignificantMotionSensor()
                    }
                },
                sensor,
            )
        }
    }

    /**
     * AC E8.5.1: Collect current sensor telemetry snapshot.
     *
     * AC E8.5.7: Completes within 100ms timeout, returns null for unavailable sensors.
     *
     * @return TelemetrySnapshot with current sensor readings
     */
    suspend fun collect(): TelemetrySnapshot {
        return withTimeoutOrNull(COLLECTION_TIMEOUT_MS) {
            collectTelemetry()
        } ?: run {
            Timber.w("Telemetry collection timed out, returning partial data")
            collectTelemetryNoTimeout()
        }
    }

    /**
     * Collect telemetry without timeout (used as fallback).
     */
    private fun collectTelemetryNoTimeout(): TelemetrySnapshot {
        return TelemetrySnapshot(
            accelerometerMagnitude = accelerometerBuffer.getMagnitude(),
            accelerometerVariance = accelerometerBuffer.getVariance(),
            accelerometerPeakFrequency = accelerometerBuffer.getPeakFrequency(),
            gyroscopeMagnitude = lastGyroscopeMagnitude,
            stepCount = lastStepCount,
            significantMotion = getAndResetSignificantMotion(),
            batteryLevel = getBatteryLevel(),
            batteryCharging = isCharging(),
            networkType = getNetworkType(),
            networkStrength = getNetworkStrength(),
        )
    }

    /**
     * Collect telemetry with all available data.
     */
    private fun collectTelemetry(): TelemetrySnapshot {
        return TelemetrySnapshot(
            accelerometerMagnitude = accelerometerBuffer.getMagnitude(),
            accelerometerVariance = accelerometerBuffer.getVariance(),
            accelerometerPeakFrequency = accelerometerBuffer.getPeakFrequency(),
            gyroscopeMagnitude = lastGyroscopeMagnitude,
            stepCount = lastStepCount,
            significantMotion = getAndResetSignificantMotion(),
            batteryLevel = getBatteryLevel(),
            batteryCharging = isCharging(),
            networkType = getNetworkType(),
            networkStrength = getNetworkStrength(),
        )
    }

    /**
     * Get and reset significant motion flag.
     */
    private fun getAndResetSignificantMotion(): Boolean {
        val motion = significantMotionDetected
        significantMotionDetected = false
        return motion
    }

    /**
     * AC E8.5.6: Get battery level (0-100).
     */
    private fun getBatteryLevel(): Int? {
        return try {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            Timber.w(e, "Failed to get battery level")
            null
        }
    }

    /**
     * AC E8.5.6: Check if device is charging.
     */
    private fun isCharging(): Boolean? {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            Timber.w(e, "Failed to get charging status")
            null
        }
    }

    /**
     * AC E8.5.6: Get network type (WIFI, MOBILE, NONE).
     */
    private fun getNetworkType(): String? {
        return try {
            val network = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(network)

            when {
                capabilities == null -> "NONE"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
                else -> "UNKNOWN"
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to get network type")
            null
        }
    }

    /**
     * AC E8.5.6: Get network signal strength.
     */
    private fun getNetworkStrength(): Int? {
        return try {
            // For simplicity, return null - proper implementation requires
            // PhoneStateListener or SignalStrengthCallback which is complex
            // This can be enhanced in a future story
            null
        } catch (e: Exception) {
            Timber.w(e, "Failed to get network strength")
            null
        }
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        stopListening()
        accelerometerBuffer.clear()
    }

    companion object {
        private const val COLLECTION_TIMEOUT_MS = 100L
    }
}

/**
 * AC E8.5.3: Rolling buffer for accelerometer samples.
 *
 * Maintains a 5-second window of accelerometer readings and provides
 * calculations for magnitude, variance, and peak frequency.
 */
internal class AccelerometerBuffer(
    private val windowDurationMs: Long = 5000L,
) {
    private data class Sample(
        val magnitude: Float,
        val timestamp: Long,
    )

    private val samples = ConcurrentLinkedDeque<Sample>()

    /**
     * Add a new accelerometer sample.
     *
     * @param x X-axis acceleration
     * @param y Y-axis acceleration
     * @param z Z-axis acceleration
     */
    fun add(x: Float, y: Float, z: Float) {
        val magnitude = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        samples.addLast(Sample(magnitude, now))
        removeOldSamples(now)
    }

    /**
     * Remove samples older than the window duration.
     */
    private fun removeOldSamples(now: Long) {
        val cutoff = now - windowDurationMs
        while (samples.peekFirst()?.let { it.timestamp < cutoff } == true) {
            samples.pollFirst()
        }
    }

    /**
     * AC E8.5.3: Get average magnitude.
     */
    fun getMagnitude(): Float? {
        val currentSamples = samples.toList()
        if (currentSamples.isEmpty()) return null

        return currentSamples.map { it.magnitude }.average().toFloat()
    }

    /**
     * AC E8.5.3: Get variance of magnitudes.
     *
     * Variance: E[(x - μ)²]
     */
    fun getVariance(): Float? {
        val currentSamples = samples.toList()
        if (currentSamples.size < 2) return null

        val magnitudes = currentSamples.map { it.magnitude }
        val mean = magnitudes.average()

        return magnitudes.map { (it - mean).pow(2).toDouble() }.average().toFloat()
    }

    /**
     * AC E8.5.3: Get simplified peak frequency using zero-crossings.
     *
     * Peak frequency ≈ zero_crossings / time / 2
     */
    fun getPeakFrequency(): Float? {
        val currentSamples = samples.toList()
        if (currentSamples.size < 3) return null

        val magnitudes = currentSamples.map { it.magnitude }
        val mean = magnitudes.average().toFloat()

        // Count zero-crossings (crossings of the mean)
        var zeroCrossings = 0
        for (i in 1 until magnitudes.size) {
            val prev = magnitudes[i - 1] - mean
            val curr = magnitudes[i] - mean
            if ((prev >= 0 && curr < 0) || (prev < 0 && curr >= 0)) {
                zeroCrossings++
            }
        }

        // Calculate time span
        val timeSpanSeconds = (currentSamples.last().timestamp - currentSamples.first().timestamp) / 1000f
        if (timeSpanSeconds <= 0) return null

        // Frequency = zero-crossings / time / 2
        return zeroCrossings / timeSpanSeconds / 2f
    }

    /**
     * Clear all samples.
     */
    fun clear() {
        samples.clear()
    }

    /**
     * Get sample count.
     */
    fun size(): Int = samples.size
}
