# Story AP-7.4: Trip Data View

**Story ID**: AP-7.4
**Epic**: AP-7 - Webhooks & Trips Administration
**Priority**: Medium
**Estimate**: 4 story points (3-4 days)
**Status**: Ready for Development
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

- [ ] Task 1: Add Trip Types (AC: AP-7.4.1)
  - [ ] Add Trip type to types/index.ts
  - [ ] Add TripStatus enum (in_progress, completed, paused)
  - [ ] Add TripPoint type for path coordinates
  - [ ] Add admin trips API endpoints
- [ ] Task 2: Create Trips Page (AC: AP-7.4.1)
  - [ ] Create app/(dashboard)/trips/page.tsx
  - [ ] List trips with filters
- [ ] Task 3: Create Trip List Component (AC: AP-7.4.1)
  - [ ] Create components/trips/admin-trip-list.tsx
  - [ ] Display columns: device, status, start time, duration, distance
  - [ ] Add trip status badge
  - [ ] Filter by device, organization, date range, status
- [ ] Task 4: Create Trip Detail Page (AC: AP-7.4.2, AP-7.4.3)
  - [ ] Create app/(dashboard)/trips/[id]/page.tsx
  - [ ] Show trip summary (device, times, distance)
  - [ ] CSS-based path visualization
- [ ] Task 5: Trip Map Component (AC: AP-7.4.2, AP-7.4.3)
  - [ ] Create components/trips/trip-map.tsx
  - [ ] Display path as connected line
  - [ ] Start marker (green), end marker (red)
  - [ ] Show intermediate stops
- [ ] Task 6: Movement Timeline (AC: AP-7.4.4)
  - [ ] Create components/trips/trip-timeline.tsx
  - [ ] Display movement events (start, stop, speed changes)
  - [ ] Timestamp and location for each event
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
**Dependencies**: Epic AP-4 (Device Management)
