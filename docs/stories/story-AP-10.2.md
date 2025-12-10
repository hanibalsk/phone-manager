# Story AP-10.2: User Analytics

**Story ID**: AP-10.2
**Epic**: AP-10 - Dashboard & Analytics
**Priority**: High
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Development
**Created**: 2025-12-10
**PRD Reference**: FR-10.2 (Admin Portal PRD)

---

## Story

As an admin,
I want user analytics,
so that I can track adoption.

## Acceptance Criteria

### AC AP-10.2.1: User Growth Chart
**Given** I am an admin viewing user analytics
**When** I view the growth chart
**Then** I can see user growth (daily, weekly, monthly)

### AC AP-10.2.2: Active Users Over Time
**Given** I am viewing user analytics
**When** I check activity trends
**Then** I can see active users over time

### AC AP-10.2.3: User Retention Metrics
**Given** I am viewing user analytics
**When** I analyze retention
**Then** I can see user retention metrics

### AC AP-10.2.4: New vs Returning Users
**Given** I am viewing user analytics
**When** I compare user types
**Then** I can see new vs returning users breakdown

## Tasks / Subtasks

- [ ] Task 1: Add User Analytics Types (AC: AP-10.2.1)
  - [ ] Add UserAnalytics type to types/index.ts
  - [ ] Add UserGrowthData type
  - [ ] Add UserRetentionData type
  - [ ] Add user analytics API endpoints
- [ ] Task 2: Create User Analytics Component (AC: AP-10.2.1, AP-10.2.2)
  - [ ] Create components/analytics/user-analytics.tsx
  - [ ] User growth chart with time period selector
  - [ ] Active users trend line
- [ ] Task 3: Add Retention Metrics (AC: AP-10.2.3)
  - [ ] Retention cohort analysis display
  - [ ] Churn rate indicator
- [ ] Task 4: Add User Comparison (AC: AP-10.2.4)
  - [ ] New vs returning users chart
  - [ ] User segment breakdown
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test analytics components

## Dev Notes

### Architecture
- Charts using recharts or similar library
- Time period selectors (7d, 30d, 90d, custom)
- Data aggregation on backend
- Export functionality for charts

### Dependencies
- Story AP-10.1 (Dashboard page exists)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/analytics/users/growth
GET /api/admin/analytics/users/active
GET /api/admin/analytics/users/retention
GET /api/admin/analytics/users/segments
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add analytics types)
- `admin-portal/lib/api-client.ts` (MODIFY - add analytics API)
- `admin-portal/components/analytics/user-analytics.tsx` (NEW)
- `admin-portal/components/analytics/index.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-10.2]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-10: Dashboard & Analytics

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
**Dependencies**: Story AP-10.1 (Overview Dashboard)
