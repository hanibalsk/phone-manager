package three.two.bit.phonemanager.auth

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import three.two.bit.phonemanager.data.repository.AuthRepository
import three.two.bit.phonemanager.security.SecureStorage
import timber.log.Timber

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
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository
) {

    /**
     * Configure request with authentication headers
     *
     * AC E9.11.2: Add Bearer token to Authorization header if available
     */
    fun configureRequest(request: HttpRequestBuilder) {
        val accessToken = secureStorage.getAccessToken()

        if (accessToken != null && !secureStorage.isTokenExpired()) {
            // Add Bearer token (AC E9.11.2)
            request.header("Authorization", "Bearer $accessToken")
            Timber.v("Added Bearer token to request: ${request.url}")
        } else {
            // Fall back to API key for unauthenticated endpoints
            runBlocking {
                val apiKey = preferencesRepository.apiKey.value
                if (apiKey.isNotBlank()) {
                    request.header("X-API-Key", apiKey)
                    Timber.v("Added API key to request: ${request.url}")
                }
            }
        }
    }

    /**
     * Handle 401 Unauthorized responses
     *
     * AC E9.11.8: Refresh token on 401, retry request, or logout if refresh fails
     *
     * @param response Original 401 response
     * @return true if token was refreshed and request should be retried
     */
    suspend fun handle401Response(response: HttpResponse): Boolean {
        if (response.status != HttpStatusCode.Unauthorized) {
            return false
        }

        Timber.w("Received 401 Unauthorized, attempting token refresh")

        // Skip token refresh for auth endpoints (prevent infinite loop)
        val url = response.call.request.url.toString()
        if (url.contains("/auth/")) {
            Timber.d("Skipping token refresh for auth endpoint: $url")
            return false
        }

        // Attempt to refresh token (AC E9.11.8)
        val refreshResult = authRepository.refreshToken()

        return if (refreshResult.isSuccess) {
            Timber.i("Token refreshed successfully, retrying request")
            true // Retry the request
        } else {
            Timber.e(refreshResult.exceptionOrNull(), "Token refresh failed, logging out user")
            // Logout user if refresh fails (AC E9.11.8)
            authRepository.logout()
            false // Don't retry
        }
    }
}

/**
 * Ktor plugin for authentication
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

    // Handle 401 responses and retry with refreshed token
    onResponse { response ->
        if (authInterceptor.handle401Response(response)) {
            // Token was refreshed, Ktor will automatically retry the request
            Timber.d("Request will be retried with new token")
        }
    }
}

/**
 * Configuration for AuthPlugin
 */
class AuthPluginConfig {
    lateinit var authInterceptor: AuthInterceptor
}
