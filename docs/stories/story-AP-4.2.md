# Story AP-4.2: Device Details View

**Story ID**: AP-4.2
**Epic**: AP-4 - Device Fleet Administration
**Priority**: Must-Have (High)
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Development
**Created**: 2025-12-09
**PRD Reference**: FR-4.2 (Admin Portal PRD)

---

## Story

As an admin,
I want to view device details,
so that I can troubleshoot issues.

## Acceptance Criteria

### AC AP-4.2.1: Device Info Section
**Given** I am viewing a device details page
**When** the page loads
**Then** I should see device info: name, UUID, platform, owner, organization
**And** the data should be formatted clearly

### AC AP-4.2.2: Device Status Section
**Given** I am viewing device details
**When** I look at the status section
**Then** I should see: online/offline status, last seen timestamp, enrollment status
**And** status should have appropriate visual indicators

### AC AP-4.2.3: Device Metrics Section
**Given** I am viewing device details
**When** I look at the metrics section
**Then** I should see: location count, trip count, last location (if available)
**And** metrics should update when I refresh

### AC AP-4.2.4: Policy Compliance Section
**Given** I am viewing device details
**When** I look at the compliance section
**Then** I should see policy compliance status (if policies are assigned)
**And** non-compliant items should be highlighted

### AC AP-4.2.5: Device Actions
**Given** I am viewing device details
**When** I have appropriate permissions
**Then** I should see action buttons: Suspend, Reactivate, Delete
**And** actions should require confirmation

### AC AP-4.2.6: Navigation
**Given** I click on a device in the device list
**When** the details page loads
**Then** I should see a back button to return to the list
**And** breadcrumb navigation showing: Devices > Device Name

## Tasks / Subtasks

- [ ] Task 1: Add Device Details API (AC: All)
  - [ ] Add getDevice method to devicesApi
  - [ ] Add DeviceDetails interface with full device info
- [ ] Task 2: Create Device Info Component (AC: AP-4.2.1)
  - [ ] Create components/devices/device-info.tsx
  - [ ] Display name, UUID, platform, owner, organization
  - [ ] Add platform icon indicators
- [ ] Task 3: Create Device Status Component (AC: AP-4.2.2)
  - [ ] Create components/devices/device-status.tsx
  - [ ] Show online/offline with indicator
  - [ ] Show last seen with relative time
  - [ ] Show enrollment status badge
- [ ] Task 4: Create Device Metrics Component (AC: AP-4.2.3)
  - [ ] Create components/devices/device-metrics.tsx
  - [ ] Show location count, trip count
  - [ ] Show last location (lat/lng) if available
- [ ] Task 5: Create Device Actions Component (AC: AP-4.2.5)
  - [ ] Create components/devices/device-actions.tsx
  - [ ] Add Suspend/Reactivate toggle based on status
  - [ ] Add Delete button with confirmation dialog
  - [ ] Implement action handlers
- [ ] Task 6: Create Device Details Page (AC: All)
  - [ ] Create app/(dashboard)/devices/[id]/page.tsx
  - [ ] Add breadcrumb navigation
  - [ ] Compose all device components
  - [ ] Add loading and error states
- [ ] Task 7: Testing (All ACs)
  - [ ] Unit test device components
  - [ ] Test action confirmation flows
  - [ ] Test navigation

## Dev Notes

### Architecture
- Follow existing detail page patterns
- Use Card components for sections
- Implement device actions with confirmation dialogs
- Use relative time formatting for last seen

### Dependencies
- Existing: shadcn/ui components, api-client
- Backend: `/api/admin/devices/:id` endpoint

### API Endpoint Expected
```typescript
GET /api/admin/devices/:id
Response: DeviceDetails

POST /api/admin/devices/:id/suspend
Response: { success: boolean }

POST /api/admin/devices/:id/reactivate
Response: { success: boolean }

DELETE /api/admin/devices/:id
Response: { success: boolean }
```

### Implementation Details
```typescript
interface DeviceDetails extends AdminDevice {
  enrollment_status: 'enrolled' | 'pending' | 'expired';
  policy_id: string | null;
  policy_name: string | null;
  policy_compliant: boolean;
  compliance_issues: string[];
  last_location: {
    latitude: number;
    longitude: number;
    timestamp: string;
  } | null;
}
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add DeviceDetails)
- `admin-portal/lib/api-client.ts` (MODIFY - add device detail methods)
- `admin-portal/components/devices/device-info.tsx` (NEW)
- `admin-portal/components/devices/device-status.tsx` (NEW)
- `admin-portal/components/devices/device-metrics.tsx` (NEW)
- `admin-portal/components/devices/device-actions.tsx` (NEW)
- `admin-portal/app/(dashboard)/devices/[id]/page.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-4.2]

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
**Dependencies**: Story AP-4.1 (Device List)
