package three.two.bit.phonemanager.domain.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Story E13.10: Android Enrollment Flow Domain Models
 *
 * Models for device enrollment, policies, and organization information.
 */

/**
 * Enrollment token for device enrollment.
 * AC E13.10.2: Enrollment code input (16-20 alphanumeric chars)
 *
 * @property token The enrollment token/code
 * @property isValid Whether the token format is valid
 */
data class EnrollmentToken(
    val token: String,
) {
    /**
     * Check if the token format is valid (16-20 alphanumeric characters).
     */
    val isValid: Boolean
        get() = token.length in 16..20 && token.all { it.isLetterOrDigit() }

    companion object {
        /** Minimum length for enrollment token */
        const val MIN_LENGTH = 16

        /** Maximum length for enrollment token */
        const val MAX_LENGTH = 20

        /** Empty token */
        val EMPTY = EnrollmentToken("")

        /**
         * Parse token from QR code data.
         * Expected format: phonemanager://enroll/{token}
         */
        fun fromQRCode(data: String): EnrollmentToken? {
            val prefix = "phonemanager://enroll/"
            return if (data.startsWith(prefix)) {
                val token = data.removePrefix(prefix).trim()
                if (token.isNotEmpty()) EnrollmentToken(token) else null
            } else {
                null
            }
        }
    }
}

/**
 * Device policy received after enrollment.
 * AC E13.10.5: Apply device policies
 *
 * @property settings Map of setting keys to their values
 * @property locks List of setting keys that are locked
 * @property groupId The group ID the device belongs to
 */
data class DevicePolicy(
    val settings: Map<String, Any>,
    val locks: List<String>,
    val groupId: String?,
) {
    /**
     * Check if a specific setting is defined in the policy.
     */
    fun hasPolicy(key: String): Boolean = settings.containsKey(key)

    /**
     * Get the value for a specific policy setting.
     *
     * @param key The setting key
     * @param default The default value if not found
     * @return The setting value or default
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getPolicyValue(key: String, default: T): T {
        return (settings[key] as? T) ?: default
    }

    /**
     * Get the value for a specific policy setting, or null if not found.
     *
     * @param key The setting key
     * @return The setting value or null
     */
    fun getPolicyValue(key: String): Any? {
        return settings[key]
    }

    /**
     * Check if a specific setting is locked by policy.
     */
    fun isLocked(key: String): Boolean = key in locks

    /**
     * Get the count of locked settings.
     */
    fun lockedCount(): Int = locks.size

    /**
     * Get all setting keys.
     */
    fun allSettingKeys(): Set<String> = settings.keys

    companion object {
        /** Empty policy for initial state */
        val EMPTY = DevicePolicy(
            settings = emptyMap(),
            locks = emptyList(),
            groupId = null,
        )

        /** Common policy setting keys */
        object Keys {
            const val TRACKING_ENABLED = "tracking_enabled"
            const val TRACKING_INTERVAL_SECONDS = "tracking_interval_seconds"
            const val SECRET_MODE_ENABLED = "secret_mode_enabled"
            const val BATTERY_OPTIMIZATION = "battery_optimization"
            const val AUTO_START = "auto_start"
            const val GEOFENCE_ENABLED = "geofence_enabled"
        }
    }
}

/**
 * Organization information for enrolled devices.
 * AC E13.10.6: Display organization name, AC E13.10.8: Show IT contact
 *
 * @property id The organization ID
 * @property name The organization display name
 * @property contactEmail IT contact email
 * @property supportPhone IT support phone number
 */
@Serializable
data class OrganizationInfo(
    val id: String,
    val name: String,
    val contactEmail: String?,
    val supportPhone: String?,
) {
    /**
     * Check if IT contact information is available.
     */
    fun hasContactInfo(): Boolean = !contactEmail.isNullOrBlank() || !supportPhone.isNullOrBlank()

    /**
     * Get formatted contact display string.
     */
    fun getContactDisplay(): String {
        return buildString {
            contactEmail?.let { append("Email: $it") }
            if (!contactEmail.isNullOrBlank() && !supportPhone.isNullOrBlank()) {
                append(" | ")
            }
            supportPhone?.let { append("Phone: $it") }
        }
    }

    companion object {
        /** Empty organization info */
        val EMPTY = OrganizationInfo(
            id = "",
            name = "",
            contactEmail = null,
            supportPhone = null,
        )
    }
}

/**
 * Enrollment result containing all data after successful enrollment.
 * AC E13.10.4, E13.10.5: Enrollment response with user, org, and policy
 *
 * @property success Whether enrollment was successful
 * @property userId The created/linked user ID
 * @property email The user's email
 * @property accessToken The JWT access token
 * @property refreshToken The refresh token
 * @property organization The organization info
 * @property policy The device policy to apply
 */
data class EnrollmentResult(
    val success: Boolean,
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val organization: OrganizationInfo,
    val policy: DevicePolicy,
) {
    companion object {
        /** Create a failure result */
        fun failure(): EnrollmentResult = EnrollmentResult(
            success = false,
            userId = "",
            email = "",
            accessToken = "",
            refreshToken = "",
            organization = OrganizationInfo.EMPTY,
            policy = DevicePolicy.EMPTY,
        )
    }
}

/**
 * Enrollment status for the device.
 * AC E13.10.8: Managed device indicator
 */
enum class EnrollmentStatus {
    /** Device is not enrolled */
    NOT_ENROLLED,

    /** Device is currently enrolling */
    ENROLLING,

    /** Device is enrolled and managed */
    ENROLLED,

    /** Device is being unenrolled */
    UNENROLLING,
}

/**
 * Device information sent during enrollment.
 * AC E13.10.4: Call POST /enroll with device info
 */
data class DeviceEnrollmentInfo(
    val deviceId: String,
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val appVersion: String,
)
