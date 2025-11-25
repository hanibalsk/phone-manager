package three.two.bit.phonemanager.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story 0.2.2: SecureStorage - Encrypted storage for sensitive data
 *
 * Uses EncryptedSharedPreferences to securely store:
 * - API keys
 * - Device ID
 * - API base URLs
 */
@Singleton
class SecureStorage @Inject constructor(@ApplicationContext private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                "phone_manager_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            // SECURITY WARNING: Falling back to unencrypted storage
            // This should only happen on devices with broken KeyStore or during testing
            Timber.e(
                e,
                "SECURITY WARNING: Failed to create EncryptedSharedPreferences, " +
                    "falling back to regular SharedPreferences. Sensitive data will NOT be encrypted!",
            )
            // Fallback to regular SharedPreferences if encryption fails
            // Use a different name to avoid mixing encrypted and unencrypted data
            context.getSharedPreferences("phone_manager_secure_fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_DEVICE_ID = "device_id"
    }

    /**
     * Get API key for backend authentication
     */
    fun getApiKey(): String? = encryptedPrefs.getString(KEY_API_KEY, null)

    /**
     * Store API key securely
     */
    fun setApiKey(apiKey: String) {
        encryptedPrefs.edit()
            .putString(KEY_API_KEY, apiKey)
            .apply()
        Timber.d("API key stored securely")
    }

    /**
     * Get API base URL
     */
    fun getApiBaseUrl(): String? = encryptedPrefs.getString(KEY_API_BASE_URL, null)

    /**
     * Store API base URL
     */
    fun setApiBaseUrl(baseUrl: String) {
        encryptedPrefs.edit()
            .putString(KEY_API_BASE_URL, baseUrl)
            .apply()
        Timber.d("API base URL stored: $baseUrl")
    }

    /**
     * Get unique device ID for location tracking
     *
     * If device ID doesn't exist, generates a new UUID
     */
    fun getDeviceId(): String {
        var deviceId = encryptedPrefs.getString(KEY_DEVICE_ID, null)

        if (deviceId == null) {
            deviceId = java.util.UUID.randomUUID().toString()
            encryptedPrefs.edit()
                .putString(KEY_DEVICE_ID, deviceId)
                .apply()
            Timber.i("Generated new device ID: $deviceId")
        }

        return deviceId
    }

    /**
     * Clear all secure storage (for logout/reset)
     */
    fun clear() {
        encryptedPrefs.edit().clear().apply()
        Timber.d("Secure storage cleared")
    }
}
