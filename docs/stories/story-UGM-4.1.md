# Story UGM-4.1: Detect Registration Group After Login

**Story ID**: UGM-4.1
**Epic**: UGM-4 - Group Migration Wizard
**Priority**: High
**Estimate**: 2 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: UGM-1 (Device-User Linking)

---

## Story

As a user who registered anonymously and now logged in,
I want the system to detect my registration group,
So that I can be prompted to migrate to an authenticated group.

**FRs Covered**: FR5

---

## Acceptance Criteria

### AC 1: Check for registration group after login
**Given** I have a device in a registration group
**When** I successfully log in
**Then** system checks if device is in a registration group

### AC 2: Store registration group info
**Given** the registration group check
**When** API returns registration group info
**Then** store group ID and device count locally

### AC 3: Trigger migration prompt
**Given** device is in registration group
**When** check completes
**Then** trigger migration prompt flow (Story UGM-4.2)

### AC 4: No registration group
**Given** device is not in any registration group
**When** check completes
**Then** proceed to normal authenticated flow

### AC 5: Performance
**Given** check is performed
**When** response received
**Then** complete within 500ms (NFR-P2)

---

## Tasks / Subtasks

- [x] Task 1: Add registration group detection API endpoint (AC: 1, 5)
  - [x] Add `checkRegistrationGroup()` to GroupApiService
  - [x] Add `getRegistrationGroup()` to GroupRepository
  - [x] Create RegistrationGroupInfo data class
  - [x] Create RegistrationGroupResponse API model

- [x] Task 2: Add migration state tracking to AuthViewModel (AC: 2, 3)
  - [x] Add MigrationState sealed interface (NoMigration, Checking, HasRegistrationGroup, Dismissed)
  - [x] Add _migrationState StateFlow
  - [x] Add checkAndPromptGroupMigration() method

- [x] Task 3: Integrate into login flow (AC: 1, 3, 4)
  - [x] Call checkAndPromptGroupMigration after successful login
  - [x] Call checkAndPromptGroupMigration after successful registration
  - [x] Call checkAndPromptGroupMigration after successful OAuth
  - [x] Handle case where no registration group exists
  - [x] Handle case where registration group is detected

---

## Dev Notes

### Technical Notes
- Call `GET /api/v1/devices/me/registration-group` after login
- Add `checkRegistrationGroup()` to `AuthViewModel`
- Store result in `MigrationState` sealed interface
- Detection happens after device linking and settings sync

### Implementation Details
Registration group detection:
1. Called after successful login/register/OAuth
2. Queries backend for registration groups this device belongs to
3. If found, sets MigrationState.HasRegistrationGroup with group info
4. If not found, sets MigrationState.NoMigration
5. Errors are non-blocking - continue without migration prompt

---

## Dev Agent Record

### Debug Log

No issues encountered during implementation.

### Implementation Plan

1. Create RegistrationGroupInfo data class in domain/model
2. Add RegistrationGroupResponse to GroupModels.kt
3. Add checkRegistrationGroup() to GroupApiService interface and implementation
4. Add checkRegistrationGroup() to GroupRepository
5. Create MigrationState sealed interface in AuthViewModel
6. Add migration state flow and checkAndPromptGroupMigration method
7. Integrate into login(), register(), and oauthSignIn() flows

### Completion Notes

Implementation completed successfully:
- API endpoint for checking registration groups added
- MigrationState tracks detection flow
- Detection triggered after all authentication methods
- Non-blocking error handling ensures auth flow continues

---

## File List

### New Files
- `app/src/main/java/three/two/bit/phonemanager/domain/model/RegistrationGroupInfo.kt`

### Modified Files
- `app/src/main/java/three/two/bit/phonemanager/network/models/GroupModels.kt`
- `app/src/main/java/three/two/bit/phonemanager/network/GroupApiService.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/GroupRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/auth/AuthViewModel.kt`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created from UGM epics specification |
| 2025-12-18 | Claude | Implementation completed - all ACs satisfied |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-1
**Blocking**: UGM-4.2
