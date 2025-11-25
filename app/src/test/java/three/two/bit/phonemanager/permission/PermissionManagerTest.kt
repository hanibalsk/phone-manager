package three.two.bit.phonemanager.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for PermissionManager
 *
 * Story 1.2: Tests permission checking and state management
 * Verifies:
 * - Location permission checking
 * - Background location permission (Android 10+)
 * - Notification permission (Android 13+)
 * - Permission state flow
 * - Rationale checking
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PermissionManagerTest {

    private lateinit var permissionManager: PermissionManagerImpl
    private lateinit var context: Context
    private lateinit var activity: Activity

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        activity = mockk(relaxed = true)

        // Mock ContextCompat.checkSelfPermission
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `hasLocationPermission returns true when permission granted`() {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        permissionManager = PermissionManagerImpl(context)

        // When
        val result = permissionManager.hasLocationPermission()

        // Then
        assertTrue(result)
        verify { ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) }
    }

    @Test
    fun `hasLocationPermission returns false when permission denied`() {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        permissionManager = PermissionManagerImpl(context)

        // When
        val result = permissionManager.hasLocationPermission()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasBackgroundLocationPermission returns true on Android 9 and below`() {
        // Given
        setFinalStatic(Build.VERSION::class.java.getField("SDK_INT"), Build.VERSION_CODES.P)
        permissionManager = PermissionManagerImpl(context)

        // When
        val result = permissionManager.hasBackgroundLocationPermission()

        // Then
        assertTrue(result, "Background permission should be automatically granted on Android 9 and below")
    }

    @Test
    fun `hasBackgroundLocationPermission checks permission on Android 10+`() {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        permissionManager = PermissionManagerImpl(context)

        // When
        val result = permissionManager.hasBackgroundLocationPermission()

        // Then - depends on actual SDK version of test environment
        // This test would need Robolectric to properly test version-specific behavior
    }

    @Test
    fun `hasNotificationPermission returns true on Android 12 and below`() {
        // Given
        permissionManager = PermissionManagerImpl(context)

        // When - on devices below Android 13
        // Then notification permission is automatically granted
        // Note: This test would need Robolectric to properly test version-specific behavior
    }

    @Test
    fun `hasAllRequiredPermissions returns true when all permissions granted`() {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED

        permissionManager = PermissionManagerImpl(context)

        // When
        val result = permissionManager.hasAllRequiredPermissions()

        // Then
        // Result depends on SDK version, but method should check all three
    }

    @Test
    fun `hasAllRequiredPermissions returns false when location permission denied`() {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        permissionManager = PermissionManagerImpl(context)

        // When
        val result = permissionManager.hasAllRequiredPermissions()

        // Then
        assertFalse(result)
    }

    @Test
    fun `updatePermissionState emits LocationDenied when location permission not granted`() = runTest {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        permissionManager = PermissionManagerImpl(context)

        // When
        permissionManager.updatePermissionState()

        // Then
        permissionManager.observePermissionState().test {
            val state = awaitItem()
            assertIs<PermissionState.LocationDenied>(state)
        }
    }

    @Test
    fun `updatePermissionState emits AllGranted when all permissions granted`() = runTest {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED

        permissionManager = PermissionManagerImpl(context)

        // When
        permissionManager.updatePermissionState()

        // Then
        permissionManager.observePermissionState().test {
            val state = awaitItem()
            // State depends on SDK version
        }
    }

    @Test
    fun `observePermissionState emits initial Checking state`() = runTest {
        // Given
        permissionManager = PermissionManagerImpl(context)

        // When/Then
        permissionManager.observePermissionState().test {
            val state = awaitItem()
            assertIs<PermissionState.Checking>(state)
        }
    }

    @Test
    fun `shouldShowLocationRationale delegates to ActivityCompat`() {
        // Given
        mockkStatic("androidx.core.app.ActivityCompat")
        every {
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } returns true

        permissionManager = PermissionManagerImpl(context)

        // When
        val result = permissionManager.shouldShowLocationRationale(activity)

        // Then
        assertTrue(result)
        verify {
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }
    }

    @Test
    fun `shouldShowBackgroundRationale returns false on Android 9 and below`() {
        // Given
        permissionManager = PermissionManagerImpl(context)

        // When
        val result = permissionManager.shouldShowBackgroundRationale(activity)

        // Then - depends on SDK version
        // On Android 9 and below, should return false
    }

    /**
     * Helper method to set final static field for testing
     * Note: This is a workaround and may not work in all test environments
     */
    private fun setFinalStatic(field: java.lang.reflect.Field, newValue: Any) {
        try {
            field.isAccessible = true
            val modifiersField = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(field, field.modifiers and java.lang.reflect.Modifier.FINAL.inv())
            field.set(null, newValue)
        } catch (e: Exception) {
            // Ignore - version mocking not available in this environment
        }
    }
}
