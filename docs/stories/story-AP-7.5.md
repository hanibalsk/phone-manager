# Story AP-7.5: Trip Data Export

**Story ID**: AP-7.5
**Epic**: AP-7 - Webhooks & Trips Administration
**Priority**: Medium
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Development
**Created**: 2025-12-10
**PRD Reference**: FR-7.5 (Admin Portal PRD)

---

## Story

As an admin,
I want to export trip data,
so that I can perform offline analysis.

## Acceptance Criteria

### AC AP-7.5.1: Export Filters
**Given** I want to export trips
**When** I configure the export
**Then** I can filter by device, date range, organization

### AC AP-7.5.2: Export Formats
**Given** I am exporting trips
**Then** I can choose CSV or JSON format

### AC AP-7.5.3: Include Path Coordinates
**Given** I am exporting trips
**Then** I can optionally include path coordinates

### AC AP-7.5.4: Async Export for Large Datasets
**Given** I export a large dataset
**Then** the export should be handled asynchronously

## Tasks / Subtasks

- [ ] Task 1: Add Export Types (AC: AP-7.5.1)
  - [ ] Add TripExportRequest type to types/index.ts
  - [ ] Add trip export API endpoint
- [ ] Task 2: Create Export Modal (AC: AP-7.5.1, AP-7.5.2, AP-7.5.3)
  - [ ] Create components/trips/trip-export-modal.tsx
  - [ ] Device/organization selection
  - [ ] Date range picker
  - [ ] Format selection (CSV/JSON)
  - [ ] Include coordinates checkbox
- [ ] Task 3: Implement Export Functionality (AC: AP-7.5.2)
  - [ ] CSV export with trip summary data
  - [ ] JSON export with full trip data
  - [ ] Include path coordinates when selected
- [ ] Task 4: Add Export to Trip List (AC: AP-7.5.1)
  - [ ] Add export button to trips page
  - [ ] Pass current filters to export
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test export functionality
  - [ ] Test different formats

## Dev Notes

### Architecture
- Export runs on client for smaller datasets (<1000 trips)
- Large exports handled via backend job (async) - deferred
- CSV includes: trip_id, device, start_time, end_time, duration, distance, status
- JSON includes full trip object with optional path array

### Dependencies
- Story AP-7.4 (Trip types)
- Existing: export-utils.ts

### API Endpoints (To Add)
```typescript
POST /api/admin/trips/export - For async large exports (future)
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add export types)
- `admin-portal/lib/api-client.ts` (MODIFY - add export endpoint)
- `admin-portal/components/trips/trip-export-modal.tsx` (NEW)
- `admin-portal/components/trips/admin-trip-list.tsx` (MODIFY - add export button)

### References
- [Source: PRD-admin-portal.md - FR-7.5]

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
**Dependencies**: Story AP-7.4 (Trip Data View)
