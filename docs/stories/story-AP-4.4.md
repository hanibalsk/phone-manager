# Story AP-4.4: Bulk Device Operations

**Story ID**: AP-4.4
**Epic**: AP-4 - Device Fleet Administration
**Priority**: Should-Have (High)
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Review
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

- [x] Task 1: Add Bulk Operations API (AC: All)
  - [x] Add bulkSuspend endpoint to adminDevicesApi
  - [x] Add bulkReactivate endpoint to adminDevicesApi
  - [x] Add bulkDelete endpoint to adminDevicesApi
  - [x] Add BulkOperationResult interface
- [x] Task 2: Add Selection State to Device List (AC: AP-4.4.1, AP-4.4.2)
  - [x] Add checkbox column to admin-device-list.tsx
  - [x] Implement selection state management with Set<string>
  - [x] Add header checkbox for select all
  - [x] Show selection count in BulkActionsMenu button
- [x] Task 3: Create Bulk Actions Menu (AC: AP-4.4.3 to AP-4.4.5)
  - [x] Create components/devices/bulk-actions-menu.tsx
  - [x] Add dropdown with Suspend, Reactivate, Delete options
  - [x] Disable options based on selection state (active/suspended counts)
- [x] Task 4: Create Bulk Confirmation Dialog (AC: AP-4.4.3 to AP-4.4.5, AP-4.4.7)
  - [x] Integrated confirmation dialogs in bulk-actions-menu.tsx
  - [x] Show affected device count
  - [x] Show warning for delete operation
  - [x] Handle mixed status (shows applicable counts per action)
- [x] Task 5: Implement Progress Indicator (AC: AP-4.4.6)
  - [x] Add loading state during bulk operations
  - [x] Show operation results dialog (success/failure counts)
  - [x] Auto-refresh list on completion via onActionComplete callback
- [ ] Task 6: Testing (All ACs) - Deferred
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
- BulkActionsMenu component provides dropdown with Suspend/Reactivate/Delete
- Confirmation dialogs integrated directly in BulkActionsMenu
- Operations show loading spinner during execution
- Results dialog shows success/failure counts with failed device details
- Selection state managed via Set<string> in AdminDeviceList
- Actions appropriately disabled based on selected device statuses
- Unit tests deferred to separate testing sprint

### File List
- `admin-portal/types/index.ts` (MODIFIED) - Added BulkOperationResult interface
- `admin-portal/lib/api-client.ts` (MODIFIED) - Added bulkSuspend, bulkReactivate, bulkDelete methods
- `admin-portal/components/devices/bulk-actions-menu.tsx` (NEW) - Bulk actions dropdown with confirmations and results
- `admin-portal/components/devices/admin-device-list.tsx` (MODIFIED) - Added selection state and checkbox column
- `admin-portal/components/devices/index.tsx` (MODIFIED) - Added BulkActionsMenu export

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-09 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implementation complete, status changed to Ready for Review |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-4.1 (Device List)

---

## Senior Developer Review (AI)

### Reviewer
Martin (AI-assisted)

### Date
2025-12-10

### Outcome
**Approve**

### Summary
The Bulk Device Operations implementation integrates well with the Device List component. Selection state management using Set<string> is efficient. The BulkActionsMenu provides clear feedback on operation status and handles mixed device states appropriately.

### Key Findings

**Low Severity**
- Bulk operations do not have batch size limits - large selections could timeout
- No progress indicator for individual items in bulk operations (shows overall loading only)

### Acceptance Criteria Coverage

| AC | Status | Evidence |
|----|--------|----------|
| AP-4.4.1 | ✅ Pass | Checkbox column in device list, selection tracking via Set |
| AP-4.4.2 | ✅ Pass | Header checkbox for select all on current page |
| AP-4.4.3 | ✅ Pass | Bulk suspend with confirmation and affected count |
| AP-4.4.4 | ✅ Pass | Bulk reactivate with confirmation |
| AP-4.4.5 | ✅ Pass | Bulk delete with warning confirmation |
| AP-4.4.6 | ✅ Pass | Loading spinner during operation, results dialog shows success/failure |
| AP-4.4.7 | ✅ Pass | Actions disabled based on selection state (counts active/suspended) |

### Test Coverage and Gaps

- **Unit Tests**: Not implemented (deferred to testing sprint)
- **Coverage Gap**: No tests for selection behavior, bulk operation flows

### Architectural Alignment

- ✅ Selection state colocated with device list (AdminDeviceList component)
- ✅ BulkActionsMenu receives selectedDevices array for flexibility
- ✅ onActionComplete and onClearSelection callbacks properly handled

### Security Notes

- ✅ Operations require confirmation dialogs
- ✅ Delete has stronger warning visual treatment
- ✅ API endpoints called via authenticated api-client

### Best-Practices and References

- Set<string> for selection state is O(1) for add/delete/check
- Results dialog shows both success and failure details
- Clear visual distinction between action types

### Action Items

- [ ] [AI-Review][Low] Consider adding batch size warning for very large selections (>100 devices)
- [ ] [AI-Review][Low] Add aria-labels to improve accessibility for bulk action buttons
