package three.two.bit.phonemanager.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider

/**
 * Robot for interacting with the Home screen.
 *
 * Usage:
 * ```kotlin
 * HomeScreenRobot(composeTestRule)
 *     .verifyHomeScreenDisplayed()
 *     .verifyTrackingEnabled()
 *     .toggleTracking()
 * ```
 */
class HomeScreenRobot(
    semanticsProvider: SemanticsNodeInteractionsProvider
) : BaseRobot(semanticsProvider) {

    companion object {
        const val TAG_HOME_SCREEN = "home_screen"
        const val TAG_TRACKING_TOGGLE = "tracking_toggle"
        const val TAG_TRACKING_STATUS = "tracking_status"
        const val TAG_LOCATION_CARD = "location_card"
        const val TAG_GROUPS_CARD = "groups_card"
        const val TAG_TRIPS_CARD = "trips_card"
        const val TAG_REFRESH_BUTTON = "refresh_button"
        const val TAG_SETTINGS_BUTTON = "settings_button"
        const val TAG_MAP_BUTTON = "map_button"
        const val TAG_BATTERY_INDICATOR = "battery_indicator"
        const val TAG_LAST_UPDATE_TIME = "last_update_time"
        const val TAG_SECRET_MODE_BUTTON = "secret_mode_button"
        const val TAG_SECRET_MODE_ACTIVE = "secret_mode_active"
    }

    // =============================================================================
    // Navigation Actions
    // =============================================================================

    /**
     * Navigate to Settings screen.
     */
    fun navigateToSettings(): SettingsScreenRobot {
        clickOnTag(TAG_SETTINGS_BUTTON)
        return SettingsScreenRobot(semanticsProvider)
    }

    /**
     * Navigate to Map screen.
     */
    fun navigateToMap(): MapScreenRobot {
        clickOnTag(TAG_MAP_BUTTON)
        return MapScreenRobot(semanticsProvider)
    }

    /**
     * Navigate to Groups via groups card.
     */
    fun navigateToGroups(): GroupsScreenRobot {
        clickOnTag(TAG_GROUPS_CARD)
        return GroupsScreenRobot(semanticsProvider)
    }

    // =============================================================================
    // Tracking Actions
    // =============================================================================

    /**
     * Toggle location tracking on/off.
     */
    fun toggleTracking(): HomeScreenRobot {
        clickOnTag(TAG_TRACKING_TOGGLE)
        return this
    }

    /**
     * Enable tracking if not already enabled.
     */
    fun enableTracking(): HomeScreenRobot {
        // First check if tracking is disabled, then toggle
        try {
            assertTextDisplayed("Tracking: Off")
            toggleTracking()
        } catch (e: AssertionError) {
            // Already enabled
        }
        return this
    }

    /**
     * Disable tracking if not already disabled.
     */
    fun disableTracking(): HomeScreenRobot {
        try {
            assertTextDisplayed("Tracking: On")
            toggleTracking()
        } catch (e: AssertionError) {
            // Already disabled
        }
        return this
    }

    /**
     * Refresh location data.
     */
    fun refreshLocation(): HomeScreenRobot {
        clickOnTag(TAG_REFRESH_BUTTON)
        return this
    }

    /**
     * Toggle secret mode (hide location from group members).
     */
    fun toggleSecretMode(): HomeScreenRobot {
        clickOnTag(TAG_SECRET_MODE_BUTTON)
        return this
    }

    // =============================================================================
    // Assertions
    // =============================================================================

    /**
     * Verify home screen is displayed.
     */
    fun verifyHomeScreenDisplayed(): HomeScreenRobot {
        assertTagDisplayed(TAG_HOME_SCREEN)
        return this
    }

    /**
     * Verify tracking is enabled.
     */
    fun verifyTrackingEnabled(): HomeScreenRobot {
        assertTextDisplayed("Tracking: On")
        return this
    }

    /**
     * Verify tracking is disabled.
     */
    fun verifyTrackingDisabled(): HomeScreenRobot {
        assertTextDisplayed("Tracking: Off")
        return this
    }

    /**
     * Verify location card is displayed with data.
     */
    fun verifyLocationCardDisplayed(): HomeScreenRobot {
        assertTagDisplayed(TAG_LOCATION_CARD)
        return this
    }

    /**
     * Verify last update time is displayed.
     */
    fun verifyLastUpdateDisplayed(): HomeScreenRobot {
        assertTagDisplayed(TAG_LAST_UPDATE_TIME)
        return this
    }

    /**
     * Verify secret mode is active.
     */
    fun verifySecretModeActive(): HomeScreenRobot {
        assertTagDisplayed(TAG_SECRET_MODE_ACTIVE)
        return this
    }

    /**
     * Verify battery indicator is displayed.
     */
    fun verifyBatteryIndicatorDisplayed(): HomeScreenRobot {
        assertTagDisplayed(TAG_BATTERY_INDICATOR)
        return this
    }

    /**
     * Wait for home screen to be displayed.
     */
    fun waitForHomeScreen(timeoutMs: Long = 10000): HomeScreenRobot {
        waitForTag(TAG_HOME_SCREEN, timeoutMs)
        return this
    }

    /**
     * Wait for location update.
     */
    fun waitForLocationUpdate(timeoutMs: Long = 10000): HomeScreenRobot {
        waitForTag(TAG_LAST_UPDATE_TIME, timeoutMs)
        return this
    }
}
