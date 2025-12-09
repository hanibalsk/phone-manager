# Story AP-1.1: Define System Roles with Permissions

## Story
As a super admin, I want to define system roles with specific permissions so I can control access across the platform.

## Status
In Progress

## Acceptance Criteria
1. [ ] 5 system roles implemented (SUPER_ADMIN, ORG_ADMIN, ORG_MANAGER, SUPPORT, VIEWER)
2. [ ] Role-permission mapping stored in database (backend)
3. [ ] API middleware validates permissions on every request (backend)
4. [ ] Permission denied returns 403 with clear message (backend)
5. [ ] Admin UI displays all roles with their permissions
6. [ ] Admin can view permission matrix for each role

## Implementation Notes

### Frontend Components (admin-portal)
- `RoleList` - Display all system roles with permissions
- `RoleDetailView` - Show detailed permissions for a role
- `PermissionMatrix` - Visual matrix of role-permission mappings

### API Endpoints Required (backend must implement)
- `GET /api/admin/roles` - List all system roles
- `GET /api/admin/roles/:id` - Get role details with permissions
- `GET /api/admin/permissions` - List all available permissions

### Data Structures
```typescript
// System Roles
export type SystemRole = "super_admin" | "org_admin" | "org_manager" | "support" | "viewer";

export interface Role {
  id: string;
  name: string;
  code: SystemRole;
  description: string;
  is_system: boolean; // true for predefined roles
  permissions: Permission[];
  created_at: string;
  updated_at: string;
}

export interface Permission {
  id: string;
  code: string; // e.g., "USERS.CREATE", "DEVICES.READ"
  name: string;
  description: string;
  category: string; // e.g., "users", "devices", "organizations"
}

export interface RolePermissionMapping {
  role_id: string;
  permission_id: string;
}
```

### Permission Categories
| Category | Permissions |
|----------|------------|
| users | CREATE, READ, UPDATE, DELETE, SUSPEND, RESET_PASSWORD |
| organizations | CREATE, READ, UPDATE, DELETE, SUSPEND |
| devices | CREATE, READ, UPDATE, DELETE, ADMIN_READ, EXPORT |
| locations | READ, EXPORT, DELETE |
| geofences | CREATE, READ, UPDATE, DELETE |
| alerts | CREATE, READ, UPDATE, DELETE |
| webhooks | CREATE, READ, UPDATE, DELETE |
| trips | READ, EXPORT |
| groups | CREATE, READ, UPDATE, DELETE, ADMIN |
| audit | READ, EXPORT |
| config | READ, UPDATE |
| reports | READ, EXPORT |

## Tasks
- [x] Create Role and Permission types in types/index.ts
- [x] Add rolesApi to api-client.ts
- [x] Create RoleList component
- [x] Create RoleDetailDialog component
- [x] Create PermissionMatrix component
- [x] Add Roles page at /roles
- [x] Add Roles link to sidebar navigation

## Dev Agent Record

### Debug Log
- Planning: Implement frontend components for role management
- Backend API endpoints are assumed to exist and return the expected data structures
- Following existing patterns from organization and user management

### Completion Notes
Frontend components created for role and permission management.

## File List
- admin-portal/types/index.ts (modified)
- admin-portal/lib/api-client.ts (modified)
- admin-portal/components/roles/role-list.tsx (new)
- admin-portal/components/roles/role-detail-dialog.tsx (new)
- admin-portal/components/roles/permission-matrix.tsx (new)
- admin-portal/components/roles/index.ts (new)
- admin-portal/app/(dashboard)/roles/page.tsx (new)
- admin-portal/components/layout/sidebar.tsx (modified)

## Change Log
- Created Role and Permission types
- Created rolesApi with list, get, and permissions methods
- Created RoleList, RoleDetailDialog, PermissionMatrix components
- Added Roles page and sidebar navigation
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
Story AP-1.1 has been successfully implemented with all frontend components for role and permission management. The implementation follows existing patterns in the codebase and provides a complete UI for viewing system roles, their permissions, and a permission matrix. The code is well-structured, type-safe, and follows React best practices.

### Key Findings

#### Low Severity
1. **Missing unit tests** - No test files created for new components (RoleList, RoleDetailDialog, PermissionMatrix, etc.)
2. **Acceptance criteria checkboxes unchecked** - Story file shows tasks completed but acceptance criteria items remain unchecked
3. **Role type constraint** - `Role.code` is typed as `SystemRole` which limits custom roles; consider using `string` type

### Acceptance Criteria Coverage

| Criteria | Status | Notes |
|----------|--------|-------|
| 5 system roles implemented | ✅ Partial | Frontend supports display; backend implementation required |
| Role-permission mapping in database | N/A | Backend responsibility |
| API middleware validates permissions | N/A | Backend responsibility |
| Permission denied returns 403 | N/A | Backend responsibility |
| Admin UI displays all roles | ✅ Complete | RoleList component implemented |
| Admin can view permission matrix | ✅ Complete | PermissionMatrix component implemented |

### Test Coverage and Gaps

- **Gap**: No unit tests for new components
- **Gap**: No integration tests for API client methods
- **Recommendation**: Add tests for RoleList, RoleDetailDialog, PermissionMatrix components

### Architectural Alignment

- ✅ Follows existing component patterns (Card, Badge, Dialog structure)
- ✅ Uses existing hooks (useApi, useFocusTrap)
- ✅ Proper TypeScript typing with interface definitions
- ✅ Follows Next.js App Router conventions

### Security Notes

- ✅ No sensitive data exposed in frontend
- ✅ API calls use authenticated requests via api-client
- ⚠️ Frontend permission checks are UX-only; backend must enforce

### Best-Practices and References

- [Next.js App Router](https://nextjs.org/docs/app)
- [Radix UI Checkbox](https://www.radix-ui.com/docs/primitives/components/checkbox)
- [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/)

### Action Items

1. **[Low]** Add unit tests for RoleList, RoleDetailDialog, and PermissionMatrix components
2. **[Low]** Update acceptance criteria checkboxes in story file to reflect completion
3. **[Low]** Consider relaxing Role.code type from SystemRole to string for custom roles
