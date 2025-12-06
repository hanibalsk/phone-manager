package three.two.bit.phonemanager.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider

/**
 * Robot for interacting with the Map screen.
 *
 * Usage:
 * ```kotlin
 * MapScreenRobot(composeTestRule)
 *     .verifyMapDisplayed()
 *     .verifyMemberMarkerDisplayed("Alice")
 *     .clickOnMemberMarker("Alice")
 * ```
 */
class MapScreenRobot(
    semanticsProvider: SemanticsNodeInteractionsProvider
) : BaseRobot(semanticsProvider) {

    companion object {
        const val TAG_MAP_SCREEN = "map_screen"
        const val TAG_MAP_VIEW = "map_view"
        const val TAG_MY_LOCATION_BUTTON = "my_location_button"
        const val TAG_ZOOM_IN_BUTTON = "zoom_in_button"
        const val TAG_ZOOM_OUT_BUTTON = "zoom_out_button"
        const val TAG_MEMBER_LIST_TOGGLE = "member_list_toggle"
        const val TAG_MEMBER_LIST = "member_list"
        const val TAG_MEMBER_MARKER_PREFIX = "member_marker_"
        const val TAG_GEOFENCE_MARKER_PREFIX = "geofence_marker_"
        const val TAG_MEMBER_INFO_CARD = "member_info_card"
        const val TAG_GEOFENCE_INFO_CARD = "geofence_info_card"
        const val TAG_CREATE_GEOFENCE_BUTTON = "create_geofence_button"
        const val TAG_FILTER_BUTTON = "filter_button"
        const val TAG_BACK_BUTTON = "back_button"
    }

    // =============================================================================
    // Map Control Actions
    // =============================================================================

    /**
     * Center map on current location.
     */
    fun centerOnMyLocation(): MapScreenRobot {
        clickOnTag(TAG_MY_LOCATION_BUTTON)
        return this
    }

    /**
     * Zoom in on the map.
     */
    fun zoomIn(): MapScreenRobot {
        clickOnTag(TAG_ZOOM_IN_BUTTON)
        return this
    }

    /**
     * Zoom out on the map.
     */
    fun zoomOut(): MapScreenRobot {
        clickOnTag(TAG_ZOOM_OUT_BUTTON)
        return this
    }

    /**
     * Toggle member list visibility.
     */
    fun toggleMemberList(): MapScreenRobot {
        clickOnTag(TAG_MEMBER_LIST_TOGGLE)
        return this
    }

    /**
     * Open filter options.
     */
    fun openFilters(): MapScreenRobot {
        clickOnTag(TAG_FILTER_BUTTON)
        return this
    }

    /**
     * Navigate back.
     */
    fun navigateBack(): HomeScreenRobot {
        clickOnTag(TAG_BACK_BUTTON)
        return HomeScreenRobot(semanticsProvider)
    }

    // =============================================================================
    // Member Interaction Actions
    // =============================================================================

    /**
     * Click on a member's marker on the map.
     */
    fun clickOnMemberMarker(memberName: String): MapScreenRobot {
        clickOnTag("${TAG_MEMBER_MARKER_PREFIX}$memberName")
        return this
    }

    /**
     * Click on a member in the list.
     */
    fun clickOnMemberInList(memberName: String): MapScreenRobot {
        clickOnText(memberName)
        return this
    }

    /**
     * Dismiss member info card.
     */
    fun dismissMemberInfoCard(): MapScreenRobot {
        // Click outside the card or on close button
        clickOnTag(TAG_MAP_VIEW)
        return this
    }

    // =============================================================================
    // Geofence Actions
    // =============================================================================

    /**
     * Click on a geofence marker.
     */
    fun clickOnGeofenceMarker(geofenceName: String): MapScreenRobot {
        clickOnTag("${TAG_GEOFENCE_MARKER_PREFIX}$geofenceName")
        return this
    }

    /**
     * Start creating a new geofence.
     */
    fun startCreateGeofence(): MapScreenRobot {
        clickOnTag(TAG_CREATE_GEOFENCE_BUTTON)
        return this
    }

    // =============================================================================
    // Assertions
    // =============================================================================

    /**
     * Verify map screen is displayed.
     */
    fun verifyMapDisplayed(): MapScreenRobot {
        assertTagDisplayed(TAG_MAP_SCREEN)
        assertTagDisplayed(TAG_MAP_VIEW)
        return this
    }

    /**
     * Verify a member's marker is displayed.
     */
    fun verifyMemberMarkerDisplayed(memberName: String): MapScreenRobot {
        assertTagDisplayed("${TAG_MEMBER_MARKER_PREFIX}$memberName")
        return this
    }

    /**
     * Verify member info card is displayed.
     */
    fun verifyMemberInfoCardDisplayed(): MapScreenRobot {
        assertTagDisplayed(TAG_MEMBER_INFO_CARD)
        return this
    }

    /**
     * Verify member info shows correct name.
     */
    fun verifyMemberInfoName(name: String): MapScreenRobot {
        assertTextDisplayed(name)
        return this
    }

    /**
     * Verify member list is visible.
     */
    fun verifyMemberListDisplayed(): MapScreenRobot {
        assertTagDisplayed(TAG_MEMBER_LIST)
        return this
    }

    /**
     * Verify a geofence marker is displayed.
     */
    fun verifyGeofenceMarkerDisplayed(geofenceName: String): MapScreenRobot {
        assertTagDisplayed("${TAG_GEOFENCE_MARKER_PREFIX}$geofenceName")
        return this
    }

    /**
     * Verify geofence info card is displayed.
     */
    fun verifyGeofenceInfoCardDisplayed(): MapScreenRobot {
        assertTagDisplayed(TAG_GEOFENCE_INFO_CARD)
        return this
    }

    /**
     * Wait for map to be displayed.
     */
    fun waitForMapDisplayed(timeoutMs: Long = 10000): MapScreenRobot {
        waitForTag(TAG_MAP_VIEW, timeoutMs)
        return this
    }

    /**
     * Wait for member markers to load.
     */
    fun waitForMemberMarker(memberName: String, timeoutMs: Long = 10000): MapScreenRobot {
        waitForTag("${TAG_MEMBER_MARKER_PREFIX}$memberName", timeoutMs)
        return this
    }
}
