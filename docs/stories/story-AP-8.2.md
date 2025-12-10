# Story AP-8.2: Configure App Limits

**Story ID**: AP-8.2
**Epic**: AP-8 - App Usage & Unlock Requests
**Priority**: Medium
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-8.2 (Admin Portal PRD)

---

## Story

As an admin,
I want to configure app limits,
so that I can control device usage.

## Acceptance Criteria

### AC AP-8.2.1: Set Time Limits
**Given** I am configuring app limits
**When** I set a limit for an app
**Then** I can set daily/weekly time limits

### AC AP-8.2.2: Configure Time Windows
**Given** I am configuring app limits
**When** I configure allowed hours
**Then** I can set allowed time windows (e.g., 3pm-6pm)

### AC AP-8.2.3: Block Apps
**Given** I am configuring app limits
**When** I want to block an app
**Then** I can block apps completely

### AC AP-8.2.4: Apply to Device/Group
**Given** I am configuring app limits
**When** I save the configuration
**Then** I can apply limits to a device or group

## Tasks / Subtasks

- [x] Task 1: Add App Limit Types (AC: AP-8.2.1)
  - [x] Add AppLimit type to types/index.ts
  - [x] Add TimeWindow type
  - [x] Add app limits API endpoints
- [x] Task 2: Create App Limits Page (AC: AP-8.2.1)
  - [x] Create app/(dashboard)/app-limits/page.tsx
- [x] Task 3: Create App Limits List Component (AC: AP-8.2.1, AP-8.2.3)
  - [x] Create components/app-limits/admin-app-limits-list.tsx
  - [x] Show existing limits with status
  - [x] Delete limit functionality
- [x] Task 4: Create App Limit Form (AC: AP-8.2.1, AP-8.2.2, AP-8.2.3, AP-8.2.4)
  - [x] Create components/app-limits/app-limit-form.tsx
  - [x] App/category selection
  - [x] Time limit inputs (daily/weekly)
  - [x] Time window configuration
  - [x] Block toggle
  - [x] Device/group assignment
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test app limits configuration

## Dev Notes

### Architecture
- Limits can be per-app or per-category
- Time limits in minutes
- Time windows: start_time, end_time (24h format)
- Block mode overrides time limits
- Can apply to single device or entire group

### Dependencies
- Story AP-8.1 (App Usage types)
- Epic AP-5 (Groups Administration)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/app-limits?device_id=...&group_id=...
POST /api/admin/app-limits
PUT /api/admin/app-limits/:id
DELETE /api/admin/app-limits/:id
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add limit types)
- `admin-portal/lib/api-client.ts` (MODIFY - add limits API)
- `admin-portal/app/(dashboard)/app-limits/page.tsx` (NEW)
- `admin-portal/components/app-limits/admin-app-limits-list.tsx` (NEW)
- `admin-portal/components/app-limits/app-limit-form.tsx` (NEW)
- `admin-portal/components/app-limits/index.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-8.2]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-8: App Usage & Unlock Requests

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- Types and API added in AP-8.1

### Completion Notes List
- App limits list with filtering by org/device/group/target type
- Toggle enable/disable functionality
- Delete with confirmation modal
- Full form with time limits, time windows, and block mode
- Quick preset buttons for common time limits
- Device/group assignment with organization filtering

### File List
- `admin-portal/app/(dashboard)/app-limits/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/app-limits/new/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/app-limits/[id]/edit/page.tsx` (NEW)
- `admin-portal/components/app-limits/admin-app-limits-list.tsx` (NEW)
- `admin-portal/components/app-limits/app-limit-form.tsx` (NEW)
- `admin-portal/components/app-limits/limit-type-badge.tsx` (NEW)
- `admin-portal/components/app-limits/index.tsx` (NEW)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented app limits configuration |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-8.1 (App Usage)

---

## Senior Developer Review (AI)

### Reviewer
Martin

### Date
2025-12-10

### Outcome
**Approve**

### Summary
App limits configuration provides comprehensive limit management with filtering by org/device/group, time limits (daily/weekly), time windows, and block mode. Form supports both app-specific and category-level limits with device/group assignment.

### Key Findings

**[None - High Severity]**

**[None - Medium Severity]**

**[Low] Missing data-testid Attributes**
- `admin-app-limits-list.tsx` and `app-limit-form.tsx` lack data-testid attributes
- Add data-testid to: card, table, rows, filters, form inputs, delete modal

### Acceptance Criteria Coverage

| AC | Description | Status | Evidence |
|----|-------------|--------|----------|
| AP-8.2.1 | Set Time Limits | ✅ Pass | Form has daily/weekly limit inputs with quick presets |
| AP-8.2.2 | Configure Time Windows | ✅ Pass | Time window configuration with start/end time and day selection |
| AP-8.2.3 | Block Apps | ✅ Pass | LimitTypeBadge shows "blocked" type with toggle |
| AP-8.2.4 | Apply to Device/Group | ✅ Pass | Device/group selectors with organization filtering |

### Test Coverage and Gaps
- Unit tests deferred to testing sprint (Task 5)
- Components have proper loading, error, and empty states

### Architectural Alignment
- ✅ Follows useApi hook pattern
- ✅ Uses barrel exports via index.tsx
- ✅ Proper component separation (list, form, type badge)
- ✅ Reuses AppCategoryBadge from AP-8.1
- ✅ Delete confirmation modal pattern

### Security Notes
- Organization-scoped limits prevent cross-org access
- Delete confirmation prevents accidental deletion

### Best-Practices and References
- Quick preset buttons for common time limits improve UX
- Client-side search with debounce
- Cascading filters (org → device/group)

### Action Items
- [ ] [AI-Review][Low] Add data-testid attributes for E2E testing (AC: All)
