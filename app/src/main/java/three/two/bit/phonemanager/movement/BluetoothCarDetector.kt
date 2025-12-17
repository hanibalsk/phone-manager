package three.two.bit.phonemanager.movement

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects connection to car Bluetooth systems.
 * Monitors A2DP (audio) and Headset (hands-free) profiles which are commonly used by cars.
 *
 * This helps identify when the user is in a vehicle by detecting:
 * - Car stereo Bluetooth connections (A2DP profile)
 * - Car hands-free systems (Headset profile)
 * - Device class hints indicating car audio/hands-free
 */
@Singleton
class BluetoothCarDetector @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _isConnectedToCar = MutableStateFlow(false)
    val isConnectedToCar: StateFlow<Boolean> = _isConnectedToCar.asStateFlow()

    private val _connectedCarDevice = MutableStateFlow<String?>(null)
    val connectedCarDevice: StateFlow<String?> = _connectedCarDevice.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var bluetoothReceiver: BluetoothConnectionReceiver? = null
    private var a2dpProxy: BluetoothA2dp? = null
    private var headsetProxy: BluetoothHeadset? = null

    companion object {
        // Common car Bluetooth device name patterns (case-insensitive)
        private val CAR_NAME_PATTERNS = listOf(
            "car",
            "auto",
            "vehicle",
            "ford",
            "toyota",
            "honda",
            "bmw",
            "mercedes",
            "audi",
            "volkswagen",
            "vw",
            "chevrolet",
            "chevy",
            "nissan",
            "hyundai",
            "kia",
            "mazda",
            "subaru",
            "lexus",
            "acura",
            "infiniti",
            "jeep",
            "dodge",
            "chrysler",
            "ram",
            "buick",
            "cadillac",
            "gmc",
            "porsche",
            "tesla",
            "volvo",
            "jaguar",
            "land rover",
            "range rover",
            "mini",
            "fiat",
            "alfa romeo",
            "maserati",
            "ferrari",
            "lamborghini",
            "bentley",
            "rolls royce",
            "aston martin",
            "carplay",
            "sync",
            "uconnect",
            "mylink",
            "entune",
            "mbux",
            "idrive",
            "mmi",
            "sensus",
            "hands-free",
            "handsfree",
            "hfp",
            "a2dp",
        )

        // Bluetooth device major classes that suggest car audio
        private const val DEVICE_CLASS_AUDIO_VIDEO = 0x0400
        private const val DEVICE_CLASS_CAR_AUDIO = 0x0420
        private const val DEVICE_CLASS_HIFI_AUDIO = 0x0428
    }

    /**
     * Check if Bluetooth permission is granted.
     */
    fun hasPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        // Bluetooth permission not required before Android S
        true
    }

    /**
     * Check if Bluetooth is available and enabled.
     */
    fun isBluetoothAvailable(): Boolean {
        if (!hasPermission()) return false
        return try {
            bluetoothAdapter?.isEnabled == true
        } catch (e: SecurityException) {
            Timber.w(e, "Security exception checking Bluetooth status")
            false
        }
    }

    /**
     * Start monitoring Bluetooth connections for car detection.
     */
    fun startMonitoring() {
        if (!hasPermission()) {
            Timber.w("Bluetooth permission not granted, cannot start monitoring")
            return
        }

        if (_isMonitoring.value) {
            Timber.d("Bluetooth car detection already monitoring")
            return
        }

        try {
            // Register broadcast receiver for connection changes
            bluetoothReceiver = BluetoothConnectionReceiver()
            val intentFilter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    bluetoothReceiver,
                    intentFilter,
                    Context.RECEIVER_NOT_EXPORTED,
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(bluetoothReceiver, intentFilter)
            }

            // Get profile proxies to check current connections
            setupProfileProxies()

            _isMonitoring.value = true
            Timber.i("Bluetooth car detection monitoring started")

            // Check current connections
            checkCurrentConnections()
        } catch (e: Exception) {
            Timber.e(e, "Exception starting Bluetooth monitoring")
            cleanupReceiver()
        }
    }

    /**
     * Stop monitoring Bluetooth connections.
     */
    fun stopMonitoring() {
        if (!_isMonitoring.value) {
            return
        }

        try {
            cleanupReceiver()
            cleanupProfileProxies()

            _isMonitoring.value = false
            _isConnectedToCar.value = false
            _connectedCarDevice.value = null

            Timber.i("Bluetooth car detection monitoring stopped")
        } catch (e: Exception) {
            Timber.e(e, "Exception stopping Bluetooth monitoring")
        }
    }

    private fun setupProfileProxies() {
        if (!hasPermission()) return

        try {
            bluetoothAdapter?.getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (profile == BluetoothProfile.A2DP) {
                            a2dpProxy = proxy as BluetoothA2dp
                            checkA2dpConnections()
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.A2DP) {
                            a2dpProxy = null
                        }
                    }
                },
                BluetoothProfile.A2DP,
            )

            bluetoothAdapter?.getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (profile == BluetoothProfile.HEADSET) {
                            headsetProxy = proxy as BluetoothHeadset
                            checkHeadsetConnections()
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.HEADSET) {
                            headsetProxy = null
                        }
                    }
                },
                BluetoothProfile.HEADSET,
            )
        } catch (e: SecurityException) {
            Timber.w(e, "Security exception setting up Bluetooth profile proxies")
        }
    }

    private fun cleanupReceiver() {
        bluetoothReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                Timber.w("Bluetooth receiver was not registered")
            }
        }
        bluetoothReceiver = null
    }

    private fun cleanupProfileProxies() {
        try {
            a2dpProxy?.let {
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, it)
            }
            headsetProxy?.let {
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, it)
            }
        } catch (e: Exception) {
            Timber.w(e, "Exception cleaning up profile proxies")
        }
        a2dpProxy = null
        headsetProxy = null
    }

    private fun checkCurrentConnections() {
        checkA2dpConnections()
        checkHeadsetConnections()
    }

    private fun checkA2dpConnections() {
        if (!hasPermission()) return

        try {
            val connectedDevices = a2dpProxy?.connectedDevices ?: return
            for (device in connectedDevices) {
                if (isLikelyCarDevice(device)) {
                    updateCarConnectionState(true, device)
                    return
                }
            }
        } catch (e: SecurityException) {
            Timber.w(e, "Security exception checking A2DP connections")
        }
    }

    private fun checkHeadsetConnections() {
        if (!hasPermission()) return

        try {
            val connectedDevices = headsetProxy?.connectedDevices ?: return
            for (device in connectedDevices) {
                if (isLikelyCarDevice(device)) {
                    updateCarConnectionState(true, device)
                    return
                }
            }
        } catch (e: SecurityException) {
            Timber.w(e, "Security exception checking headset connections")
        }
    }

    /**
     * Determine if a Bluetooth device is likely a car audio system.
     */
    private fun isLikelyCarDevice(device: BluetoothDevice): Boolean {
        if (!hasPermission()) return false

        return try {
            // Check device name for car-related patterns
            val deviceName = device.name?.lowercase() ?: ""
            val nameMatch = CAR_NAME_PATTERNS.any { pattern ->
                deviceName.contains(pattern)
            }

            // Check device class
            val deviceClass = device.bluetoothClass?.deviceClass ?: 0
            val majorClass = device.bluetoothClass?.majorDeviceClass ?: 0
            val classMatch = deviceClass == DEVICE_CLASS_CAR_AUDIO ||
                deviceClass == DEVICE_CLASS_HIFI_AUDIO ||
                majorClass == DEVICE_CLASS_AUDIO_VIDEO

            val isCarDevice = nameMatch || classMatch

            if (isCarDevice) {
                Timber.d(
                    "Detected car device: $deviceName (class: $deviceClass, major: $majorClass)",
                )
            }

            isCarDevice
        } catch (e: SecurityException) {
            Timber.w(e, "Security exception checking device")
            false
        }
    }

    private fun updateCarConnectionState(connected: Boolean, device: BluetoothDevice?) {
        val deviceName = try {
            if (hasPermission()) device?.name else null
        } catch (e: SecurityException) {
            null
        }

        _isConnectedToCar.value = connected
        _connectedCarDevice.value = if (connected) deviceName else null

        Timber.i(
            "Car Bluetooth connection state: connected=$connected, device=$deviceName",
        )
    }

    /**
     * Internal broadcast receiver for Bluetooth connection events.
     */
    private inner class BluetoothConnectionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!hasPermission()) return

            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        if (isLikelyCarDevice(it)) {
                            updateCarConnectionState(true, it)
                        }
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        if (isLikelyCarDevice(it)) {
                            // Check if any other car devices are still connected
                            checkCurrentConnections()
                            if (!_isConnectedToCar.value) {
                                updateCarConnectionState(false, null)
                            }
                        }
                    }
                }

                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
                -> {
                    val state = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_DISCONNECTED,
                    )
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        if (isLikelyCarDevice(it)) {
                            when (state) {
                                BluetoothProfile.STATE_CONNECTED -> {
                                    updateCarConnectionState(true, it)
                                }

                                BluetoothProfile.STATE_DISCONNECTED -> {
                                    checkCurrentConnections()
                                    if (!_isConnectedToCar.value) {
                                        updateCarConnectionState(false, null)
                                    }
                                }
                            }
                        }
                    }
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR,
                    )
                    if (state == BluetoothAdapter.STATE_OFF) {
                        updateCarConnectionState(false, null)
                    }
                }
            }
        }
    }
}
