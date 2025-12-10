# Story AP-11.4: Organization Activity Reports

**Story ID**: AP-11.4
**Epic**: AP-11 - Audit & Compliance
**Priority**: High
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Development
**Created**: 2025-12-10
**PRD Reference**: FR-13.4 (Admin Portal PRD)

---

## Story

As an admin,
I want organization activity reports,
so that I can monitor usage.

## Acceptance Criteria

### AC AP-11.4.1: Activity Summary
**Given** I am viewing an organization's activity
**When** I open their activity report
**Then** I see an activity summary per organization

### AC AP-11.4.2: User Action Counts
**Given** I am viewing organization activity
**When** I analyze user data
**Then** I see user action counts

### AC AP-11.4.3: Resource Changes Summary
**Given** I am viewing organization activity
**When** I analyze changes
**Then** I see a resource changes summary

### AC AP-11.4.4: Anomaly Highlighting
**Given** I am viewing organization activity
**When** unusual patterns occur
**Then** anomalies are highlighted

## Tasks / Subtasks

- [ ] Task 1: Create Organization Activity Page
  - [ ] Create app/(dashboard)/audit/organizations/page.tsx
  - [ ] Organization selector
  - [ ] Activity summary cards
- [ ] Task 2: Add User Action Counts
  - [ ] User action breakdown
  - [ ] Action count visualization
- [ ] Task 3: Add Resource Changes Summary
  - [ ] Resource type breakdown
  - [ ] Change frequency chart
- [ ] Task 4: Add Anomaly Detection
  - [ ] Anomaly highlighting
  - [ ] Unusual activity alerts
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test organization activity components

## Dev Notes

### Architecture
- Organization selector with search
- Summary dashboard cards
- Bar charts for action counts
- Anomaly detection with visual indicators

### Dependencies
- Story AP-11.1 (Audit logging exists)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/audit/organizations/:orgId/activity
GET /api/admin/audit/organizations/:orgId/anomalies
```

### Files to Create/Modify
- `admin-portal/app/(dashboard)/audit/organizations/page.tsx` (NEW)
- `admin-portal/lib/api-client.ts` (MODIFY - add org activity API)

### References
- [Source: PRD-admin-portal.md - Epic AP-11]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-11: Audit & Compliance

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
**Dependencies**: Story AP-11.1
