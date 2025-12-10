# Story AP-5.1: Groups List with Filtering

**Story ID**: AP-5.1
**Epic**: AP-5 - Groups Administration
**Priority**: Must-Have (Medium)
**Estimate**: 2 story points (1-2 days)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-5.1 (Admin Portal PRD)

---

## Story

As an admin,
I want to view all groups,
so that I can manage group structures.

## Acceptance Criteria

### AC AP-5.1.1: Groups List Display
**Given** I am logged in as an admin
**When** I navigate to the Groups page
**Then** I should see a paginated list of groups with 50 items per page
**And** each row should display: group name, owner name/email, member count, device count, organization, status, created date

### AC AP-5.1.2: Filter by Organization
**Given** I am on the Groups page
**When** I select an organization from the filter dropdown
**Then** only groups belonging to that organization should be displayed
**And** the total count should reflect the filtered results

### AC AP-5.1.3: Search Functionality
**Given** I am on the Groups page
**When** I enter a search term
**Then** groups should be filtered by name
**And** search should be case-insensitive with debounce

### AC AP-5.1.4: Sort Functionality
**Given** I am on the Groups page
**When** I click on a column header
**Then** the list should sort by that column (name, member_count, created_at)
**And** clicking again should toggle sort direction

### AC AP-5.1.5: Loading and Error States
**Given** I navigate to the Groups page
**When** data is loading
**Then** I should see a loading skeleton
**And** if an error occurs, I should see an error message with retry

### AC AP-5.1.6: Group Actions Menu
**Given** I am viewing a group row
**When** I click the actions menu
**Then** I should see options: View Members, Transfer Ownership, Suspend, Archive

## Tasks / Subtasks

- [x] Task 1: Add Group Types and API Client (AC: All)
  - [x] Add AdminGroup interface to types/index.ts
  - [x] Add GroupListParams interface for query parameters
  - [x] Add adminGroupsApi to lib/api-client.ts with list method
- [x] Task 2: Create GroupList Component (AC: AP-5.1.1)
  - [x] Create components/groups/admin-group-list.tsx with data table
  - [x] Display columns: name, owner, member count, device count, organization, status, created
  - [x] Add pagination controls (50 items per page)
  - [x] Add group status badge
- [x] Task 3: Implement Filters (AC: AP-5.1.2)
  - [x] Add organization filter dropdown
  - [x] Add status filter dropdown
  - [x] Add clear filters button
- [x] Task 4: Implement Search (AC: AP-5.1.3)
  - [x] Add search input with debounce (300ms)
  - [x] Filter by group name
- [x] Task 5: Implement Sorting (AC: AP-5.1.4)
  - [x] Add sortable column headers (name, member_count, device_count, created_at)
  - [x] Implement sort state management
  - [x] Add sort direction indicators
- [x] Task 6: Create Groups Page (AC: AP-5.1.1, AP-5.1.5)
  - [x] Create app/(dashboard)/groups/page.tsx
  - [x] Add loading skeleton
  - [x] Add error state with retry
- [x] Task 7: Add Actions Menu (AC: AP-5.1.6)
  - [x] Create components/groups/group-actions-menu.tsx
  - [x] Add View Members, Transfer Ownership, Suspend, Archive options
- [ ] Task 8: Testing (All ACs) - Deferred
  - [ ] Unit test GroupList component
  - [ ] Test filter combinations
  - [ ] Test sorting behavior

## Dev Notes

### Architecture
- Follow existing patterns from AdminDeviceList
- Use shadcn/ui DataTable component pattern
- Reuse useApi hook and existing patterns

### Dependencies
- Existing: shadcn/ui components, useApi hook, api-client
- Backend: `/api/admin/groups` endpoint

### API Endpoint Expected
```typescript
GET /api/admin/groups
Query params:
  - page: number (default 1)
  - limit: number (default 50)
  - search: string (optional)
  - organization_id: string (optional)
  - status: string (optional)
  - sort_by: string (optional)
  - sort_order: 'asc' | 'desc' (optional)
Response: { items: AdminGroup[], total: number, page: number, limit: number }
```

### Implementation Details
```typescript
interface AdminGroup {
  id: string;
  name: string;
  description: string | null;
  owner_id: string;
  owner_name: string;
  owner_email: string;
  organization_id: string;
  organization_name: string;
  member_count: number;
  device_count: number;
  status: 'active' | 'suspended' | 'archived';
  invite_code: string | null;
  created_at: string;
  updated_at: string;
}

type GroupStatus = 'active' | 'suspended' | 'archived';
```

### Files to Create/Modify
- `admin-portal/types/index.ts` (MODIFY - add AdminGroup)
- `admin-portal/lib/api-client.ts` (MODIFY - add adminGroupsApi)
- `admin-portal/components/groups/admin-group-list.tsx` (NEW)
- `admin-portal/components/groups/group-status-badge.tsx` (NEW)
- `admin-portal/components/groups/group-actions-menu.tsx` (NEW)
- `admin-portal/components/groups/index.tsx` (NEW)
- `admin-portal/app/(dashboard)/groups/page.tsx` (NEW)

### References
- [Source: PRD-admin-portal.md - FR-5.1]
- [Source: AdminDeviceList pattern]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-5: Groups Administration

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
Implementation completed without issues.

### Completion Notes List
- Implemented AdminGroupList component with full filtering, sorting, pagination
- Used existing patterns from AdminDeviceList and UserList components
- Added GroupStatusBadge for visual status indicators
- Search uses useDebounce hook for 300ms debounce
- Pagination set to 50 items per page as specified
- GroupActionsMenu provides Suspend/Reactivate/Archive actions with confirmation dialogs
- Unit tests deferred to separate testing sprint

### File List
- `admin-portal/types/index.ts` (MODIFIED) - Added AdminGroup, GroupStatus, GroupMemberRole, GroupListParams, GroupMember, GroupInvite, InviteStatus types
- `admin-portal/lib/api-client.ts` (MODIFIED) - Added adminGroupsApi with list, get, suspend, reactivate, archive, member management, and invite management methods
- `admin-portal/components/groups/admin-group-list.tsx` (NEW) - Main group list component with filtering/sorting/pagination
- `admin-portal/components/groups/group-status-badge.tsx` (NEW) - Status badge component
- `admin-portal/components/groups/group-actions-menu.tsx` (NEW) - Actions dropdown with confirmation dialogs
- `admin-portal/components/groups/index.tsx` (NEW) - Barrel exports
- `admin-portal/app/(dashboard)/groups/page.tsx` (NEW) - Groups administration page

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implementation complete, status changed to Ready for Review |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Admin Portal foundation (Epics AP-1, AP-2)

---

## Senior Developer Review (AI)

### Reviewer
Martin (AI-assisted)

### Date
2025-12-10

### Outcome
**Approve**

### Summary
The Groups List implementation follows established patterns from AdminDeviceList, providing comprehensive filtering, sorting, and pagination. The GroupActionsMenu includes proper accessibility attributes (aria-label, role="menu", role="menuitem") and confirmation dialogs for destructive actions.

### Key Findings

**Low Severity**
- Missing data-testid attributes for E2E testing consistency (same pattern as AP-4.1)
- Consider adding keyboard navigation for action menu items

### Acceptance Criteria Coverage

| AC | Status | Evidence |
|----|--------|----------|
| AP-5.1.1 | ✅ Pass | Paginated list with 50 items per page, displays all required columns |
| AP-5.1.2 | ✅ Pass | Organization filter dropdown fetches from API |
| AP-5.1.3 | ✅ Pass | Search with 300ms debounce, case-insensitive |
| AP-5.1.4 | ✅ Pass | Sortable columns: name, member_count, device_count, created_at |
| AP-5.1.5 | ✅ Pass | Loading skeleton and error state with retry button |
| AP-5.1.6 | ✅ Pass | Actions menu with View Members, Transfer Ownership, Suspend, Archive |

### Test Coverage and Gaps

- **Unit Tests**: Not implemented (deferred to testing sprint)
- **Coverage Gap**: No tests for filter combinations and sorting behavior

### Architectural Alignment

- ✅ Follows AdminDeviceList patterns consistently
- ✅ Uses useApi hook correctly
- ✅ Proper separation of concerns (list, badge, actions components)
- ✅ Uses Card component for consistent UI

### Security Notes

- ✅ Actions require confirmation dialogs
- ✅ No sensitive data exposed in UI
- ✅ Proper escaping of user-generated content (group names, descriptions)

### Best-Practices and References

- Good use of aria-label and role attributes for accessibility
- Confirmation dialogs prevent accidental destructive actions
- Description truncation prevents layout issues

### Action Items

- [ ] [AI-Review][Low] Add data-testid attributes for E2E testing consistency
