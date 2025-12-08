# Story AP-3.4: Reset User Passwords

**Story ID**: AP-3.4
**Epic**: AP-3 - User Administration
**Priority**: Must-Have (Critical)
**Status**: Done
**Created**: 2025-12-09

---

## Story

As an admin,
I want to reset user passwords,
so that I can help locked-out users.

## Acceptance Criteria

### AC AP-3.4.1: Password Reset Action
**Given** I select a user
**When** I click "Reset Password"
**Then** a password reset email should be sent to the user

### AC AP-3.4.2: Force Password Change Option
**Given** I am resetting a password
**When** I submit the reset
**Then** I can choose to force password change on next login

### AC AP-3.4.3: Audit Logging
**Given** I reset a password
**When** the action is completed
**Then** the action should be logged in the audit trail

## Dev Agent Record

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Completion Notes List
- Added usersApi.resetPassword() method with forceChange parameter
- Created UserResetPasswordDialog component
- Dialog shows user info and has force change checkbox
- Integrated into UserActionsMenu

### File List
- `admin-portal/lib/api-client.ts` (MODIFIED)
- `admin-portal/components/users/user-reset-password-dialog.tsx` (NEW)
- `admin-portal/components/users/user-actions-menu.tsx` (MODIFIED)

---

**Status**: Done
**Dependencies**: Story AP-3.3 (Complete)
