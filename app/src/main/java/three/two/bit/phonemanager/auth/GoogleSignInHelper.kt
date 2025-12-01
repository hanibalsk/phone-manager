package three.two.bit.phonemanager.auth

import android.app.Activity
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E9.11, Task 8: Google Sign-In Helper
 *
 * Handles Google OAuth sign-in flow for Android.
 *
 * Mock Implementation:
 * - Simulates Google Sign-In without SDK dependency
 * - Returns mock ID token for testing
 * - Use for development until backend OAuth is ready
 *
 * Production Implementation (TODO):
 * - Add dependency: implementation("com.google.android.gms:play-services-auth:21.0.0")
 * - Configure OAuth client ID in google-services.json
 * - Use GoogleSignInClient from Play Services
 * - Handle actual OAuth flow with Google
 */
@Singleton
class GoogleSignInHelper @Inject constructor() {

    companion object {
        // Mock mode toggle (set false when integrating real Google Sign-In)
        private const val USE_MOCK = true

        // Google OAuth provider identifier
        const val PROVIDER_ID = "google"
    }

    /**
     * Initiate Google Sign-In flow
     *
     * Mock: Simulates successful sign-in after delay
     * Production: Launches GoogleSignInClient intent
     *
     * @param activity Activity context for launching intent
     * @return Result with ID token on success, error on failure
     */
    suspend fun signIn(activity: Activity): Result<String> {
        return if (USE_MOCK) {
            mockSignIn()
        } else {
            realSignIn(activity)
        }
    }

    /**
     * Mock Google Sign-In implementation
     *
     * Simulates user completing OAuth flow successfully.
     */
    private suspend fun mockSignIn(): Result<String> {
        Timber.d("[MOCK] Starting Google Sign-In")

        try {
            // Simulate user interaction delay
            delay(1000)

            // Simulate random cancellation (10% chance)
            if (Math.random() < 0.1) {
                Timber.w("[MOCK] User cancelled Google Sign-In")
                return Result.failure(Exception("User cancelled sign-in"))
            }

            // Return mock ID token
            val mockIdToken = "mock_google_id_token_${System.currentTimeMillis()}"
            Timber.i("[MOCK] Google Sign-In successful")

            return Result.success(mockIdToken)
        } catch (e: Exception) {
            Timber.e(e, "[MOCK] Google Sign-In failed")
            return Result.failure(e)
        }
    }

    /**
     * Real Google Sign-In implementation
     *
     * TODO: Implement when integrating Google Play Services
     *
     * Steps:
     * 1. Create GoogleSignInOptions with requestIdToken()
     * 2. Build GoogleSignInClient
     * 3. Launch signInIntent
     * 4. Handle result in onActivityResult
     * 5. Extract ID token from GoogleSignInAccount
     */
    private suspend fun realSignIn(activity: Activity): Result<String> {
        // TODO: Implement real Google Sign-In
        /*
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.google_oauth_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        val signInIntent = googleSignInClient.signInIntent

        // Launch intent and handle result...
        // Extract ID token from GoogleSignInAccount
        */

        return Result.failure(
            NotImplementedError("Real Google Sign-In not yet implemented. Set USE_MOCK=true for testing.")
        )
    }

    /**
     * Sign out from Google
     *
     * Clears Google account from device.
     */
    suspend fun signOut(activity: Activity) {
        if (USE_MOCK) {
            Timber.d("[MOCK] Google Sign-Out")
            delay(200)
        } else {
            // TODO: Implement real sign out
            // GoogleSignIn.getClient(activity, gso).signOut()
        }
    }
}
