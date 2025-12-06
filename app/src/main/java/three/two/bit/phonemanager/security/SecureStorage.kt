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
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_GROUP_ID = "group_id"

        // E9.11: JWT Token Storage Keys
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY_TIME = "token_expiry_time"

        // E9.11: User Info Storage Keys
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_DISPLAY_NAME = "user_display_name"
        private const val KEY_USER_CREATED_AT = "user_created_at"

        // E13.10: Enrollment Storage Keys
        private const val KEY_ENROLLMENT_STATUS = "enrollment_status"
        private const val KEY_ORG_ID = "org_id"
        private const val KEY_ORG_NAME = "org_name"
        private const val KEY_ORG_CONTACT_EMAIL = "org_contact_email"
        private const val KEY_ORG_SUPPORT_PHONE = "org_support_phone"
        private const val KEY_POLICY_SETTINGS = "policy_settings"
        private const val KEY_POLICY_LOCKS = "policy_locks"
        private const val KEY_POLICY_GROUP_ID = "policy_group_id"
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
     * Get display name for device identification
     */
    fun getDisplayName(): String? = encryptedPrefs.getString(KEY_DISPLAY_NAME, null)

    /**
     * Store display name securely
     */
    fun setDisplayName(displayName: String) {
        encryptedPrefs.edit()
            .putString(KEY_DISPLAY_NAME, displayName)
            .apply()
        Timber.d("Display name stored")
    }

    /**
     * Get group ID for device grouping
     */
    fun getGroupId(): String? = encryptedPrefs.getString(KEY_GROUP_ID, null)

    /**
     * Store group ID securely
     */
    fun setGroupId(groupId: String) {
        encryptedPrefs.edit()
            .putString(KEY_GROUP_ID, groupId)
            .apply()
        Timber.d("Group ID stored")
    }

    /**
     * Check if device is registered (has displayName and groupId)
     */
    fun isRegistered(): Boolean = getDisplayName() != null && getGroupId() != null

    // ========================================================================
    // E9.11: JWT Token Management
    // ========================================================================

    /**
     * Save access token securely
     */
    fun saveAccessToken(token: String) {
        encryptedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
        Timber.d("Access token stored securely")
    }

    /**
     * Save refresh token securely
     */
    fun saveRefreshToken(token: String) {
        encryptedPrefs.edit()
            .putString(KEY_REFRESH_TOKEN, token)
            .apply()
        Timber.d("Refresh token stored securely")
    }

    /**
     * Get access token
     */
    fun getAccessToken(): String? = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)

    /**
     * Get refresh token
     */
    fun getRefreshToken(): String? = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)

    /**
     * Save token expiry time (Unix timestamp in milliseconds)
     */
    fun saveTokenExpiryTime(expiryTimeMs: Long) {
        encryptedPrefs.edit()
            .putLong(KEY_TOKEN_EXPIRY_TIME, expiryTimeMs)
            .apply()
        Timber.d("Token expiry time stored: $expiryTimeMs")
    }

    /**
     * Get token expiry time (Unix timestamp in milliseconds)
     */
    fun getTokenExpiryTime(): Long? {
        val expiryTime = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY_TIME, -1L)
        return if (expiryTime == -1L) null else expiryTime
    }

    /**
     * Check if access token is expired
     * Returns true if token is expired or expiry time is not set
     */
    fun isTokenExpired(): Boolean {
        val expiryTime = getTokenExpiryTime() ?: return true
        val currentTime = System.currentTimeMillis()
        // Add 5-minute buffer before actual expiry to refresh proactively
        val bufferMs = 5 * 60 * 1000L
        return currentTime >= (expiryTime - bufferMs)
    }

    /**
     * Clear all JWT tokens
     */
    fun clearTokens() {
        encryptedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY_TIME)
            .apply()
        Timber.d("JWT tokens cleared")
    }

    /**
     * Check if user is authenticated (has valid tokens)
     */
    fun isAuthenticated(): Boolean {
        val hasTokens = getAccessToken() != null && getRefreshToken() != null
        return hasTokens && !isTokenExpired()
    }

    // ========================================================================
    // E9.11: User Info Storage
    // ========================================================================

    /**
     * Save user info securely
     */
    fun saveUserInfo(userId: String, email: String, displayName: String, createdAt: String) {
        encryptedPrefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_DISPLAY_NAME, displayName)
            .putString(KEY_USER_CREATED_AT, createdAt)
            .apply()
        Timber.d("User info stored securely")
    }

    /**
     * Get stored user ID
     */
    fun getUserId(): String? = encryptedPrefs.getString(KEY_USER_ID, null)

    /**
     * Get stored user email
     */
    fun getUserEmail(): String? = encryptedPrefs.getString(KEY_USER_EMAIL, null)

    /**
     * Get stored user display name
     */
    fun getUserDisplayName(): String? = encryptedPrefs.getString(KEY_USER_DISPLAY_NAME, null)

    /**
     * Get stored user created at timestamp
     */
    fun getUserCreatedAt(): String? = encryptedPrefs.getString(KEY_USER_CREATED_AT, null)

    /**
     * Check if user info is stored
     */
    fun hasUserInfo(): Boolean = getUserId() != null && getUserEmail() != null

    /**
     * Clear user info
     */
    fun clearUserInfo() {
        encryptedPrefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_DISPLAY_NAME)
            .remove(KEY_USER_CREATED_AT)
            .apply()
        Timber.d("User info cleared")
    }

    /**
     * Clear all secure storage (for logout/reset)
     */
    fun clear() {
        encryptedPrefs.edit().clear().apply()
        Timber.d("Secure storage cleared")
    }

    // ========================================================================
    // E13.10: Enrollment Management
    // ========================================================================

    /**
     * Get enrollment status
     */
    fun getEnrollmentStatus(): three.two.bit.phonemanager.domain.model.EnrollmentStatus {
        val statusName = encryptedPrefs.getString(KEY_ENROLLMENT_STATUS, null)
        return if (statusName != null) {
            try {
                three.two.bit.phonemanager.domain.model.EnrollmentStatus.valueOf(statusName)
            } catch (_: Exception) {
                three.two.bit.phonemanager.domain.model.EnrollmentStatus.NOT_ENROLLED
            }
        } else {
            three.two.bit.phonemanager.domain.model.EnrollmentStatus.NOT_ENROLLED
        }
    }

    /**
     * Save enrollment status
     */
    fun saveEnrollmentStatus(status: three.two.bit.phonemanager.domain.model.EnrollmentStatus) {
        encryptedPrefs.edit()
            .putString(KEY_ENROLLMENT_STATUS, status.name)
            .apply()
        Timber.d("Enrollment status saved: ${status.name}")
    }

    /**
     * Get organization info
     */
    fun getOrganizationInfo(): three.two.bit.phonemanager.domain.model.OrganizationInfo? {
        val orgId = encryptedPrefs.getString(KEY_ORG_ID, null) ?: return null
        val orgName = encryptedPrefs.getString(KEY_ORG_NAME, null) ?: return null
        return three.two.bit.phonemanager.domain.model.OrganizationInfo(
            id = orgId,
            name = orgName,
            contactEmail = encryptedPrefs.getString(KEY_ORG_CONTACT_EMAIL, null),
            supportPhone = encryptedPrefs.getString(KEY_ORG_SUPPORT_PHONE, null),
        )
    }

    /**
     * Save organization info
     */
    fun saveOrganizationInfo(org: three.two.bit.phonemanager.domain.model.OrganizationInfo) {
        encryptedPrefs.edit()
            .putString(KEY_ORG_ID, org.id)
            .putString(KEY_ORG_NAME, org.name)
            .putString(KEY_ORG_CONTACT_EMAIL, org.contactEmail)
            .putString(KEY_ORG_SUPPORT_PHONE, org.supportPhone)
            .apply()
        saveEnrollmentStatus(three.two.bit.phonemanager.domain.model.EnrollmentStatus.ENROLLED)
        Timber.d("Organization info saved: ${org.name}")
    }

    /**
     * Get device policy
     */
    fun getDevicePolicy(): three.two.bit.phonemanager.domain.model.DevicePolicy? {
        val settingsJson = encryptedPrefs.getString(KEY_POLICY_SETTINGS, null) ?: return null
        val locksJson = encryptedPrefs.getString(KEY_POLICY_LOCKS, null) ?: "[]"
        val groupId = encryptedPrefs.getString(KEY_POLICY_GROUP_ID, null)

        return try {
            val settings = parseSettingsJson(settingsJson)
            val locks = parseLocksJson(locksJson)
            three.two.bit.phonemanager.domain.model.DevicePolicy(
                settings = settings,
                locks = locks,
                groupId = groupId,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse device policy")
            null
        }
    }

    /**
     * Save device policy
     */
    fun saveDevicePolicy(policy: three.two.bit.phonemanager.domain.model.DevicePolicy) {
        val settingsJson = serializeSettingsToJson(policy.settings)
        val locksJson = serializeLocksToJson(policy.locks)

        encryptedPrefs.edit()
            .putString(KEY_POLICY_SETTINGS, settingsJson)
            .putString(KEY_POLICY_LOCKS, locksJson)
            .putString(KEY_POLICY_GROUP_ID, policy.groupId)
            .apply()
        Timber.d("Device policy saved: ${policy.settings.size} settings, ${policy.locks.size} locks")
    }

    /**
     * Clear enrollment data
     */
    fun clearEnrollmentData() {
        encryptedPrefs.edit()
            .remove(KEY_ENROLLMENT_STATUS)
            .remove(KEY_ORG_ID)
            .remove(KEY_ORG_NAME)
            .remove(KEY_ORG_CONTACT_EMAIL)
            .remove(KEY_ORG_SUPPORT_PHONE)
            .remove(KEY_POLICY_SETTINGS)
            .remove(KEY_POLICY_LOCKS)
            .remove(KEY_POLICY_GROUP_ID)
            .apply()
        Timber.d("Enrollment data cleared")
    }

    /**
     * Check if device is enrolled
     */
    fun isEnrolled(): Boolean {
        return getEnrollmentStatus() == three.two.bit.phonemanager.domain.model.EnrollmentStatus.ENROLLED
    }

    /**
     * Serialize settings map to JSON string
     */
    private fun serializeSettingsToJson(settings: Map<String, Any>): String {
        return buildString {
            append("{")
            settings.entries.forEachIndexed { index, (key, value) ->
                if (index > 0) append(",")
                append("\"$key\":")
                when (value) {
                    is String -> append("\"$value\"")
                    is Boolean -> append(value)
                    is Number -> append(value)
                    else -> append("\"$value\"")
                }
            }
            append("}")
        }
    }

    /**
     * Serialize locks list to JSON string
     */
    private fun serializeLocksToJson(locks: List<String>): String {
        return "[${locks.joinToString(",") { "\"$it\"" }}]"
    }

    /**
     * Parse settings JSON string to map
     */
    private fun parseSettingsJson(json: String): Map<String, Any> {
        if (json == "{}") return emptyMap()

        val result = mutableMapOf<String, Any>()
        val content = json.trim().removePrefix("{").removeSuffix("}")
        if (content.isBlank()) return result

        // Simple JSON parser for basic types
        val regex = "\"([^\"]+)\":(?:\"([^\"]*)\"|([^,}]+))".toRegex()
        regex.findAll(content).forEach { match ->
            val key = match.groupValues[1]
            val stringValue = match.groupValues[2]
            val otherValue = match.groupValues[3].trim()

            val value: Any = when {
                stringValue.isNotEmpty() -> stringValue
                otherValue == "true" -> true
                otherValue == "false" -> false
                otherValue.toIntOrNull() != null -> otherValue.toInt()
                otherValue.toLongOrNull() != null -> otherValue.toLong()
                otherValue.toDoubleOrNull() != null -> otherValue.toDouble()
                else -> otherValue
            }
            result[key] = value
        }
        return result
    }

    /**
     * Parse locks JSON string to list
     */
    private fun parseLocksJson(json: String): List<String> {
        if (json == "[]") return emptyList()

        val content = json.trim().removePrefix("[").removeSuffix("]")
        if (content.isBlank()) return emptyList()

        return content.split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }
}
