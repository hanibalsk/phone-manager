# Story UGM-4.3: Execute Group Migration

**Story ID**: UGM-4.3
**Epic**: UGM-4 - Group Migration Wizard
**Priority**: High
**Estimate**: 3 story points
**Status**: Completed
**Created**: 2025-12-18

**PRD Reference**: docs/PRD.md, docs/epics-ugm.md
**Dependencies**: Story UGM-4.2

---

## Story

As a user who chose to migrate,
I want to complete the migration process with a group name,
So that my registration group becomes a full authenticated group.

**FRs Covered**: FR8, FR9, FR10, FR11

---

## Acceptance Criteria

### AC 1: Pre-fill group name
**Given** I am on the migration screen
**When** it loads
**Then** pre-fill group name with registration group ID or existing name

### AC 2: Validate group name
**Given** the migration form
**When** I enter/modify the group name
**Then** validate name (3-50 chars, alphanumeric and spaces)

### AC 3: Call migration API
**Given** valid name entered
**When** I tap "Migrate"
**Then** call migration API with name and registration group ID

### AC 4: Set as OWNER
**Given** migration API succeeds
**When** response received
**Then** I am set as OWNER of the new authenticated group

### AC 5: Delete registration group
**Given** migration completes
**When** all devices moved
**Then** registration group is deleted automatically

### AC 6: Performance
**Given** migration in progress
**When** showing status
**Then** show progress indicator, complete < 2s perceived (NFR-P3)

---

## Tasks / Subtasks

- [x] Task 1: Create GroupMigrationScreen and ViewModel (AC: 1, 2, 6)
  - [x] Create GroupMigrationViewModel with Hilt
  - [x] Create GroupMigrationScreen composable
  - [x] Add Screen.GroupMigration to navigation
  - [x] Pre-fill group name field
  - [x] Add name validation (3-50 chars)

- [x] Task 2: Add migration API integration (AC: 3, 4, 5)
  - [x] Add migrateGroup() to GroupApiService (done in UGM-4.1)
  - [x] Add migrateRegistrationGroup() to GroupRepository (done in UGM-4.1)
  - [x] Handle success response with new group ID
  - [x] Store new group as current group

- [x] Task 3: Add navigation and success handling
  - [x] Add composable route to PhoneManagerNavHost
  - [x] Navigate to new group detail on success
  - [x] Show error via snackbar

- [x] Task 4: Add UI strings
  - [x] Migration screen title
  - [x] Form labels and placeholders
  - [x] Button labels
  - [x] Success/progress messages

---

## Dev Notes

### Technical Notes
- Create `GroupMigrationScreen` composable and ViewModel
- Call `POST /api/v1/groups/migrate` endpoint
- Migration is atomic - all or nothing (NFR-R1)

### Implementation Details
Migration screen should:
1. Show current registration group info (name, device count)
2. Allow editing the new group name
3. Validate name before allowing migration
4. Call backend migration API (atomic operation)
5. Backend handles: create new group, transfer devices, assign OWNER, delete old group
6. On success, navigate to new group detail

---

## Dev Agent Record

### Debug Log

No issues encountered during implementation.

### Implementation Plan

1. Create GroupMigrationViewModel with state management
2. Create GroupMigrationScreen with form UI
3. Add Screen.GroupMigration to PhoneManagerNavHost
4. Add migrateGroup() to GroupApiService
5. Add migrateRegistrationGroup() to GroupRepository
6. Wire up navigation from MigrationPromptDialog
7. Handle success/failure states

### Completion Notes

Implementation completed successfully:
- Created GroupMigrationViewModel with validation and API integration
- Created GroupMigrationScreen with form UI showing benefits
- API methods were already added in UGM-4.1
- Navigation wired up from LoginScreen, RegisterScreen
- On success, navigates to new group detail
- Error handling via snackbar messages

---

## File List

### New Files
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupMigrationViewModel.kt`
- `app/src/main/java/three/two/bit/phonemanager/ui/groups/GroupMigrationScreen.kt`

### Modified Files
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/three/two/bit/phonemanager/ui/navigation/PhoneManagerNavHost.kt`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-18 | Claude | Story created from UGM epics specification |
| 2025-12-18 | Claude | Implementation completed - all ACs satisfied |

---

**Last Updated**: 2025-12-18
**Status**: Completed
**Dependencies**: UGM-4.2
**Blocking**: UGM-4.4
