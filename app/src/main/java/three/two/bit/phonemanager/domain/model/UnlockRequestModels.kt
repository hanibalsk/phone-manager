package three.two.bit.phonemanager.domain.model

import kotlin.time.Instant

/**
 * Story E12.8: Unlock Request Models
 *
 * Domain models for unlock request functionality.
 *
 * AC E12.8.1: Request Unlock from Locked Setting
 * AC E12.8.3: View My Unlock Requests
 * AC E12.8.6: Admin Response Display
 */

/**
 * Status of an unlock request.
 *
 * AC E12.8.3: Status badge (pending, approved, denied)
 * AC E12.8.7: Filter by status
 */
enum class UnlockRequestStatus {
    /** Request is waiting for admin response */
    PENDING,

    /** Admin approved the unlock request */
    APPROVED,

    /** Admin denied the unlock request */
    DENIED,

    /** User withdrew the request before admin responded */
    WITHDRAWN;

    companion object {
        fun fromString(value: String): UnlockRequestStatus {
            return when (value.lowercase()) {
                "pending" -> PENDING
                "approved" -> APPROVED
                "denied" -> DENIED
                "withdrawn" -> WITHDRAWN
                else -> PENDING
            }
        }
    }
}

/**
 * Represents an unlock request for a locked setting.
 *
 * AC E12.8.1: Request includes setting name, reason
 * AC E12.8.3: Shows status, timestamp, admin response
 * AC E12.8.6: Shows admin decision details
 */
data class UnlockRequest(
    /** Unique identifier for the request */
    val id: String,

    /** Device ID the request is for */
    val deviceId: String,

    /** Setting key being requested to unlock */
    val settingKey: String,

    /** User-provided reason for the unlock request */
    val reason: String,

    /** Current status of the request */
    val status: UnlockRequestStatus,

    /** Email/name of user who made the request */
    val requestedBy: String,

    /** Display name of the requester */
    val requestedByName: String? = null,

    /** When the request was created */
    val createdAt: Instant,

    /** Admin who responded (null if pending) */
    val respondedBy: String? = null,

    /** Display name of admin who responded */
    val respondedByName: String? = null,

    /** Admin's response message (null if pending) */
    val response: String? = null,

    /** When the admin responded (null if pending) */
    val respondedAt: Instant? = null,
) {
    /**
     * Check if request is still pending admin response.
     * AC E12.8.4: Withdraw button for pending requests only
     */
    fun isPending(): Boolean = status == UnlockRequestStatus.PENDING

    /**
     * Check if user can withdraw this request.
     * Only pending requests can be withdrawn.
     */
    fun canWithdraw(): Boolean = isPending()

    /**
     * Check if request was approved.
     * AC E12.8.8: Setting Auto-Unlock on Approval
     */
    fun isApproved(): Boolean = status == UnlockRequestStatus.APPROVED

    /**
     * Check if request was denied.
     */
    fun isDenied(): Boolean = status == UnlockRequestStatus.DENIED

    /**
     * Check if request has been decided (approved or denied).
     * AC E12.8.6: Admin Response Display
     */
    fun isDecided(): Boolean = isApproved() || isDenied()

    /**
     * Get display name for the setting.
     * Uses SettingDefinition if available, otherwise formats key.
     */
    fun getSettingDisplayName(): String {
        return SettingDefinition.forKey(settingKey)?.displayName
            ?: settingKey.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}

/**
 * Summary of unlock request counts by status.
 * Used for badge display.
 * AC E12.8.10: Show badge with pending request count
 */
data class UnlockRequestSummary(
    val pendingCount: Int = 0,
    val approvedCount: Int = 0,
    val deniedCount: Int = 0,
    val withdrawnCount: Int = 0,
) {
    val totalCount: Int
        get() = pendingCount + approvedCount + deniedCount + withdrawnCount
}

/**
 * Filter options for unlock request list.
 * AC E12.8.7: Filter by status
 */
enum class UnlockRequestFilter {
    ALL,
    PENDING,
    APPROVED,
    DENIED,
    WITHDRAWN,
}
