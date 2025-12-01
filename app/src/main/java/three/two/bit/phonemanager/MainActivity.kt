package three.two.bit.phonemanager

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import dagger.hilt.android.AndroidEntryPoint
import three.two.bit.phonemanager.security.SecureStorage
import three.two.bit.phonemanager.ui.navigation.PhoneManagerNavHost
import three.two.bit.phonemanager.ui.permissions.PermissionViewModel
import three.two.bit.phonemanager.ui.theme.PhoneManagerTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * MainActivity - Main entry point with Hilt integration and permission handling
 *
 * Story E11.9: Added deep link support for group invites (AC E11.9.8)
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var secureStorage: SecureStorage

    private val permissionViewModel: PermissionViewModel by viewModels()

    // Story E7.2: State for deep link navigation (AC E7.2.4)
    private var navigationDestination by mutableStateOf<String?>(null)

    // Story E11.9: State for invite code from deep link (AC E11.9.8)
    private var pendingInviteCode by mutableStateOf<String?>(null)

    // Permission launchers - must be registered before onCreate
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        permissionViewModel.onLocationPermissionResult(locationGranted, shouldShow)
    }

    private val backgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val shouldShow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            )
        } else {
            false
        }

        permissionViewModel.onBackgroundPermissionResult(granted, shouldShow)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionViewModel.onNotificationPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("MainActivity created")

        // Story E7.2: Extract navigation destination from intent (AC E7.2.4)
        navigationDestination = intent?.getStringExtra(EXTRA_NAVIGATE_TO)

        // Story E11.9: Extract invite code from deep link (AC E11.9.8)
        handleDeepLinkIntent(intent)

        setContent {
            PhoneManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PhoneManagerNavHost(
                        permissionViewModel = permissionViewModel,
                        onRequestLocationPermission = ::requestLocationPermission,
                        onRequestBackgroundPermission = ::requestBackgroundPermission,
                        onRequestNotificationPermission = ::requestNotificationPermission,
                        isRegistered = secureStorage.isRegistered(),
                        initialDestination = navigationDestination,
                        // Story E11.9: Pass invite code from deep link (AC E11.9.8)
                        pendingInviteCode = pendingInviteCode,
                        onInviteCodeConsumed = { pendingInviteCode = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Story E7.2: Handle notification tap when app is already running (AC E7.2.4)
        val destination = intent.getStringExtra(EXTRA_NAVIGATE_TO)
        Timber.d("onNewIntent called with destination: $destination")
        if (destination != null) {
            Timber.d("Deep link navigation to: $destination")
            navigationDestination = destination
            // Update the intent so it's available in onCreate after process recreation
            setIntent(intent)
        }

        // Story E11.9: Handle invite deep link when app is already running (AC E11.9.8)
        handleDeepLinkIntent(intent)
    }

    /**
     * Story E11.9: Extract invite code from deep link URI (AC E11.9.8)
     *
     * Handles deep links in the format: phonemanager://join/{code}
     */
    private fun handleDeepLinkIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == DEEP_LINK_SCHEME && uri.host == DEEP_LINK_HOST_JOIN) {
            val code = uri.pathSegments.firstOrNull()
            if (code != null && code.length == 8 && code.all { it.isLetterOrDigit() }) {
                Timber.i("Deep link invite code received: $code")
                pendingInviteCode = code.uppercase()
            } else {
                Timber.w("Invalid invite code in deep link: $code")
            }
        }
    }

    companion object {
        // Story E7.2: Intent extra for deep link navigation
        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val DESTINATION_WEATHER = "weather"

        // Story E11.9: Deep link constants for invite codes (AC E11.9.8)
        const val DEEP_LINK_SCHEME = "phonemanager"
        const val DEEP_LINK_HOST_JOIN = "join"
    }

    override fun onResume() {
        super.onResume()
        // Recheck permissions when returning from Settings
        permissionViewModel.checkPermissions()
    }

    private fun requestLocationPermission() {
        Timber.d("Launching location permission request")
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    private fun requestBackgroundPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Timber.d("Launching background permission request")
            backgroundPermissionLauncher.launch(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Timber.d("Launching notification permission request")
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }
}
