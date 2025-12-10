# Story AP-7.1: Webhook List

**Story ID**: AP-7.1
**Epic**: AP-7 - Webhooks & Trips Administration
**Priority**: Medium
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-7.1 (Admin Portal PRD)

---

## Story

As an admin,
I want to view all webhooks,
so that I can monitor integrations.

## Acceptance Criteria

### AC AP-7.1.1: Webhook List Display
**Given** I navigate to webhooks
**When** the page loads
**Then** I should see all webhooks with status indicators

### AC AP-7.1.2: Filter Webhooks
**Given** I am viewing webhooks
**When** I use the filter controls
**Then** I can filter by organization, status, event types

### AC AP-7.1.3: Delivery Statistics
**Given** I am viewing webhooks
**Then** I should see delivery success/failure counts for each webhook

### AC AP-7.1.4: Search Webhooks
**Given** I am viewing webhooks
**When** I use the search
**Then** I can search by URL or name

## Tasks / Subtasks

- [x] Task 1: Add Webhook Types (AC: AP-7.1.1)
  - [x] Add Webhook type to types/index.ts
  - [x] Add WebhookEvent enum (location_update, geofence_event, etc.)
  - [x] Add WebhookStatus enum (active, paused, failed)
  - [x] Add admin webhooks API endpoints
- [x] Task 2: Create Webhooks Page (AC: AP-7.1.1)
  - [x] Create app/(dashboard)/webhooks/page.tsx
  - [x] List webhooks with status indicators
- [x] Task 3: Create Webhook List Component (AC: AP-7.1.1, AP-7.1.3)
  - [x] Create components/webhooks/admin-webhook-list.tsx
  - [x] Display columns: name, URL, status, events, success/failure counts
  - [x] Add status badge component
- [x] Task 4: Filtering and Search (AC: AP-7.1.2, AP-7.1.4)
  - [x] Add organization filter
  - [x] Add status filter
  - [x] Add event type filter
  - [x] Add search by URL or name
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test webhook listing
  - [ ] Test filtering

## Dev Notes

### Architecture
- Webhooks are organization-scoped
- Event types: location_update, geofence_event, proximity_alert, trip_complete, device_status
- Status: active (receiving events), paused (not sending), failed (error threshold exceeded)
- Track success/failure counts and last delivery timestamp

### Dependencies
- Existing: shadcn/ui components, useApi hook, api-client

### API Endpoints (To Add)
```typescript
GET /api/admin/webhooks?org_id=...&status=...&event_type=...&search=...
GET /api/admin/webhooks/:id
POST /api/admin/webhooks
PUT /api/admin/webhooks/:id
DELETE /api/admin/webhooks/:id
PATCH /api/admin/webhooks/:id/toggle - Enable/disable
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add webhook types)
- `admin-portal/lib/api-client.ts` (MODIFY - add webhooks API)
- `admin-portal/app/(dashboard)/webhooks/page.tsx` (NEW)
- `admin-portal/components/webhooks/admin-webhook-list.tsx` (NEW)
- `admin-portal/components/webhooks/webhook-status-badge.tsx` (NEW)
- `admin-portal/components/webhooks/index.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-7.1]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-7: Webhooks & Trips Administration

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- Added all Epic AP-7 types for webhooks and trips in a single batch
- Created webhooksApi and tripsApi in api-client.ts

### Completion Notes List
- Implemented webhook list with status indicators (active/paused/failed)
- Added filtering by organization, status, and event types
- Added search by URL or name with debounced input
- Display success/failure delivery counts per webhook
- Added webhook event type badges with icons
- Included toggle and delete actions with confirmation modal

### File List
- `admin-portal/types/index.ts` (MODIFIED - added Webhook, WebhookDelivery, Trip types)
- `admin-portal/lib/api-client.ts` (MODIFIED - added webhooksApi, tripsApi)
- `admin-portal/app/(dashboard)/webhooks/page.tsx` (NEW)
- `admin-portal/components/webhooks/admin-webhook-list.tsx` (NEW)
- `admin-portal/components/webhooks/webhook-status-badge.tsx` (NEW)
- `admin-portal/components/webhooks/webhook-event-badge.tsx` (NEW)
- `admin-portal/components/webhooks/index.tsx` (NEW)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented webhook list, filtering, search, delivery stats |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Epic AP-1 (RBAC), Epic AP-4 (Device Management)
