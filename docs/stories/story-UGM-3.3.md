# Story UGM-3.3: Validate and Preview Group Before Joining

**Story ID**: UGM-3.3
**Epic**: UGM-3 - Device-to-Group Assignment
**Priority**: High
**Estimate**: 2 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: Story UGM-3.2

---

## Story

As a user with an invite code,
I want to preview the group information before joining,
So that I can verify I'm joining the correct group.

**FRs Covered**: FR30, FR32

---

## Acceptance Criteria

### AC 1: Code format validation
**Given** I have an invite code
**When** I enter it in the join screen
**Then** system validates the code format (XXX-XXX-XXX)

### AC 2: Preview API call
**Given** a valid format code
**When** I tap "Preview"
**Then** API validates and returns group info without joining

### AC 3: Display group preview
**Given** validation succeeds
**When** preview is shown
**Then** display group name, member count, and owner name

### AC 4: Error handling
**Given** an invalid or expired code
**When** validation fails
**Then** show clear error message (invalid/expired)

### AC 5: Join button
**Given** preview is successful
**When** displayed
**Then** show "Join Group" button to proceed

---

## Tasks / Subtasks

- [x] Task 1: JoinGroupScreen code input (AC: 1)
  - [x] Monospace styled input field
  - [x] Format validation with InviteCodeUtils

- [x] Task 2: Validate API integration (AC: 2)
  - [x] Call groupRepository.validateInviteCode()
  - [x] Loading state during validation

- [x] Task 3: GroupPreviewCard composable (AC: 3, 5)
  - [x] Display group name and member count
  - [x] Show Join Group button when authenticated

- [x] Task 4: Error handling (AC: 4)
  - [x] Display error messages in supporting text
  - [x] Handle invalid/expired codes

---

## Dev Notes

### Technical Notes
- Use existing `JoinGroupScreen` and `JoinGroupViewModel`
- Call `groupRepository.validateInviteCode()` endpoint
- Display `GroupPreview` composable with group details

### Implementation Details
This story was already fully implemented:
- `JoinGroupScreen.kt` has code input with validation
- `JoinGroupViewModel.kt` validates code via API
- `GroupPreviewCard` displays group info
- Error handling for invalid/expired codes

---

## Dev Agent Record

### Debug Log

No issues - verified existing implementation.

### Completion Notes

Story verified as already complete with existing implementation.

---

## File List

### Modified Files
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/JoinGroupScreen.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/JoinGroupViewModel.kt`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created - verified existing implementation complete |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-3.2
**Blocking**: UGM-3.4
