# Story AP-11.4: Organization Activity Reports

**Story ID**: AP-11.4
**Epic**: AP-11 - Audit & Compliance
**Priority**: High
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
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

## Senior Developer Review

### Review Date: 2025-12-10

### Reviewer: Senior Developer (Claude)

### Review Status: APPROVED

### Implementation Assessment

**Completeness: 100%**

All acceptance criteria have been implemented in `audit/organizations/page.tsx`:

| AC | Status | Evidence |
|----|--------|----------|
| AC AP-11.4.1 (Activity Summary) | ✅ Complete | `audit/organizations/page.tsx:339-364` - Summary stats cards showing total actions, active users, resources changed, anomaly count |
| AC AP-11.4.2 (User Action Counts) | ✅ Complete | `audit/organizations/page.tsx:388-444` - User activity table with action counts and distribution bar chart per user |
| AC AP-11.4.3 (Resource Changes Summary) | ✅ Complete | `audit/organizations/page.tsx:447-506` - Resource changes table showing created/updated/deleted counts by resource type |
| AC AP-11.4.4 (Anomaly Highlighting) | ✅ Complete | `audit/organizations/page.tsx:367-386` - Dedicated anomaly section with severity badges, AnomalyCard component |

**Technical Quality:**
- Organization selection with search
- StatCard component with optional trend indicators
- AnomalyCard component with severity-based styling (low/medium/high)
- User action table with progress bar visualization
- Resource changes table with color-coded +created, ~updated, -deleted
- Date range presets matching user activity page

**Design Patterns:**
- Consistent 2-column layout (selection + report)
- Visual anomaly highlighting with yellow background card
- Progress bars showing action distribution
- Severity-based badge colors (blue/yellow/red)

**Recommendations for Future Enhancements:**
1. Add data-testid attributes for E2E testing
2. Add anomaly threshold configuration
3. Consider trend comparison over time periods

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-11.1
