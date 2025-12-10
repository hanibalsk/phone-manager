# Story AP-10.3: Device Analytics

**Story ID**: AP-10.3
**Epic**: AP-10 - Dashboard & Analytics
**Priority**: High
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Development
**Created**: 2025-12-10
**PRD Reference**: FR-10.3 (Admin Portal PRD)

---

## Story

As an admin,
I want device analytics,
so that I can monitor fleet health.

## Acceptance Criteria

### AC AP-10.3.1: Device Distribution
**Given** I am an admin viewing device analytics
**When** I view distribution charts
**Then** I can see device distribution by platform, status

### AC AP-10.3.2: Online/Offline Counts
**Given** I am viewing device analytics
**When** I check connectivity
**Then** I can see online/offline device counts over time

### AC AP-10.3.3: Location Upload Volume
**Given** I am viewing device analytics
**When** I check data volume
**Then** I can see location upload volume

### AC AP-10.3.4: Device Activity Heatmap
**Given** I am viewing device analytics
**When** I analyze activity patterns
**Then** I can see a device activity heatmap

## Tasks / Subtasks

- [ ] Task 1: Add Device Analytics Types (AC: AP-10.3.1)
  - [ ] Add DeviceAnalytics type to types/index.ts
  - [ ] Add DeviceDistribution type
  - [ ] Add LocationVolumeData type
  - [ ] Add device analytics API endpoints
- [ ] Task 2: Create Device Analytics Component (AC: AP-10.3.1, AP-10.3.2)
  - [ ] Create components/analytics/device-analytics.tsx
  - [ ] Platform distribution pie/bar chart
  - [ ] Status distribution chart
  - [ ] Online/offline trend line
- [ ] Task 3: Add Volume Metrics (AC: AP-10.3.3)
  - [ ] Location upload volume chart
  - [ ] Data throughput indicators
- [ ] Task 4: Add Activity Heatmap (AC: AP-10.3.4)
  - [ ] Device activity heatmap by hour/day
  - [ ] Peak activity indicators
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test device analytics components

## Dev Notes

### Architecture
- Pie charts for distribution
- Line charts for trends over time
- Heatmap grid for activity patterns
- Real-time refresh option

### Dependencies
- Story AP-10.1 (Dashboard page exists)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/analytics/devices/distribution
GET /api/admin/analytics/devices/connectivity
GET /api/admin/analytics/devices/volume
GET /api/admin/analytics/devices/heatmap
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add device analytics types)
- `admin-portal/lib/api-client.ts` (MODIFY - add device analytics API)
- `admin-portal/components/analytics/device-analytics.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-10.3]

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
