package three.two.bit.phonemanager.domain.model

/**
 * Story UGM-4.1: Information about a registration group that can be migrated.
 *
 * A registration group is an anonymous/unauthenticated group that was created
 * during initial device setup. When a user logs in, they can migrate this group
 * to an authenticated group to gain full management features.
 *
 * @property groupId The ID of the registration group
 * @property groupName The name of the registration group (or a fallback like the group ID)
 * @property deviceCount Number of devices in the registration group
 */
data class RegistrationGroupInfo(
    val groupId: String,
    val groupName: String,
    val deviceCount: Int,
)
