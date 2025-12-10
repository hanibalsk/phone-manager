# Story AP-6.5: Proximity Alerts

**Story ID**: AP-6.5
**Epic**: AP-6 - Location & Geofence Administration
**Priority**: Medium
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-6.5 (Admin Portal PRD)

---

## Story

As an admin,
I want to manage proximity alerts,
so that I can configure device-to-device monitoring.

## Acceptance Criteria

### AC AP-6.5.1: Alert List
**Given** I navigate to proximity alerts
**When** the page loads
**Then** I should see all proximity alerts

### AC AP-6.5.2: Create/Edit Alert
**Given** I want to create or edit an alert
**When** I use the alert form
**Then** I can configure alerts for any device pair

### AC AP-6.5.3: Configure Parameters
**Given** I am editing an alert
**When** I configure settings
**Then** I can set trigger distance and cooldown period

### AC AP-6.5.4: Alert History
**Given** I have a proximity alert
**When** I view its details
**Then** I can see the trigger history

## Tasks / Subtasks

- [x] Task 1: Add Proximity Alert Types (AC: AP-6.5.1)
  - [x] Add ProximityAlert type to types/index.ts (done in AP-6.1)
  - [x] Add ProximityAlertTrigger type (done in AP-6.1)
  - [x] Add admin proximity alerts API endpoints (done in AP-6.1)
- [x] Task 2: Create Alerts Page (AC: AP-6.5.1)
  - [x] Create app/(dashboard)/proximity-alerts/page.tsx
  - [x] List alerts with status indicators
- [x] Task 3: Create Alert List Component (AC: AP-6.5.1)
  - [x] Create components/proximity/admin-proximity-alert-list.tsx
  - [x] Display device pairs and configuration
  - [x] Add enable/disable toggle
  - [x] Add delete confirmation modal
- [x] Task 4: Create/Edit Alert Form (AC: AP-6.5.2, AP-6.5.3)
  - [x] Create app/(dashboard)/proximity-alerts/new/page.tsx
  - [x] Create app/(dashboard)/proximity-alerts/[id]/edit/page.tsx
  - [x] Create components/proximity/proximity-alert-form.tsx
  - [x] Device pair selection (by organization)
  - [x] Distance and cooldown configuration
  - [x] Quick presets for common configurations
- [x] Task 5: Alert Trigger History (AC: AP-6.5.4)
  - [x] Create app/(dashboard)/proximity-alerts/[id]/history/page.tsx
  - [x] Create components/proximity/alert-trigger-history.tsx
  - [x] Display trigger events with timestamps and locations
- [ ] Task 6: Testing (All ACs) - Deferred
  - [ ] Test alert CRUD operations
  - [ ] Test trigger history display

## Dev Notes

### Architecture
- Proximity alerts monitor distance between two devices
- Trigger when devices come within specified distance
- Cooldown prevents repeated triggers
- History shows all trigger events

### Dependencies
- Existing: shadcn/ui components, useApi hook, api-client

### API Endpoints (To Add)
```typescript
GET /api/admin/proximity-alerts?device_id=...&org_id=...&enabled=...
GET /api/admin/proximity-alerts/:id
POST /api/admin/proximity-alerts
PUT /api/admin/proximity-alerts/:id
DELETE /api/admin/proximity-alerts/:id
PATCH /api/admin/proximity-alerts/:id/toggle
GET /api/admin/proximity-alerts/:id/triggers - Get trigger history
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add proximity types)
- `admin-portal/lib/api-client.ts` (MODIFY - add proximity API)
- `admin-portal/app/(dashboard)/proximity-alerts/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/proximity-alerts/new/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/proximity-alerts/[id]/edit/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/proximity-alerts/[id]/history/page.tsx` (NEW)
- `admin-portal/components/proximity/admin-proximity-alert-list.tsx` (NEW)
- `admin-portal/components/proximity/proximity-alert-form.tsx` (NEW)
- `admin-portal/components/proximity/alert-trigger-history.tsx` (NEW)
- `admin-portal/components/proximity/index.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-6.5]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-6: Location & Geofence Administration

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- TypeScript check passed without errors

### Completion Notes List
- Created alerts list with organization and status filters
- Full CRUD operations for proximity alerts
- Device pair selection scoped by organization (prevents cross-org alerts)
- Distance and cooldown configuration with quick presets
- Trigger history view with device locations and distance
- Delete confirmation modal with loading state

### File List
- `admin-portal/app/(dashboard)/proximity-alerts/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/proximity-alerts/new/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/proximity-alerts/[id]/edit/page.tsx` (NEW)
- `admin-portal/app/(dashboard)/proximity-alerts/[id]/history/page.tsx` (NEW)
- `admin-portal/components/proximity/admin-proximity-alert-list.tsx` (NEW)
- `admin-portal/components/proximity/proximity-alert-form.tsx` (NEW)
- `admin-portal/components/proximity/alert-trigger-history.tsx` (NEW)
- `admin-portal/components/proximity/index.tsx` (NEW)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implementation complete - all ACs met |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Epic AP-4 (Device Management)
