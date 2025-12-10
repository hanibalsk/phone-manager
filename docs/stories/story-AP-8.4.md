# Story AP-8.4: Manage Unlock Requests

**Story ID**: AP-8.4
**Epic**: AP-8 - App Usage & Unlock Requests
**Priority**: Medium
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-8.4 (Admin Portal PRD)

---

## Story

As an admin,
I want to manage unlock requests,
so that I can approve device access.

## Acceptance Criteria

### AC AP-8.4.1: Pending Request Queue
**Given** I navigate to unlock requests
**When** the page loads
**Then** I should see pending requests sorted by time

### AC AP-8.4.2: View Request Details
**Given** I am viewing unlock requests
**When** I view a request
**Then** I should see device, user, and reason

### AC AP-8.4.3: Approve or Deny
**Given** I am viewing a request
**When** I take action
**Then** I can approve with duration or deny with note

### AC AP-8.4.4: Request History
**Given** I select a device
**Then** I should see request history for that device

## Tasks / Subtasks

- [x] Task 1: Add Unlock Request Types (AC: AP-8.4.1)
  - [x] Add AdminUnlockRequest type to types/index.ts (added in AP-8.1)
  - [x] Add AdminUnlockRequestStatus type
  - [x] Add unlock requests API endpoints (added in AP-8.1)
- [x] Task 2: Create Unlock Requests Page (AC: AP-8.4.1)
  - [x] Update app/(dashboard)/unlock-requests/page.tsx
- [x] Task 3: Create Request Queue Component (AC: AP-8.4.1, AP-8.4.2)
  - [x] Create components/unlock-requests/unlock-request-queue.tsx
  - [x] Pending requests list with org/device/status filters
  - [x] Request details expandable with full info
  - [x] Auto-approval badge indicator
- [x] Task 4: Create Action Modal (AC: AP-8.4.3)
  - [x] Create components/unlock-requests/request-action-modal.tsx
  - [x] Approve with duration selector (presets + custom)
  - [x] Deny with note input and quick reasons
- [x] Task 5: Create Request History Component (AC: AP-8.4.4)
  - [x] Create components/unlock-requests/request-history.tsx
  - [x] Status filter
  - [x] History cards with stats summary
- [x] Task 6: Create Status Badge Component
  - [x] Create components/unlock-requests/request-status-badge.tsx
- [ ] Task 7: Testing (All ACs) - Deferred
  - [ ] Test unlock request management

## Dev Notes

### Architecture
- Requests sorted by created_at (oldest first for fairness)
- Duration options: 15min, 30min, 1hr, 2hr, custom
- Deny requires reason note
- History shows all actions with timestamps

### Dependencies
- Epic AP-4 (Device Management)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/unlock-requests?status=pending&device_id=...
GET /api/admin/unlock-requests/:id
POST /api/admin/unlock-requests/:id/approve - Body: { duration_minutes }
POST /api/admin/unlock-requests/:id/deny - Body: { note }
GET /api/admin/unlock-requests/history?device_id=...
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add request types)
- `admin-portal/lib/api-client.ts` (MODIFY - add requests API)
- `admin-portal/app/(dashboard)/unlock-requests/page.tsx` (NEW)
- `admin-portal/components/unlock-requests/unlock-request-queue.tsx` (NEW)
- `admin-portal/components/unlock-requests/request-action-modal.tsx` (NEW)
- `admin-portal/components/unlock-requests/request-history.tsx` (NEW)
- `admin-portal/components/unlock-requests/index.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-8.4]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-8: App Usage & Unlock Requests

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- Types and API endpoints added in AP-8.1 for efficiency

### Completion Notes List
- Enhanced unlock request queue with organization/device/status filters
- Expandable request cards with full details (user, device, reason, timestamps)
- Auto-approval badge (Zap icon) for auto-approved requests
- Approve modal with duration presets (15m, 30m, 1h, 2h) + custom option
- Match requested duration shortcut
- Deny modal with quick reasons (Outside allowed hours, Too many requests, etc.)
- Request history component with statistics (total, approved, denied, auto-approved)
- Link to Auto-Approval Rules page (placeholder for AP-8.5)

### File List
- `admin-portal/components/unlock-requests/request-status-badge.tsx` (NEW)
- `admin-portal/components/unlock-requests/unlock-request-queue.tsx` (NEW)
- `admin-portal/components/unlock-requests/request-action-modal.tsx` (NEW)
- `admin-portal/components/unlock-requests/request-history.tsx` (NEW)
- `admin-portal/components/unlock-requests/index.tsx` (MODIFIED)
- `admin-portal/app/(dashboard)/unlock-requests/page.tsx` (MODIFIED)
- `admin-portal/app/(dashboard)/unlock-requests/rules/page.tsx` (NEW - placeholder)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented unlock request management feature |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Epic AP-4 (Device Management)
