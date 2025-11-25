package three.two.bit.phonemanager.analytics

/**
 * Analytics abstraction layer for tracking app events
 *
 * This interface provides a provider-agnostic way to track analytics events.
 * Can be implemented with Firebase Analytics, Amplitude, Mixpanel, or any other
 * analytics provider.
 *
 * Story 1.2, AC 1.2.12: Permission flow analytics tracking
 */
interface Analytics {
    /**
     * Track a custom event with optional parameters
     *
     * @param eventName Name of the event (e.g., "permission_granted")
     * @param params Optional key-value pairs for event parameters
     */
    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap())

    /**
     * Set a user property
     *
     * @param name Property name
     * @param value Property value
     */
    fun setUserProperty(name: String, value: String)

    /**
     * Track permission rationale shown event
     *
     * @param permissionType Type of permission (location, background, notification)
     */
    fun logPermissionRationaleShown(permissionType: String) {
        logEvent("permission_rationale_shown", mapOf("permission_type" to permissionType))
    }

    /**
     * Track permission granted event
     *
     * @param permissionType Type of permission granted
     */
    fun logPermissionGranted(permissionType: String) {
        logEvent("permission_granted", mapOf("permission_type" to permissionType))
    }

    /**
     * Track permission denied event
     *
     * @param permissionType Type of permission denied
     * @param reason Denial reason (user_denied, permanently_denied)
     */
    fun logPermissionDenied(permissionType: String, reason: String) {
        logEvent(
            "permission_denied",
            mapOf(
                "permission_type" to permissionType,
                "denial_reason" to reason,
            ),
        )
    }

    /**
     * Track settings opened event
     */
    fun logPermissionSettingsOpened() {
        logEvent("permission_settings_opened")
    }

    /**
     * Track permission flow completion event
     *
     * @param allGranted Whether all required permissions were granted
     */
    fun logPermissionFlowCompleted(allGranted: Boolean) {
        logEvent("permission_flow_completed", mapOf("all_granted" to allGranted))
    }

    /**
     * Track tracking toggle event
     *
     * @param enabled Whether tracking was enabled or disabled
     */
    fun logTrackingToggled(enabled: Boolean) {
        logEvent("tracking_toggled", mapOf("enabled" to enabled))
    }

    /**
     * Track service state change event
     *
     * @param state Service state (stopped, starting, running, error)
     */
    fun logServiceStateChanged(state: String) {
        logEvent("service_state_changed", mapOf("state" to state))
    }
}

/**
 * No-op analytics implementation for testing or when analytics is disabled
 */
class NoOpAnalytics : Analytics {
    override fun logEvent(eventName: String, params: Map<String, Any>) {
        // No-op
    }

    override fun setUserProperty(name: String, value: String) {
        // No-op
    }
}

/**
 * Debug analytics implementation that logs to Timber
 */
class DebugAnalytics : Analytics {
    override fun logEvent(eventName: String, params: Map<String, Any>) {
        timber.log.Timber.d("Analytics: $eventName | Params: $params")
    }

    override fun setUserProperty(name: String, value: String) {
        timber.log.Timber.d("Analytics User Property: $name = $value")
    }
}
