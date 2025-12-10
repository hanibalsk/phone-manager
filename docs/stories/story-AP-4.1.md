# Story AP-4.1: Device Fleet List with Filtering

**Story ID**: AP-4.1
**Epic**: AP-4 - Device Fleet Administration
**Priority**: Must-Have (High)
**Estimate**: 3 story points (2-3 days)
**Status**: Ready for Development
**Created**: 2025-12-09
**PRD Reference**: FR-4.1 (Admin Portal PRD)

---

## Story

As an admin,
I want to view all devices with filtering,
so that I can manage the device fleet.

## Acceptance Criteria

### AC AP-4.1.1: Device List Display
**Given** I am logged in as an admin
**When** I navigate to the Devices page
**Then** I should see a paginated list of devices with 50 items per page
**And** each row should display: device name, UUID, platform, owner email, organization, group, status, last seen

### AC AP-4.1.2: Filter by Organization
**Given** I am on the Devices page
**When** I select an organization from the filter dropdown
**Then** only devices belonging to that organization should be displayed
**And** the total count should reflect the filtered results

### AC AP-4.1.3: Filter by Group
**Given** I am on the Devices page
**When** I select a group from the filter dropdown
**Then** only devices in that group should be displayed

### AC AP-4.1.4: Filter by Status
**Given** I am on the Devices page
**When** I select a status filter (active, suspended, offline, pending)
**Then** only devices with that status should be displayed

### AC AP-4.1.5: Filter by Platform
**Given** I am on the Devices page
**When** I select a platform filter (android, ios)
**Then** only devices on that platform should be displayed

### AC AP-4.1.6: Search Functionality
**Given** I am on the Devices page
**When** I enter a search term
**Then** devices should be filtered by name, UUID, or owner email
**And** search should be case-insensitive with debounce

### AC AP-4.1.7: Sort Functionality
**Given** I am on the Devices page
**When** I click on a column header
**Then** the list should sort by that column (name, last_seen, location_count)
**And** clicking again should toggle sort direction

### AC AP-4.1.8: Loading and Error States
**Given** I navigate to the Devices page
**When** data is loading
**Then** I should see a loading skeleton
**And** if an error occurs, I should see an error message with retry

## Tasks / Subtasks

- [ ] Task 1: Add Device Types and API Client (AC: All)
  - [ ] Add AdminDevice interface to types/index.ts
  - [ ] Add DeviceListParams interface for query parameters
  - [ ] Add devicesApi to lib/api-client.ts with list method
- [ ] Task 2: Create DeviceList Component (AC: AP-4.1.1)
  - [ ] Create components/devices/DeviceList.tsx with data table
  - [ ] Display columns: name, uuid, platform, owner, organization, group, status, lastSeen
  - [ ] Add pagination controls (50 items per page)
  - [ ] Add platform and status badges
- [ ] Task 3: Implement Filters (AC: AP-4.1.2 to AP-4.1.5)
  - [ ] Add organization filter dropdown
  - [ ] Add group filter dropdown
  - [ ] Add status filter dropdown
  - [ ] Add platform filter dropdown
  - [ ] Add clear filters button
- [ ] Task 4: Implement Search (AC: AP-4.1.6)
  - [ ] Add search input with debounce (300ms)
  - [ ] Filter by name, UUID, owner email
- [ ] Task 5: Implement Sorting (AC: AP-4.1.7)
  - [ ] Add sortable column headers
  - [ ] Implement sort state management
  - [ ] Add sort direction indicators
- [ ] Task 6: Create Devices Page (AC: AP-4.1.1, AP-4.1.8)
  - [ ] Update existing app/(dashboard)/devices/page.tsx
  - [ ] Add loading skeleton
  - [ ] Add error state with retry
- [ ] Task 7: Testing (All ACs)
  - [ ] Unit test DeviceList component
  - [ ] Test filter combinations
  - [ ] Test sorting behavior

## Dev Notes

### Architecture
- Enhance existing devices page with advanced filtering
- Use shadcn/ui DataTable component pattern
- Reuse useApi hook and existing patterns
- Use URL search params for filter state persistence

### Dependencies
- Existing: shadcn/ui components, useApi hook, api-client
- Backend: `/api/admin/devices` endpoint (partially exists)

### API Endpoint Expected
```typescript
GET /api/admin/devices
Query params:
  - page: number (default 1)
  - limit: number (default 50)
  - search: string (optional)
  - organization_id: string (optional)
  - group_id: string (optional)
  - status: string (optional)
  - platform: string (optional)
  - sort_by: string (optional)
  - sort_order: 'asc' | 'desc' (optional)
Response: { devices: AdminDevice[], total: number, page: number, limit: number }
```

### Implementation Details
```typescript
interface AdminDevice {
  id: string;
  device_id: string;
  display_name: string;
  platform: 'android' | 'ios';
  status: 'active' | 'suspended' | 'offline' | 'pending';
  owner_id: string;
  owner_email: string;
  organization_id: string;
  organization_name: string;
  group_id: string | null;
  group_name: string | null;
  last_seen: string | null;
  location_count: number;
  trip_count: number;
  created_at: string;
}
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add AdminDevice)
- `admin-portal/lib/api-client.ts` (MODIFY - enhance devicesApi)
- `admin-portal/components/devices/device-list.tsx` (NEW)
- `admin-portal/components/devices/device-filters.tsx` (NEW)
- `admin-portal/components/devices/device-status-badge.tsx` (NEW)
- `admin-portal/app/(dashboard)/devices/page.tsx` (MODIFY)

### References
- [Source: PRD-admin-portal.md - FR-4.1]
- [Source: Existing users list pattern]

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
**Dependencies**: Admin Portal foundation (Epic AP-12) - In Progress
