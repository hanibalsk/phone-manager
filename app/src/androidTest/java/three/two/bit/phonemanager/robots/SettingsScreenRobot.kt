package three.two.bit.phonemanager.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider

/**
 * Robot for interacting with Settings screens.
 *
 * Usage:
 * ```kotlin
 * SettingsScreenRobot(composeTestRule)
 *     .verifySettingsDisplayed()
 *     .clickPermissions()
 *     .verifyLocationPermissionEnabled()
 * ```
 */
class SettingsScreenRobot(
    semanticsProvider: SemanticsNodeInteractionsProvider
) : BaseRobot(semanticsProvider) {

    companion object {
        const val TAG_SETTINGS_SCREEN = "settings_screen"
        const val TAG_PROFILE_SECTION = "profile_section"
        const val TAG_PERMISSIONS_SECTION = "permissions_section"
        const val TAG_NOTIFICATIONS_SECTION = "notifications_section"
        const val TAG_PRIVACY_SECTION = "privacy_section"
        const val TAG_ABOUT_SECTION = "about_section"
        const val TAG_LOGOUT_BUTTON = "logout_button"
        const val TAG_DELETE_ACCOUNT_BUTTON = "delete_account_button"

        // Profile
        const val TAG_EDIT_PROFILE_BUTTON = "edit_profile_button"
        const val TAG_NAME_INPUT = "name_input"
        const val TAG_EMAIL_DISPLAY = "email_display"
        const val TAG_SAVE_PROFILE_BUTTON = "save_profile_button"

        // Permissions
        const val TAG_LOCATION_PERMISSION_TOGGLE = "location_permission_toggle"
        const val TAG_BACKGROUND_LOCATION_TOGGLE = "background_location_toggle"
        const val TAG_NOTIFICATION_PERMISSION_TOGGLE = "notification_permission_toggle"
        const val TAG_CAMERA_PERMISSION_TOGGLE = "camera_permission_toggle"
        const val TAG_PERMISSION_STATUS_PREFIX = "permission_status_"

        // Notifications
        const val TAG_PUSH_NOTIFICATIONS_TOGGLE = "push_notifications_toggle"
        const val TAG_GEOFENCE_ALERTS_TOGGLE = "geofence_alerts_toggle"
        const val TAG_PROXIMITY_ALERTS_TOGGLE = "proximity_alerts_toggle"
        const val TAG_TRIP_NOTIFICATIONS_TOGGLE = "trip_notifications_toggle"

        // Privacy
        const val TAG_LOCATION_HISTORY_TOGGLE = "location_history_toggle"
        const val TAG_SHARE_LOCATION_TOGGLE = "share_location_toggle"
        const val TAG_SECRET_MODE_TOGGLE = "secret_mode_toggle"
        const val TAG_CLEAR_HISTORY_BUTTON = "clear_history_button"

        // About
        const val TAG_VERSION_INFO = "version_info"
        const val TAG_PRIVACY_POLICY_LINK = "privacy_policy_link"
        const val TAG_TERMS_OF_SERVICE_LINK = "terms_of_service_link"
        const val TAG_SUPPORT_LINK = "support_link"

        // Dialogs
        const val TAG_CONFIRM_DIALOG = "confirm_dialog"
        const val TAG_CONFIRM_BUTTON = "confirm_button"
        const val TAG_CANCEL_BUTTON = "cancel_button"

        const val TAG_BACK_BUTTON = "back_button"
    }

    // =============================================================================
    // Navigation Actions
    // =============================================================================

    /**
     * Navigate back.
     */
    fun navigateBack(): HomeScreenRobot {
        clickOnTag(TAG_BACK_BUTTON)
        return HomeScreenRobot(semanticsProvider)
    }

    /**
     * Click on profile section.
     */
    fun clickProfile(): SettingsScreenRobot {
        clickOnTag(TAG_PROFILE_SECTION)
        return this
    }

    /**
     * Click on permissions section.
     */
    fun clickPermissions(): SettingsScreenRobot {
        clickOnTag(TAG_PERMISSIONS_SECTION)
        return this
    }

    /**
     * Click on notifications section.
     */
    fun clickNotifications(): SettingsScreenRobot {
        clickOnTag(TAG_NOTIFICATIONS_SECTION)
        return this
    }

    /**
     * Click on privacy section.
     */
    fun clickPrivacy(): SettingsScreenRobot {
        clickOnTag(TAG_PRIVACY_SECTION)
        return this
    }

    /**
     * Click on about section.
     */
    fun clickAbout(): SettingsScreenRobot {
        clickOnTag(TAG_ABOUT_SECTION)
        return this
    }

    // =============================================================================
    // Profile Actions
    // =============================================================================

    /**
     * Click edit profile button.
     */
    fun clickEditProfile(): SettingsScreenRobot {
        clickOnTag(TAG_EDIT_PROFILE_BUTTON)
        return this
    }

    /**
     * Update profile name.
     */
    fun updateName(name: String): SettingsScreenRobot {
        enterTextByTag(TAG_NAME_INPUT, name)
        return this
    }

    /**
     * Save profile changes.
     */
    fun saveProfile(): SettingsScreenRobot {
        clickOnTag(TAG_SAVE_PROFILE_BUTTON)
        return this
    }

    // =============================================================================
    // Permission Actions
    // =============================================================================

    /**
     * Toggle location permission.
     */
    fun toggleLocationPermission(): SettingsScreenRobot {
        clickOnTag(TAG_LOCATION_PERMISSION_TOGGLE)
        return this
    }

    /**
     * Toggle background location permission.
     */
    fun toggleBackgroundLocation(): SettingsScreenRobot {
        clickOnTag(TAG_BACKGROUND_LOCATION_TOGGLE)
        return this
    }

    /**
     * Toggle notification permission.
     */
    fun toggleNotificationPermission(): SettingsScreenRobot {
        clickOnTag(TAG_NOTIFICATION_PERMISSION_TOGGLE)
        return this
    }

    /**
     * Toggle camera permission.
     */
    fun toggleCameraPermission(): SettingsScreenRobot {
        clickOnTag(TAG_CAMERA_PERMISSION_TOGGLE)
        return this
    }

    // =============================================================================
    // Notification Settings Actions
    // =============================================================================

    /**
     * Toggle push notifications.
     */
    fun togglePushNotifications(): SettingsScreenRobot {
        clickOnTag(TAG_PUSH_NOTIFICATIONS_TOGGLE)
        return this
    }

    /**
     * Toggle geofence alerts.
     */
    fun toggleGeofenceAlerts(): SettingsScreenRobot {
        clickOnTag(TAG_GEOFENCE_ALERTS_TOGGLE)
        return this
    }

    /**
     * Toggle proximity alerts.
     */
    fun toggleProximityAlerts(): SettingsScreenRobot {
        clickOnTag(TAG_PROXIMITY_ALERTS_TOGGLE)
        return this
    }

    /**
     * Toggle trip notifications.
     */
    fun toggleTripNotifications(): SettingsScreenRobot {
        clickOnTag(TAG_TRIP_NOTIFICATIONS_TOGGLE)
        return this
    }

    // =============================================================================
    // Privacy Actions
    // =============================================================================

    /**
     * Toggle location history.
     */
    fun toggleLocationHistory(): SettingsScreenRobot {
        clickOnTag(TAG_LOCATION_HISTORY_TOGGLE)
        return this
    }

    /**
     * Toggle share location.
     */
    fun toggleShareLocation(): SettingsScreenRobot {
        clickOnTag(TAG_SHARE_LOCATION_TOGGLE)
        return this
    }

    /**
     * Toggle secret mode.
     */
    fun toggleSecretMode(): SettingsScreenRobot {
        clickOnTag(TAG_SECRET_MODE_TOGGLE)
        return this
    }

    /**
     * Click clear history button.
     */
    fun clickClearHistory(): SettingsScreenRobot {
        clickOnTag(TAG_CLEAR_HISTORY_BUTTON)
        return this
    }

    // =============================================================================
    // Account Actions
    // =============================================================================

    /**
     * Click logout button.
     */
    fun clickLogout(): SettingsScreenRobot {
        scrollToTag(TAG_LOGOUT_BUTTON)
        clickOnTag(TAG_LOGOUT_BUTTON)
        return this
    }

    /**
     * Confirm logout in dialog.
     */
    fun confirmLogout(): AuthScreenRobot {
        clickOnTag(TAG_CONFIRM_BUTTON)
        return AuthScreenRobot(semanticsProvider)
    }

    /**
     * Cancel logout.
     */
    fun cancelLogout(): SettingsScreenRobot {
        clickOnTag(TAG_CANCEL_BUTTON)
        return this
    }

    /**
     * Perform complete logout.
     */
    fun performLogout(): AuthScreenRobot {
        clickLogout()
        return confirmLogout()
    }

    /**
     * Click delete account button.
     */
    fun clickDeleteAccount(): SettingsScreenRobot {
        scrollToTag(TAG_DELETE_ACCOUNT_BUTTON)
        clickOnTag(TAG_DELETE_ACCOUNT_BUTTON)
        return this
    }

    // =============================================================================
    // Dialog Actions
    // =============================================================================

    /**
     * Confirm action in dialog.
     */
    fun confirmDialog(): SettingsScreenRobot {
        clickOnTag(TAG_CONFIRM_BUTTON)
        return this
    }

    /**
     * Cancel dialog.
     */
    fun cancelDialog(): SettingsScreenRobot {
        clickOnTag(TAG_CANCEL_BUTTON)
        return this
    }

    // =============================================================================
    // Assertions
    // =============================================================================

    /**
     * Verify settings screen is displayed.
     */
    fun verifySettingsDisplayed(): SettingsScreenRobot {
        assertTagDisplayed(TAG_SETTINGS_SCREEN)
        return this
    }

    /**
     * Verify profile section is displayed.
     */
    fun verifyProfileSectionDisplayed(): SettingsScreenRobot {
        assertTagDisplayed(TAG_PROFILE_SECTION)
        return this
    }

    /**
     * Verify permissions section is displayed.
     */
    fun verifyPermissionsSectionDisplayed(): SettingsScreenRobot {
        assertTagDisplayed(TAG_PERMISSIONS_SECTION)
        return this
    }

    /**
     * Verify location permission is enabled.
     */
    fun verifyLocationPermissionEnabled(): SettingsScreenRobot {
        assertTagDisplayed("${TAG_PERMISSION_STATUS_PREFIX}location_granted")
        return this
    }

    /**
     * Verify location permission is disabled.
     */
    fun verifyLocationPermissionDisabled(): SettingsScreenRobot {
        assertTagDisplayed("${TAG_PERMISSION_STATUS_PREFIX}location_denied")
        return this
    }

    /**
     * Verify background location permission is enabled.
     */
    fun verifyBackgroundLocationEnabled(): SettingsScreenRobot {
        assertTagDisplayed("${TAG_PERMISSION_STATUS_PREFIX}background_location_granted")
        return this
    }

    /**
     * Verify notification permission is enabled.
     */
    fun verifyNotificationPermissionEnabled(): SettingsScreenRobot {
        assertTagDisplayed("${TAG_PERMISSION_STATUS_PREFIX}notification_granted")
        return this
    }

    /**
     * Verify version info is displayed.
     */
    fun verifyVersionInfoDisplayed(): SettingsScreenRobot {
        assertTagDisplayed(TAG_VERSION_INFO)
        return this
    }

    /**
     * Verify confirmation dialog is displayed.
     */
    fun verifyConfirmDialogDisplayed(): SettingsScreenRobot {
        assertTagDisplayed(TAG_CONFIRM_DIALOG)
        return this
    }

    /**
     * Wait for settings screen to load.
     */
    fun waitForSettingsDisplayed(timeoutMs: Long = 5000): SettingsScreenRobot {
        waitForTag(TAG_SETTINGS_SCREEN, timeoutMs)
        return this
    }
}
