# Story UGM-4.4: Handle Migration Errors and Offline

**Story ID**: UGM-4.4
**Epic**: UGM-4 - Group Migration Wizard
**Priority**: High
**Estimate**: 2 story points
**Status**: Ready-for-dev
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

- [ ] Task 1: Add network state monitoring (AC: 3, 4)
  - [ ] Check network connectivity before migration
  - [ ] Show offline message when no connection
  - [ ] Disable migrate button when offline

- [ ] Task 2: Add error handling to migration flow (AC: 1, 5)
  - [ ] Handle network errors with specific message
  - [ ] Handle server errors (4xx, 5xx) with appropriate messages
  - [ ] Display errors in MigrationState.Error

- [ ] Task 3: Add retry functionality (AC: 2, 6)
  - [ ] Add Retry button to error state UI
  - [ ] Re-call migration with same parameters on retry
  - [ ] Clear error state when retrying

- [ ] Task 4: Add error strings
  - [ ] Network error message
  - [ ] Offline message
  - [ ] Server error messages
  - [ ] Retry button label

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

_To be filled during implementation_

### Implementation Plan

1. Add NetworkConnectivityManager utility or use existing
2. Add connectivity check before migration in ViewModel
3. Add MigrationState.Error with message field
4. Add error UI to GroupMigrationScreen
5. Add retry button handling
6. Add error strings to strings.xml

### Completion Notes

_To be filled after implementation_

---

## File List

### New Files
_To be filled during implementation_

### Modified Files
_To be filled during implementation_

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created from UGM epics specification |

---

**Last Updated**: 2025-12-18
**Status**: Ready-for-dev
**Dependencies**: UGM-4.3
**Blocking**: None
