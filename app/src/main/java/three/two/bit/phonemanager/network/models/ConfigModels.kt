package three.two.bit.phonemanager.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Public configuration response from the server.
 * Contains feature flags and authentication settings.
 *
 * GET /api/v1/config/public
 */
@Serializable
data class PublicConfigResponse(
    val auth: AuthConfigResponse,
    val features: FeaturesConfigResponse,
)

/**
 * Authentication configuration from the server.
 */
@Serializable
data class AuthConfigResponse(
    @SerialName("registration_enabled")
    val registrationEnabled: Boolean,
    @SerialName("invite_only")
    val inviteOnly: Boolean,
    @SerialName("oauth_only")
    val oauthOnly: Boolean,
    @SerialName("google_enabled")
    val googleEnabled: Boolean,
    @SerialName("apple_enabled")
    val appleEnabled: Boolean,
)

/**
 * Feature flags from the server.
 */
@Serializable
data class FeaturesConfigResponse(
    val geofences: Boolean,
    @SerialName("proximity_alerts")
    val proximityAlerts: Boolean,
    val webhooks: Boolean,
    @SerialName("movement_tracking")
    val movementTracking: Boolean,
    val b2b: Boolean,
    @SerialName("geofence_events")
    val geofenceEvents: Boolean,
)
