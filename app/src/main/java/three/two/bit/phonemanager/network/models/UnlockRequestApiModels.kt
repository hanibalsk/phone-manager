package three.two.bit.phonemanager.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Story E12.8: Unlock Request API Models
 *
 * Network models for unlock request API communication.
 *
 * AC E12.8.2: Submit Unlock Request
 * AC E12.8.3: View My Unlock Requests
 * AC E12.8.4: Withdraw Unlock Request
 */

/**
 * Request body for creating an unlock request.
 *
 * POST /devices/{deviceId}/settings/unlock-requests
 */
@Serializable
data class CreateUnlockRequestBody(
    @SerialName("setting_key") val settingKey: String,
    @SerialName("reason") val reason: String,
)

/**
 * Response from creating an unlock request.
 */
@Serializable
data class CreateUnlockRequestResponse(
    @SerialName("id") val id: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("setting_key") val settingKey: String,
    @SerialName("reason") val reason: String,
    @SerialName("status") val status: String,
    @SerialName("requested_by") val requestedBy: String,
    @SerialName("requested_by_name") val requestedByName: String? = null,
    @SerialName("created_at") val createdAt: String,
)

/**
 * Single unlock request in API response.
 */
@Serializable
data class UnlockRequestResponse(
    @SerialName("id") val id: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("setting_key") val settingKey: String,
    @SerialName("reason") val reason: String,
    @SerialName("status") val status: String,
    @SerialName("requested_by") val requestedBy: String,
    @SerialName("requested_by_name") val requestedByName: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("responded_by") val respondedBy: String? = null,
    @SerialName("responded_by_name") val respondedByName: String? = null,
    @SerialName("response") val response: String? = null,
    @SerialName("responded_at") val respondedAt: String? = null,
)

/**
 * Response from listing unlock requests.
 *
 * GET /devices/{deviceId}/settings/unlock-requests
 */
@Serializable
data class UnlockRequestListResponse(
    @SerialName("requests") val requests: List<UnlockRequestResponse>,
    @SerialName("total") val total: Int? = null,
)

/**
 * Response from withdrawing an unlock request.
 *
 * DELETE /unlock-requests/{requestId}
 */
@Serializable
data class WithdrawUnlockRequestResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String? = null,
    @SerialName("error") val error: String? = null,
)

/**
 * Error response for unlock request operations.
 */
@Serializable
data class UnlockRequestErrorResponse(
    @SerialName("error") val error: String,
    @SerialName("code") val code: String? = null,
    @SerialName("details") val details: String? = null,
)
