package three.two.bit.phonemanager.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import three.two.bit.phonemanager.BuildConfig
import three.two.bit.phonemanager.network.ApiConfiguration
import three.two.bit.phonemanager.network.DeviceApiService
import three.two.bit.phonemanager.network.DeviceApiServiceImpl
import three.two.bit.phonemanager.network.GeofenceApiService
import three.two.bit.phonemanager.network.GeofenceApiServiceImpl
import three.two.bit.phonemanager.network.GeofenceEventApiService
import three.two.bit.phonemanager.network.GeofenceEventApiServiceImpl
import three.two.bit.phonemanager.network.LocationApiService
import three.two.bit.phonemanager.network.LocationApiServiceImpl
import three.two.bit.phonemanager.network.ProximityAlertApiService
import three.two.bit.phonemanager.network.ProximityAlertApiServiceImpl
import three.two.bit.phonemanager.network.MovementEventApiService
import three.two.bit.phonemanager.network.MovementEventApiServiceImpl
import three.two.bit.phonemanager.network.TripApiService
import three.two.bit.phonemanager.network.TripApiServiceImpl
import three.two.bit.phonemanager.network.WebhookApiService
import three.two.bit.phonemanager.network.WebhookApiServiceImpl
import three.two.bit.phonemanager.network.GroupApiService
import three.two.bit.phonemanager.network.GroupApiServiceImpl
import three.two.bit.phonemanager.network.EnrollmentApiService
import three.two.bit.phonemanager.network.EnrollmentApiServiceImpl
import three.two.bit.phonemanager.network.ConfigApiService
import three.two.bit.phonemanager.network.ConfigApiServiceImpl
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber
import java.util.UUID
import javax.inject.Singleton

/**
 * Story 0.2.2: NetworkModule - Provides Ktor HttpClient and API services
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = BuildConfig.DEBUG
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient(Android) {
        // JSON serialization
        install(ContentNegotiation) {
            json(json)
        }

        // Logging (only in debug builds)
        if (BuildConfig.DEBUG) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Timber.tag("Ktor").d(message)
                    }
                }
                level = LogLevel.HEADERS
            }
        }

        // Default request configuration
        defaultRequest {
            contentType(ContentType.Application.Json)
            // API Compatibility: Add X-Request-ID header for request tracing
            headers.append("X-Request-ID", UUID.randomUUID().toString())
            // Version compatibility: Send client version for backend compatibility checks
            headers.append("X-Client-Version", BuildConfig.VERSION_NAME)
        }

        // Configure engine
        engine {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
    }

    @Provides
    @Singleton
    fun provideApiConfiguration(@ApplicationContext context: Context, secureStorage: SecureStorage): ApiConfiguration {
        // Get base URL from secure storage - require HTTPS in production
        val baseUrl = secureStorage.getApiBaseUrl()
            ?: BuildConfig.API_BASE_URL.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "API base URL not configured. Set API_BASE_URL in build config or configure via SecureStorage.",
            )

        // Validate URL uses HTTPS in release builds
        if (!BuildConfig.DEBUG && !baseUrl.startsWith("https://")) {
            throw IllegalStateException(
                "API base URL must use HTTPS in production builds: $baseUrl",
            )
        }

        // Get API key from secure storage - require proper configuration
        val apiKey = secureStorage.getApiKey()
            ?: BuildConfig.API_KEY.takeIf { it.isNotBlank() && it != "default-api-key-change-me" }
            ?: throw IllegalStateException(
                "API key not configured. Set API_KEY in build config or configure via SecureStorage.",
            )

        return ApiConfiguration(
            baseUrl = baseUrl,
            apiKey = apiKey,
        )
    }

    @Provides
    @Singleton
    fun provideLocationApiService(httpClient: HttpClient, apiConfig: ApiConfiguration): LocationApiService =
        LocationApiServiceImpl(httpClient, apiConfig)

    @Provides
    @Singleton
    fun provideDeviceApiService(httpClient: HttpClient, apiConfig: ApiConfiguration): DeviceApiService =
        DeviceApiServiceImpl(httpClient, apiConfig)

    /**
     * Story E5.1: Proximity Alert API Service
     */
    @Provides
    @Singleton
    fun provideProximityAlertApiService(httpClient: HttpClient, apiConfig: ApiConfiguration): ProximityAlertApiService =
        ProximityAlertApiServiceImpl(httpClient, apiConfig)

    /**
     * Story E6.1: Geofence API Service
     */
    @Provides
    @Singleton
    fun provideGeofenceApiService(httpClient: HttpClient, apiConfig: ApiConfiguration): GeofenceApiService =
        GeofenceApiServiceImpl(httpClient, apiConfig)

    /**
     * Story E6.2: Geofence Event API Service
     */
    @Provides
    @Singleton
    fun provideGeofenceEventApiService(httpClient: HttpClient, apiConfig: ApiConfiguration): GeofenceEventApiService =
        GeofenceEventApiServiceImpl(httpClient, apiConfig)

    /**
     * Story E6.3: Webhook API Service
     */
    @Provides
    @Singleton
    fun provideWebhookApiService(httpClient: HttpClient, apiConfig: ApiConfiguration): WebhookApiService =
        WebhookApiServiceImpl(httpClient, apiConfig)

    /**
     * API Compatibility: Trip API Service
     */
    @Provides
    @Singleton
    fun provideTripApiService(httpClient: HttpClient, apiConfig: ApiConfiguration): TripApiService =
        TripApiServiceImpl(httpClient, apiConfig)

    /**
     * API Compatibility: Movement Event API Service
     */
    @Provides
    @Singleton
    fun provideMovementEventApiService(httpClient: HttpClient, apiConfig: ApiConfiguration): MovementEventApiService =
        MovementEventApiServiceImpl(httpClient, apiConfig)

    /**
     * Story E11.8: Group API Service
     */
    @Provides
    @Singleton
    fun provideGroupApiService(httpClient: HttpClient, apiConfig: ApiConfiguration): GroupApiService =
        GroupApiServiceImpl(httpClient, apiConfig)

    /**
     * Story E13.10: Enrollment API Service
     */
    @Provides
    @Singleton
    fun provideEnrollmentApiService(httpClient: HttpClient, apiConfig: ApiConfiguration): EnrollmentApiService =
        EnrollmentApiServiceImpl(httpClient, apiConfig)

    /**
     * Config API Service - Public configuration and feature flags
     */
    @Provides
    @Singleton
    fun provideConfigApiService(httpClient: HttpClient, apiConfig: ApiConfiguration): ConfigApiService =
        ConfigApiServiceImpl(httpClient, apiConfig)
}
