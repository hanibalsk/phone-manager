# Story AP-8.1: App Usage Statistics

**Story ID**: AP-8.1
**Epic**: AP-8 - App Usage & Unlock Requests
**Priority**: Medium
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-8.1 (Admin Portal PRD)

---

## Story

As an admin,
I want to view app usage statistics,
so that I can understand device activity.

## Acceptance Criteria

### AC AP-8.1.1: Aggregated Usage by Category
**Given** I navigate to app usage
**When** the page loads
**Then** I should see aggregated usage by app category

### AC AP-8.1.2: Per-Device Breakdown
**Given** I am viewing app usage
**When** I select a device
**Then** I should see usage breakdown for that device

### AC AP-8.1.3: Time-Based Charts
**Given** I am viewing app usage
**Then** I should see time-based usage charts (daily, weekly)

### AC AP-8.1.4: Top Apps by Usage
**Given** I am viewing app usage
**Then** I should see top apps ranked by usage time

## Tasks / Subtasks

- [x] Task 1: Add App Usage Types (AC: AP-8.1.1)
  - [x] Add AppUsageStat type to types/index.ts
  - [x] Add AppCategory enum
  - [x] Add app usage API endpoint to api-client.ts
- [x] Task 2: Create App Usage Page (AC: AP-8.1.1)
  - [x] Create app/(dashboard)/app-usage/page.tsx
- [x] Task 3: Create Usage Statistics Component (AC: AP-8.1.1, AP-8.1.4)
  - [x] Create components/app-usage/admin-app-usage.tsx
  - [x] Category aggregation cards
  - [x] Top apps list
- [x] Task 4: Device Breakdown Component (AC: AP-8.1.2)
  - [x] Create components/app-usage/device-usage-breakdown.tsx
  - [x] Device selector
  - [x] Per-app usage table
- [x] Task 5: Usage Charts Component (AC: AP-8.1.3)
  - [x] Create components/app-usage/usage-chart.tsx
  - [x] Daily/weekly toggle
  - [x] CSS-based bar chart visualization
- [ ] Task 6: Testing (All ACs) - Deferred
  - [ ] Test app usage page

## Dev Notes

### Architecture
- App usage data aggregated from device reports
- Categories: social, games, productivity, entertainment, other
- Charts rendered with CSS (no external charting library)
- Time filters: today, 7 days, 30 days

### Dependencies
- Epic AP-4 (Device Management)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/app-usage?device_id=...&org_id=...&from=...&to=...
GET /api/admin/app-usage/categories - Aggregated by category
GET /api/admin/app-usage/top-apps - Top apps by usage
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add usage types)
- `admin-portal/lib/api-client.ts` (MODIFY - add usage API)
- `admin-portal/app/(dashboard)/app-usage/page.tsx` (NEW)
- `admin-portal/components/app-usage/admin-app-usage.tsx` (NEW)
- `admin-portal/components/app-usage/device-usage-breakdown.tsx` (NEW)
- `admin-portal/components/app-usage/usage-chart.tsx` (NEW)
- `admin-portal/components/app-usage/index.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-8.1]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-8: App Usage & Unlock Requests

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- Added Epic AP-8 types to types/index.ts (all 5 stories)
- Added Epic AP-8 API endpoints to api-client.ts (all 5 stories)

### Completion Notes List
- Implemented app usage statistics with category aggregation
- Added CSS-based bar charts (no external charting library)
- Device breakdown with expandable app details
- Top apps ranked by usage time
- Time period filters: Today, 7 Days, 30 Days

### File List
- `admin-portal/types/index.ts` (MODIFIED - added Epic AP-8 types)
- `admin-portal/lib/api-client.ts` (MODIFIED - added Epic AP-8 APIs)
- `admin-portal/app/(dashboard)/app-usage/page.tsx` (NEW)
- `admin-portal/components/app-usage/admin-app-usage.tsx` (NEW)
- `admin-portal/components/app-usage/device-usage-breakdown.tsx` (NEW)
- `admin-portal/components/app-usage/usage-chart.tsx` (NEW)
- `admin-portal/components/app-usage/app-category-badge.tsx` (NEW)
- `admin-portal/components/app-usage/index.tsx` (NEW)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented app usage statistics feature |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Epic AP-4 (Device Management)
