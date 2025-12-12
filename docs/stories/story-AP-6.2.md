# Story AP-6.2: Location History Query

**Story ID**: AP-6.2
**Epic**: AP-6 - Location & Geofence Administration
**Priority**: Medium
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-6.2 (Admin Portal PRD)

---

## Story

As an admin,
I want to query location history,
so that I can investigate activity.

## Acceptance Criteria

### AC AP-6.2.1: Query Interface
**Given** I navigate to location history
**When** the page loads
**Then** I should see query controls for device, date range, and geographic bounds

### AC AP-6.2.2: Results Display
**Given** I submit a location query
**When** results are returned
**Then** I should see them displayed on both map and list views

### AC AP-6.2.3: Export Functionality
**Given** I have query results
**When** I click export
**Then** I can download results in CSV, JSON, or GPX format

### AC AP-6.2.4: Result Limits
**Given** a query returns many results
**Then** results should be limited to 10,000 maximum
**And** I should see a warning if limit is reached

## Tasks / Subtasks

- [x] Task 1: Create History Page (AC: AP-6.2.1)
  - [x] Create app/(dashboard)/locations/history/page.tsx
  - [x] Add query form with device, date range
- [x] Task 2: Create Query Results Component (AC: AP-6.2.2)
  - [x] Create components/locations/location-history-list.tsx
  - [x] Display results in table format
  - [x] Show results on map with path visualization
- [x] Task 3: Implement Geographic Bounds (AC: AP-6.2.1)
  - [x] Add SVG-based bounds selection component
  - [x] Draw rectangle for area selection
  - [x] Support manual coordinate input
  - [x] Integrate with location history query
- [x] Task 4: Implement Export (AC: AP-6.2.3)
  - [x] Add export dropdown with format options
  - [x] Implement CSV export
  - [x] Implement JSON export
  - [x] Implement GPX export (waypoints + tracks)
- [x] Task 5: Handle Result Limits (AC: AP-6.2.4)
  - [x] Display warning when limit reached
  - [x] Show total count vs returned count
- [ ] Task 6: Testing (All ACs) - Deferred
  - [ ] Test query functionality
  - [ ] Test export formats

## Dev Notes

### Architecture
- Reuse map component from AP-6.1
- Path visualization using Leaflet polyline
- Client-side export generation
- Paginated API with max 10,000 results

### Dependencies
- Story AP-6.1 (Map component)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/locations/history?device_id=...&from=...&to=...&bbox=...&limit=10000
```

### Files to Create/Modify
- `admin-portal/app/(dashboard)/locations/history/page.tsx` (NEW)
- `admin-portal/components/locations/location-history-list.tsx` (NEW)
- `admin-portal/components/locations/location-query-form.tsx` (NEW)
- `admin-portal/components/locations/export-dropdown.tsx` (NEW)
- `admin-portal/lib/export-utils.ts` (NEW - export helpers)

### References
- [Source: PRD-admin-portal.md - FR-6.2]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-6: Location & Geofence Administration

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- TypeScript check passed without errors

### Completion Notes List
- Created location history query page with device/org/date filters
- Implemented list view with sortable table showing all location fields
- Created map view with device path visualization (colored by device)
- Export functionality: CSV, JSON, and GPX (with waypoints and tracks)
- Result limit warning when 10,000 max reached
- View toggle between list and map modes
- Geographic bounds selector with SVG-based rectangle drawing
- Manual coordinate input for precise bounds specification

### File List
- `admin-portal/app/(dashboard)/locations/history/page.tsx` (NEW, MODIFIED)
- `admin-portal/components/locations/location-history-list.tsx` (NEW)
- `admin-portal/components/locations/location-history-map.tsx` (NEW)
- `admin-portal/components/locations/export-dropdown.tsx` (NEW)
- `admin-portal/components/locations/bounds-selector.tsx` (NEW)
- `admin-portal/lib/export-utils.ts` (NEW)
- `admin-portal/components/locations/index.tsx` (MODIFIED)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implementation complete - core ACs met |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-6.1 (Location Map View)

---

## Senior Developer Review (AI)

**Reviewer**: Martin
**Date**: 2025-12-10
**Outcome**: Approve

### Summary
Story AP-6.2 implements location history querying with export functionality. The implementation provides list and map views for query results, supports CSV/JSON/GPX exports, and properly handles the 10,000 result limit with user warnings.

### Key Findings

| Severity | Finding | Location |
|----------|---------|----------|
| Low | Missing data-testid attributes | Multiple components |
| Info | Query requires org or device selection - good UX safeguard | `page.tsx:59-61` |

### Acceptance Criteria Coverage

| AC | Status | Notes |
|----|--------|-------|
| AP-6.2.1 Query Interface | ✅ Pass | Device, date range, and geographic bounds filters implemented |
| AP-6.2.2 Results Display | ✅ Pass | Map and list views with toggle button |
| AP-6.2.3 Export Functionality | ✅ Pass | CSV, JSON, GPX formats all implemented |
| AP-6.2.4 Result Limits | ✅ Pass | 10,000 limit with warning when truncated |

### Test Coverage and Gaps
- Unit tests deferred per story notes
- TypeScript check passes
- Gap: No tests for export format validation

### Architectural Alignment
- Reuses LocationHistoryMap and LocationHistoryList components
- ExportDropdown provides clean separation of export logic
- Follows established useApi patterns

### Security Notes
- No direct security concerns
- Organization/device filtering prevents unauthorized data access at API level

### Best-Practices and References
- Proper loading states and error handling
- View mode toggle follows established UI patterns
- Export utilities properly handle different format requirements

### Action Items
- [Low] Add data-testid attributes for E2E testing
