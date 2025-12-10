# Story AP-11.2: Audit Log Search

**Story ID**: AP-11.2
**Epic**: AP-11 - Audit & Compliance
**Priority**: High
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-13.2 (Admin Portal PRD)

---

## Story

As an admin,
I want to search audit logs,
so that I can investigate incidents.

## Acceptance Criteria

### AC AP-11.2.1: Search Capability
**Given** I am an admin viewing audit logs
**When** I search for entries
**Then** I can search by actor, action, resource, date range

### AC AP-11.2.2: Filter Options
**Given** I am searching audit logs
**When** I apply filters
**Then** I can filter by organization and action type

### AC AP-11.2.3: Detail View
**Given** I find relevant audit entries
**When** I select an entry
**Then** I see full detail view with all metadata

### AC AP-11.2.4: Export Results
**Given** I have search results
**When** I want to save them
**Then** I can export search results

## Tasks / Subtasks

- [ ] Task 1: Add Search Functionality
  - [ ] Search input component
  - [ ] Search by actor, action, resource
  - [ ] Date range picker for search
- [ ] Task 2: Add Advanced Filters
  - [ ] Organization filter dropdown
  - [ ] Action type filter
  - [ ] Combined filter state management
- [ ] Task 3: Add Detail View
  - [ ] Log entry detail panel/modal
  - [ ] Full metadata display
  - [ ] Related entries linking
- [ ] Task 4: Add Export
  - [ ] Export to CSV
  - [ ] Export to JSON
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test search functionality

## Dev Notes

### Architecture
- Debounced search input
- Multi-select filters
- Detail slide-over panel
- Export as CSV or JSON

### Dependencies
- Story AP-11.1 (Audit logging exists)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/audit/logs/search
GET /api/admin/audit/logs/export
```

### Files to Create/Modify
- `admin-portal/app/(dashboard)/audit/page.tsx` (MODIFY - add search)
- `admin-portal/lib/api-client.ts` (MODIFY - add search endpoints)

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

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-11.1
