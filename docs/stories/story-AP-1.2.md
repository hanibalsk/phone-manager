# Story AP-1.2: Role Assignment UI

## Story Description
As a super admin, I want to assign roles to users for specific organizations so they have appropriate access.

## Acceptance Criteria
- [ ] User can have different roles in different organizations
- [ ] Role assignment UI with organization selector
- [ ] Role changes take effect immediately
- [ ] Audit log captures role changes (backend responsibility)

## Technical Implementation

### Frontend Components

1. **UserRoleAssignmentDialog** (`components/users/user-role-assignment-dialog.tsx`)
   - Modal for viewing and managing a user's role assignments
   - Lists current role assignments with organization context
   - Add new role assignment with role and organization selector
   - Remove existing role assignments
   - Cannot remove last super_admin role

2. **RoleAssignmentCard** (`components/users/role-assignment-card.tsx`)
   - Displays individual role assignment
   - Shows role name, badge, organization name
   - Remove button with confirmation
   - Date assigned and assigned by info

### API Endpoints Used
- `GET /api/admin/users/{userId}/roles` - Get user's role assignments
- `POST /api/admin/users/{userId}/roles` - Assign role to user
- `DELETE /api/admin/users/{userId}/roles/{assignmentId}` - Remove role assignment
- `GET /api/admin/roles` - Get available roles for dropdown
- `GET /api/admin/organizations` - Get organizations for dropdown

### Integration Points
- Integrate with existing UserDetailDialog in users page
- Add "Manage Roles" button to user actions
- Show role assignments count in user list

## Dependencies
- Story AP-1.1 (System roles UI)
- Existing users API and components

## Notes
- Role assignment changes should take effect immediately (no page reload needed)
- Super admins can assign roles to any organization
- Org admins can only assign roles within their organization scope
