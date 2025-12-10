# Story AP-7.4: Trip Data View

**Story ID**: AP-7.4
**Epic**: AP-7 - Webhooks & Trips Administration
**Priority**: Medium
**Estimate**: 4 story points (3-4 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-7.4 (Admin Portal PRD)

---

## Story

As an admin,
I want to view trip data,
so that I can analyze movement patterns.

## Acceptance Criteria

### AC AP-7.4.1: Trip List
**Given** I navigate to trips
**When** the page loads
**Then** I should see a trip list with device, status, duration, distance

### AC AP-7.4.2: Trip Detail Map
**Given** I select a trip
**When** the detail view loads
**Then** I should see an interactive map with the trip path

### AC AP-7.4.3: Path Visualization
**Given** I am viewing a trip detail
**Then** I should see the path with start/end markers

### AC AP-7.4.4: Movement Events
**Given** I am viewing a trip detail
**Then** I should see a movement event timeline

## Tasks / Subtasks

- [x] Task 1: Add Trip Types (AC: AP-7.4.1)
  - [x] Add Trip type to types/index.ts
  - [x] Add TripStatus enum (in_progress, completed, paused)
  - [x] Add TripPoint type for path coordinates
  - [x] Add admin trips API endpoints
- [x] Task 2: Create Trips Page (AC: AP-7.4.1)
  - [x] Create app/(dashboard)/trips/page.tsx
  - [x] List trips with filters
- [x] Task 3: Create Trip List Component (AC: AP-7.4.1)
  - [x] Create components/trips/admin-trip-list.tsx
  - [x] Display columns: device, status, start time, duration, distance
  - [x] Add trip status badge
  - [x] Filter by device, organization, date range, status
- [x] Task 4: Create Trip Detail Page (AC: AP-7.4.2, AP-7.4.3)
  - [x] Create app/(dashboard)/trips/[id]/page.tsx
  - [x] Show trip summary (device, times, distance)
  - [x] CSS-based path visualization
- [x] Task 5: Trip Map Component (AC: AP-7.4.2, AP-7.4.3)
  - [x] Create components/trips/trip-map.tsx
  - [x] Display path as connected line
  - [x] Start marker (green), end marker (red)
  - [x] Speed-based gradient coloring
- [x] Task 6: Movement Timeline (AC: AP-7.4.4)
  - [x] Create components/trips/trip-timeline.tsx
  - [x] Display movement events (start, stop, speed changes)
  - [x] Timestamp and location for each event
- [ ] Task 7: Testing (All ACs) - Deferred
  - [ ] Test trip list
  - [ ] Test trip detail view

## Dev Notes

### Architecture
- Trips represent continuous movement sessions
- Trip points are GPS coordinates with timestamps
- Movement events: trip_start, stop_detected, resumed, trip_end
- Calculate duration and distance from trip points

### Dependencies
- Existing: shadcn/ui components, useApi hook, api-client

### API Endpoints (To Add)
```typescript
GET /api/admin/trips?device_id=...&org_id=...&status=...&from=...&to=...
GET /api/admin/trips/:id
GET /api/admin/trips/:id/path - Returns trip points
GET /api/admin/trips/:id/events - Returns movement events
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add trip types)
- `admin-portal/lib/api-client.ts` (MODIFY - add trips API)
- `admin-portal/app/(dashboard)/trips/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/trips/[id]/page.tsx` (NEW)
- `admin-portal/components/trips/admin-trip-list.tsx` (NEW)
- `admin-portal/components/trips/trip-status-badge.tsx` (NEW)
- `admin-portal/components/trips/trip-map.tsx` (NEW)
- `admin-portal/components/trips/trip-timeline.tsx` (NEW)
- `admin-portal/components/trips/index.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-7.4]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-7: Webhooks & Trips Administration

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- Trip types added in Story AP-7.1 (types already existed)
- tripsApi added in Story AP-7.1 (already existed)

### Completion Notes List
- Created trip list with org/device/status/date filters
- Trip status badge with in_progress/completed/paused states
- SVG-based trip map with path visualization
- Speed-based gradient coloring (green=slow, red=fast)
- Start/end markers on map
- Movement timeline grouped by date
- Event icons for trip_start, stop_detected, resumed, trip_end
- Summary cards showing duration, distance, device, organization
- Button-based view switcher (Map/Timeline) instead of tabs

### File List
- `admin-portal/components/trips/admin-trip-list.tsx` (NEW)
- `admin-portal/components/trips/trip-status-badge.tsx` (NEW)
- `admin-portal/components/trips/trip-map.tsx` (NEW)
- `admin-portal/components/trips/trip-timeline.tsx` (NEW)
- `admin-portal/components/trips/index.tsx` (NEW)
- `admin-portal/app/(dashboard)/trips/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/trips/[id]/page.tsx` (NEW)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented trip list, detail view, map, and timeline |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-7.1 (Webhook List - includes Trip types)
