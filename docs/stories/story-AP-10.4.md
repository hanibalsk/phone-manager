# Story AP-10.4: API Analytics

**Story ID**: AP-10.4
**Epic**: AP-10 - Dashboard & Analytics
**Priority**: High
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Development
**Created**: 2025-12-10
**PRD Reference**: FR-10.4 (Admin Portal PRD)

---

## Story

As an admin,
I want API analytics,
so that I can monitor system usage.

## Acceptance Criteria

### AC AP-10.4.1: Request Volume by Endpoint
**Given** I am an admin viewing API analytics
**When** I view request metrics
**Then** I can see request volume by endpoint

### AC AP-10.4.2: Response Time Percentiles
**Given** I am viewing API analytics
**When** I check performance
**Then** I can see response time percentiles

### AC AP-10.4.3: Error Rate Tracking
**Given** I am viewing API analytics
**When** I monitor errors
**Then** I can see error rate tracking

### AC AP-10.4.4: Top Consumers
**Given** I am viewing API analytics
**When** I analyze usage patterns
**Then** I can see top consumers by request count

## Tasks / Subtasks

- [ ] Task 1: Add API Analytics Types (AC: AP-10.4.1)
  - [ ] Add ApiAnalytics type to types/index.ts
  - [ ] Add EndpointMetrics type
  - [ ] Add ResponseTimeData type
  - [ ] Add API analytics endpoints
- [ ] Task 2: Create API Analytics Component (AC: AP-10.4.1, AP-10.4.2)
  - [ ] Create components/analytics/api-analytics.tsx
  - [ ] Request volume chart by endpoint
  - [ ] Response time percentile chart (p50, p90, p99)
- [ ] Task 3: Add Error Tracking (AC: AP-10.4.3)
  - [ ] Error rate trend line
  - [ ] Error breakdown by type/endpoint
- [ ] Task 4: Add Consumer Analysis (AC: AP-10.4.4)
  - [ ] Top consumers table/chart
  - [ ] Consumer request patterns
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test API analytics components

## Dev Notes

### Architecture
- Bar charts for endpoint comparison
- Line charts for time-based trends
- Percentile visualization (p50, p90, p95, p99)
- Consumer leaderboard with drill-down

### Dependencies
- Story AP-10.1 (Dashboard page exists)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/analytics/api/endpoints
GET /api/admin/analytics/api/latency
GET /api/admin/analytics/api/errors
GET /api/admin/analytics/api/consumers
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add API analytics types)
- `admin-portal/lib/api-client.ts` (MODIFY - add API analytics endpoints)
- `admin-portal/components/analytics/api-analytics.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-10.4]

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
