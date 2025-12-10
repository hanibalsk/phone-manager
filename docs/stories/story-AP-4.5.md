# Story AP-4.5: Inactive Device Management

**Story ID**: AP-4.5
**Epic**: AP-4 - Device Fleet Administration
**Priority**: Should-Have (Medium)
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Development
**Created**: 2025-12-09
**PRD Reference**: FR-4.5 (Admin Portal PRD)

---

## Story

As an admin,
I want to manage inactive devices,
so that I can clean up the fleet.

## Acceptance Criteria

### AC AP-4.5.1: Inactive Device List
**Given** I am on the Inactive Devices page
**When** the page loads
**Then** I should see devices that have been inactive for X days (configurable)
**And** default threshold should be 30 days

### AC AP-4.5.2: Configurable Threshold
**Given** I am on the Inactive Devices page
**When** I change the inactivity threshold (e.g., 30, 60, 90 days)
**Then** the list should update to show devices inactive for that duration
**And** my preference should persist in the session

### AC AP-4.5.3: Inactivity Details
**Given** I am viewing the inactive device list
**When** I look at a device row
**Then** I should see: device name, last seen date, days inactive, owner
**And** days inactive should be prominently displayed

### AC AP-4.5.4: Bulk Delete Inactive
**Given** I have selected inactive devices
**When** I click "Delete Selected"
**Then** I should see a confirmation dialog with strong warning
**And** the dialog should mention data retention policy

### AC AP-4.5.5: Send Notification Option
**Given** I have selected inactive devices
**When** I click "Send Notification"
**Then** a notification should be sent to device owners
**And** I should see a confirmation of sent notifications

### AC AP-4.5.6: Data Retention Display
**Given** I am about to delete an inactive device
**When** I see the confirmation dialog
**Then** I should see information about data retention
**And** what data will be preserved vs deleted per policy

## Tasks / Subtasks

- [ ] Task 1: Add Inactive Devices API (AC: All)
  - [ ] Add getInactiveDevices method to devicesApi
  - [ ] Add InactiveDevice interface
  - [ ] Add notification endpoint
- [ ] Task 2: Create Inactive Devices List (AC: AP-4.5.1, AP-4.5.3)
  - [ ] Create components/devices/inactive-device-list.tsx
  - [ ] Display device info with days inactive
  - [ ] Highlight severely inactive devices (90+ days)
  - [ ] Add selection checkboxes
- [ ] Task 3: Add Threshold Selector (AC: AP-4.5.2)
  - [ ] Create threshold dropdown (30/60/90/custom days)
  - [ ] Implement filter state
  - [ ] Persist in URL params
- [ ] Task 4: Implement Bulk Delete (AC: AP-4.5.4, AP-4.5.6)
  - [ ] Reuse bulk delete from AP-4.4
  - [ ] Add data retention warning
  - [ ] Show what will be preserved
- [ ] Task 5: Implement Notification (AC: AP-4.5.5)
  - [ ] Create notification confirmation dialog
  - [ ] Implement send notification API call
  - [ ] Show success/failure results
- [ ] Task 6: Create Inactive Devices Page (AC: All)
  - [ ] Create app/(dashboard)/devices/inactive/page.tsx
  - [ ] Add navigation link from devices page
  - [ ] Compose all components
- [ ] Task 7: Testing (All ACs)
  - [ ] Unit test inactive device components
  - [ ] Test threshold filtering
  - [ ] Test notification flow

## Dev Notes

### Architecture
- Reuse bulk operations from AP-4.4
- Threshold options: 30, 60, 90, 180 days
- Notification should queue email to device owners
- Data retention info should come from system configuration

### Dependencies
- Story AP-4.4 (Bulk Operations)
- Backend: Inactive devices endpoint, notification service

### API Endpoint Expected
```typescript
GET /api/admin/devices/inactive
Query params:
  - days: number (minimum days inactive)
  - page: number
  - limit: number
Response: { devices: InactiveDevice[], total: number }

POST /api/admin/devices/notify
Body: { device_ids: string[], message_template: string }
Response: { sent: number, failed: number }
```

### Implementation Details
```typescript
interface InactiveDevice extends AdminDevice {
  days_inactive: number;
  last_activity_type: 'location' | 'sync' | 'login';
}

interface DataRetentionPolicy {
  locations_retained_days: number;
  trips_retained: boolean;
  user_data_deleted: boolean;
}
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add InactiveDevice)
- `admin-portal/lib/api-client.ts` (MODIFY - add inactive endpoints)
- `admin-portal/components/devices/inactive-device-list.tsx` (NEW)
- `admin-portal/components/devices/inactivity-threshold-selector.tsx` (NEW)
- `admin-portal/components/devices/notify-owners-dialog.tsx` (NEW)
- `admin-portal/app/(dashboard)/devices/inactive/page.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-4.5]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-4: Device Fleet Administration

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
| 2025-12-09 | Claude | Initial story creation from PRD |

---

**Last Updated**: 2025-12-09
**Status**: Ready for Development
**Dependencies**: Story AP-4.4 (Bulk Operations)
