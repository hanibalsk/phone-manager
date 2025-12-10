# Story AP-4.4: Bulk Device Operations

**Story ID**: AP-4.4
**Epic**: AP-4 - Device Fleet Administration
**Priority**: Should-Have (High)
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Development
**Created**: 2025-12-09
**PRD Reference**: FR-4.4 (Admin Portal PRD)

---

## Story

As an admin,
I want to perform bulk operations on devices,
so that I can manage at scale.

## Acceptance Criteria

### AC AP-4.4.1: Multi-Select Devices
**Given** I am on the Device List page
**When** I check the checkbox on device rows
**Then** those devices should be selected
**And** I should see a selection count indicator

### AC AP-4.4.2: Select All on Page
**Given** I am on the Device List page
**When** I click the header checkbox
**Then** all devices on the current page should be selected
**And** clicking again should deselect all

### AC AP-4.4.3: Bulk Suspend Action
**Given** I have selected multiple devices
**When** I click "Bulk Actions" > "Suspend"
**Then** I should see a confirmation dialog with affected count
**And** upon confirmation, all selected devices should be suspended

### AC AP-4.4.4: Bulk Reactivate Action
**Given** I have selected multiple suspended devices
**When** I click "Bulk Actions" > "Reactivate"
**Then** I should see a confirmation dialog
**And** upon confirmation, all selected devices should be reactivated

### AC AP-4.4.5: Bulk Delete Action
**Given** I have selected multiple devices
**When** I click "Bulk Actions" > "Delete"
**Then** I should see a confirmation dialog with strong warning
**And** upon confirmation, all selected devices should be deleted

### AC AP-4.4.6: Progress Indicator
**Given** I have confirmed a bulk operation
**When** the operation is in progress
**Then** I should see a progress indicator
**And** upon completion, I should see success/failure count

### AC AP-4.4.7: Action Restrictions
**Given** I have selected devices with mixed statuses
**When** I view bulk actions
**Then** inappropriate actions should be disabled or show warnings
**And** e.g., cannot suspend already-suspended devices

## Tasks / Subtasks

- [ ] Task 1: Add Bulk Operations API (AC: All)
  - [ ] Add bulk suspend endpoint to devicesApi
  - [ ] Add bulk reactivate endpoint to devicesApi
  - [ ] Add bulk delete endpoint to devicesApi
  - [ ] Add BulkOperationResult interface
- [ ] Task 2: Add Selection State to Device List (AC: AP-4.4.1, AP-4.4.2)
  - [ ] Add checkbox column to device list
  - [ ] Implement selection state management
  - [ ] Add header checkbox for select all
  - [ ] Show selection count badge
- [ ] Task 3: Create Bulk Actions Menu (AC: AP-4.4.3 to AP-4.4.5)
  - [ ] Create components/devices/bulk-actions-menu.tsx
  - [ ] Add dropdown with Suspend, Reactivate, Delete options
  - [ ] Disable options based on selection state
- [ ] Task 4: Create Bulk Confirmation Dialog (AC: AP-4.4.3 to AP-4.4.5, AP-4.4.7)
  - [ ] Create components/devices/bulk-confirm-dialog.tsx
  - [ ] Show affected device count
  - [ ] Show warning for delete operation
  - [ ] Handle mixed status warnings
- [ ] Task 5: Implement Progress Indicator (AC: AP-4.4.6)
  - [ ] Add progress state during bulk operations
  - [ ] Show operation results (success/failure counts)
  - [ ] Auto-refresh list on completion
- [ ] Task 6: Testing (All ACs)
  - [ ] Unit test bulk operations components
  - [ ] Test selection behavior
  - [ ] Test confirmation flows

## Dev Notes

### Architecture
- Use React state for selection management (or Zustand if complex)
- Bulk operations should be atomic where possible
- Backend may process in batches for large selections
- Results should show partial success if some items fail

### Dependencies
- Story AP-4.1 (Device List)
- Existing: shadcn/ui components

### API Endpoint Expected
```typescript
POST /api/admin/devices/bulk/suspend
Body: { device_ids: string[] }
Response: BulkOperationResult

POST /api/admin/devices/bulk/reactivate
Body: { device_ids: string[] }
Response: BulkOperationResult

POST /api/admin/devices/bulk/delete
Body: { device_ids: string[] }
Response: BulkOperationResult
```

### Implementation Details
```typescript
interface BulkOperationResult {
  total: number;
  success_count: number;
  failure_count: number;
  failures: {
    device_id: string;
    device_name: string;
    error: string;
  }[];
}
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add BulkOperationResult)
- `admin-portal/lib/api-client.ts` (MODIFY - add bulk operations)
- `admin-portal/components/devices/device-list.tsx` (MODIFY - add selection)
- `admin-portal/components/devices/bulk-actions-menu.tsx` (NEW)
- `admin-portal/components/devices/bulk-confirm-dialog.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-4.4]

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
