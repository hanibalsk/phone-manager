# Story AP-8.4: Manage Unlock Requests

**Story ID**: AP-8.4
**Epic**: AP-8 - App Usage & Unlock Requests
**Priority**: Medium
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Development
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

- [ ] Task 1: Add Unlock Request Types (AC: AP-8.4.1)
  - [ ] Add UnlockRequest type to types/index.ts
  - [ ] Add RequestStatus enum
  - [ ] Add unlock requests API endpoints
- [ ] Task 2: Create Unlock Requests Page (AC: AP-8.4.1)
  - [ ] Create app/(dashboard)/unlock-requests/page.tsx
- [ ] Task 3: Create Request Queue Component (AC: AP-8.4.1, AP-8.4.2)
  - [ ] Create components/unlock-requests/unlock-request-queue.tsx
  - [ ] Pending requests list
  - [ ] Request details expandable
- [ ] Task 4: Create Action Modal (AC: AP-8.4.3)
  - [ ] Create components/unlock-requests/request-action-modal.tsx
  - [ ] Approve with duration selector
  - [ ] Deny with note input
- [ ] Task 5: Create Request History Component (AC: AP-8.4.4)
  - [ ] Create components/unlock-requests/request-history.tsx
  - [ ] Device selector
  - [ ] History table with status
- [ ] Task 6: Testing (All ACs) - Deferred
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
