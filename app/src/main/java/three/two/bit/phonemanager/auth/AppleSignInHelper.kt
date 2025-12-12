package three.two.bit.phonemanager.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import three.two.bit.phonemanager.BuildConfig
import timber.log.Timber
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Story E9.11, Task 9: Apple Sign-In Helper
 *
 * Handles Apple OAuth sign-in flow for Android using web-based OAuth.
 *
 * Production Implementation:
 * - Uses web-based OAuth flow (recommended for Android)
 * - Opens Apple's authorization page in Custom Tab
 * - Handles callback via deep link
 * - Requires server-side configuration for Apple OAuth
 *
 * Mock Implementation:
 * - Controlled by BuildConfig.USE_MOCK_AUTH
 * - Returns mock ID token for testing
 *
 * Note: Apple Sign-In on Android requires:
 * 1. Apple Developer account with Sign In with Apple configured
 * 2. Backend service to handle the OAuth callback
 * 3. Deep link configuration for the app
 */
@Singleton
class AppleSignInHelper @Inject constructor() {

    companion object {
        // Apple OAuth provider identifier
        const val PROVIDER_ID = "apple"

        // Apple OAuth endpoints
        private const val APPLE_AUTH_URL = "https://appleid.apple.com/auth/authorize"

        // Response type for web-based flow
        private const val RESPONSE_TYPE = "code id_token"
        private const val RESPONSE_MODE = "form_post"
        private const val SCOPE = "email name"
    }

    // Callback handler for deep link result
    private var pendingCallback: ((Result<String>) -> Unit)? = null
    private var currentState: String? = null

    /**
     * Initiate Apple Sign-In flow
     *
     * Uses BuildConfig.USE_MOCK_AUTH to determine mock vs real implementation.
     *
     * @param activity Activity context for launching flow
     * @return Result with ID token on success, error on failure
     */
    suspend fun signIn(activity: Activity): Result<String> {
        return if (BuildConfig.USE_MOCK_AUTH) {
            mockSignIn()
        } else {
            realSignIn(activity)
        }
    }

    /**
     * Mock Apple Sign-In implementation
     *
     * Simulates user completing OAuth flow successfully.
     */
    private suspend fun mockSignIn(): Result<String> {
        Timber.d("[MOCK] Starting Apple Sign-In")

        try {
            // Simulate user interaction delay
            delay(1000)

            // Simulate random cancellation (10% chance)
            if (Math.random() < 0.1) {
                Timber.w("[MOCK] User cancelled Apple Sign-In")
                return Result.failure(Exception("User cancelled sign-in"))
            }

            // Return mock ID token
            val mockIdToken = "mock_apple_id_token_${System.currentTimeMillis()}"
            Timber.i("[MOCK] Apple Sign-In successful")

            return Result.success(mockIdToken)
        } catch (e: Exception) {
            Timber.e(e, "[MOCK] Apple Sign-In failed")
            return Result.failure(e)
        }
    }

    /**
     * Real Apple Sign-In implementation using web-based OAuth
     *
     * Opens Apple's authorization page in a Custom Tab browser.
     * The server-side redirect URI handles the callback and extracts the ID token.
     */
    private suspend fun realSignIn(activity: Activity): Result<String> {
        // Note: Apple Sign-In on Android requires server-side support
        // The ID token is exchanged server-side after the OAuth callback
        // This implementation launches the OAuth flow and relies on deep link callback

        val clientId = getAppleClientId()
        val redirectUri = getRedirectUri()

        if (clientId.isBlank() || redirectUri.isBlank()) {
            Timber.e("Apple OAuth not configured")
            return Result.failure(
                IllegalStateException("Apple OAuth not configured. Contact support for assistance."),
            )
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                // Generate state for CSRF protection
                val state = UUID.randomUUID().toString()
                currentState = state

                // Build authorization URL
                val authUrl = buildAuthUrl(clientId, redirectUri, state)

                Timber.d("Starting Apple Sign-In with URL: $authUrl")

                // Store callback for deep link handling
                pendingCallback = { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                // Launch Custom Tab for OAuth
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()

                customTabsIntent.launchUrl(activity, Uri.parse(authUrl))

                // Set up cancellation handler
                continuation.invokeOnCancellation {
                    pendingCallback = null
                    currentState = null
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to launch Apple Sign-In")
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Build the Apple OAuth authorization URL
     */
    private fun buildAuthUrl(clientId: String, redirectUri: String, state: String): String {
        val params = mapOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to RESPONSE_TYPE,
            "response_mode" to RESPONSE_MODE,
            "scope" to SCOPE,
            "state" to state,
        )

        val queryString = params.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }

        return "$APPLE_AUTH_URL?$queryString"
    }

    /**
     * Handle the OAuth callback from the deep link
     *
     * Called by the app when receiving the OAuth redirect.
     *
     * @param idToken The ID token from the OAuth callback
     * @param state The state parameter for CSRF validation
     */
    fun handleCallback(idToken: String?, state: String?, error: String?) {
        val callback = pendingCallback
        pendingCallback = null

        if (callback == null) {
            Timber.w("Received Apple Sign-In callback but no pending request")
            return
        }

        when {
            error != null -> {
                Timber.e("Apple Sign-In error: $error")
                callback(Result.failure(Exception("Apple Sign-In failed: $error")))
            }
            state != currentState -> {
                Timber.e("Apple Sign-In state mismatch")
                callback(Result.failure(Exception("Security validation failed")))
            }
            idToken.isNullOrBlank() -> {
                Timber.e("Apple Sign-In missing ID token")
                callback(Result.failure(Exception("No ID token received")))
            }
            else -> {
                Timber.i("Apple Sign-In successful, got ID token")
                callback(Result.success(idToken))
            }
        }

        currentState = null
    }

    /**
     * Cancel any pending sign-in request
     */
    fun cancelPendingSignIn() {
        pendingCallback?.invoke(Result.failure(Exception("Sign-in cancelled")))
        pendingCallback = null
        currentState = null
    }

    /**
     * Get Apple Client ID from configuration
     *
     * Note: Apple Sign-In requires a Services ID configured in Apple Developer Portal.
     * Set APPLE_OAUTH_CLIENT_ID in local.properties.
     */
    private fun getAppleClientId(): String {
        return BuildConfig.APPLE_OAUTH_CLIENT_ID
    }

    /**
     * Get redirect URI for OAuth callback
     *
     * This should point to the backend server that handles the OAuth callback
     * and redirects to the app via deep link.
     */
    private fun getRedirectUri(): String {
        // The redirect URI should point to the backend OAuth handler
        // which will exchange the code for tokens and redirect to the app
        val baseUrl = BuildConfig.API_BASE_URL
        return if (baseUrl.isNotBlank()) {
            "$baseUrl/api/v1/auth/apple/callback"
        } else {
            ""
        }
    }

    /**
     * Sign out from Apple
     *
     * Note: Apple doesn't have a client-side sign out API.
     * Sign out is handled by clearing local session.
     */
    suspend fun signOut() {
        if (BuildConfig.USE_MOCK_AUTH) {
            Timber.d("[MOCK] Apple Sign-Out")
            delay(200)
        } else {
            // No client-side action needed for Apple
            // Just clear local tokens (handled by auth manager)
            Timber.d("Apple Sign-Out (local session cleared)")
        }
    }
}
