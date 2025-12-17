package three.two.bit.phonemanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import three.two.bit.phonemanager.network.ConfigApiService
import three.two.bit.phonemanager.network.models.PublicConfigResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ConfigRepository - Manages public configuration and feature flags
 *
 * Provides access to server configuration including:
 * - Authentication settings (registration enabled, OAuth providers, etc.)
 * - Feature flags (geofences, proximity alerts, webhooks, etc.)
 *
 * The configuration is cached in memory and refreshed on demand.
 */
interface ConfigRepository {
    /** Current configuration as a Flow */
    val config: Flow<PublicConfigResponse?>

    /** Whether config has been loaded */
    val isLoaded: Boolean

    /** Fetch configuration from server */
    suspend fun fetchConfig(): Result<PublicConfigResponse>

    /** Get cached configuration (may be null if not loaded) */
    fun getCachedConfig(): PublicConfigResponse?

    // Feature flag helpers
    fun isRegistrationEnabled(): Boolean
    fun isInviteOnly(): Boolean
    fun isOAuthOnly(): Boolean
    fun isGoogleSignInEnabled(): Boolean
    fun isAppleSignInEnabled(): Boolean
    fun isGeofencesEnabled(): Boolean
    fun isProximityAlertsEnabled(): Boolean
    fun isWebhooksEnabled(): Boolean
    fun isMovementTrackingEnabled(): Boolean
    fun isB2BEnabled(): Boolean
    fun isGeofenceEventsEnabled(): Boolean
}

@Singleton
class ConfigRepositoryImpl @Inject constructor(private val configApiService: ConfigApiService) : ConfigRepository {

    private val _config = MutableStateFlow<PublicConfigResponse?>(null)
    override val config: Flow<PublicConfigResponse?> = _config.asStateFlow()

    override val isLoaded: Boolean
        get() = _config.value != null

    override suspend fun fetchConfig(): Result<PublicConfigResponse> {
        Timber.d("Fetching public configuration from server")

        return configApiService.getPublicConfig()
            .onSuccess { response ->
                _config.value = response
                Timber.i("Configuration loaded successfully")
            }
            .onFailure { error ->
                Timber.e(error, "Failed to fetch configuration")
            }
    }

    override fun getCachedConfig(): PublicConfigResponse? = _config.value

    // Auth feature helpers
    override fun isRegistrationEnabled(): Boolean = _config.value?.auth?.registrationEnabled ?: true

    override fun isInviteOnly(): Boolean = _config.value?.auth?.inviteOnly ?: false

    override fun isOAuthOnly(): Boolean = _config.value?.auth?.oauthOnly ?: false

    override fun isGoogleSignInEnabled(): Boolean = _config.value?.auth?.googleEnabled ?: false

    override fun isAppleSignInEnabled(): Boolean = _config.value?.auth?.appleEnabled ?: false

    // Feature flag helpers
    override fun isGeofencesEnabled(): Boolean = _config.value?.features?.geofences ?: true

    override fun isProximityAlertsEnabled(): Boolean = _config.value?.features?.proximityAlerts ?: true

    override fun isWebhooksEnabled(): Boolean = _config.value?.features?.webhooks ?: true

    override fun isMovementTrackingEnabled(): Boolean = _config.value?.features?.movementTracking ?: true

    override fun isB2BEnabled(): Boolean = _config.value?.features?.b2b ?: false

    override fun isGeofenceEventsEnabled(): Boolean = _config.value?.features?.geofenceEvents ?: true
}
