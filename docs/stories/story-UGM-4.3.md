# Story UGM-4.3: Execute Group Migration

**Story ID**: UGM-4.3
**Epic**: UGM-4 - Group Migration Wizard
**Priority**: High
**Estimate**: 3 story points
**Status**: Ready-for-dev
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

- [ ] Task 1: Create GroupMigrationScreen and ViewModel (AC: 1, 2, 6)
  - [ ] Create GroupMigrationViewModel with Hilt
  - [ ] Create GroupMigrationScreen composable
  - [ ] Add Screen.GroupMigration to navigation
  - [ ] Pre-fill group name field
  - [ ] Add name validation (3-50 chars)

- [ ] Task 2: Add migration API integration (AC: 3, 4, 5)
  - [ ] Add migrateGroup() to GroupApiService
  - [ ] Add migrateRegistrationGroup() to GroupRepository
  - [ ] Handle success response with new group ID
  - [ ] Store new group as current group

- [ ] Task 3: Add navigation and success handling
  - [ ] Add composable route to PhoneManagerNavHost
  - [ ] Navigate to new group detail on success
  - [ ] Show success snackbar

- [ ] Task 4: Add UI strings
  - [ ] Migration screen title
  - [ ] Form labels and placeholders
  - [ ] Button labels
  - [ ] Success/progress messages

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

_To be filled during implementation_

### Implementation Plan

1. Create GroupMigrationViewModel with state management
2. Create GroupMigrationScreen with form UI
3. Add Screen.GroupMigration to PhoneManagerNavHost
4. Add migrateGroup() to GroupApiService
5. Add migrateRegistrationGroup() to GroupRepository
6. Wire up navigation from MigrationPromptDialog
7. Handle success/failure states

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
**Dependencies**: UGM-4.2
**Blocking**: UGM-4.4
