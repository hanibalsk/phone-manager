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
