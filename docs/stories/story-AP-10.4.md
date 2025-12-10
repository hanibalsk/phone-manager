# Story AP-10.4: API Analytics

**Story ID**: AP-10.4
**Epic**: AP-10 - Dashboard & Analytics
**Priority**: High
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
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

- [x] Task 1: Add API Analytics Types (AC: AP-10.4.1)
  - [x] Add ApiAnalytics type to types/index.ts (done in AP-10.1)
  - [x] Add EndpointMetrics type (done in AP-10.1)
  - [x] Add ResponseTimeData type (done in AP-10.1)
  - [x] Add API analytics endpoints (done in AP-10.1)
- [x] Task 2: Create API Analytics Page (AC: AP-10.4.1, AP-10.4.2)
  - [x] Create app/(dashboard)/analytics/api/page.tsx
  - [x] Summary cards (total requests, avg response, error rate, p50 latency)
  - [x] Response time percentile chart (P50, P90, P95, P99)
- [x] Task 3: Add Error Tracking (AC: AP-10.4.3)
  - [x] Error rate trend chart
  - [x] Error rate by endpoint in metrics table
  - [x] Color-coded error badges
- [x] Task 4: Add Consumer Analysis (AC: AP-10.4.4)
  - [x] Top consumers list with progress bars
  - [x] Consumer type icons and badges
  - [x] Endpoint metrics table with method, requests, response time, errors
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
- Types and API endpoints already added in AP-10.1

### Completion Notes List
- Implemented comprehensive API analytics page with:
  - Time period selector (7d, 30d, 90d)
  - Summary cards (total requests, avg response time, error rate, P50 latency)
  - Response time percentile chart (P50, P90, P95, P99) with multi-line SVG
  - Error rate trend chart with area fill
  - Top API consumers list with progress bars and type icons
  - Endpoint metrics table with method badges, request count, response time, errors
  - Color-coded badges and text for performance indicators
  - Quick stats section
  - Refresh button for real-time updates

### File List
- `admin-portal/app/(dashboard)/analytics/api/page.tsx` (NEW - ~520 lines)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented API analytics page (Tasks 1-4 complete) |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-10.1 (Overview Dashboard)
