# Story AP-1.3: Custom Roles UI

## Story Description
As a super admin, I want to create custom roles with selected permissions so I can fine-tune access control.

## Acceptance Criteria
- [x] Custom role creation form with permission checkboxes (implemented in AP-1.1)
- [ ] Custom roles can be assigned like system roles
- [ ] Custom roles can be edited or deleted
- [ ] Cannot delete role while users are assigned

## Technical Implementation

### Frontend Components

1. **RoleEditDialog** (`components/roles/role-edit-dialog.tsx`)
   - Modal for editing existing custom roles
   - Pre-populated form with current role data
   - Permission checkboxes grouped by category
   - Cannot edit system roles (is_system: true)

2. **RoleDeleteDialog** (`components/roles/role-delete-dialog.tsx`)
   - Confirmation dialog for deleting custom roles
   - Shows user count warning if users are assigned
   - Prevents deletion if users are assigned
   - Success/error handling

3. **Update RoleDetailDialog**
   - Add Edit and Delete buttons for custom roles
   - Hide action buttons for system roles

### API Endpoints Used
- `PUT /api/admin/roles/{id}` - Update role
- `DELETE /api/admin/roles/{id}` - Delete role
- `GET /api/admin/roles` - List roles (includes user_count)

### Integration Points
- Update RoleDetailDialog with edit/delete actions
- Ensure RoleList refreshes after edit/delete operations

## Dependencies
- Story AP-1.1 (System roles UI)

## Notes
- System roles (is_system: true) cannot be edited or deleted
- Custom roles can only be deleted when no users are assigned
- Permission changes take effect immediately for all users with that role
