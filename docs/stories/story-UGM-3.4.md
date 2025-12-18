# Story UGM-3.4: Join Authenticated Group

**Story ID**: UGM-3.4
**Epic**: UGM-3 - Device-to-Group Assignment
**Priority**: High
**Estimate**: 2 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: Story UGM-3.3

---

## Story

As a user who previewed a group,
I want to join the group,
So that I can see and interact with group members.

**FRs Covered**: FR31, FR33

---

## Acceptance Criteria

### AC 1: Authentication check
**Given** I have previewed a valid group
**When** I tap "Join Group"
**Then** check if I am authenticated

### AC 2: Redirect to login
**Given** I am not authenticated
**When** attempting to join
**Then** redirect to login with return-to-join flow

### AC 3: Join as member
**Given** I am authenticated and tap join
**When** API call succeeds
**Then** I am added to the group as MEMBER role

### AC 4: Success navigation
**Given** join succeeds
**When** confirming success
**Then** show success message and navigate to group detail

### AC 5: Device assignment prompt
**Given** I just joined a group
**When** on the success screen
**Then** proceed to device assignment prompt (Story UGM-3.5)

---

## Tasks / Subtasks

- [x] Task 1: Authentication check (AC: 1, 2)
  - [x] Check isAuthenticated state in ViewModel
  - [x] Handle auth redirect with JoinResult.AuthenticationRequired

- [x] Task 2: Join API integration (AC: 3)
  - [x] Call groupRepository.joinGroup()
  - [x] Handle MEMBER role assignment

- [x] Task 3: Success handling (AC: 4)
  - [x] Show success snackbar
  - [x] Navigate to group detail via onJoinSuccess callback

- [x] Task 4: Join confirmation dialog
  - [x] JoinConfirmationDialog shown before joining
  - [x] Displays member count and confirmation hint

---

## Dev Notes

### Technical Notes
- Extend `JoinGroupViewModel` join flow
- Handle auth redirect with deep link return
- Call `groupRepository.joinGroup()` endpoint

### Implementation Details
This story was already fully implemented:
- `JoinGroupScreen.kt` has Join button in GroupPreviewCard
- `JoinGroupViewModel.kt` handles joinGroup() with auth check
- JoinConfirmationDialog confirms before joining
- Success navigates to group detail

Note: AC 5 (device assignment prompt) is addressed in UGM-3.5

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
**Dependencies**: UGM-3.3
**Blocking**: UGM-3.5
