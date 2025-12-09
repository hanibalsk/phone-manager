# Story AP-1.4: Permission-Protected Endpoints

## Story Description
As an admin, I want all API endpoints protected by permission checks so unauthorized access is prevented.

## Acceptance Criteria
- [ ] All 45+ endpoints have permission requirements defined
- [ ] Permission middleware applied to all routes (backend responsibility)
- [ ] Organization-scoped data isolation enforced (backend responsibility)
- [ ] Frontend permission checks to hide/disable unauthorized actions

## Technical Implementation (Frontend)

Since the backend is external, this frontend implementation provides:

### Frontend Components & Hooks

1. **usePermissions Hook** (`hooks/use-permissions.ts`)
   - Fetches current user's effective permissions from auth context
   - Provides `hasPermission(permissionCode)` function
   - Provides `hasAnyPermission([...codes])` function
   - Provides `hasAllPermissions([...codes])` function

2. **PermissionGuard Component** (`components/auth/permission-guard.tsx`)
   - Wrapper component that conditionally renders children based on permissions
   - Shows fallback content for unauthorized users
   - Supports single permission or array of permissions

3. **Permission Constants** (`lib/permissions.ts`)
   - Define all permission codes as constants
   - Group permissions by category for easy reference

### Integration Points
- Wrap sensitive actions (Create, Edit, Delete buttons) with PermissionGuard
- Hide navigation items user doesn't have access to
- Show permission-denied message when accessing protected routes

## Dependencies
- Story AP-1.1 through AP-1.3 (Role system)
- Auth context with user role information

## Notes
- Actual enforcement happens on backend - frontend checks are for UX only
- User's permissions come from their role assignments via auth context
- Permission checks should fail-closed (deny if unsure)
