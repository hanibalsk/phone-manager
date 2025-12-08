package three.two.bit.phonemanager.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import three.two.bit.phonemanager.network.models.PublicConfigResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ConfigApiService - HTTP client for public configuration endpoint
 *
 * Fetches feature flags and auth configuration from the server.
 * This is a public endpoint that does not require authentication.
 */
interface ConfigApiService {
    /**
     * Get public configuration (feature flags and auth settings)
     * GET /api/v1/config/public
     *
     * @return Result with configuration on success, exception on failure
     */
    suspend fun getPublicConfig(): Result<PublicConfigResponse>
}

@Singleton
class ConfigApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfiguration,
) : ConfigApiService {

    /**
     * Fetch public configuration from server
     * GET /api/v1/config/public
     *
     * This endpoint is public and does not require authentication.
     */
    override suspend fun getPublicConfig(): Result<PublicConfigResponse> = try {
        Timber.d("Fetching public configuration")

        val response: PublicConfigResponse = httpClient.get(
            "${apiConfig.baseUrl}/api/v1/config/public",
        ).body()

        Timber.i("Fetched public config: auth.registrationEnabled=${response.auth.registrationEnabled}")
        Result.success(response)
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch public configuration")
        Result.failure(e)
    }
}
