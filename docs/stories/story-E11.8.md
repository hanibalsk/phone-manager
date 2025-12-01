# Story E11.8: Android Group List and Detail Screens

**Story ID**: E11.8
**Epic**: 11 - Group Management
**Priority**: High
**Estimate**: 8 story points (3-4 days)
**Status**: Planned
**Created**: 2025-12-01
**PRD Reference**: PRD-user-management.md, GROUP_MANAGEMENT_SPEC.md
**Dependencies**: E10.6 (Device Management), Backend E11.1-E11.7

---

## Story

As a user,
I want to view and manage groups in the Android app,
so that I can create groups, view members, and manage member roles.

## Acceptance Criteria

### AC E11.8.1: Group List Screen
**Given** a user is authenticated
**When** they navigate to "Groups" from Settings
**Then** they should see:
  - List of all groups they belong to
  - Group name
  - Member count
  - User's role in group (owner, admin, member)
  - "Create Group" FloatingActionButton
  - Pull-to-refresh functionality
  - Empty state if no groups

### AC E11.8.2: Create Group Dialog
**Given** a user wants to create a new group
**When** they tap "Create Group"
**Then** they should see:
  - Group name input field
  - Optional description field
  - "Create" and "Cancel" buttons
  - Loading indicator during creation
  - Success/error feedback
  - Navigate to group detail on success

### AC E11.8.3: Group Detail Screen
**Given** a user views a group
**When** they tap on a group in the list
**Then** they should see:
  - Group name and description
  - Member list with names and roles
  - Device count per member
  - "Invite Members" button
  - "Manage Members" button (admins/owners only)
  - "Leave Group" button (non-owners)
  - "Delete Group" button (owners only)
  - Group settings section

### AC E11.8.4: Member List Display
**Given** a group has multiple members
**When** viewing the group detail
**Then** each member card should show:
  - Display name
  - Email address
  - Role badge (owner, admin, member)
  - Device count
  - Last active timestamp
  - "View Devices" button

### AC E11.8.5: Role Management (Admins Only)
**Given** user is admin or owner
**When** they tap "Manage Members"
**Then** they should:
  - See list of members with role selectors
  - Change member roles (owner can promote to admin)
  - Remove members from group (confirmation required)
  - Transfer ownership (owners only)
  - See role change confirmation dialogs

### AC E11.8.6: Leave Group
**Given** user is a member (not owner)
**When** they tap "Leave Group"
**Then** they should:
  - See confirmation dialog with warning
  - Call `DELETE /groups/{groupId}/members/{userId}` endpoint
  - Return to group list on success
  - Remove group from local cache

### AC E11.8.7: Delete Group (Owner Only)
**Given** user is group owner
**When** they tap "Delete Group" and confirm
**Then** the system should:
  - Show warning about permanent deletion
  - Require typed confirmation ("DELETE")
  - Call `DELETE /groups/{groupId}` endpoint
  - Return to group list on success
  - Clear all group data locally

### AC E11.8.8: Group Settings
**Given** user is admin or owner
**When** they view group settings
**Then** they should be able to:
  - Edit group name
  - Edit group description
  - Toggle privacy settings
  - View group invite codes
  - Manage invite link expiry

## Tasks / Subtasks

- [ ] Task 1: Create Group Models (AC: E11.8.1)
  - [ ] Create Group data class (id, name, description, ownerId, memberCount)
  - [ ] Create GroupMembership data class (groupId, userId, role, joinedAt)
  - [ ] Create GroupRole enum (OWNER, ADMIN, MEMBER)
  - [ ] Add helper methods: isOwner(), isAdmin(), canManageMembers()

- [ ] Task 2: Create GroupApiService (AC: All)
  - [ ] Add `POST /groups` - Create group
  - [ ] Add `GET /groups` - List user's groups
  - [ ] Add `GET /groups/{id}` - Get group details
  - [ ] Add `PUT /groups/{id}` - Update group
  - [ ] Add `DELETE /groups/{id}` - Delete group
  - [ ] Add `GET /groups/{id}/members` - List members
  - [ ] Add `PUT /groups/{id}/members/{userId}/role` - Update role
  - [ ] Add `DELETE /groups/{id}/members/{userId}` - Remove member
  - [ ] Add `POST /groups/{id}/leave` - Leave group

- [ ] Task 3: Create GroupRepository (AC: All)
  - [ ] Implement createGroup() function
  - [ ] Implement getUserGroups() function
  - [ ] Implement getGroupDetails(groupId) function
  - [ ] Implement updateGroup() function
  - [ ] Implement deleteGroup() function
  - [ ] Implement getGroupMembers(groupId) function
  - [ ] Implement updateMemberRole() function
  - [ ] Implement removeMember() function
  - [ ] Implement leaveGroup() function

- [ ] Task 4: Create GroupListViewModel (AC: E11.8.1, E11.8.2)
  - [ ] Create GroupListViewModel with Hilt
  - [ ] Create GroupListUiState (Loading, Success, Error, Empty)
  - [ ] Inject GroupRepository
  - [ ] Add StateFlow<List<Group>> groups
  - [ ] Add createGroup(name, description) function
  - [ ] Add refreshGroups() function
  - [ ] Handle authentication state changes
  - [ ] Cache groups locally

- [ ] Task 5: Create GroupDetailViewModel (AC: E11.8.3-E11.8.8)
  - [ ] Create GroupDetailViewModel with Hilt
  - [ ] Create GroupDetailUiState sealed class
  - [ ] Add StateFlow<Group> currentGroup
  - [ ] Add StateFlow<List<GroupMembership>> members
  - [ ] Add updateGroup() function
  - [ ] Add deleteGroup() function
  - [ ] Add leaveGroup() function
  - [ ] Add updateMemberRole() function
  - [ ] Add removeMember() function
  - [ ] Add canManageMembers() helper

- [ ] Task 6: Create GroupListScreen UI (AC: E11.8.1)
  - [ ] Create GroupListScreen composable
  - [ ] Display LazyColumn of group cards
  - [ ] Show group name, member count, user role badge
  - [ ] Add "Create Group" FAB
  - [ ] Implement pull-to-refresh
  - [ ] Show empty state with "Create Your First Group"
  - [ ] Display loading indicator
  - [ ] Handle navigation to group detail

- [ ] Task 7: Create CreateGroupDialog (AC: E11.8.2)
  - [ ] Create CreateGroupDialog composable
  - [ ] Add group name TextField (required)
  - [ ] Add description TextField (optional, multiline)
  - [ ] Add character count for name (max 50)
  - [ ] Add "Create" and "Cancel" buttons
  - [ ] Validate name is not empty
  - [ ] Call viewModel.createGroup()
  - [ ] Show success/error feedback

- [ ] Task 8: Create GroupDetailScreen UI (AC: E11.8.3, E11.8.4)
  - [ ] Create GroupDetailScreen composable
  - [ ] Show group header (name, description, member count)
  - [ ] Display member list with role badges
  - [ ] Show device count per member
  - [ ] Add action buttons (Invite, Manage, Leave/Delete)
  - [ ] Implement member card navigation to devices
  - [ ] Show group settings section
  - [ ] Display loading/error states

- [ ] Task 9: Create ManageMembersScreen (AC: E11.8.5)
  - [ ] Create ManageMembersScreen composable
  - [ ] List members with role dropdown (owner can promote)
  - [ ] Add "Remove Member" button per member
  - [ ] Show confirmation dialog for role changes
  - [ ] Show confirmation dialog for member removal
  - [ ] Disable self-removal/demotion
  - [ ] Call viewModel.updateMemberRole() / removeMember()
  - [ ] Show success/error feedback

- [ ] Task 10: Create Confirmation Dialogs (AC: E11.8.6, E11.8.7)
  - [ ] Create LeaveGroupDialog with warning
  - [ ] Create DeleteGroupDialog with typed confirmation
  - [ ] Create RemoveMemberDialog
  - [ ] Create ChangeRoleDialog
  - [ ] Show appropriate warnings for each action
  - [ ] Handle confirmation/cancellation

- [ ] Task 11: Update Navigation (AC: E11.8.1, E11.8.3)
  - [ ] Add Screen.GroupList to sealed class
  - [ ] Add Screen.GroupDetail(groupId) to sealed class
  - [ ] Add Screen.ManageMembers(groupId) to sealed class
  - [ ] Add composable routes in NavHost
  - [ ] Add navigation from Settings → Groups
  - [ ] Handle deep links for group screens

- [ ] Task 12: Update SettingsScreen (AC: E11.8.1)
  - [ ] Add "Groups" navigation item if authenticated
  - [ ] Show group count badge
  - [ ] Add icon for groups section

- [ ] Task 13: Testing (All ACs)
  - [ ] Write unit tests for GroupListViewModel
  - [ ] Write unit tests for GroupDetailViewModel
  - [ ] Write unit tests for GroupRepository
  - [ ] Write UI tests for GroupListScreen
  - [ ] Write UI tests for GroupDetailScreen
  - [ ] Test role management flows
  - [ ] Test leave/delete group flows
  - [ ] Test error handling

## Dev Notes

### Dependencies

**Backend Requirements:**
- Depends on backend Epic 11 stories (E11.1-E11.7)
- Requires: Group CRUD, membership management, role management, permission middleware

**Frontend Requirements:**
- Depends on E10.6 (Device Management) for member device lists
- Requires authenticated user session

### API Contracts

**Create Group:**
```kotlin
POST /groups
Body: {
  name: "Smith Family",
  description: "Family location sharing"
}
Response 201: {
  id: "group-123",
  name: "Smith Family",
  description: "Family location sharing",
  ownerId: "user-456",
  memberCount: 1,
  createdAt: "2025-12-01T10:00:00Z"
}
```

**Get User Groups:**
```kotlin
GET /groups
Response 200: {
  groups: [
    {
      id: "group-123",
      name: "Smith Family",
      memberCount: 4,
      userRole: "owner"
    }
  ]
}
```

**Get Group Members:**
```kotlin
GET /groups/{groupId}/members
Response 200: {
  members: [
    {
      userId: "user-456",
      email: "dad@example.com",
      displayName: "Dad",
      role: "owner",
      deviceCount: 2,
      joinedAt: "2025-12-01T10:00:00Z",
      lastActiveAt: "2025-12-01T12:00:00Z"
    }
  ]
}
```

**Update Member Role:**
```kotlin
PUT /groups/{groupId}/members/{userId}/role
Body: { role: "admin" }
Response 200: { success: true }
Response 403: { error: "Only owners can promote to admin" }
```

### UI/UX Considerations

1. **Role Badges**: Use distinct colors (owner: gold, admin: blue, member: gray)
2. **Permission Gating**: Hide admin actions from members
3. **Confirmation Dialogs**: Always confirm destructive actions
4. **Empty States**: Show helpful guidance for first-time users
5. **Loading States**: Show skeleton loaders during fetch

### Security Considerations

1. **Role Enforcement**: Backend validates all role-based actions
2. **Owner Protection**: Cannot remove/demote self if owner
3. **Deletion Warning**: Emphasize permanence of group deletion
4. **Member Privacy**: Only show member details to group members

### Files to Create/Modify

**New Files:**
- `app/src/main/java/three/two/bit/phonemanager/data/model/Group.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/model/GroupMembership.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/model/GroupRole.kt`
- `app/src/main/java/three/two/bit/phonemanager/network/GroupApiService.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/GroupRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupListScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupDetailScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/ManageMembersScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupListViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupDetailViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/CreateGroupDialog.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/ConfirmationDialogs.kt`

**Modified Files:**
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/NavGraph.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/Screen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/settings/SettingsScreen.kt`

### References

- [Source: PRD-user-management.md - Epic 11: Group Management]
- [Source: GROUP_MANAGEMENT_SPEC.md - Group Features Implementation]
- [Source: UI_SCREENS_SPEC.md - Group UI Designs]

---

## Dev Agent Record

### Debug Log

_To be filled during implementation_

### Completion Notes

_To be filled after implementation_

---

## File List

### Created Files

_To be filled during implementation_

### Modified Files

_To be filled during implementation_

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-01 | Martin (PM) | Story created from Epic 11 specification |

---

**Last Updated**: 2025-12-01
**Status**: Planned
**Dependencies**: E10.6 (Device Management), Backend E11.1-E11.7
**Blocking**: E11.9 (Invite Sharing), E12.6-E12.8 (Settings Control)
