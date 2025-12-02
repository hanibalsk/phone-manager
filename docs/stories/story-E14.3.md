# Story E14.3: Unlock Request Dashboard

**Story ID**: E14.3
**Epic**: 14 - Admin Web Portal
**Priority**: High
**Estimate**: 3 story points (1-2 days)
**Status**: Changes Requested
**Created**: 2025-12-02
**Reviewed**: 2025-12-02

## Review Report
**Date**: 2025-12-02
**Reviewer**: Code Quality Reviewer (Agent)
**Outcome**: Changes Requested
**Report**: [epic-14-code-review.md](/docs/reviews/epic-14-code-review.md)

### Issues Found
- Missing authentication (security critical) - Critical
- No loading states for mutations (duplicate submissions) - High
- No toast notifications for actions - Medium

### Required Actions
1. Add authentication protection
2. Disable approve/deny buttons during API calls
3. Add toast notifications for action results
**PRD Reference**: PRD-user-management.md
**Dependencies**: E14.1, E14.2

---

## Story

As an administrator,
I want to view and manage unlock requests from the web portal,
so that I can approve or deny requests without using the mobile app.

## Acceptance Criteria

### AC E14.3.1: Request List View
**Given** an administrator accessing the unlock requests page
**When** the page loads
**Then** they should see:
  - List of all unlock requests
  - Device name and request reason
  - Request timestamp
  - Requested duration
  - Current status (pending/approved/denied)

### AC E14.3.2: Approve/Deny Actions
**Given** a pending unlock request
**When** the admin takes action
**Then** they can:
  - Approve with optional message
  - Deny with optional reason
  - See confirmation of action

### AC E14.3.3: Filter by Status
**Given** multiple requests
**When** filtering the list
**Then** users can:
  - Filter by pending/approved/denied
  - See count of pending requests
  - Default to showing pending first

## Tasks / Subtasks

- [x] Task 1: Create Unlock Requests Page
- [x] Task 2: Implement Request List Component
- [x] Task 3: Add Approve/Deny Dialog
- [x] Task 4: Add Status Filters
- [x] Task 5: Connect to API

---

## File List

### Created Files

- `admin-portal/app/unlock-requests/page.tsx` - Unlock requests page
- `admin-portal/components/unlock-requests/request-list.tsx` - Request list component
- `admin-portal/components/unlock-requests/request-action-dialog.tsx` - Approve/deny dialog
- `admin-portal/components/unlock-requests/index.tsx` - Component exports

---

**Last Updated**: 2025-12-02
**Status**: Complete
