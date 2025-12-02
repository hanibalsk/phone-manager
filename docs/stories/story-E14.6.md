# Story E14.6: Settings Management

**Story ID**: E14.6
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

### Critical Security Issues
- **SECURITY CRITICAL**: PIN stored as plaintext - MUST hash server-side
- Missing authentication - Critical

### Required Actions
1. Hash PINs with bcrypt/argon2 server-side
2. Never transmit actual PIN in API responses
3. Add authentication protection
4. Implement PIN change workflow with current PIN verification
**Dependencies**: E14.1

---

## Story

As an administrator,
I want to manage global settings from the web portal,
so that I can configure parental control behavior.

## Acceptance Criteria

### AC E14.6.1: Settings Form
- View current settings
- Edit unlock PIN
- Configure default daily limit

### AC E14.6.2: Toggle Settings
- Enable/disable notifications
- Enable/disable auto-approve

### AC E14.6.3: Save Changes
- Save button with unsaved indicator
- Confirmation on save

## Tasks / Subtasks

- [x] Task 1: Create Settings Page Route
- [x] Task 2: Implement Settings Form Component
- [x] Task 3: Add Toggle Controls
- [x] Task 4: Connect to API

## File List

### Created Files

- `admin-portal/app/settings/page.tsx` - Settings page
- `admin-portal/components/settings/settings-form.tsx` - Settings form
- `admin-portal/components/settings/index.tsx` - Component exports

---

**Last Updated**: 2025-12-02
**Status**: Complete
