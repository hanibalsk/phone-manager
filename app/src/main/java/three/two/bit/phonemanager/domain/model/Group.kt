package three.two.bit.phonemanager.domain.model

import kotlin.time.Instant

/**
 * Story E11.8: Represents a group for location sharing between users.
 * Story UGM-3.7: Added hasCurrentDevice for device assignment indicator.
 *
 * Groups allow users to share device locations with family, friends, or colleagues.
 *
 * @property id Unique identifier for the group
 * @property name Human-readable name for the group (e.g., "Smith Family")
 * @property description Optional description of the group
 * @property ownerId User ID of the group owner
 * @property memberCount Number of members in the group
 * @property userRole Current user's role in the group
 * @property hasCurrentDevice Whether user's current device is assigned to this group (UGM-3.7 AC 3)
 * @property createdAt When the group was created
 * @property updatedAt When the group was last updated
 */
data class Group(
    val id: String,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val memberCount: Int = 1,
    val userRole: GroupRole = GroupRole.MEMBER,
    val hasCurrentDevice: Boolean = false,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
) {
    /**
     * Check if the current user is the owner of this group.
     *
     * @return true if the user is the owner
     */
    fun isOwner(): Boolean = userRole == GroupRole.OWNER

    /**
     * Check if the current user is an admin of this group.
     *
     * @return true if the user is an admin (but not owner)
     */
    fun isAdmin(): Boolean = userRole == GroupRole.ADMIN

    /**
     * Check if the current user can manage members (invite, remove, change roles).
     * Both owners and admins can manage members.
     *
     * @return true if the user can manage members
     */
    fun canManageMembers(): Boolean = userRole == GroupRole.OWNER || userRole == GroupRole.ADMIN

    /**
     * Check if the current user can delete the group.
     * Only owners can delete groups.
     *
     * @return true if the user can delete the group
     */
    fun canDelete(): Boolean = userRole == GroupRole.OWNER

    /**
     * Check if the current user can leave the group.
     * Owners cannot leave - they must transfer ownership or delete the group.
     *
     * @return true if the user can leave the group
     */
    fun canLeave(): Boolean = userRole != GroupRole.OWNER
}

/**
 * Story E11.8: Represents a user's role within a group.
 *
 * Roles determine permissions for group management actions.
 */
enum class GroupRole {
    /**
     * Full control: can delete group, transfer ownership, manage all members.
     */
    OWNER,

    /**
     * Can invite/remove members, change roles (except owner).
     */
    ADMIN,

    /**
     * Basic access: can view group, view members, leave group.
     */
    MEMBER,

    ;

    companion object {
        /**
         * Parse a role string from the API.
         *
         * @param value The role string (e.g., "owner", "admin", "member")
         * @return The corresponding GroupRole, defaults to MEMBER if unknown
         */
        fun fromString(value: String): GroupRole = when (value.lowercase()) {
            "owner" -> OWNER
            "admin" -> ADMIN
            else -> MEMBER
        }
    }
}

/**
 * Story E11.8: Represents a member's relationship to a group.
 *
 * Contains detailed information about a user's membership in a group.
 *
 * @property userId User's unique identifier
 * @property groupId Group's unique identifier
 * @property email User's email address
 * @property displayName User's display name
 * @property role User's role in the group
 * @property deviceCount Number of devices the user has linked
 * @property joinedAt When the user joined the group
 * @property lastActiveAt When the user was last active in the group
 */
data class GroupMembership(
    val userId: String,
    val groupId: String,
    val email: String,
    val displayName: String,
    val role: GroupRole,
    val deviceCount: Int = 0,
    val joinedAt: Instant? = null,
    val lastActiveAt: Instant? = null,
) {
    /**
     * Check if this member is the owner of the group.
     *
     * @return true if the member is the owner
     */
    fun isOwner(): Boolean = role == GroupRole.OWNER

    /**
     * Check if this member is an admin of the group.
     *
     * @return true if the member is an admin
     */
    fun isAdmin(): Boolean = role == GroupRole.ADMIN

    /**
     * Check if this member can manage other members.
     *
     * @return true if the member can manage members
     */
    fun canManageMembers(): Boolean = role == GroupRole.OWNER || role == GroupRole.ADMIN
}
