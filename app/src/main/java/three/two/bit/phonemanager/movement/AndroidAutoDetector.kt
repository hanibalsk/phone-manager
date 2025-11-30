package three.two.bit.phonemanager.movement

import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects Android Auto connection using UiModeManager's car mode detection.
 *
 * Android Auto triggers UI_MODE_TYPE_CAR when connected, which can be detected via:
 * - UiModeManager.getCurrentModeType()
 * - ACTION_ENTER_CAR_MODE / ACTION_EXIT_CAR_MODE broadcasts
 *
 * This also covers other car modes like:
 * - Android Auto (wired and wireless)
 * - Car docks with car mode enabled
 * - Manual car mode activation
 */
@Singleton
class AndroidAutoDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val uiModeManager: UiModeManager =
        context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

    private val _isInCarMode = MutableStateFlow(false)
    val isInCarMode: StateFlow<Boolean> = _isInCarMode.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var carModeReceiver: CarModeReceiver? = null

    companion object {
        // Action broadcast when entering car mode
        private const val ACTION_ENTER_CAR_MODE = "android.app.action.ENTER_CAR_MODE"

        // Action broadcast when exiting car mode
        private const val ACTION_EXIT_CAR_MODE = "android.app.action.EXIT_CAR_MODE"
    }

    /**
     * Check if currently in car mode (Android Auto or car dock).
     */
    fun isCurrentlyInCarMode(): Boolean {
        return try {
            val currentMode = uiModeManager.currentModeType
            currentMode == Configuration.UI_MODE_TYPE_CAR
        } catch (e: Exception) {
            Timber.w(e, "Exception checking car mode")
            false
        }
    }

    /**
     * Start monitoring for car mode changes.
     */
    fun startMonitoring() {
        if (_isMonitoring.value) {
            Timber.d("Android Auto detection already monitoring")
            return
        }

        try {
            // Check current state
            _isInCarMode.value = isCurrentlyInCarMode()
            Timber.d("Initial car mode state: ${_isInCarMode.value}")

            // Register broadcast receiver for car mode changes
            carModeReceiver = CarModeReceiver()
            val intentFilter = IntentFilter().apply {
                addAction(ACTION_ENTER_CAR_MODE)
                addAction(ACTION_EXIT_CAR_MODE)
                addAction(UiModeManager.ACTION_ENTER_CAR_MODE)
                addAction(UiModeManager.ACTION_EXIT_CAR_MODE)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    carModeReceiver,
                    intentFilter,
                    Context.RECEIVER_NOT_EXPORTED,
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(carModeReceiver, intentFilter)
            }

            _isMonitoring.value = true
            Timber.i("Android Auto detection monitoring started (initial state: ${_isInCarMode.value})")
        } catch (e: Exception) {
            Timber.e(e, "Exception starting Android Auto monitoring")
            cleanupReceiver()
        }
    }

    /**
     * Stop monitoring for car mode changes.
     */
    fun stopMonitoring() {
        if (!_isMonitoring.value) {
            return
        }

        try {
            cleanupReceiver()
            _isMonitoring.value = false
            _isInCarMode.value = false

            Timber.i("Android Auto detection monitoring stopped")
        } catch (e: Exception) {
            Timber.e(e, "Exception stopping Android Auto monitoring")
        }
    }

    private fun cleanupReceiver() {
        carModeReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                Timber.w("Car mode receiver was not registered")
            }
        }
        carModeReceiver = null
    }

    /**
     * Internal broadcast receiver for car mode events.
     */
    private inner class CarModeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_ENTER_CAR_MODE,
                UiModeManager.ACTION_ENTER_CAR_MODE,
                -> {
                    Timber.i("Entered car mode (Android Auto / car dock)")
                    _isInCarMode.value = true
                }

                ACTION_EXIT_CAR_MODE,
                UiModeManager.ACTION_EXIT_CAR_MODE,
                -> {
                    Timber.i("Exited car mode")
                    _isInCarMode.value = false
                }
            }
        }
    }
}
