# Story AP-7.2: Webhook Delivery Logs

**Story ID**: AP-7.2
**Epic**: AP-7 - Webhooks & Trips Administration
**Priority**: Medium
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Development
**Created**: 2025-12-10
**PRD Reference**: FR-7.2 (Admin Portal PRD)

---

## Story

As an admin,
I want to view webhook delivery logs,
so that I can troubleshoot failures.

## Acceptance Criteria

### AC AP-7.2.1: Delivery Log Display
**Given** I navigate to webhook delivery logs
**When** the page loads
**Then** I should see a log with request/response details

### AC AP-7.2.2: Filter by Status
**Given** I am viewing delivery logs
**When** I filter by status
**Then** I can filter by success or failed deliveries

### AC AP-7.2.3: Retry Information
**Given** I am viewing a delivery log entry
**Then** I should see retry attempts and error details

### AC AP-7.2.4: Resend Failed Deliveries
**Given** I am viewing a failed delivery
**When** I click resend
**Then** the delivery should be retried

## Tasks / Subtasks

- [ ] Task 1: Add Delivery Log Types (AC: AP-7.2.1)
  - [ ] Add WebhookDelivery type to types/index.ts
  - [ ] Add delivery status types
  - [ ] Add admin webhook deliveries API endpoints
- [ ] Task 2: Create Delivery Logs Page (AC: AP-7.2.1)
  - [ ] Create app/(dashboard)/webhooks/[id]/deliveries/page.tsx
  - [ ] Display delivery history with pagination
- [ ] Task 3: Create Delivery Log Component (AC: AP-7.2.1, AP-7.2.3)
  - [ ] Create components/webhooks/webhook-delivery-log.tsx
  - [ ] Display columns: timestamp, status, response code, duration
  - [ ] Show retry count and error messages
- [ ] Task 4: Delivery Details Modal (AC: AP-7.2.1, AP-7.2.3)
  - [ ] Show full request payload
  - [ ] Show response body
  - [ ] Show headers
- [ ] Task 5: Resend Functionality (AC: AP-7.2.4)
  - [ ] Add resend button for failed deliveries
  - [ ] Confirmation dialog
  - [ ] Show resend result
- [ ] Task 6: Testing (All ACs) - Deferred
  - [ ] Test delivery log display
  - [ ] Test resend functionality

## Dev Notes

### Architecture
- Delivery logs per webhook
- Store request payload, response, timing
- Track retry attempts with individual outcomes
- Resend creates new delivery attempt

### Dependencies
- Story AP-7.1 (Webhook types)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
GET /api/admin/webhooks/:id/deliveries?status=...&from=...&to=...
GET /api/admin/webhooks/:id/deliveries/:deliveryId
POST /api/admin/webhooks/:id/deliveries/:deliveryId/resend
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add delivery types)
- `admin-portal/lib/api-client.ts` (MODIFY - add delivery API)
- `admin-portal/app/(dashboard)/webhooks/[id]/deliveries/page.tsx` (NEW)
- `admin-portal/components/webhooks/webhook-delivery-log.tsx` (NEW)
- `admin-portal/components/webhooks/delivery-details-modal.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-7.2]

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
**Dependencies**: Story AP-7.1 (Webhook List)
