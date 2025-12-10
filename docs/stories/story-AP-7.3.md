# Story AP-7.3: Test Webhooks

**Story ID**: AP-7.3
**Epic**: AP-7 - Webhooks & Trips Administration
**Priority**: Medium
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Development
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

- [ ] Task 1: Add Test Types (AC: AP-7.3.1)
  - [ ] Add WebhookTestResult type to types/index.ts
  - [ ] Add test webhook API endpoint
- [ ] Task 2: Create Test Button (AC: AP-7.3.1)
  - [ ] Add test button to webhook list row
  - [ ] Add test button to webhook detail page
- [ ] Task 3: Create Test Modal (AC: AP-7.3.1, AP-7.3.2)
  - [ ] Create components/webhooks/webhook-test-modal.tsx
  - [ ] Event type selection for test payload
  - [ ] Show test progress/spinner
- [ ] Task 4: Display Test Results (AC: AP-7.3.2, AP-7.3.3)
  - [ ] Show response status code
  - [ ] Show response body (formatted)
  - [ ] Show response time
  - [ ] Show accessibility check result
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
