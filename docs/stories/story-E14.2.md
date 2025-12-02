# Story E14.2: Device List & Management

**Story ID**: E14.2
**Epic**: 14 - Admin Web Portal
**Priority**: High
**Estimate**: 3 story points (1-2 days)
**Status**: Complete
**Created**: 2025-12-02
**PRD Reference**: PRD-user-management.md
**Dependencies**: E14.1 (Portal Project Setup)

---

## Story

As an administrator,
I want to view and manage enrolled devices in the web portal,
so that I can monitor device status and manage device settings.

## Acceptance Criteria

### AC E14.2.1: Device List View
**Given** an administrator accessing the devices page
**When** the page loads
**Then** they should see:
  - List of all enrolled devices
  - Device name and Android ID
  - Enrollment date
  - Last seen timestamp
  - Device status (active/inactive)

### AC E14.2.2: Device Details View
**Given** a device in the list
**When** clicking on a device
**Then** they should see:
  - Detailed device information
  - Recent activity summary
  - Quick actions (view usage, set limits)

### AC E14.2.3: Device Status Indicators
**Given** the device list
**When** viewing device status
**Then** the status should show:
  - Active (green) - seen within last hour
  - Inactive (yellow) - not seen in last hour
  - Offline (red) - not seen in 24+ hours

### AC E14.2.4: Search and Filter
**Given** multiple devices
**When** using search/filter
**Then** users can:
  - Search by device name
  - Filter by status
  - Sort by various columns

## Tasks / Subtasks

- [x] Task 1: Create Devices Page Route (AC: E14.2.1)
  - [x] Create /devices page
  - [x] Set up page layout with header
  - [x] Add loading state

- [x] Task 2: Implement Device List Component (AC: E14.2.1)
  - [x] Create DeviceList component
  - [x] Display device table with columns
  - [x] Add status badges

- [x] Task 3: Add Device Details Dialog (AC: E14.2.2)
  - [x] Create DeviceDetails component
  - [x] Show device information
  - [x] Add action buttons

- [x] Task 4: Implement Status Logic (AC: E14.2.3)
  - [x] Create status calculation helper
  - [x] Add status badge component
  - [x] Implement color coding

- [x] Task 5: Add Search and Filters (AC: E14.2.4)
  - [x] Add search input
  - [x] Add status filter dropdown
  - [x] Implement client-side filtering

- [x] Task 6: Connect to API (AC: E14.2.1)
  - [x] Use deviceApi.list()
  - [x] Handle loading and error states
  - [x] Add refresh capability

## Dev Notes

### API Endpoints Used
- GET /api/admin/devices - List all devices
- GET /api/admin/devices/{id} - Get device details

### Component Structure
```
/app/devices/
  page.tsx - Devices list page
/components/devices/
  device-list.tsx - Device list component
  device-card.tsx - Device card for grid view
  device-details.tsx - Device details dialog
  device-status-badge.tsx - Status indicator
```

---

## Dev Agent Record

### Debug Log

No issues encountered during implementation.

### Completion Notes

Implemented device list and management features:
- Devices page with table view showing all enrolled devices
- Status badge component with color-coded indicators (active/inactive/offline)
- Device details modal with quick actions
- Search by device name
- Filter by device status
- Refresh capability with loading states

---

## File List

### Created Files

- `admin-portal/app/devices/page.tsx` - Devices list page
- `admin-portal/components/devices/device-list.tsx` - Device list table component
- `admin-portal/components/devices/device-details.tsx` - Device details modal
- `admin-portal/components/devices/device-status-badge.tsx` - Status badge component
- `admin-portal/components/devices/index.tsx` - Component exports

### Modified Files

None

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-02 | Claude | Story created |
| 2025-12-02 | Claude | Implemented all tasks |

---

**Last Updated**: 2025-12-02
**Status**: Complete
**Dependencies**: E14.1
**Blocking**: None
