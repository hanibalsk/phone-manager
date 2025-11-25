package three.two.bit.phonemanager

import android.Manifest
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
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import dagger.hilt.android.AndroidEntryPoint
import three.two.bit.phonemanager.ui.navigation.PhoneManagerNavHost
import three.two.bit.phonemanager.ui.permissions.PermissionViewModel
import three.two.bit.phonemanager.ui.theme.PhoneManagerTheme
import timber.log.Timber

/**
 * MainActivity - Main entry point with Hilt integration and permission handling
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionViewModel: PermissionViewModel by viewModels()

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
                    )
                }
            }
        }
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
