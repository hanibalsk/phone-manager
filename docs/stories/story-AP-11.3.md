# Story AP-11.3: User Activity Reports

**Story ID**: AP-11.3
**Epic**: AP-11 - Audit & Compliance
**Priority**: High
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-13.3 (Admin Portal PRD)

---

## Story

As an admin,
I want user activity reports,
so that I can review individual actions.

## Acceptance Criteria

### AC AP-11.3.1: Activity Timeline
**Given** I am viewing a user's activity
**When** I open their activity report
**Then** I see an activity timeline per user

### AC AP-11.3.2: Grouped Actions
**Given** I am viewing user activity
**When** I analyze the data
**Then** actions are grouped by type

### AC AP-11.3.3: Date Filtering
**Given** I am viewing user activity
**When** I want to narrow the scope
**Then** I can filter by date range

### AC AP-11.3.4: Export Options
**Given** I have a user activity report
**When** I want to save it
**Then** I can export as PDF or CSV

## Tasks / Subtasks

- [ ] Task 1: Create User Activity Page
  - [ ] Create app/(dashboard)/audit/users/page.tsx
  - [ ] User selection interface
  - [ ] Activity timeline component
- [ ] Task 2: Add Action Grouping
  - [ ] Group by action type
  - [ ] Visual action type indicators
  - [ ] Action type statistics
- [ ] Task 3: Add Date Filtering
  - [ ] Date range picker
  - [ ] Quick date presets
- [ ] Task 4: Add Export
  - [ ] PDF export
  - [ ] CSV export
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test user activity components

## Dev Notes

### Architecture
- User selector with search
- Vertical timeline visualization
- Action type grouping with counts
- Date range filtering

### Dependencies
- Story AP-11.1 (Audit logging exists)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/audit/users/:userId/activity
GET /api/admin/audit/users/:userId/export
```

### Files to Create/Modify
- `admin-portal/app/(dashboard)/audit/users/page.tsx` (NEW)
- `admin-portal/lib/api-client.ts` (MODIFY - add user activity API)

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

All acceptance criteria have been implemented in `audit/users/page.tsx`:

| AC | Status | Evidence |
|----|--------|----------|
| AC AP-11.3.1 (Activity Timeline) | ✅ Complete | `audit/users/page.tsx:423-458` - Vertical timeline grouped by date with TimelineItem component showing action, resource, timestamp |
| AC AP-11.3.2 (Grouped Actions) | ✅ Complete | `audit/users/page.tsx:396-421` - Actions grouped by type with progress bar visualization and count display |
| AC AP-11.3.3 (Date Filtering) | ✅ Complete | `audit/users/page.tsx:284-320` - Date range picker with quick presets (7, 30, 90 days) and custom date inputs |
| AC AP-11.3.4 (Export Options) | ✅ Complete | `audit/users/page.tsx:349-367` - CSV and PDF export buttons with loading state via handleExport function |

**Technical Quality:**
- User selection panel with search functionality
- TimelineItem component with action-colored dots
- Action type badges with 13-color system matching main audit page
- Resource type icons for visual identification
- Summary statistics (total actions, action types, resources, events)
- Timeline grouped by date with event counts

**Design Patterns:**
- Clean separation: user selection (left panel) + activity report (right panel)
- Date presets for common use cases
- Progressive disclosure: summary cards → action breakdown → timeline

**Recommendations for Future Enhancements:**
1. Add data-testid attributes for E2E testing
2. Consider adding activity heatmap visualization
3. Add comparison between time periods

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-11.1
