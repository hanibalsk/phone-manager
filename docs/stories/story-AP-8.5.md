# Story AP-8.5: Auto-Approval Rules

**Story ID**: AP-8.5
**Epic**: AP-8 - App Usage & Unlock Requests
**Priority**: Medium
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Development
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

- [ ] Task 1: Add Auto-Approval Types (AC: AP-8.5.1)
  - [ ] Add AutoApprovalRule type to types/index.ts
  - [ ] Add RuleCondition type
  - [ ] Add auto-approval API endpoints
- [ ] Task 2: Create Auto-Approval Page (AC: AP-8.5.1)
  - [ ] Create app/(dashboard)/unlock-requests/rules/page.tsx
- [ ] Task 3: Create Rules List Component (AC: AP-8.5.1, AP-8.5.3)
  - [ ] Create components/unlock-requests/auto-approval-rules.tsx
  - [ ] Show rules with priority
  - [ ] Drag to reorder or priority buttons
  - [ ] Toggle enable/disable
- [ ] Task 4: Create Rule Form (AC: AP-8.5.1, AP-8.5.2)
  - [ ] Create components/unlock-requests/auto-approval-rule-form.tsx
  - [ ] Time window conditions
  - [ ] User/device conditions
  - [ ] Max duration setting
- [ ] Task 5: Create Audit Log Component (AC: AP-8.5.4)
  - [ ] Create components/unlock-requests/auto-approval-log.tsx
  - [ ] Show auto-approved requests
  - [ ] Which rule triggered
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
(To be filled during development)

### Completion Notes List
(To be filled during development)

### File List
(To be filled during development)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Development
**Dependencies**: Story AP-8.4 (Manage Unlock Requests)
