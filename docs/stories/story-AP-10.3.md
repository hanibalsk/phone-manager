# Story AP-10.3: Device Analytics

**Story ID**: AP-10.3
**Epic**: AP-10 - Dashboard & Analytics
**Priority**: High
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
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

- [x] Task 1: Add Device Analytics Types (AC: AP-10.3.1)
  - [x] Add DeviceAnalytics type to types/index.ts (done in AP-10.1)
  - [x] Add DeviceDistribution type (done in AP-10.1)
  - [x] Add LocationVolumeData type (done in AP-10.1)
  - [x] Add device analytics API endpoints (done in AP-10.1)
- [x] Task 2: Create Device Analytics Page (AC: AP-10.3.1, AP-10.3.2)
  - [x] Create app/(dashboard)/analytics/devices/page.tsx
  - [x] Platform distribution pie chart
  - [x] Status distribution bar chart with progress bars
  - [x] Online/offline connectivity trend line chart
- [x] Task 3: Add Volume Metrics (AC: AP-10.3.3)
  - [x] Location upload volume chart
  - [x] Data points trend chart
  - [x] Summary cards with upload totals
- [x] Task 4: Add Activity Heatmap (AC: AP-10.3.4)
  - [x] Device activity heatmap by hour/day of week
  - [x] Color-coded intensity (green gradient)
  - [x] Quick stats with peak online, avg uploads/day
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
- Types and API endpoints already added in AP-10.1

### Completion Notes List
- Implemented comprehensive device analytics page with:
  - Time period selector (7d, 30d, 90d)
  - Summary cards (total devices, online, offline, location uploads)
  - SVG pie chart for platform distribution
  - Status distribution bar chart with progress bars
  - Generic line chart component for connectivity and volume trends
  - Activity heatmap showing 24h x 7 days grid with color intensity
  - Quick stats section with avg uploads/day, peak online, platform count
  - Refresh button for real-time updates

### File List
- `admin-portal/app/(dashboard)/analytics/devices/page.tsx` (NEW - ~490 lines)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented device analytics page (Tasks 1-4 complete) |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-10.1 (Overview Dashboard)
