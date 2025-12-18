# Story UGM-2.2: Navigate to Member Device Details

**Story ID**: UGM-2.2
**Epic**: UGM-2 - Enhanced Admin View with Member Devices
**Priority**: High
**Estimate**: 2 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: Story UGM-2.1

---

## Story

As a group owner or admin,
I want to tap on a member to see their device details,
So that I can view their specific devices and their status.

**FRs Covered**: FR22

---

## Acceptance Criteria

### AC 1: Member card tap navigation
**Given** I am viewing the members list
**When** I tap on a member card
**Then** navigate to `UserHomeScreen` for that member

### AC 2: Navigation parameters
**Given** navigation to UserHomeScreen
**When** the screen loads
**Then** pass the member's `userId` and `groupId` as parameters

### AC 3: Device list display
**Given** a member with multiple devices
**When** viewing their details screen
**Then** show list of all their devices in the current group

### AC 4: Back navigation
**Given** I am on UserHomeScreen
**When** I tap back
**Then** return to the members list

---

## Tasks / Subtasks

- [x] Task 1: Add navigation callback to ManageMembersScreen (AC: 1, 2)
  - [x] Add `onNavigateToMemberDetails: (groupId: String, userId: String) -> Unit` parameter
  - [x] Update MemberCard onClick to call navigation callback

- [x] Task 2: Update navigation composable for ManageMembersScreen (AC: 1, 2)
  - [x] Pass navigation callback from PhoneManagerNavHost
  - [x] Navigate to Screen.UserHome.createRoute(groupId, userId)

- [x] Task 3: Verify UserHomeScreen displays member devices (AC: 3, 4)
  - [x] UserHomeScreen already exists and displays device details
  - [x] Verify back navigation works correctly

---

## Dev Notes

### Technical Notes
- Add click handler to `MemberCard` in `ManageMembersScreen`
- Use existing `UserHomeScreen` with navigation parameters
- Update navigation graph to include member detail route

### Implementation Details
Current state:
- `MemberCard` has an `onClick` handler but it just sets `selectedMember` state
- Need to add `onNavigateToMemberDetails` callback to `ManageMembersScreen`
- `UserHomeScreen` already exists at `Screen.UserHome` route with groupId/userId params

---

## Dev Agent Record

### Debug Log

- No issues encountered

### Implementation Plan

1. Add `onNavigateToMemberDetails` parameter to ManageMembersScreen
2. Update MemberCard onClick to navigate instead of just setting state
3. Update PhoneManagerNavHost ManageMembersScreen composable with navigation callback
4. Test navigation flow from members list to UserHomeScreen

### Completion Notes

Implementation completed:

1. Added `onNavigateToMemberDetails: (groupId: String, userId: String) -> Unit` parameter to ManageMembersScreen
2. Updated `onMemberClick` handler in MembersList to call navigation callback with `state.group.id` and `member.userId`
3. Updated PhoneManagerNavHost to pass navigation callback that navigates to `Screen.UserHome.createRoute(navGroupId, userId)`
4. Existing UserHomeScreen already displays all member device details

---

## File List

### Modified Files

- `app/src/main/java/three/two/bit/phonemanager/ui/groups/ManageMembersScreen.kt` - Added navigation callback parameter and updated onClick handler
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/PhoneManagerNavHost.kt` - Added navigation callback to ManageMembersScreen composable

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created from UGM epics specification |
| 2025-12-18 | Claude | Implemented navigation from member cards to UserHomeScreen |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-2.1
**Blocking**: UGM-2.3
