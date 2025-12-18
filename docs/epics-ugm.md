---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - docs/PRD.md
  - docs/solution-architecture.md
project_name: 'Phone Manager - Unified Group Management'
date: '2025-12-18'
status: complete
---

# Phone Manager - Unified Group Management - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for the Unified Group Management feature, decomposing the requirements from the PRD and Architecture into implementable stories.

## Requirements Inventory

### Functional Requirements

#### Device-User Linking
- **FR1:** Authenticated user's device can be automatically linked to their account upon successful login
- **FR2:** System can detect if a device is already linked to a different user account
- **FR3:** User can view which account their device is linked to
- **FR4:** System can update device ownership in backend when linking occurs

#### Group Migration
- **FR5:** System can detect if authenticated user's device is in a registration group (not authenticated group)
- **FR6:** User can view migration prompt showing registration group name and device count
- **FR7:** User can choose to migrate their registration group to an authenticated group
- **FR8:** User can specify a name for the new authenticated group during migration
- **FR9:** System can migrate all devices from registration group to new authenticated group atomically
- **FR10:** System can assign OWNER role to migrating user in new authenticated group
- **FR11:** System can delete the registration group after successful migration
- **FR12:** User can dismiss migration prompt and continue without migrating
- **FR13:** User can retry migration if it fails due to network error

#### Device-to-Group Assignment
- **FR14:** User can view device assignment prompt after joining an authenticated group
- **FR15:** User can choose to add their device to a newly joined group
- **FR16:** User can decline to add their device to a newly joined group
- **FR17:** System can add device to authenticated group for location sharing
- **FR18:** Device can belong to multiple authenticated groups simultaneously
- **FR19:** User can view which groups their device belongs to

#### Group Membership Management
- **FR20:** Owner/Admin can view list of all members in their authenticated group
- **FR21:** Owner/Admin can view device count badge for each group member
- **FR22:** Owner/Admin can tap on a member to view their device details
- **FR23:** Owner/Admin can view member's linked devices with current location
- **FR24:** Owner/Admin can view member's device tracking status
- **FR25:** Owner/Admin can view member's device last update timestamp

#### Group Creation & Joining
- **FR26:** Authenticated user can create a new authenticated group
- **FR27:** User can specify group name when creating a group
- **FR28:** Creator becomes OWNER of newly created group
- **FR29:** User can generate invite code for their group (Owner/Admin)
- **FR30:** User can enter invite code to preview group before joining
- **FR31:** User can join authenticated group using valid invite code
- **FR32:** System can validate invite code before allowing join
- **FR33:** System requires authentication before joining authenticated group

#### Multi-Group Support
- **FR34:** User can be a member of multiple authenticated groups
- **FR35:** User can view all groups they belong to
- **FR36:** User can switch between groups in the app
- **FR37:** User can leave a group they are a member of (non-owners)
- **FR38:** Owner can delete a group they own

#### Error Handling & Recovery
- **FR39:** System can display clear error message when migration fails
- **FR40:** System can display clear error message when device linking fails
- **FR41:** User can retry failed operations with one tap
- **FR42:** System can queue device linking for retry when offline
- **FR43:** System can show "requires connection" message for migration when offline

### Non-Functional Requirements

#### Performance
- **NFR-P1:** Auto-link device on login < 500ms
- **NFR-P2:** Migration wizard load time < 500ms
- **NFR-P3:** Group migration completion < 2s perceived
- **NFR-P4:** Member list load time < 1s
- **NFR-P5:** Device assignment operation < 1s

#### Security
- **NFR-S1:** Device linking requires valid JWT
- **NFR-S2:** Group migration requires ownership proof
- **NFR-S3:** Member data visibility respects roles
- **NFR-S4:** Invite codes expire (24-48 hours)
- **NFR-S5:** Secure storage of group membership

#### Reliability
- **NFR-R1:** Migration atomicity - 100%
- **NFR-R2:** Auto-link retry on failure - 3 attempts
- **NFR-R3:** Data preservation during migration - 100%
- **NFR-R4:** Graceful degradation offline
- **NFR-R5:** Error recovery - user-initiated retry

#### Integration
- **NFR-I1:** Backend API version compatibility
- **NFR-I2:** Offline queue persistence
- **NFR-I3:** Network state monitoring
- **NFR-I4:** Backend timeout handling (30s)

#### Accessibility
- **NFR-A1:** Screen reader support
- **NFR-A2:** Touch target size (48dp minimum)
- **NFR-A3:** Color contrast (WCAG AA)
- **NFR-A4:** Focus navigation

### Additional Requirements

From Architecture document:
- Kotlin with Jetpack Compose UI framework
- MVVM + Clean Architecture pattern
- Hilt for dependency injection (migration from Koin in progress)
- Room database for offline queue persistence
- Ktor Client for network requests
- Compose Navigation for screen routing
- WorkManager for background retry operations
- Android KeyStore for secure credential storage

Project Context:
- Brownfield project - extending existing codebase
- No starter template required
- Existing screens: GroupsScreen, GroupDetailScreen, ManageMembersScreen, JoinGroupScreen
- New screens needed: GroupMigrationScreen

### FR Coverage Map

| FR | Epic | Description |
|----|------|-------------|
| FR1 | UGM-1 | Auto-link device on login |
| FR2 | UGM-1 | Detect existing device link |
| FR3 | UGM-1 | View linked account |
| FR4 | UGM-1 | Update device ownership in backend |
| FR5 | UGM-4 | Detect registration group |
| FR6 | UGM-4 | View migration prompt |
| FR7 | UGM-4 | Choose to migrate |
| FR8 | UGM-4 | Specify group name |
| FR9 | UGM-4 | Atomic migration |
| FR10 | UGM-4 | Assign OWNER role |
| FR11 | UGM-4 | Delete registration group |
| FR12 | UGM-4 | Dismiss migration prompt |
| FR13 | UGM-4 | Retry migration |
| FR14 | UGM-3 | Device assignment prompt |
| FR15 | UGM-3 | Add device to group |
| FR16 | UGM-3 | Decline device addition |
| FR17 | UGM-3 | Add device for location sharing |
| FR18 | UGM-3 | Multi-group device membership |
| FR19 | UGM-3 | View device's groups |
| FR20 | UGM-2 | View member list |
| FR21 | UGM-2 | Device count badge |
| FR22 | UGM-2 | Tap member for details |
| FR23 | UGM-2 | View member devices with location |
| FR24 | UGM-2 | View device tracking status |
| FR25 | UGM-2 | View device last update |
| FR26 | UGM-3 | Create authenticated group |
| FR27 | UGM-3 | Specify group name |
| FR28 | UGM-3 | Creator becomes OWNER |
| FR29 | UGM-3 | Generate invite code |
| FR30 | UGM-3 | Preview group before joining |
| FR31 | UGM-3 | Join with invite code |
| FR32 | UGM-3 | Validate invite code |
| FR33 | UGM-3 | Require auth for join |
| FR34 | UGM-3 | Multi-group membership |
| FR35 | UGM-3 | View all groups |
| FR36 | UGM-3 | Switch between groups |
| FR37 | UGM-3 | Leave group |
| FR38 | UGM-3 | Delete owned group |
| FR39 | UGM-4 | Migration error message |
| FR40 | UGM-1 | Device linking error message |
| FR41 | UGM-4 | Retry failed operations |
| FR42 | UGM-1 | Queue device linking offline |
| FR43 | UGM-4 | Requires connection message |

## Epic List

### UGM-1: Device-User Linking on Authentication
When a user logs in, their device is automatically linked to their account, establishing ownership for all future group operations.

**FRs Covered:** FR1, FR2, FR3, FR4, FR40, FR42

**User Value:** Device ownership is established immediately upon login, enabling all subsequent group management features.

**Implementation Notes:** Foundation epic - all other features depend on device-user binding. Extends existing AuthViewModel with auto-link logic.

---

### UGM-2: Enhanced Admin View with Member Devices
Group owners and admins can view all members in their group with device counts, tap on a member to see their devices, and view device details (location, tracking status, last seen).

**FRs Covered:** FR20, FR21, FR22, FR23, FR24, FR25

**User Value:** Admins can effectively manage their group by seeing member devices and their status.

**Implementation Notes:** Quick win using existing data. Enhances ManageMembersScreen with device count badges and adds navigation to UserHomeScreen.

---

### UGM-3: Device-to-Group Assignment
When a user joins a group via invite code, they are prompted to add their device to the group. Devices can belong to multiple groups, and users can see which groups their device belongs to.

**FRs Covered:** FR14, FR15, FR16, FR17, FR18, FR19, FR26, FR27, FR28, FR29, FR30, FR31, FR32, FR33, FR34, FR35, FR36, FR37, FR38

**User Value:** Users can join multiple groups and manage which groups their device belongs to, enabling flexible family structures.

**Implementation Notes:** Extends JoinGroupScreen with device assignment prompt. Adds multi-group support throughout the app.

---

### UGM-4: Group Migration Wizard
Users who started with anonymous registration groups can seamlessly migrate to authenticated groups, preserving all devices and enabling full group management features.

**FRs Covered:** FR5, FR6, FR7, FR8, FR9, FR10, FR11, FR12, FR13, FR39, FR41, FR43

**User Value:** Anonymous users can upgrade to full authenticated group features without losing their existing devices or group members.

**Implementation Notes:** Most complex feature requiring new GroupMigrationScreen and new backend API. Migration is optional - anonymous users continue to work.

---

## User Stories

### UGM-1: Device-User Linking on Authentication

#### Story UGM-1.1: Auto-Link Device on Successful Login

**As** an authenticated user
**I want** my device to be automatically linked to my account when I log in
**So that** I can manage my device and participate in groups without additional steps

**FRs Covered:** FR1, FR4

**Acceptance Criteria:**
1. **Given** a user successfully authenticates (email/password or OAuth)
   **When** login completes
   **Then** the system calls `linkDevice(userId, deviceId)` API endpoint
2. **Given** the device is not yet linked to any user
   **When** auto-link is triggered
   **Then** device ownership is set to the current user in the backend
3. **Given** auto-link succeeds
   **When** the operation completes
   **Then** `SecureStorage` is updated with the linked user ID
4. **Given** the link API returns success
   **When** updating local state
   **Then** the UI reflects device ownership within 500ms (NFR-P1)

**Technical Notes:**
- Extend `AuthViewModel.autoLinkCurrentDevice()` method
- Use existing `deviceApiService.linkDevice()` endpoint
- Store link status in `SecureStorage` for offline reference

**Dependencies:** None (foundation story)

---

#### Story UGM-1.2: Handle Device Already Linked to Different User

**As** a user logging in on a device already linked to another account
**I want** to see a clear message about the conflict
**So that** I understand why I cannot use this device with my account

**FRs Covered:** FR2, FR40

**Acceptance Criteria:**
1. **Given** a device already linked to User A
   **When** User B logs in and auto-link is attempted
   **Then** the API returns an error indicating device is already linked
2. **Given** a device conflict is detected
   **When** displaying the error
   **Then** show message: "This device is linked to another account"
3. **Given** a conflict error is shown
   **When** user acknowledges
   **Then** user can still proceed with login but without device linking
4. **Given** linking fails due to conflict
   **When** error is displayed
   **Then** provide option to "Contact Support" or "Log out"

**Technical Notes:**
- Handle HTTP 409 Conflict response from link endpoint
- Add conflict state to `AuthUiState`
- Create `DeviceLinkConflictDialog` composable

**Dependencies:** Story UGM-1.1

---

#### Story UGM-1.3: View Device Ownership Status

**As** an authenticated user
**I want** to see which account my device is linked to
**So that** I can verify my device is correctly associated with my account

**FRs Covered:** FR3

**Acceptance Criteria:**
1. **Given** a user is logged in with a linked device
   **When** they navigate to Settings/Account screen
   **Then** they see "Device linked to: [email@example.com]"
2. **Given** a user is logged in with an unlinked device
   **When** they navigate to Settings/Account screen
   **Then** they see "Device not linked" with option to link
3. **Given** the device ownership display
   **When** rendered
   **Then** include device name and link timestamp
4. **Given** a linked device
   **When** user taps on the ownership info
   **Then** show detailed device info (ID, linked date, device name)

**Technical Notes:**
- Add device info section to existing `SettingsScreen`
- Query `SecureStorage` for link status
- Add `DeviceInfoCard` composable

**Dependencies:** Story UGM-1.1

---

#### Story UGM-1.4: Offline Queue for Device Linking

**As** a user logging in while offline
**I want** my device linking to be queued and retried when online
**So that** I don't lose the linking operation due to network issues

**FRs Covered:** FR42

**Acceptance Criteria:**
1. **Given** user logs in successfully but network is unavailable
   **When** auto-link fails due to network
   **Then** queue the link operation for later retry
2. **Given** a queued link operation exists
   **When** network becomes available
   **Then** automatically retry the link operation
3. **Given** retry is in progress
   **When** operation succeeds
   **Then** update local state and clear the queue
4. **Given** retry fails 3 times (NFR-R2)
   **When** all retries exhausted
   **Then** show notification to user with manual retry option

**Technical Notes:**
- Use Room database for offline queue persistence
- Use WorkManager for background retry with exponential backoff
- Monitor network state via `ConnectivityManager`

**Dependencies:** Story UGM-1.1

---

### UGM-2: Enhanced Admin View with Member Devices

#### Story UGM-2.1: Display Device Count Badge on Member Cards

**As** a group owner or admin
**I want** to see the device count for each member in the member list
**So that** I can quickly see how many devices each member has in the group

**FRs Covered:** FR20, FR21

**Acceptance Criteria:**
1. **Given** I am an owner/admin viewing the members list
   **When** the member list loads
   **Then** each member card shows a device count badge (e.g., "2 devices")
2. **Given** a member has no devices in the group
   **When** viewing their card
   **Then** show "No devices" instead of "0 devices"
3. **Given** device count badge is displayed
   **When** rendered
   **Then** use appropriate styling (pill badge, secondary color)
4. **Given** member list loads
   **When** API returns data
   **Then** load completes within 1 second (NFR-P4)

**Technical Notes:**
- Enhance existing `ManageMembersScreen` member cards
- Backend already returns `device_count` in members response
- Use `GroupMembership.deviceCount` field

**Dependencies:** None (uses existing data)

---

#### Story UGM-2.2: Navigate to Member Device Details

**As** a group owner or admin
**I want** to tap on a member to see their device details
**So that** I can view their specific devices and their status

**FRs Covered:** FR22

**Acceptance Criteria:**
1. **Given** I am viewing the members list
   **When** I tap on a member card
   **Then** navigate to `UserHomeScreen` for that member
2. **Given** navigation to UserHomeScreen
   **When** the screen loads
   **Then** pass the member's `userId` and `groupId` as parameters
3. **Given** a member with multiple devices
   **When** viewing their details screen
   **Then** show list of all their devices in the current group
4. **Given** I am on UserHomeScreen
   **When** I tap back
   **Then** return to the members list

**Technical Notes:**
- Add click handler to `MemberCard` in `ManageMembersScreen`
- Use existing `UserHomeScreen` with navigation parameters
- Update navigation graph to include member detail route

**Dependencies:** Story UGM-2.1

---

#### Story UGM-2.3: View Member Device Details

**As** a group owner or admin viewing a member's devices
**I want** to see each device's location, tracking status, and last update
**So that** I can monitor the status of devices in my group

**FRs Covered:** FR23, FR24, FR25

**Acceptance Criteria:**
1. **Given** I am viewing a member's devices on UserHomeScreen
   **When** the screen loads
   **Then** show each device with its current location (if available)
2. **Given** a device in the list
   **When** viewing device details
   **Then** display tracking status (enabled/disabled)
3. **Given** a device with location data
   **When** viewing device details
   **Then** show last update timestamp in human-readable format
4. **Given** a device without location data
   **When** viewing device details
   **Then** show "Location unavailable" with appropriate styling
5. **Given** device details display
   **When** data visibility is checked
   **Then** only show data allowed by user's role (NFR-S3)

**Technical Notes:**
- Use existing `UserHomeViewModel` and `UserHomeScreen`
- Filter devices by `ownerId` matching the member's user ID
- Format timestamps using relative time (e.g., "2 minutes ago")

**Dependencies:** Story UGM-2.2

---

### UGM-3: Device-to-Group Assignment

#### Story UGM-3.1: Create New Authenticated Group

**As** an authenticated user
**I want** to create a new authenticated group with a custom name
**So that** I can start managing my family or team's devices

**FRs Covered:** FR26, FR27, FR28

**Acceptance Criteria:**
1. **Given** I am authenticated
   **When** I tap "Create Group" on the Groups screen
   **Then** show a dialog/screen to enter group name
2. **Given** the create group form
   **When** I enter a valid group name and confirm
   **Then** API creates the group with me as OWNER
3. **Given** group creation succeeds
   **When** response is received
   **Then** navigate to the new group's detail screen
4. **Given** group creation
   **When** I become OWNER
   **Then** I have full management permissions immediately
5. **Given** group name input
   **When** validating
   **Then** require 3-50 characters, alphanumeric and spaces

**Technical Notes:**
- Add "Create Group" FAB to `GroupsScreen`
- Create `CreateGroupDialog` composable
- Use existing `groupRepository.createGroup()` endpoint

**Dependencies:** None (uses existing auth)

---

#### Story UGM-3.2: Generate Invite Code for Group

**As** a group owner or admin
**I want** to generate an invite code for my group
**So that** I can share it with others to join

**FRs Covered:** FR29

**Acceptance Criteria:**
1. **Given** I am an owner/admin of a group
   **When** I tap "Invite Members" on the group detail screen
   **Then** generate and display an invite code (XXX-XXX-XXX format)
2. **Given** an invite code is displayed
   **When** I tap "Copy"
   **Then** code is copied to clipboard with confirmation toast
3. **Given** an invite code is displayed
   **When** I tap "Share"
   **Then** open system share sheet with the code
4. **Given** an invite code
   **When** checking validity
   **Then** code expires within 24-48 hours (NFR-S4)

**Technical Notes:**
- Use existing `groupRepository.generateInviteCode()` endpoint
- Create `InviteCodeScreen` or bottom sheet
- Add share intent integration

**Dependencies:** Story UGM-3.1

---

#### Story UGM-3.3: Validate and Preview Group Before Joining

**As** a user with an invite code
**I want** to preview the group information before joining
**So that** I can verify I'm joining the correct group

**FRs Covered:** FR30, FR32

**Acceptance Criteria:**
1. **Given** I have an invite code
   **When** I enter it in the join screen
   **Then** system validates the code format (XXX-XXX-XXX)
2. **Given** a valid format code
   **When** I tap "Preview"
   **Then** API validates and returns group info without joining
3. **Given** validation succeeds
   **When** preview is shown
   **Then** display group name, member count, and owner name
4. **Given** an invalid or expired code
   **When** validation fails
   **Then** show clear error message (invalid/expired)
5. **Given** preview is successful
   **When** displayed
   **Then** show "Join Group" button to proceed

**Technical Notes:**
- Use existing `JoinGroupScreen` and `JoinGroupViewModel`
- Call `groupRepository.validateInviteCode()` endpoint
- Display `GroupPreview` composable with group details

**Dependencies:** Story UGM-3.2

---

#### Story UGM-3.4: Join Authenticated Group

**As** a user who previewed a group
**I want** to join the group
**So that** I can see and interact with group members

**FRs Covered:** FR31, FR33

**Acceptance Criteria:**
1. **Given** I have previewed a valid group
   **When** I tap "Join Group"
   **Then** check if I am authenticated
2. **Given** I am not authenticated
   **When** attempting to join
   **Then** redirect to login with return-to-join flow
3. **Given** I am authenticated and tap join
   **When** API call succeeds
   **Then** I am added to the group as MEMBER role
4. **Given** join succeeds
   **When** confirming success
   **Then** show success message and navigate to group detail
5. **Given** I just joined a group
   **When** on the success screen
   **Then** proceed to device assignment prompt (Story UGM-3.5)

**Technical Notes:**
- Extend `JoinGroupViewModel` join flow
- Handle auth redirect with deep link return
- Call `groupRepository.joinGroup()` endpoint

**Dependencies:** Story UGM-3.3

---

#### Story UGM-3.5: Device Assignment Prompt After Joining

**As** a user who just joined a group
**I want** to be prompted to add my device to the group
**So that** my location can be shared with group members

**FRs Covered:** FR14, FR15, FR16, FR17

**Acceptance Criteria:**
1. **Given** I just joined a group
   **When** join completes successfully
   **Then** show device assignment prompt dialog
2. **Given** the assignment prompt
   **When** I tap "Add My Device"
   **Then** call API to add device to group for location sharing
3. **Given** the assignment prompt
   **When** I tap "Not Now"
   **Then** dismiss prompt and proceed without adding device
4. **Given** device is added to group
   **When** operation completes
   **Then** show success confirmation within 1 second (NFR-P5)
5. **Given** device addition fails
   **When** error occurs
   **Then** show error message with retry option

**Technical Notes:**
- Create `DeviceAssignmentDialog` composable
- Call new `groupRepository.addDeviceToGroup()` endpoint
- Store assignment decision in local preferences

**Dependencies:** Story UGM-3.4

---

#### Story UGM-3.6: Multi-Group Device Membership

**As** a user with a device in multiple groups
**I want** my device to be visible in all assigned groups
**So that** different groups can track my location

**FRs Covered:** FR18

**Acceptance Criteria:**
1. **Given** I am a member of Group A and Group B
   **When** I add my device to both groups
   **Then** device is visible to members of both groups
2. **Given** my device is in multiple groups
   **When** location updates
   **Then** both groups receive the updated location
3. **Given** my device is in Group A but not Group B
   **When** Group B members view my profile
   **Then** they see "No devices shared" for me
4. **Given** device multi-group support
   **When** adding to a new group
   **Then** previous group memberships are preserved

**Technical Notes:**
- Backend handles device-group mapping (many-to-many)
- No UI changes needed beyond assignment prompt
- Verify location updates broadcast to all assigned groups

**Dependencies:** Story UGM-3.5

---

#### Story UGM-3.7: View and Switch Between Groups

**As** a user in multiple groups
**I want** to view all my groups and switch between them
**So that** I can manage different family/team contexts

**FRs Covered:** FR19, FR34, FR35, FR36

**Acceptance Criteria:**
1. **Given** I am a member of multiple groups
   **When** I open the Groups screen
   **Then** see a list of all groups I belong to
2. **Given** the groups list
   **When** viewing each group card
   **Then** show group name, my role, and member count
3. **Given** my device is assigned to certain groups
   **When** viewing groups list
   **Then** indicate which groups have my device (icon/badge)
4. **Given** the groups list
   **When** I tap on a group
   **Then** navigate to that group's detail screen
5. **Given** group list display
   **When** loading
   **Then** show current group (if any) at the top

**Technical Notes:**
- Use existing `GroupsScreen` with enhanced card design
- Add device assignment indicator to group cards
- Query `groupRepository.getMyGroups()` endpoint

**Dependencies:** Story UGM-3.6

---

#### Story UGM-3.8: Leave or Delete Group

**As** a group member
**I want** to leave a group, or delete it if I'm the owner
**So that** I can manage my group memberships

**FRs Covered:** FR37, FR38

**Acceptance Criteria:**
1. **Given** I am a MEMBER or ADMIN of a group
   **When** I tap "Leave Group" in settings
   **Then** show confirmation dialog with warning
2. **Given** I confirm leaving
   **When** API succeeds
   **Then** remove group from my list and navigate to Groups screen
3. **Given** I am the OWNER of a group
   **When** I tap "Delete Group"
   **Then** show warning about permanent deletion
4. **Given** owner confirms deletion
   **When** API succeeds
   **Then** delete group and all memberships, navigate away
5. **Given** I am the only owner
   **When** trying to leave without deleting
   **Then** prompt to transfer ownership or delete group

**Technical Notes:**
- Add leave/delete options to `GroupDetailScreen` menu
- Create `LeaveGroupDialog` and `DeleteGroupDialog`
- Use `groupRepository.leaveGroup()` and `deleteGroup()` endpoints

**Dependencies:** Story UGM-3.7

---

### UGM-4: Group Migration Wizard

#### Story UGM-4.1: Detect Registration Group After Login

**As** a user who registered anonymously and now logged in
**I want** the system to detect my registration group
**So that** I can be prompted to migrate to an authenticated group

**FRs Covered:** FR5

**Acceptance Criteria:**
1. **Given** I have a device in a registration group
   **When** I successfully log in
   **Then** system checks if device is in a registration group
2. **Given** the registration group check
   **When** API returns registration group info
   **Then** store group ID and device count locally
3. **Given** device is in registration group
   **When** check completes
   **Then** trigger migration prompt flow (Story UGM-4.2)
4. **Given** device is not in any registration group
   **When** check completes
   **Then** proceed to normal authenticated flow
5. **Given** check is performed
   **When** response received
   **Then** complete within 500ms (NFR-P2)

**Technical Notes:**
- Call `GET /api/v1/devices/me/registration-group` after login
- Add `checkRegistrationGroup()` to `AuthViewModel`
- Store result in `MigrationState` data class

**Dependencies:** UGM-1 (device-user linking must work first)

---

#### Story UGM-4.2: Display Migration Prompt

**As** a user with a registration group
**I want** to see a clear migration prompt with group details
**So that** I understand what migration means and can decide

**FRs Covered:** FR6, FR7, FR12

**Acceptance Criteria:**
1. **Given** registration group is detected
   **When** showing migration prompt
   **Then** display group name and device count
2. **Given** the migration prompt
   **When** showing options
   **Then** show "Migrate Group" and "Not Now" buttons
3. **Given** the prompt shows
   **When** explaining migration
   **Then** include brief explanation of benefits (roles, invites, management)
4. **Given** user taps "Not Now"
   **When** dismissing prompt
   **Then** remember decision and don't show again this session
5. **Given** user taps "Migrate Group"
   **When** proceeding
   **Then** navigate to GroupMigrationScreen (Story UGM-4.3)

**Technical Notes:**
- Create `MigrationPromptDialog` composable
- Store dismissal state in preferences
- Navigate to new `GroupMigrationScreen`

**Dependencies:** Story UGM-4.1

---

#### Story UGM-4.3: Execute Group Migration

**As** a user who chose to migrate
**I want** to complete the migration process with a group name
**So that** my registration group becomes a full authenticated group

**FRs Covered:** FR8, FR9, FR10, FR11

**Acceptance Criteria:**
1. **Given** I am on the migration screen
   **When** it loads
   **Then** pre-fill group name with registration group ID
2. **Given** the migration form
   **When** I enter/modify the group name
   **Then** validate name (3-50 chars, alphanumeric and spaces)
3. **Given** valid name entered
   **When** I tap "Migrate"
   **Then** call migration API with name and registration group ID
4. **Given** migration API succeeds
   **When** response received
   **Then** I am set as OWNER of the new authenticated group
5. **Given** migration completes
   **When** all devices moved
   **Then** registration group is deleted automatically
6. **Given** migration in progress
   **When** showing status
   **Then** show progress indicator, complete < 2s perceived (NFR-P3)

**Technical Notes:**
- Create `GroupMigrationScreen` composable and ViewModel
- Call `POST /api/v1/groups/migrate` endpoint
- Migration is atomic - all or nothing (NFR-R1)

**Dependencies:** Story UGM-4.2

---

#### Story UGM-4.4: Handle Migration Errors and Offline

**As** a user attempting migration
**I want** clear error handling and offline support
**So that** I can retry if something goes wrong

**FRs Covered:** FR13, FR39, FR41, FR43

**Acceptance Criteria:**
1. **Given** migration fails due to network error
   **When** showing error
   **Then** display "Migration failed. Check your connection and try again."
2. **Given** an error is shown
   **When** displaying retry option
   **Then** show "Retry" button that restarts migration
3. **Given** user is offline
   **When** attempting migration
   **Then** show "Migration requires an internet connection"
4. **Given** offline state
   **When** showing message
   **Then** do NOT queue migration (unlike device linking)
5. **Given** migration fails due to server error
   **When** showing error
   **Then** display server error message with retry option
6. **Given** retry is tapped
   **When** attempting again
   **Then** re-call migration API with same parameters

**Technical Notes:**
- Monitor network state via `ConnectivityManager`
- Handle HTTP 4xx/5xx responses distinctly
- Migration cannot be queued - requires immediate connection (NFR-R4)

**Dependencies:** Story UGM-4.3

---

## Implementation Order

**Recommended Implementation Sequence:**

1. **UGM-1: Device-User Linking** - Foundation for all other features
2. **UGM-4: Group Migration** - Enables existing users to transition
3. **UGM-3: Device-to-Group Assignment** - Core group management features
4. **UGM-2: Enhanced Admin View** - Quick win using existing data

**Rationale:**
- UGM-1 must be first as all features depend on device-user binding
- UGM-4 should be early to enable existing anonymous users to upgrade
- UGM-3 provides the main group management functionality
- UGM-2 can be done anytime as it only reads existing data
