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

## Change Log
- 2025-12-09: UserRoleAssignmentDialog component created
- 2025-12-09: "Manage Roles" action added to user actions menu
- 2025-12-09: Senior Developer Review notes appended

---

## Senior Developer Review (AI)

### Reviewer
Martin

### Date
2025-12-09

### Outcome
**Approve** (with minor recommendations)

### Summary
Story AP-1.2 implements the role assignment UI allowing administrators to view, add, and remove role assignments for users. The UserRoleAssignmentDialog component integrates well with the existing user management system and provides a complete workflow for role management with organization scoping.

### Key Findings

#### Low Severity
1. **Missing RoleAssignmentCard component** - Story mentions this component but it was not created; functionality is inline in the dialog
2. **Missing unit tests** - No test files for UserRoleAssignmentDialog
3. **Select element styling** - Native `<select>` elements used instead of Radix UI Select for consistency

#### Medium Severity
1. **Super admin check is client-side only** - The check preventing removal of the last super_admin role at line 90-98 in user-role-assignment-dialog.tsx should also be enforced on the backend

### Acceptance Criteria Coverage

| Criteria | Status | Notes |
|----------|--------|-------|
| User can have different roles in organizations | ✅ Complete | Dialog supports multiple role assignments |
| Role assignment UI with org selector | ✅ Complete | Dropdown for organizations implemented |
| Role changes take effect immediately | ✅ Complete | Uses loadData() refresh after changes |
| Audit log captures role changes | N/A | Backend responsibility |

### Test Coverage and Gaps

- **Gap**: No unit tests for UserRoleAssignmentDialog
- **Gap**: No integration tests for role assignment API calls
- **Recommendation**: Add tests covering role assignment, removal, and super_admin protection

### Architectural Alignment

- ✅ Follows existing dialog patterns (Card, useFocusTrap)
- ✅ Uses useApi hook for data fetching
- ✅ Integrates with existing UserActionsMenu
- ✅ Proper error handling with user feedback

### Security Notes

- ✅ Uses authenticated API calls
- ⚠️ Super admin protection should be backend-enforced, not just frontend
- ✅ No sensitive data exposed in UI

### Best-Practices and References

- [React Hook Form](https://react-hook-form.com/) - Consider for form management
- [Radix UI Select](https://www.radix-ui.com/docs/primitives/components/select) - For consistent styling

### Action Items

1. **[Med]** Ensure backend enforces super_admin role removal protection
2. **[Low]** Add unit tests for UserRoleAssignmentDialog component
3. **[Low]** Consider using Radix UI Select for consistent styling
