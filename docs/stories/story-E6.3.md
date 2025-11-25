# Story E6.3: Webhook Integration

**Story ID**: E6.3
**Epic**: 6 - Geofencing with Webhooks
**Priority**: Must-Have
**Estimate**: 2 story points (1-2 days)
**Status**: ContextReadyDraft
**Created**: 2025-11-25
**PRD Reference**: Feature 5 (FR-5.3, FR-5.4)

---

## Story

As a user,
I want geofence events to trigger webhooks,
so that I can automate actions in Home Assistant or n8n.

## Acceptance Criteria

### AC E6.3.1: Webhook Entity
**Given** the data model is defined
**Then** Webhook entity should include:
  - id (UUID)
  - ownerDeviceId
  - name (e.g., "Home Assistant", "n8n Workflow")
  - targetUrl (HTTPS URL)
  - secret (for HMAC signing)
  - enabled (Boolean)
  - createdAt, updatedAt

### AC E6.3.2: Link Geofence to Webhook
**Given** I am creating or editing a geofence
**When** I configure the geofence
**Then** I should be able to link it to a webhook
**And** the webhook should trigger when geofence events occur

### AC E6.3.3: Backend Webhook Dispatch
**Given** a geofence event occurs with a linked webhook
**When** the backend receives the geofence event
**Then** the backend should send HTTP POST to the webhook targetUrl
**And** payload should include: event, deviceId, deviceName, geofenceId, geofenceName, eventType, timestamp, location

### AC E6.3.4: HMAC Signature
**Given** a webhook request is sent
**When** the request is dispatched
**Then** X-Signature header should contain HMAC-SHA256 of payload using webhook secret
**And** receiving system can verify authenticity

### AC E6.3.5: Webhook CRUD Operations
**Given** I am on the Webhooks screen
**Then** I should be able to:
  - View list of my webhooks
  - Create new webhook
  - Edit existing webhook
  - Delete webhook
  - Enable/disable webhook

### AC E6.3.6: URL Validation
**Given** I am creating or editing a webhook
**When** I enter the target URL
**Then** the system should validate URL format
**And** enforce HTTPS for security

### AC E6.3.7: Webhook Delivery Status
**Given** a webhook was triggered
**When** I view the geofence event
**Then** I should see webhook delivery status (delivered/failed)
**And** response code from target server

## Tasks / Subtasks

- [ ] Task 1: Create Webhook Domain Model (AC: E6.3.1)
  - [ ] Create Webhook data class
  - [ ] Generate secret on creation (UUID or secure random)
- [ ] Task 2: Create Webhook Room Entity (AC: E6.3.1)
  - [ ] Create WebhookEntity with Room annotations
  - [ ] Create WebhookDao
  - [ ] Add to AppDatabase
- [ ] Task 3: Create Network Models (AC: E6.3.3)
  - [ ] Create WebhookDto for API
  - [ ] Add CRUD endpoints to WebhookApiService
- [ ] Task 4: Create WebhookRepository (AC: E6.3.5)
  - [ ] Implement local + remote sync
  - [ ] Add CRUD operations
- [ ] Task 5: Update CreateGeofenceScreen (AC: E6.3.2)
  - [ ] Add webhook selector dropdown
  - [ ] Load available webhooks from repository
  - [ ] Save webhookId with geofence
- [ ] Task 6: Create WebhooksScreen UI (AC: E6.3.5)
  - [ ] Create WebhooksScreen composable
  - [ ] Show list of webhooks
  - [ ] Add enable/disable toggle
- [ ] Task 7: Create CreateWebhookScreen (AC: E6.3.1, E6.3.6)
  - [ ] Add name input
  - [ ] Add URL input with validation
  - [ ] Auto-generate secret (display for user)
  - [ ] Enforce HTTPS
- [ ] Task 8: Create WebhooksViewModel (AC: E6.3.5)
  - [ ] Load webhooks from repository
  - [ ] Implement CRUD operations
- [ ] Task 9: Update GeofenceEvent to Track Webhook Status (AC: E6.3.7)
  - [ ] Use webhookDelivered and webhookResponseCode from E6.2
  - [ ] Display status in event details
- [ ] Task 10: Document Backend Webhook Dispatch (AC: E6.3.3, E6.3.4)
  - [ ] Document expected backend behavior
  - [ ] Document HMAC signing process
  - [ ] Provide sample n8n/Home Assistant config
- [ ] Task 11: Testing (All ACs)
  - [ ] Unit test WebhookRepository
  - [ ] Manual test webhook creation
  - [ ] Integration test with n8n (if available)

## Dev Notes

### Architecture
- Webhook dispatch happens on backend, not client
- Client sends geofence events to backend, backend dispatches webhooks
- Client manages webhook CRUD and linking to geofences

### Webhook Payload (sent by backend)
```json
{
  "event": "geofence_trigger",
  "deviceId": "device-001",
  "deviceName": "Martin's Phone",
  "geofenceId": "geo-home-001",
  "geofenceName": "Home",
  "eventType": "ENTER",
  "timestamp": "2025-11-25T18:30:00Z",
  "location": {
    "latitude": 48.1486,
    "longitude": 17.1077
  }
}
```

### HMAC Signing (Backend)
```javascript
// Backend example (Node.js/n8n)
const crypto = require('crypto');
const payload = JSON.stringify(body);
const signature = crypto
  .createHmac('sha256', webhookSecret)
  .update(payload)
  .digest('hex');
// Set header: X-Signature: sha256={signature}
```

### Verification (Receiver - Home Assistant/n8n)
```javascript
// Verify in n8n or Home Assistant
const expectedSignature = crypto
  .createHmac('sha256', MY_SECRET)
  .update(rawBody)
  .digest('hex');
const receivedSignature = headers['x-signature'].replace('sha256=', '');
const valid = crypto.timingSafeEqual(
  Buffer.from(expectedSignature),
  Buffer.from(receivedSignature)
);
```

### Files to Create
- `domain/model/Webhook.kt` (NEW)
- `data/model/WebhookEntity.kt` (NEW)
- `data/database/dao/WebhookDao.kt` (NEW)
- `network/WebhookApiService.kt` (NEW)
- `data/repository/WebhookRepository.kt` (NEW)
- `ui/webhooks/WebhooksScreen.kt` (NEW)
- `ui/webhooks/WebhooksViewModel.kt` (NEW)
- `ui/webhooks/CreateWebhookScreen.kt` (NEW)
- `ui/geofences/CreateGeofenceScreen.kt` (MODIFY - add webhook selector)

### URL Validation
```kotlin
fun isValidWebhookUrl(url: String): Boolean {
    return try {
        val uri = URI(url)
        uri.scheme == "https" && uri.host != null
    } catch (e: Exception) {
        false
    }
}
```

### References
- [Source: PRD FR-5.3.1-5.3.5 - Webhook Integration requirements]
- [Source: PRD FR-5.4.1-5.4.3 - Webhook Management requirements]
- [Source: PRD Data Model: Webhook]
- [Source: PRD Webhook Payload Example]
- [Source: PRD Section 6.2 - /api/webhooks spec]
- [Source: epics.md - Story 6.3 description]

## Dev Agent Record

### Context Reference
- `/Users/martinjanci/cursor/phone-manager/docs/story-context-E6.3.xml` (Generated: 2025-11-25)

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
<!-- Add debug log references during implementation -->

### Completion Notes List
<!-- Add completion notes during implementation -->

### File List
<!-- Add list of files created/modified during implementation -->

---

**Last Updated**: 2025-11-25
**Status**: ContextReadyDraft
**Dependencies**: Story E6.1 (Geofence Definition), Story E6.2 (Geofence Events)
