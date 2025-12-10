# Story AP-8.5: Auto-Approval Rules

**Story ID**: AP-8.5
**Epic**: AP-8 - App Usage & Unlock Requests
**Priority**: Medium
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-8.5 (Admin Portal PRD)

---

## Story

As an admin,
I want to configure auto-approval rules,
so that I can reduce manual work.

## Acceptance Criteria

### AC AP-8.5.1: Rules Based on Conditions
**Given** I am configuring auto-approval
**When** I create a rule
**Then** I can base it on time, user, or device

### AC AP-8.5.2: Maximum Duration
**Given** I am configuring auto-approval
**When** I set the rule
**Then** I can set maximum auto-approval duration

### AC AP-8.5.3: Rule Priority
**Given** I have multiple rules
**Then** I can set rule priority ordering

### AC AP-8.5.4: Audit Log
**Given** auto-approvals occur
**Then** I should see audit log for auto-approvals

## Tasks / Subtasks

- [x] Task 1: Add Auto-Approval Types (AC: AP-8.5.1)
  - [x] Add AutoApprovalRule type to types/index.ts (added in AP-8.1)
  - [x] Add AutoApprovalConditions type
  - [x] Add auto-approval API endpoints (added in AP-8.1)
- [x] Task 2: Create Auto-Approval Page (AC: AP-8.5.1)
  - [x] Update app/(dashboard)/unlock-requests/rules/page.tsx with tabs
- [x] Task 3: Create Rules List Component (AC: AP-8.5.1, AP-8.5.3)
  - [x] Create components/unlock-requests/auto-approval-rules.tsx
  - [x] Show rules with priority number badge
  - [x] Up/down arrows to reorder
  - [x] Toggle enable/disable with icon
- [x] Task 4: Create Rule Form (AC: AP-8.5.1, AP-8.5.2)
  - [x] Create components/unlock-requests/auto-approval-rule-form.tsx
  - [x] Time window conditions (start/end time, day selection)
  - [x] User/device/group conditions with multi-select
  - [x] Max duration setting with presets
  - [x] Max daily requests limit
- [x] Task 5: Create Audit Log Component (AC: AP-8.5.4)
  - [x] Create components/unlock-requests/auto-approval-log.tsx
  - [x] Show auto-approved requests with filters
  - [x] Statistics summary (total, rules triggered, avg duration)
  - [x] Rule name and trigger details
- [ ] Task 6: Testing (All ACs) - Deferred
  - [ ] Test auto-approval rules

## Dev Notes

### Architecture
- Rules evaluated in priority order (lower number = higher priority)
- First matching rule applies
- Conditions: time_window, user_ids, device_ids, group_ids
- Max duration prevents excessive auto-approvals
- Audit log tracks: request, rule_id, approved_at

### Dependencies
- Story AP-8.4 (Unlock Requests)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/auto-approval-rules
POST /api/admin/auto-approval-rules
PUT /api/admin/auto-approval-rules/:id
DELETE /api/admin/auto-approval-rules/:id
PUT /api/admin/auto-approval-rules/reorder - Body: { rule_ids: [] }
GET /api/admin/auto-approval-log?from=...&to=...
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add rule types)
- `admin-portal/lib/api-client.ts` (MODIFY - add rules API)
- `admin-portal/app/(dashboard)/unlock-requests/rules/page.tsx` (NEW)
- `admin-portal/components/unlock-requests/auto-approval-rules.tsx` (NEW)
- `admin-portal/components/unlock-requests/auto-approval-rule-form.tsx` (NEW)
- `admin-portal/components/unlock-requests/auto-approval-log.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-8.5]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-8: App Usage & Unlock Requests

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- Types and API endpoints added in AP-8.1 for efficiency

### Completion Notes List
- Auto-approval rules list with priority badges and reorder controls
- Toggle enable/disable with visual indicator
- Comprehensive rule form with multiple condition types:
  - Time window with start/end time and day selection
  - User-specific rules with multi-select
  - Device-specific rules with multi-select
  - Group-specific rules with multi-select
  - Maximum daily requests limit
- Duration presets (15m, 30m, 1h, 2h) + custom option
- Audit log with org/rule filters and date range
- Statistics summary (total, rules triggered, avg duration)
- Tab navigation between Rules and Audit Log

### File List
- `admin-portal/types/index.ts` (MODIFIED - updated AutoApprovalConditions, TimeWindow)
- `admin-portal/lib/api-client.ts` (MODIFIED - added alias methods for auto-approval)
- `admin-portal/components/unlock-requests/auto-approval-rules.tsx` (NEW)
- `admin-portal/components/unlock-requests/auto-approval-rule-form.tsx` (NEW)
- `admin-portal/components/unlock-requests/auto-approval-log.tsx` (NEW)
- `admin-portal/components/unlock-requests/index.tsx` (MODIFIED)
- `admin-portal/app/(dashboard)/unlock-requests/rules/page.tsx` (MODIFIED)
- `admin-portal/app/(dashboard)/unlock-requests/rules/new/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/unlock-requests/rules/[id]/edit/page.tsx` (NEW)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented auto-approval rules feature |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-8.4 (Manage Unlock Requests)
