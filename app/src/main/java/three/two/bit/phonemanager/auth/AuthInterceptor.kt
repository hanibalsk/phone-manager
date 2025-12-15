package three.two.bit.phonemanager.auth

import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpStatusCode
import three.two.bit.phonemanager.data.repository.AuthRepository
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber

import javax.inject.Provider

/**
 * Story E9.11, Task 2: Authentication Interceptor for Ktor
 *
 * AC E9.11.2: Add Bearer token to Authorization header
 * AC E9.11.8: Handle 401 Unauthorized by refreshing token
 *
 * Flow:
 * 1. Check if access token exists
 * 2. Add Bearer token to Authorization header if available
 * 3. Fall back to X-API-Key if no token (for unauthenticated endpoints)
 * 4. On 401 response, attempt token refresh
 * 5. Retry original request with new token
 * 6. If refresh fails, logout user
 */
class AuthInterceptor(
    private val secureStorage: SecureStorage,
    private val authRepositoryProvider: Provider<AuthRepository>
) {

    /**
     * Get current access token
     */
    fun getAccessToken(): String? = secureStorage.getAccessToken()

    /**
     * Check if token is expired
     */
    fun isTokenExpired(): Boolean = secureStorage.isTokenExpired()

    /**
     * Get API key for fallback authentication
     */
    fun getApiKey(): String? = secureStorage.getApiKey()

    /**
     * Configure request with authentication headers
     *
     * AC E9.11.2: Add Bearer token to Authorization header if available
     *
     * Always adds Bearer token if available - even if expired.
     * The server will return 401, triggering automatic refresh via handle401Response.
     */
    fun configureRequest(request: HttpRequestBuilder) {
        val accessToken = secureStorage.getAccessToken()

        if (accessToken != null) {
            // Always add Bearer token if we have one (AC E9.11.2)
            // If expired, server will return 401 and we'll refresh via handle401Response
            request.header("Authorization", "Bearer $accessToken")
            if (secureStorage.isTokenExpired()) {
                Timber.d("Added expired Bearer token to request (will be refreshed on 401): ${request.url}")
            } else {
                Timber.v("Added Bearer token to request: ${request.url}")
            }
        } else {
            // Fall back to API key for unauthenticated endpoints (AC E9.11.8)
            val apiKey = secureStorage.getApiKey()
            if (!apiKey.isNullOrBlank()) {
                request.header("X-API-Key", apiKey)
                Timber.v("Added API key to request: ${request.url}")
            }
        }
    }

    /**
     * Attempt to refresh the access token
     *
     * AC E9.11.8: Refresh token on 401, retry request, or logout if refresh fails
     *
     * @return true if token was refreshed successfully
     */
    suspend fun refreshTokenIfNeeded(): Boolean {
        Timber.w("Attempting token refresh")

        // Attempt to refresh token (AC E9.11.8)
        // Use Provider to break circular dependency with HttpClient
        val authRepository = authRepositoryProvider.get()
        val refreshResult = authRepository.refreshToken()

        return if (refreshResult.isSuccess) {
            Timber.i("Token refreshed successfully")
            true
        } else {
            Timber.e(refreshResult.exceptionOrNull(), "Token refresh failed, logging out user")
            // Logout user if refresh fails (AC E9.11.8)
            authRepository.logout()
            false
        }
    }
}

/**
 * Ktor plugin for authentication with automatic retry on 401
 *
 * Uses HttpSend interceptor to properly retry requests after token refresh.
 *
 * Usage in NetworkModule:
 * ```
 * install(AuthPlugin) {
 *     authInterceptor = authInterceptor
 * }
 * ```
 */
val AuthPlugin = createClientPlugin("AuthPlugin", ::AuthPluginConfig) {
    val authInterceptor = pluginConfig.authInterceptor

    // Add authentication headers to outgoing requests
    onRequest { request, _ ->
        authInterceptor.configureRequest(request)
    }

    // Use HttpSend interceptor to handle 401 and retry
    client.plugin(HttpSend).intercept { request ->
        val originalCall = execute(request)

        // Check if we got a 401 Unauthorized
        if (originalCall.response.status == HttpStatusCode.Unauthorized) {
            val url = originalCall.request.url.toString()

            // Skip token refresh for auth endpoints (prevent infinite loop)
            if (url.contains("/auth/")) {
                Timber.d("Skipping token refresh for auth endpoint: $url")
                return@intercept originalCall
            }

            Timber.w("Received 401 Unauthorized for $url, attempting token refresh")

            // Try to refresh the token
            if (authInterceptor.refreshTokenIfNeeded()) {
                // Token refreshed - rebuild request with new token
                val newToken = authInterceptor.getAccessToken()
                if (newToken != null) {
                    Timber.i("Retrying request with new token: $url")

                    // Create new request with updated Authorization header
                    request.headers.remove("Authorization")
                    request.header("Authorization", "Bearer $newToken")

                    // Retry the request
                    return@intercept execute(request)
                }
            }
        }

        originalCall
    }
}

/**
 * Configuration for AuthPlugin
 */
class AuthPluginConfig {
    lateinit var authInterceptor: AuthInterceptor
}
