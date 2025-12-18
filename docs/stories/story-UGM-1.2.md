# Story UGM-1.2: Handle Device Already Linked to Different User

**Story ID**: UGM-1.2
**Epic**: UGM-1 - Device-User Linking on Authentication
**Priority**: High
**Estimate**: 2 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: UGM-1.1

---

## Story

As a user logging in on a device already linked to another account,
I want to see a clear message about the conflict,
So that I understand why I cannot use this device with my account.

**FRs Covered**: FR2, FR40

---

## Acceptance Criteria

### AC 1: Detect device conflict
**Given** a device already linked to User A
**When** User B logs in and auto-link is attempted
**Then** the API returns an error indicating device is already linked

### AC 2: Display conflict message
**Given** a device conflict is detected
**When** displaying the error
**Then** show message: "This device is linked to another account"

### AC 3: Allow login continuation
**Given** a conflict error is shown
**When** user acknowledges
**Then** user can still proceed with login but without device linking

### AC 4: Provide support options
**Given** linking fails due to conflict
**When** error is displayed
**Then** provide option to "Contact Support" or "Log out"

---

## Tasks / Subtasks

- [x] Task 1: Handle HTTP 409 Conflict response from link endpoint (AC: 1)
  - [x] Already implemented in AuthViewModel.autoLinkCurrentDevice()
  - [x] Returns DeviceLinkState.AlreadyLinked on conflict

- [x] Task 2: Add conflict state to AuthUiState (AC: 2)
  - [x] Create DeviceLinkConflictDialog composable
  - [x] Show dialog when DeviceLinkState.AlreadyLinked is detected

- [x] Task 3: Create DeviceLinkConflictDialog composable (AC: 3, 4)
  - [x] Display conflict message
  - [x] Add "Continue" button to dismiss and proceed
  - [x] Add "Contact Support" button to open support
  - [x] Add "Log out" button option

- [x] Task 4: Integrate dialog into LoginScreen and RegisterScreen (AC: 2, 3)
  - [x] Observe deviceLinkState from AuthViewModel
  - [x] Show dialog when AlreadyLinked state is detected

---

## Dev Notes

### Technical Notes
- Handle HTTP 409 Conflict response from link endpoint (already done)
- Add conflict state to `AuthUiState`
- Create `DeviceLinkConflictDialog` composable

### Implementation Details
The backend conflict detection is already implemented:
- `autoLinkCurrentDevice()` in AuthViewModel checks for 409 Conflict
- Returns `DeviceLinkState.AlreadyLinked` when device is linked to another user

What's missing:
- UI dialog to inform the user about the conflict
- Options to continue, contact support, or logout

---

## Dev Agent Record

### Debug Log

- No issues encountered during implementation

### Implementation Plan

1. Create DeviceLinkConflictDialog composable in ui/auth/components/
2. Add dialog integration to LoginScreen and RegisterScreen
3. Observe deviceLinkState and show dialog when AlreadyLinked
4. Handle user actions: continue, support, logout

### Completion Notes

Successfully implemented the device link conflict dialog:

1. Created `DeviceLinkConflictDialog.kt` composable with:
   - Warning icon and title
   - Conflict message and explanation text
   - "Continue Without Linking" button (AC 3)
   - "Contact Support" button opens email (AC 4)
   - "Log Out Instead" button (AC 4)

2. Integrated dialog into `LoginScreen.kt`:
   - Observes `deviceLinkState` from AuthViewModel
   - Shows dialog when `DeviceLinkState.AlreadyLinked` is detected
   - Handles all three user actions

3. Integrated dialog into `RegisterScreen.kt`:
   - Same behavior as LoginScreen

4. Added `clearDeviceLinkState()` method to AuthViewModel

5. Added string resources in `strings.xml`

---

## File List

### Created Files

- `app/src/main/java/three/two/bit/phonemanager/ui/auth/components/DeviceLinkConflictDialog.kt`

### Modified Files

- `app/src/main/java/three/two/bit/phonemanager/ui/auth/AuthViewModel.kt` (added clearDeviceLinkState())
- `app/src/main/java/three/two/bit/phonemanager/ui/auth/LoginScreen.kt` (dialog integration)
- `app/src/main/java/three/two/bit/phonemanager/ui/auth/RegisterScreen.kt` (dialog integration)
- `app/src/main/res/values/strings.xml` (added dialog strings)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created from UGM epics specification |
| 2025-12-18 | Claude | Implemented dialog and integration - story complete |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-1.1
**Blocking**: None
