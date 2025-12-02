# Story E14.5: App Limit Management

**Story ID**: E14.5
**Epic**: 14 - Admin Web Portal
**Priority**: Medium
**Estimate**: 2 story points
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
- Insufficient input validation (client-side only) - High
- No delete confirmation dialog - Low

### Required Actions
1. Add authentication protection
2. Implement Zod schema validation for limit forms
3. Add server-side validation
**Dependencies**: E14.1, E14.2

---

## Story

As an administrator,
I want to manage app time limits from the web portal,
so that I can configure usage restrictions without the mobile app.

## Acceptance Criteria

### AC E14.5.1: Limit List
- Display all configured limits
- Show app name, package, and limit duration
- Show enabled/disabled status

### AC E14.5.2: Create/Edit Limits
- Add new app limits
- Edit existing limits
- Toggle enabled status

### AC E14.5.3: Delete Limits
- Remove app limits
- Confirmation before deletion

## Tasks / Subtasks

- [x] Task 1: Create Limits Page Route
- [x] Task 2: Implement Limit List Component
- [x] Task 3: Add Create/Edit Dialog
- [x] Task 4: Add Device Selector
- [x] Task 5: Connect to API

## File List

### Created Files

- `admin-portal/app/limits/page.tsx` - App limits page
- `admin-portal/components/limits/limit-list.tsx` - Limit list component
- `admin-portal/components/limits/limit-edit-dialog.tsx` - Create/edit dialog
- `admin-portal/components/limits/index.tsx` - Component exports

---

**Last Updated**: 2025-12-02
**Status**: Complete
