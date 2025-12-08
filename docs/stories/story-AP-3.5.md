# Story AP-3.5: Manage User Sessions

**Story ID**: AP-3.5
**Epic**: AP-3 - User Administration
**Priority**: Must-Have (Critical)
**Status**: Done
**Created**: 2025-12-09

---

## Story

As an admin,
I want to manage user sessions,
so that I can terminate suspicious activity.

## Acceptance Criteria

### AC AP-3.5.1: View Active Sessions
**Given** I select a user
**When** I click "Manage Sessions"
**Then** I should see a list of active sessions with device, IP, and last activity

### AC AP-3.5.2: Revoke Individual Session
**Given** I am viewing a user's sessions
**When** I click revoke on a session
**Then** that session should be immediately terminated

### AC AP-3.5.3: Revoke All Sessions
**Given** I am viewing a user's sessions
**When** I click "Revoke All Sessions"
**Then** all sessions should be immediately terminated

### AC AP-3.5.4: Session Revocation is Immediate
**Given** I revoke a session
**When** the revocation completes
**Then** the user should be logged out immediately

## Dev Agent Record

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Completion Notes List
- Added UserSession interface to types
- Added usersApi.getSessions(), revokeSession(), revokeAllSessions() methods
- Created UserSessionsDialog with session list and revoke actions
- Sessions display device, IP, last activity
- Current session indicator
- Integrated into UserActionsMenu

### File List
- `admin-portal/types/index.ts` (MODIFIED)
- `admin-portal/lib/api-client.ts` (MODIFIED)
- `admin-portal/components/users/user-sessions-dialog.tsx` (NEW)
- `admin-portal/components/users/user-actions-menu.tsx` (MODIFIED)

---

**Status**: Done
**Dependencies**: Story AP-3.3 (Complete)
