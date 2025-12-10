# Story AP-10.5: Custom Reports

**Story ID**: AP-10.5
**Epic**: AP-10 - Dashboard & Analytics
**Priority**: High
**Estimate**: 4 story points (3-4 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-10.5 (Admin Portal PRD)

---

## Story

As an admin,
I want to generate custom reports,
so that I can analyze specific metrics.

## Acceptance Criteria

### AC AP-10.5.1: Report Builder
**Given** I am an admin creating a report
**When** I use the report builder
**Then** I can select metrics to include

### AC AP-10.5.2: Date Range and Filters
**Given** I am building a report
**When** I configure parameters
**Then** I can set date range and filter options

### AC AP-10.5.3: Export Options
**Given** I have generated a report
**When** I want to share it
**Then** I can export as PDF or CSV

### AC AP-10.5.4: Saved Configurations
**Given** I frequently run the same report
**When** I configure a report
**Then** I can save report configurations for reuse

## Tasks / Subtasks

- [x] Task 1: Add Report Types (AC: AP-10.5.1)
  - [x] Add ReportConfig type to types/index.ts (done in AP-10.1)
  - [x] Add ReportMetric type (done in AP-10.1)
  - [x] Add SavedReport type (done in AP-10.1)
  - [x] Add report API endpoints (done in AP-10.1)
- [x] Task 2: Create Report Builder (AC: AP-10.5.1, AP-10.5.2)
  - [x] Create app/(dashboard)/reports/page.tsx with ReportBuilder component
  - [x] Metric selection interface with badges and aggregation types
  - [x] Date range picker with quick presets (7d, 30d, 90d, 1y)
  - [x] Filter configuration with operators (eq, ne, gt, lt, contains)
  - [x] Group by selector (day, week, month, organization, platform)
- [x] Task 3: Add Report Preview (AC: AP-10.5.1)
  - [x] Report results display with metric cards
  - [x] Data table for raw results
- [x] Task 4: Add Export Functionality (AC: AP-10.5.3)
  - [x] PDF export button with API call
  - [x] CSV export button with API call
- [x] Task 5: Add Saved Reports (AC: AP-10.5.4)
  - [x] Save report configuration with name and description
  - [x] Saved reports list with SavedReportCard component
  - [x] Run saved report functionality
  - [x] Delete saved report functionality
- [ ] Task 6: Testing (All ACs) - Deferred
  - [ ] Test report builder components

## Dev Notes

### Architecture
- Multi-step report builder wizard
- Available metrics: users, devices, locations, API calls, errors
- Filter by: organization, time range, status
- Saved reports stored with user preferences

### Dependencies
- Stories AP-10.1 through AP-10.4 (analytics data available)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
POST /api/admin/reports/generate
GET /api/admin/reports/saved
POST /api/admin/reports/saved
PUT /api/admin/reports/saved/:id
DELETE /api/admin/reports/saved/:id
GET /api/admin/reports/:id/export?format=pdf|csv
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add report types)
- `admin-portal/lib/api-client.ts` (MODIFY - add reports API)
- `admin-portal/components/reports/report-builder.tsx` (NEW)
- `admin-portal/components/reports/report-preview.tsx` (NEW)
- `admin-portal/components/reports/saved-reports.tsx` (NEW)
- `admin-portal/components/reports/index.tsx` (NEW)
- `admin-portal/app/(dashboard)/reports/page.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-10.5]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-10: Dashboard & Analytics

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- Types and API endpoints already added in AP-10.1

### Completion Notes List
- Implemented comprehensive custom reports page with:
  - Report builder component with multi-step configuration
  - Metric selection from 7 available types (users, devices, orgs, locations, API calls, errors, retention)
  - Aggregation type selector (count, sum, average, min, max)
  - Date range picker with quick presets (7d, 30d, 90d, 1y)
  - Filter configuration with 7 operators (eq, ne, gt, lt, gte, lte, contains)
  - Group by selector (day, week, month, organization, platform)
  - Report results display with metric cards and data table
  - Saved reports list with SavedReportCard component
  - Run, export (PDF/CSV), and delete functionality for saved reports
  - Responsive design with proper loading states

### File List
- `admin-portal/app/(dashboard)/reports/page.tsx` (NEW - ~585 lines)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented custom reports page (Tasks 1-5 complete) |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Stories AP-10.1 through AP-10.4
