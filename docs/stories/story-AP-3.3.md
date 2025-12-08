# Story AP-3.3: Suspend and Reactivate Users

**Story ID**: AP-3.3
**Epic**: AP-3 - User Administration
**Priority**: Must-Have (Critical)
**Estimate**: 1 story point (0.5-1 day)
**Status**: In Development
**Created**: 2025-12-09
**PRD Reference**: FR-3.2 (Admin Portal PRD)

---

## Story

As an admin,
I want to suspend and reactivate users,
so that I can manage access.

## Acceptance Criteria

### AC AP-3.3.1: Suspend Action
**Given** I am viewing a user in the user list
**When** I click the suspend action
**Then** I should be prompted to enter a reason
**And** the user status should change to "suspended"

### AC AP-3.3.2: Suspended User Login Prevention
**Given** a user has been suspended
**When** they attempt to login
**Then** they should be denied access
**And** active sessions should be terminated

### AC AP-3.3.3: Reactivate Action
**Given** I am viewing a suspended user
**When** I click the reactivate action
**Then** the user status should change to "active"
**And** the user should be able to login again

### AC AP-3.3.4: Audit Logging
**Given** I suspend or reactivate a user
**When** the action is completed
**Then** the action should be logged in the audit trail

## Tasks / Subtasks

- [ ] Task 1: Add User Action APIs
  - [ ] Add suspendUser method to usersApi
  - [ ] Add reactivateUser method to usersApi
- [ ] Task 2: Add Actions Column to User Table
  - [ ] Add actions column with dropdown menu
  - [ ] Show Suspend for active users
  - [ ] Show Reactivate for suspended users
- [ ] Task 3: Create Suspend Dialog
  - [ ] Create user-suspend-dialog.tsx
  - [ ] Reason input field
  - [ ] Confirmation and submit
- [ ] Task 4: Implement Reactivate Action
  - [ ] Confirmation dialog
  - [ ] API call and refresh

## Dev Notes

### API Endpoints Expected
```typescript
POST /api/admin/users/:id/suspend
Body: { reason: string }
Response: AdminUser

POST /api/admin/users/:id/reactivate
Response: AdminUser
```

### Files to Create/Modify
- `admin-portal/lib/api-client.ts` (MODIFY - add suspend/reactivate methods)
- `admin-portal/components/users/user-actions-menu.tsx` (NEW)
- `admin-portal/components/users/user-suspend-dialog.tsx` (NEW)
- `admin-portal/components/users/user-list.tsx` (MODIFY - add actions column)

## Dev Agent Record

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Completion Notes List
- Task 1: Added usersApi.suspend() and usersApi.reactivate() methods
- Task 2: Created UserActionsMenu with dropdown for all user actions
- Task 3: Created UserSuspendDialog with reason input and confirmation
- Task 4: Reactivate action integrated in actions menu
- Added actions column to user table
- Menu includes placeholders for AP-3.4, AP-3.5, AP-3.6 actions

### File List
- `admin-portal/lib/api-client.ts` (MODIFIED)
- `admin-portal/components/users/user-actions-menu.tsx` (NEW)
- `admin-portal/components/users/user-suspend-dialog.tsx` (NEW)
- `admin-portal/components/users/user-list.tsx` (MODIFIED)
- `admin-portal/components/users/index.tsx` (MODIFIED)

---

**Status**: Done
**Dependencies**: Story AP-3.1, AP-3.2 (Complete)
