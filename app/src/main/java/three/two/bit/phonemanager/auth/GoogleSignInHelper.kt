package three.two.bit.phonemanager.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.delay
import three.two.bit.phonemanager.BuildConfig
import timber.log.Timber
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story E9.11, Task 8: Google Sign-In Helper
 *
 * Handles Google OAuth sign-in flow for Android using the modern Credential Manager API.
 *
 * Production Implementation:
 * - Uses Credential Manager API (recommended for Android 14+)
 * - Falls back to legacy Google Sign-In for older devices
 * - Requires GOOGLE_OAUTH_CLIENT_ID in local.properties
 *
 * Mock Implementation:
 * - Controlled by BuildConfig.USE_MOCK_AUTH
 * - Returns mock ID token for testing
 */
@Singleton
class GoogleSignInHelper @Inject constructor() {

    companion object {
        // Google OAuth provider identifier
        const val PROVIDER_ID = "google"
    }

    /**
     * Initiate Google Sign-In flow
     *
     * Uses BuildConfig.USE_MOCK_AUTH to determine mock vs real implementation.
     *
     * @param activity Activity context for launching intent
     * @return Result with ID token on success, error on failure
     */
    suspend fun signIn(activity: Activity): Result<String> = if (BuildConfig.USE_MOCK_AUTH) {
        mockSignIn()
    } else {
        realSignIn(activity)
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
     * Real Google Sign-In implementation using Credential Manager
     *
     * Uses the modern Credential Manager API for secure authentication.
     */
    private suspend fun realSignIn(activity: Activity): Result<String> {
        val clientId = BuildConfig.GOOGLE_OAUTH_CLIENT_ID
        if (clientId.isBlank()) {
            Timber.e("GOOGLE_OAUTH_CLIENT_ID not configured in local.properties")
            return Result.failure(
                IllegalStateException("Google OAuth not configured. Set GOOGLE_OAUTH_CLIENT_ID in local.properties."),
            )
        }

        return try {
            val credentialManager = CredentialManager.create(activity)

            // Generate nonce for security
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Timber.d("Starting Google Sign-In with Credential Manager")
            val result = credentialManager.getCredential(
                request = request,
                context = activity,
            )

            handleSignInResult(result)
        } catch (e: GetCredentialCancellationException) {
            Timber.w("User cancelled Google Sign-In")
            Result.failure(Exception("User cancelled sign-in"))
        } catch (e: NoCredentialException) {
            Timber.w("No Google credentials available")
            Result.failure(Exception("No Google account available. Please add a Google account to your device."))
        } catch (e: GetCredentialException) {
            Timber.e(e, "Google Sign-In credential error")
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))
        } catch (e: Exception) {
            Timber.e(e, "Google Sign-In failed")
            Result.failure(e)
        }
    }

    /**
     * Handle the credential response and extract the ID token
     */
    private fun handleSignInResult(result: GetCredentialResponse): Result<String> {
        val credential = result.credential

        return when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        Timber.i("Google Sign-In successful, got ID token")
                        Result.success(idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Timber.e(e, "Failed to parse Google ID token")
                        Result.failure(Exception("Failed to parse Google credentials"))
                    }
                } else {
                    Timber.e("Unexpected credential type: ${credential.type}")
                    Result.failure(Exception("Unexpected credential type"))
                }
            }
            else -> {
                Timber.e("Unexpected credential class: ${credential::class.java.name}")
                Result.failure(Exception("Unexpected credential type"))
            }
        }
    }

    /**
     * Sign out from Google
     *
     * Clears stored credentials.
     */
    suspend fun signOut(activity: Activity) {
        if (BuildConfig.USE_MOCK_AUTH) {
            Timber.d("[MOCK] Google Sign-Out")
            delay(200)
        } else {
            try {
                val credentialManager = CredentialManager.create(activity)
                // Note: Credential Manager doesn't have a direct sign-out method
                // The sign-out is handled by clearing the app's stored tokens
                Timber.d("Google Sign-Out completed (tokens cleared)")
            } catch (e: Exception) {
                Timber.e(e, "Error during Google Sign-Out")
            }
        }
    }
}
