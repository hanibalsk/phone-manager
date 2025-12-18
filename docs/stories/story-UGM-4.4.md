# Story UGM-4.4: Handle Migration Errors and Offline

**Story ID**: UGM-4.4
**Epic**: UGM-4 - Group Migration Wizard
**Priority**: High
**Estimate**: 2 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: Story UGM-4.3

---

## Story

As a user attempting migration,
I want clear error handling and offline support,
So that I can retry if something goes wrong.

**FRs Covered**: FR13, FR39, FR41, FR43

---

## Acceptance Criteria

### AC 1: Network error handling
**Given** migration fails due to network error
**When** showing error
**Then** display "Migration failed. Check your connection and try again."

### AC 2: Retry option
**Given** an error is shown
**When** displaying retry option
**Then** show "Retry" button that restarts migration

### AC 3: Offline detection
**Given** user is offline
**When** attempting migration
**Then** show "Migration requires an internet connection"

### AC 4: No offline queue
**Given** offline state
**When** showing message
**Then** do NOT queue migration (unlike device linking)

### AC 5: Server error handling
**Given** migration fails due to server error
**When** showing error
**Then** display server error message with retry option

### AC 6: Retry functionality
**Given** retry is tapped
**When** attempting again
**Then** re-call migration API with same parameters

---

## Tasks / Subtasks

- [x] Task 1: Add network state monitoring (AC: 3, 4)
  - [x] Check network connectivity before migration
  - [x] Show offline message when no connection
  - [x] Disable migrate button when offline

- [x] Task 2: Add error handling to migration flow (AC: 1, 5)
  - [x] Handle network errors with specific message
  - [x] Handle server errors (4xx, 5xx) with appropriate messages
  - [x] Display errors in MigrationState.Error

- [x] Task 3: Add retry functionality (AC: 2, 6)
  - [x] Add Retry button to error state UI
  - [x] Re-call migration with same parameters on retry
  - [x] Clear error state when retrying

- [x] Task 4: Add error strings
  - [x] Network error message
  - [x] Offline message
  - [x] Server error messages
  - [x] Retry button label

---

## Dev Notes

### Technical Notes
- Monitor network state via `ConnectivityManager`
- Handle HTTP 4xx/5xx responses distinctly
- Migration cannot be queued - requires immediate connection (NFR-R4)

### Implementation Details
Error handling should:
1. Check network before attempting migration
2. Show inline error in GroupMigrationScreen
3. Provide clear retry button
4. Log errors with Timber for debugging
5. Distinguish between network errors and server errors

---

## Dev Agent Record

### Debug Log

No issues encountered during implementation.

### Implementation Plan

1. Add NetworkConnectivityManager utility or use existing
2. Add connectivity check before migration in ViewModel
3. Add MigrationState.Error with message field
4. Add error UI to GroupMigrationScreen
5. Add retry button handling
6. Add error strings to strings.xml

### Completion Notes

Implementation completed successfully:
- Used existing ConnectivityMonitor for network state
- Added isOnline StateFlow to GroupMigrationViewModel
- Added Offline state to MigrationUiState
- Enhanced Error state with isRetryable flag
- Added offline banner and error card with retry to UI
- Migration blocked when offline with appropriate message
- Retryable errors show Retry button

---

## File List

### New Files
None

### Modified Files
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupMigrationViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupMigrationScreen.kt`
- `app/src/main/res/values/strings.xml`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created from UGM epics specification |
| 2025-12-18 | Claude | Implementation completed - all ACs satisfied |
| 2025-12-18 | Claude | Code review: Added MigrationErrorType sealed class for i18n, error messages now use string resources |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-4.3
**Blocking**: None
