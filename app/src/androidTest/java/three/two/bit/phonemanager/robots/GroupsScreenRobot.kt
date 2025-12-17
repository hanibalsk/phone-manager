package three.two.bit.phonemanager.robots

import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Robot for interacting with Groups screens (list, detail, create, join).
 *
 * Usage:
 * ```kotlin
 * GroupsScreenRobot(composeTestRule)
 *     .verifyGroupsListDisplayed()
 *     .clickCreateGroup()
 *     .enterGroupName("Family")
 *     .clickCreate()
 * ```
 */
class GroupsScreenRobot(composeTestRule: ComposeTestRule) : BaseRobot(composeTestRule) {

    companion object {
        const val TAG_GROUPS_SCREEN = "groups_screen"
        const val TAG_GROUPS_LIST = "groups_list"
        const val TAG_CREATE_GROUP_BUTTON = "create_group_button"
        const val TAG_JOIN_GROUP_BUTTON = "join_group_button"
        const val TAG_GROUP_ITEM_PREFIX = "group_item_"
        const val TAG_GROUP_DETAIL_SCREEN = "group_detail_screen"
        const val TAG_GROUP_NAME_INPUT = "group_name_input"
        const val TAG_GROUP_DESCRIPTION_INPUT = "group_description_input"
        const val TAG_INVITE_CODE_DISPLAY = "invite_code_display"
        const val TAG_INVITE_CODE_INPUT = "invite_code_input"
        const val TAG_QR_CODE_DISPLAY = "qr_code_display"
        const val TAG_SCAN_QR_BUTTON = "scan_qr_button"
        const val TAG_SHARE_INVITE_BUTTON = "share_invite_button"
        const val TAG_CREATE_BUTTON = "create_button"
        const val TAG_JOIN_BUTTON = "join_button"
        const val TAG_LEAVE_GROUP_BUTTON = "leave_group_button"
        const val TAG_MEMBERS_LIST = "members_list"
        const val TAG_MEMBER_ITEM_PREFIX = "member_item_"
        const val TAG_REMOVE_MEMBER_BUTTON_PREFIX = "remove_member_"
        const val TAG_BACK_BUTTON = "back_button"
        const val TAG_EMPTY_STATE = "empty_groups_state"
        const val TAG_ERROR_MESSAGE = "error_message"
        const val TAG_SUCCESS_MESSAGE = "success_message"
    }

    // =============================================================================
    // Groups List Actions
    // =============================================================================

    /**
     * Click to create a new group.
     */
    fun clickCreateGroup(): GroupsScreenRobot {
        clickOnTag(TAG_CREATE_GROUP_BUTTON)
        return this
    }

    /**
     * Click to join a group.
     */
    fun clickJoinGroup(): GroupsScreenRobot {
        clickOnTag(TAG_JOIN_GROUP_BUTTON)
        return this
    }

    /**
     * Click on a specific group in the list.
     */
    fun clickOnGroup(groupName: String): GroupsScreenRobot {
        clickOnTag("${TAG_GROUP_ITEM_PREFIX}$groupName")
        return this
    }

    /**
     * Navigate back.
     */
    fun navigateBack(): HomeScreenRobot {
        clickOnTag(TAG_BACK_BUTTON)
        return HomeScreenRobot(composeTestRule)
    }

    // =============================================================================
    // Create Group Actions
    // =============================================================================

    /**
     * Enter group name.
     */
    fun enterGroupName(name: String): GroupsScreenRobot {
        enterTextByTag(TAG_GROUP_NAME_INPUT, name)
        return this
    }

    /**
     * Enter group description.
     */
    fun enterGroupDescription(description: String): GroupsScreenRobot {
        enterTextByTag(TAG_GROUP_DESCRIPTION_INPUT, description)
        return this
    }

    /**
     * Click create button.
     */
    fun clickCreate(): GroupsScreenRobot {
        clickOnTag(TAG_CREATE_BUTTON)
        return this
    }

    /**
     * Perform complete group creation.
     */
    fun createGroup(name: String, description: String = ""): GroupsScreenRobot {
        enterGroupName(name)
        if (description.isNotEmpty()) {
            enterGroupDescription(description)
        }
        return clickCreate()
    }

    // =============================================================================
    // Join Group Actions
    // =============================================================================

    /**
     * Enter invite code.
     */
    fun enterInviteCode(code: String): GroupsScreenRobot {
        enterTextByTag(TAG_INVITE_CODE_INPUT, code)
        return this
    }

    /**
     * Click join button.
     */
    fun clickJoin(): GroupsScreenRobot {
        clickOnTag(TAG_JOIN_BUTTON)
        return this
    }

    /**
     * Click to scan QR code.
     */
    fun clickScanQR(): GroupsScreenRobot {
        clickOnTag(TAG_SCAN_QR_BUTTON)
        return this
    }

    /**
     * Perform complete group join via code.
     */
    fun joinGroupWithCode(inviteCode: String): GroupsScreenRobot = clickJoinGroup()
        .enterInviteCode(inviteCode)
        .clickJoin()

    // =============================================================================
    // Group Detail Actions
    // =============================================================================

    /**
     * Click to share invite link/code.
     */
    fun clickShareInvite(): GroupsScreenRobot {
        clickOnTag(TAG_SHARE_INVITE_BUTTON)
        return this
    }

    /**
     * Click to leave group.
     */
    fun clickLeaveGroup(): GroupsScreenRobot {
        clickOnTag(TAG_LEAVE_GROUP_BUTTON)
        return this
    }

    /**
     * Click on a member in the group.
     */
    fun clickOnMember(memberName: String): GroupsScreenRobot {
        clickOnTag("${TAG_MEMBER_ITEM_PREFIX}$memberName")
        return this
    }

    /**
     * Click to remove a member from group.
     */
    fun clickRemoveMember(memberName: String): GroupsScreenRobot {
        clickOnTag("${TAG_REMOVE_MEMBER_BUTTON_PREFIX}$memberName")
        return this
    }

    // =============================================================================
    // Assertions
    // =============================================================================

    /**
     * Verify groups list screen is displayed.
     */
    fun verifyGroupsListDisplayed(): GroupsScreenRobot {
        assertTagDisplayed(TAG_GROUPS_SCREEN)
        return this
    }

    /**
     * Verify groups list has items.
     */
    fun verifyGroupsListNotEmpty(): GroupsScreenRobot {
        assertTagDisplayed(TAG_GROUPS_LIST)
        return this
    }

    /**
     * Verify empty state is displayed.
     */
    fun verifyEmptyStateDisplayed(): GroupsScreenRobot {
        assertTagDisplayed(TAG_EMPTY_STATE)
        return this
    }

    /**
     * Verify a specific group is in the list.
     */
    fun verifyGroupInList(groupName: String): GroupsScreenRobot {
        assertTagDisplayed("${TAG_GROUP_ITEM_PREFIX}$groupName")
        return this
    }

    /**
     * Verify group detail screen is displayed.
     */
    fun verifyGroupDetailDisplayed(): GroupsScreenRobot {
        assertTagDisplayed(TAG_GROUP_DETAIL_SCREEN)
        return this
    }

    /**
     * Verify invite code is displayed.
     */
    fun verifyInviteCodeDisplayed(): GroupsScreenRobot {
        assertTagDisplayed(TAG_INVITE_CODE_DISPLAY)
        return this
    }

    /**
     * Verify QR code is displayed.
     */
    fun verifyQRCodeDisplayed(): GroupsScreenRobot {
        assertTagDisplayed(TAG_QR_CODE_DISPLAY)
        return this
    }

    /**
     * Verify members list is displayed.
     */
    fun verifyMembersListDisplayed(): GroupsScreenRobot {
        assertTagDisplayed(TAG_MEMBERS_LIST)
        return this
    }

    /**
     * Verify a member is in the group.
     */
    fun verifyMemberInGroup(memberName: String): GroupsScreenRobot {
        assertTagDisplayed("${TAG_MEMBER_ITEM_PREFIX}$memberName")
        return this
    }

    /**
     * Verify error message is displayed.
     */
    fun verifyErrorDisplayed(message: String? = null): GroupsScreenRobot {
        assertTagDisplayed(TAG_ERROR_MESSAGE)
        if (message != null) {
            assertTextDisplayed(message)
        }
        return this
    }

    /**
     * Verify success message is displayed.
     */
    fun verifySuccessDisplayed(message: String? = null): GroupsScreenRobot {
        assertTagDisplayed(TAG_SUCCESS_MESSAGE)
        if (message != null) {
            assertTextDisplayed(message)
        }
        return this
    }

    /**
     * Wait for group creation success.
     */
    fun waitForGroupCreated(timeoutMs: Long = 10000): GroupsScreenRobot {
        waitForTag(TAG_GROUP_DETAIL_SCREEN, timeoutMs)
        return this
    }

    /**
     * Wait for join success.
     */
    fun waitForJoinSuccess(timeoutMs: Long = 10000): GroupsScreenRobot {
        waitForTag(TAG_GROUP_DETAIL_SCREEN, timeoutMs)
        return this
    }
}
