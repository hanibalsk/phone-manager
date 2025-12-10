# Story AP-7.3: Test Webhooks

**Story ID**: AP-7.3
**Epic**: AP-7 - Webhooks & Trips Administration
**Priority**: Medium
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-7.3 (Admin Portal PRD)

---

## Story

As an admin,
I want to test webhooks,
so that I can verify configurations.

## Acceptance Criteria

### AC AP-7.3.1: Send Test Event
**Given** I am viewing a webhook
**When** I click test
**Then** a test event should be sent to the webhook

### AC AP-7.3.2: Show Test Response
**Given** I send a test event
**When** the response is received
**Then** I should see the response status and body

### AC AP-7.3.3: Validate Accessibility
**Given** I test a webhook
**Then** the endpoint accessibility should be validated

### AC AP-7.3.4: No Rate Limit Impact
**Given** I send a test event
**Then** the test should not count against rate limits

## Tasks / Subtasks

- [x] Task 1: Add Test Types (AC: AP-7.3.1)
  - [x] Add WebhookTestResult type to types/index.ts
  - [x] Add test webhook API endpoint
- [x] Task 2: Create Test Button (AC: AP-7.3.1)
  - [x] Add test button to webhook list row
- [x] Task 3: Create Test Modal (AC: AP-7.3.1, AP-7.3.2)
  - [x] Create components/webhooks/webhook-test-modal.tsx
  - [x] Event type selection for test payload
  - [x] Show test progress/spinner
- [x] Task 4: Display Test Results (AC: AP-7.3.2, AP-7.3.3)
  - [x] Show response status code
  - [x] Show response body (formatted)
  - [x] Show response time
  - [x] Show accessibility check result
- [ ] Task 5: Testing (All ACs) - Deferred
  - [ ] Test webhook testing functionality

## Dev Notes

### Architecture
- Test sends sample payload matching event type
- Test requests marked as test=true (backend excludes from rate limits)
- Response shows full HTTP response details
- Accessibility check validates URL is reachable

### Dependencies
- Story AP-7.1 (Webhook types)
- Existing: shadcn/ui components, useApi hook

### API Endpoints (To Add)
```typescript
POST /api/admin/webhooks/:id/test?event_type=...
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add test result type)
- `admin-portal/lib/api-client.ts` (MODIFY - add test endpoint)
- `admin-portal/components/webhooks/webhook-test-modal.tsx` (NEW)
- `admin-portal/components/webhooks/admin-webhook-list.tsx` (MODIFY - add test button)

### References
- [Source: PRD-admin-portal.md - FR-7.3]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-7: Webhooks & Trips Administration

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
- WebhookTestResult type already added in AP-7.1
- Test API endpoint already added in AP-7.1

### Completion Notes List
- Created test modal with event type selection
- Display HTTP status code with color coding (2xx green, others red)
- Show response time in milliseconds
- Show accessibility status (reachable/unreachable/timeout) with icons
- Format JSON response body with pretty printing
- Display error messages for failed tests
- Test button added to webhook list actions

### File List
- `admin-portal/components/webhooks/webhook-test-modal.tsx` (NEW)
- `admin-portal/components/webhooks/admin-webhook-list.tsx` (MODIFIED - added test button and modal)
- `admin-portal/components/webhooks/index.tsx` (MODIFIED - added export)

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implemented test modal with event selection and result display |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-7.1 (Webhook List)
