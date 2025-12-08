# Story AP-3.6: Manage User MFA Settings

**Story ID**: AP-3.6
**Epic**: AP-3 - User Administration
**Priority**: Must-Have (Critical)
**Status**: Done
**Created**: 2025-12-09

---

## Story

As an admin,
I want to manage user MFA settings,
so that I can enforce security policies.

## Acceptance Criteria

### AC AP-3.6.1: View MFA Status
**Given** I select a user
**When** I click "Manage MFA"
**Then** I should see the user's MFA status (enabled/disabled, method, enrolled date)

### AC AP-3.6.2: Force MFA Enrollment
**Given** a user has MFA disabled
**When** I click "Force Enrollment"
**Then** the user should be required to enroll in MFA on next login

### AC AP-3.6.3: Reset MFA
**Given** a user has MFA enabled
**When** I click "Reset MFA"
**Then** the user's MFA should be reset and they must re-enroll

### AC AP-3.6.4: MFA Changes Logged
**Given** I change a user's MFA settings
**When** the action is completed
**Then** the change should be logged in the audit trail

## Dev Agent Record

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Completion Notes List
- Added MfaStatus interface to types
- Added usersApi.getMfaStatus(), forceMfaEnrollment(), resetMfa() methods
- Created UserMfaDialog showing:
  - MFA status (enabled/disabled badge)
  - Method (Authenticator/SMS/Email)
  - Enrollment date
  - Backup codes remaining
- Force enrollment action for disabled MFA
- Reset MFA action for enabled MFA
- Warning when MFA is not enabled
- Integrated into UserActionsMenu

### File List
- `admin-portal/types/index.ts` (MODIFIED)
- `admin-portal/lib/api-client.ts` (MODIFIED)
- `admin-portal/components/users/user-mfa-dialog.tsx` (NEW)
- `admin-portal/components/users/user-actions-menu.tsx` (MODIFIED)

---

**Status**: Done
**Dependencies**: Story AP-3.3 (Complete)
