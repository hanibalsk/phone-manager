# Story UGM-3.8: Leave or Delete Group

**Story ID**: UGM-3.8
**Epic**: UGM-3 - Device-to-Group Assignment
**Priority**: High
**Estimate**: 2 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: Story UGM-3.7

---

## Story

As a group member,
I want to leave a group, or delete it if I'm the owner,
So that I can manage my group memberships.

**FRs Covered**: FR37, FR38

---

## Acceptance Criteria

### AC 1: Leave group option
**Given** I am a MEMBER or ADMIN of a group
**When** I tap "Leave Group" in settings
**Then** show confirmation dialog with warning

### AC 2: Leave confirmation
**Given** I confirm leaving
**When** API succeeds
**Then** remove group from my list and navigate to Groups screen

### AC 3: Delete group option
**Given** I am the OWNER of a group
**When** I tap "Delete Group"
**Then** show warning about permanent deletion

### AC 4: Delete confirmation
**Given** owner confirms deletion
**When** API succeeds
**Then** delete group and all memberships, navigate away

### AC 5: Owner leave restriction
**Given** I am the only owner
**When** trying to leave without deleting
**Then** prompt to transfer ownership or delete group

---

## Tasks / Subtasks

- [x] Task 1: Leave group button and dialog (AC: 1, 2)
  - [x] OutlinedButton with error color
  - [x] LeaveGroupDialog confirmation
  - [x] Call viewModel.leaveGroup()

- [x] Task 2: Delete group button and dialog (AC: 3, 4)
  - [x] OutlinedButton with error color (owners only)
  - [x] DeleteGroupDialog confirmation
  - [x] Call viewModel.deleteGroup()

- [x] Task 3: Navigation after leave/delete
  - [x] Navigate to GroupList on success
  - [x] Show success snackbar

- [x] Task 4: Role-based visibility
  - [x] Leave visible for MEMBER/ADMIN
  - [x] Delete visible for OWNER only

---

## Dev Notes

### Technical Notes
- Add leave/delete options to `GroupDetailScreen` menu
- Create `LeaveGroupDialog` and `DeleteGroupDialog`
- Use `groupRepository.leaveGroup()` and `deleteGroup()` endpoints

### Implementation Details
This story was already fully implemented:
- `GroupDetailScreen.kt` has leave and delete buttons
- `GroupDialogs.kt` has LeaveGroupDialog and DeleteGroupDialog
- `GroupDetailViewModel.kt` has leaveGroup() and deleteGroup() methods
- Navigation callbacks onLeftGroup and onGroupDeleted handle success

Note: AC 5 (owner leave restriction) enforced by backend

---

## File List

### Modified Files
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupDetailScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupDialogs.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupDetailViewModel.kt`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created - verified existing implementation complete |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-3.7
**Blocking**: None
