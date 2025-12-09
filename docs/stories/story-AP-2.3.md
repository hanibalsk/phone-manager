# Story AP-2.3: Manage Organization Status

## Story
As a super admin, I want to suspend/reactivate/archive organizations, so that I can manage organization lifecycle and access control.

## Status
Implemented

## Acceptance Criteria
1. ✅ Can suspend organization with reason
2. ✅ Can reactivate suspended organization
3. ✅ Can archive organization
4. ✅ Status changes reflected in UI immediately
5. ✅ Warning displayed when suspending (users will lose access)

## Implementation Notes

### Components Created
- `OrganizationSuspendDialog` - Modal for suspending with reason
- `OrganizationActionsMenu` - Contains reactivate and archive actions

### API Endpoints Used
- `POST /api/admin/organizations/:id/suspend` - Suspend organization
- `POST /api/admin/organizations/:id/reactivate` - Reactivate organization
- `POST /api/admin/organizations/:id/archive` - Archive organization

### Status Flow
```
pending → active ⇄ suspended → archived
                     ↓
              (reactivate)
```

## Tasks
- [x] Create OrganizationSuspendDialog component with reason field
- [x] Add reactivate action in OrganizationActionsMenu
- [x] Add archive action in OrganizationActionsMenu
- [x] Display warning about user access impact
- [x] Integrate with organizationsApi status endpoints

---

## Senior Developer Review (AI)

**Reviewer**: Martin
**Date**: 2025-12-09
**Outcome**: Approve

### Summary
The organization status management feature is well-implemented with proper state transitions, warning dialogs, and user feedback. The suspend dialog appropriately warns users about the impact and requires a reason, while reactivate and archive actions are accessible from the actions menu.

### Key Findings

**Medium**
- No unit tests for `OrganizationSuspendDialog` and status action handlers in `OrganizationActionsMenu`
- No confirmation dialog for archive action (direct action without user confirmation)

**Low**
- Missing loading state feedback for reactivate/archive in the menu button text (shows only while loading, then menu closes)
- Error handling for reactivate/archive doesn't display errors to user (silent failure)

### Acceptance Criteria Coverage
| AC | Status | Notes |
|----|--------|-------|
| AC1: Suspend with reason | ✅ Complete | OrganizationSuspendDialog with required reason |
| AC2: Reactivate suspended org | ✅ Complete | Reactivate button in actions menu |
| AC3: Archive organization | ✅ Complete | Archive button in actions menu |
| AC4: Status changes reflected immediately | ✅ Complete | onActionComplete triggers refresh |
| AC5: Warning on suspend | ✅ Complete | Warning box about user access impact |

### Test Coverage and Gaps
- **Unit Tests**: Not present
- **Integration Tests**: Not present
- **Gap**: Should add tests for:
  - Suspend dialog form validation (reason required)
  - Status-based conditional rendering (different buttons per status)
  - API call error handling
  - State transition validations

### Architectural Alignment
- ✅ Follows existing dialog pattern (Card-based modal, useFocusTrap)
- ✅ Proper separation of concerns (dialog vs menu)
- ✅ Consistent API client usage pattern
- ✅ Status-based conditional rendering is clean

### Security Notes
- ✅ Suspend reason is required (audit trail support)
- ✅ API endpoints properly authenticated
- ⚠️ Consider adding confirmation for destructive archive action
- ⚠️ Backend should log all status changes for audit

### Best-Practices and References
- Consider adding AlertDialog component for destructive actions (archive)
- Status transitions should be validated server-side
- [ARIA Dialog Pattern](https://www.w3.org/WAI/ARIA/apg/patterns/dialog-modal/)

### Action Items
| Priority | Action | Type | Related |
|----------|--------|------|---------|
| Medium | Add unit tests for OrganizationSuspendDialog | TechDebt | AC1, AC5 |
| Medium | Add confirmation dialog for archive action | Enhancement | AC3 |
| Low | Display error messages for reactivate/archive failures | Bug | AC2, AC3 |
| Low | Backend: Log status changes in audit trail | Enhancement | API |

---

**Change Log**
| Date | Change | Author |
|------|--------|--------|
| 2025-12-09 | Senior Developer Review notes appended | AI |
