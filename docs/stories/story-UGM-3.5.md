# Story UGM-3.5: Device Assignment Prompt After Joining

**Story ID**: UGM-3.5
**Epic**: UGM-3 - Device-to-Group Assignment
**Priority**: High
**Estimate**: 2 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: Story UGM-3.4

---

## Story

As a user who just joined a group,
I want to be prompted to add my device to the group,
So that my location can be shared with group members.

**FRs Covered**: FR14, FR15, FR16, FR17

---

## Acceptance Criteria

### AC 1: Show prompt after join
**Given** I just joined a group
**When** join completes successfully
**Then** show device assignment prompt dialog

### AC 2: Add device option
**Given** the assignment prompt
**When** I tap "Add My Device"
**Then** call API to add device to group for location sharing

### AC 3: Skip option
**Given** the assignment prompt
**When** I tap "Not Now"
**Then** dismiss prompt and proceed without adding device

### AC 4: Success confirmation
**Given** device is added to group
**When** operation completes
**Then** show success confirmation within 1 second (NFR-P5)

### AC 5: Error handling
**Given** device addition fails
**When** error occurs
**Then** show error message with retry option

---

## Tasks / Subtasks

- [x] Task 1: Create DeviceAssignmentDialog composable (AC: 1)
  - [x] Dialog with icon and explanation text
  - [x] "Add My Device" primary button
  - [x] "Not Now" secondary button

- [x] Task 2: Show dialog after join success (AC: 1)
  - [x] Update JoinGroupViewModel to track showDevicePrompt state
  - [x] Show dialog in JoinGroupScreen after successful join

- [x] Task 3: Add device API integration (AC: 2, 4, 5)
  - [x] Call groupRepository.addCurrentDeviceToGroup()
  - [x] Show loading state during operation
  - [x] Handle success/error responses

- [x] Task 4: Skip functionality (AC: 3)
  - [x] Dismiss dialog and navigate to group detail

---

## Dev Notes

### Technical Notes
- Created `DeviceAssignmentDialog` composable
- Uses existing `groupRepository.addCurrentDeviceToGroup()` endpoint
- Integrated into JoinGroupScreen flow

### Implementation Details
Implementation approach:
1. Created DeviceAssignmentDialog.kt with PhonelinkRing icon
2. Added DeviceAssignmentState sealed interface to JoinGroupViewModel
3. Added state flows: showDeviceAssignmentPrompt, deviceAssignmentState
4. Added methods: addDeviceToGroup(), skipDeviceAssignment(), getJoinedGroupName()
5. Updated JoinGroupScreen with LaunchedEffect handlers for state changes
6. Added dialog display when showDeviceAssignmentPrompt is true

---

## Dev Agent Record

### Debug Log

No issues encountered during implementation.

### Implementation Plan

1. Create DeviceAssignmentDialog composable with icon, text, and two buttons
2. Add showDeviceAssignmentPrompt state to JoinGroupViewModel
3. Update JoinGroupScreen to show dialog after successful join
4. Call addCurrentDeviceToGroup when user confirms
5. Navigate to group detail on completion (success or skip)

### Completion Notes

Implementation completed successfully:
- DeviceAssignmentDialog shows after successful group join
- User can add device or skip
- Success/error snackbars provide feedback
- Navigation proceeds after dialog action

---

## File List

### New Files
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/DeviceAssignmentDialog.kt`

### Modified Files
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/JoinGroupScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/JoinGroupViewModel.kt`
- `app/src/main/res/values/strings.xml`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created from UGM epics specification |
| 2025-12-18 | Claude | Implementation completed - all ACs satisfied |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-3.4
**Blocking**: UGM-3.6
