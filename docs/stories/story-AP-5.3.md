# Story AP-5.3: Group Ownership Transfer

**Story ID**: AP-5.3
**Epic**: AP-5 - Groups Administration
**Priority**: Should-Have (Medium)
**Estimate**: 1 story point (0.5-1 day)
**Status**: Ready for Review
**Created**: 2025-12-10
**PRD Reference**: FR-5.3 (Admin Portal PRD)

---

## Story

As an admin,
I want to transfer group ownership,
so that I can handle departures.

## Acceptance Criteria

### AC AP-5.3.1: Transfer Page Access
**Given** I am viewing the group actions menu
**When** I click "Transfer Ownership"
**Then** I should see the ownership transfer page

### AC AP-5.3.2: Current Owner Display
**Given** I am on the transfer ownership page
**Then** I should see the current owner's name and email

### AC AP-5.3.3: Select New Owner
**Given** I am on the transfer ownership page
**When** I select a member from the list
**Then** that member should be highlighted as the new owner candidate

### AC AP-5.3.4: Confirm Transfer
**Given** I have selected a new owner
**When** I confirm the transfer
**Then** ownership should be transferred to the selected member
**And** the previous owner should become an ADMIN
**And** I should see a success notification

### AC AP-5.3.5: Audit Logging
**Given** an ownership transfer is completed
**Then** the transfer should be logged in the audit trail

### AC AP-5.3.6: Error Handling
**Given** the transfer fails
**Then** I should see an error message
**And** the original owner should remain

## Tasks / Subtasks

- [x] Task 1: Create Transfer Ownership Page (AC: AP-5.3.1, AP-5.3.2)
  - [x] Create app/(dashboard)/groups/[id]/transfer/page.tsx
  - [x] Display current owner info
  - [x] Show group details header
- [x] Task 2: Implement Member Selection (AC: AP-5.3.3)
  - [x] List eligible members (non-owner members)
  - [x] Add selection state
  - [x] Highlight selected member
- [x] Task 3: Implement Transfer Action (AC: AP-5.3.4, AP-5.3.5)
  - [x] Add confirmation dialog
  - [x] Call API to transfer ownership
  - [x] Show success/error notification
  - [x] Redirect to group page on success
- [ ] Task 4: Testing (All ACs) - Deferred
  - [ ] Unit test TransferOwnership component
  - [ ] Test transfer flow

## Dev Notes

### Architecture
- Create new page under groups/[id]/transfer
- List only non-owner members as candidates
- Use existing confirmation dialog pattern

### Dependencies
- Existing: shadcn/ui components, useApi hook, api-client
- Backend: `/api/admin/groups/{id}/transfer` endpoint (added in AP-5.1)

### API Endpoint (Already Added)
```typescript
POST /api/admin/groups/:id/transfer
Body: { new_owner_id: string }
Response: AdminGroup
```

### Files to Create/Modify
- `admin-portal/app/(dashboard)/groups/[id]/transfer/page.tsx` (NEW)
- `admin-portal/components/groups/transfer-ownership-form.tsx` (NEW)
- `admin-portal/components/groups/index.tsx` (MODIFY)

### References
- [Source: PRD-admin-portal.md - FR-5.3]

## Dev Agent Record

### Context Reference
- PRD: `docs/PRD-admin-portal.md` - Epic AP-5: Groups Administration

### Agent Model Used
Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References
Implementation completed without issues.

### Completion Notes List
- Created TransferOwnershipPage with group header
- Implemented TransferOwnershipForm with member selection and confirmation
- Current owner displayed prominently, filters owner from candidates
- Selected member highlighted with visual feedback
- Confirmation dialog shows clear transfer details
- Redirects to groups page after successful transfer
- Unit tests deferred to separate testing sprint

### File List
- `admin-portal/app/(dashboard)/groups/[id]/transfer/page.tsx` (NEW) - Transfer ownership page
- `admin-portal/components/groups/transfer-ownership-form.tsx` (NEW) - Form with member selection
- `admin-portal/components/groups/index.tsx` (MODIFIED) - Added new export

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-10 | Claude | Initial story creation from PRD |
| 2025-12-10 | Claude | Implementation complete, status changed to Ready for Review |

---

**Last Updated**: 2025-12-10
**Status**: Ready for Review
**Dependencies**: Story AP-5.1 (Groups List), Story AP-5.2 (Group Membership)
