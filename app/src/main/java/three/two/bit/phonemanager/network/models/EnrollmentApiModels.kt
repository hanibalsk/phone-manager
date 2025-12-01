package three.two.bit.phonemanager.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import three.two.bit.phonemanager.domain.model.DeviceEnrollmentInfo
import three.two.bit.phonemanager.domain.model.DevicePolicy
import three.two.bit.phonemanager.domain.model.EnrollmentResult
import three.two.bit.phonemanager.domain.model.OrganizationInfo

/**
 * Story E13.10: Android Enrollment Flow API Models
 *
 * Request and response models for device enrollment API endpoints.
 */

// ============================================================================
// Enrollment Request/Response Models (AC E13.10.4)
// ============================================================================

/**
 * Device info sent during enrollment.
 * Part of EnrollDeviceRequest body.
 */
@Serializable
data class DeviceInfoDto(
    val deviceId: String,
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val appVersion: String,
) {
    companion object {
        fun fromDomain(info: DeviceEnrollmentInfo): DeviceInfoDto = DeviceInfoDto(
            deviceId = info.deviceId,
            manufacturer = info.manufacturer,
            model = info.model,
            osVersion = info.osVersion,
            appVersion = info.appVersion,
        )
    }
}

/**
 * Request body for POST /enroll endpoint.
 * AC E13.10.4: Enroll device with token and device info.
 */
@Serializable
data class EnrollDeviceRequest(
    val enrollmentToken: String,
    val deviceInfo: DeviceInfoDto,
)

/**
 * User data in enrollment response.
 */
@Serializable
data class EnrollmentUserDto(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
)

/**
 * Organization data in enrollment response.
 * AC E13.10.6: Display organization name.
 */
@Serializable
data class EnrollmentOrganizationDto(
    val id: String,
    val name: String,
    val contactEmail: String? = null,
    val supportPhone: String? = null,
) {
    fun toDomain(): OrganizationInfo = OrganizationInfo(
        id = id,
        name = name,
        contactEmail = contactEmail,
        supportPhone = supportPhone,
    )
}

/**
 * Policy data in enrollment response.
 * AC E13.10.5: Device policies to apply.
 */
@Serializable
data class EnrollmentPolicyDto(
    val settings: Map<String, String>,
    val locks: List<String>,
    val groupId: String? = null,
) {
    fun toDomain(): DevicePolicy {
        // Convert string values to appropriate types
        val typedSettings: Map<String, Any> = settings.mapValues { (_, value) ->
            when {
                value == "true" -> true
                value == "false" -> false
                value.toIntOrNull() != null -> value.toInt()
                value.toLongOrNull() != null -> value.toLong()
                value.toDoubleOrNull() != null -> value.toDouble()
                else -> value
            }
        }
        return DevicePolicy(
            settings = typedSettings,
            locks = locks,
            groupId = groupId,
        )
    }
}

/**
 * Response from POST /enroll endpoint.
 * AC E13.10.4, E13.10.5: Enrollment response with user, org, and policy.
 */
@Serializable
data class EnrollDeviceResponse(
    val success: Boolean,
    val user: EnrollmentUserDto,
    val organization: EnrollmentOrganizationDto,
    val policy: EnrollmentPolicyDto,
) {
    fun toDomain(): EnrollmentResult = EnrollmentResult(
        success = success,
        userId = user.userId,
        email = user.email,
        accessToken = user.accessToken,
        refreshToken = user.refreshToken,
        organization = organization.toDomain(),
        policy = policy.toDomain(),
    )
}

/**
 * Error response from enrollment endpoints.
 * AC E13.10.7: Enrollment error handling.
 */
@Serializable
data class EnrollmentErrorResponse(
    val error: String,
    val code: String? = null,
    val details: String? = null,
) {
    companion object {
        const val ERROR_INVALID_TOKEN = "invalid_token"
        const val ERROR_EXPIRED_TOKEN = "expired_token"
        const val ERROR_ALREADY_ENROLLED = "already_enrolled"
        const val ERROR_DEVICE_LIMIT = "device_limit_exceeded"
        const val ERROR_POLICY_FAILED = "policy_application_failed"
    }
}

// ============================================================================
// Unenrollment Request/Response Models (AC E13.10.9)
// ============================================================================

/**
 * Response from POST /devices/{deviceId}/unenroll endpoint.
 * AC E13.10.9: Unenroll device.
 */
@Serializable
data class UnenrollDeviceResponse(
    val success: Boolean,
    val message: String? = null,
)

/**
 * Error response when unenrollment is blocked.
 * AC E13.10.9: Unenrollment may be prevented by policy.
 */
@Serializable
data class UnenrollmentBlockedResponse(
    val error: String,
    @SerialName("allowed")
    val isAllowed: Boolean = false,
    val reason: String? = null,
)
