# Admin Portal Backend API Specification

## Overview

This document specifies all backend API endpoints required to support the Admin Portal functionality for epics AP-1 through AP-12. The Admin Portal provides system administrators with comprehensive tools for managing organizations, users, devices, groups, locations, webhooks, app usage, system configuration, analytics, and audit/compliance features.

**Total Endpoints:** 115
**Base URL:** `/api/admin`
**API Version:** v1

## Table of Contents

1. [Authentication](#authentication)
2. [Common Patterns](#common-patterns)
3. [Epic AP-1: RBAC & Access Control](#epic-ap-1-rbac--access-control)
4. [Epic AP-2: Organization Management](#epic-ap-2-organization-management)
5. [Epic AP-3: User Administration](#epic-ap-3-user-administration)
6. [Epic AP-4: Device Fleet Administration](#epic-ap-4-device-fleet-administration)
7. [Epic AP-5: Groups Administration](#epic-ap-5-groups-administration)
8. [Epic AP-6: Location & Geofence Administration](#epic-ap-6-location--geofence-administration)
9. [Epic AP-7: Webhook Administration](#epic-ap-7-webhook-administration)
10. [Epic AP-8: App Usage & Unlock Requests](#epic-ap-8-app-usage--unlock-requests)
11. [Epic AP-9: System Configuration](#epic-ap-9-system-configuration)
12. [Epic AP-10: Dashboard & Analytics](#epic-ap-10-dashboard--analytics)
13. [Epic AP-11: Audit & Compliance](#epic-ap-11-audit--compliance)
14. [Error Handling](#error-handling)
15. [Rate Limiting](#rate-limiting)

---

## Authentication

All Admin Portal API endpoints require authentication via Bearer token.

### Request Header

```
Authorization: Bearer {access_token}
```

### Token Requirements

- Tokens must be obtained through the standard authentication flow
- Admin endpoints require users with appropriate admin roles/permissions
- Tokens expire after the configured session duration
- Invalid or expired tokens return `401 Unauthorized`

### Permission Model

Admin API access is controlled through Role-Based Access Control (RBAC):
- **Super Admin**: Full access to all endpoints
- **Organization Admin**: Access limited to their organization's resources
- **Support Admin**: Read-only access with limited write operations
- **Auditor**: Read-only access to audit logs and compliance data

---

## Common Patterns

### Pagination

Paginated endpoints accept the following query parameters:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 1 | Page number (1-indexed) |
| `limit` | integer | 20 | Items per page (max: 100) |

**Paginated Response Structure:**

```typescript
interface PaginatedResponse<T> {
  data: T[];
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
}
```

### Sorting

Sortable endpoints accept:

| Parameter | Type | Description |
|-----------|------|-------------|
| `sort_by` | string | Field name to sort by |
| `sort_order` | string | `asc` or `desc` (default: `asc`) |

### Filtering

Filter parameters are endpoint-specific and documented with each endpoint.

### Standard Response Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created |
| 204 | No Content (successful deletion) |
| 400 | Bad Request - Invalid parameters |
| 401 | Unauthorized - Invalid or missing token |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found |
| 409 | Conflict - Resource already exists |
| 422 | Unprocessable Entity - Validation error |
| 429 | Too Many Requests - Rate limited |
| 500 | Internal Server Error |

### Common TypeScript Types

```typescript
type UUID = string;
type ISO8601DateTime = string;
type Email = string;

interface AuditInfo {
  created_at: ISO8601DateTime;
  created_by: UUID;
  updated_at: ISO8601DateTime;
  updated_by: UUID;
}

interface ApiError {
  error: {
    code: string;
    message: string;
    details?: Record<string, string[]>;
  };
}
```

---

## Epic AP-1: RBAC & Access Control

Role-Based Access Control endpoints for managing roles and permissions.

### Types

```typescript
interface Permission {
  id: UUID;
  name: string;
  description: string;
  resource: string;
  action: 'create' | 'read' | 'update' | 'delete' | 'manage';
  created_at: ISO8601DateTime;
}

interface Role {
  id: UUID;
  name: string;
  description: string;
  permissions: Permission[];
  is_system: boolean;
  user_count: number;
  created_at: ISO8601DateTime;
  updated_at: ISO8601DateTime;
}

interface RoleListItem {
  id: UUID;
  name: string;
  description: string;
  permission_count: number;
  user_count: number;
  is_system: boolean;
  created_at: ISO8601DateTime;
}
```

### Endpoints

#### GET /api/admin/roles

List all roles with pagination.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `search` | string | Search by role name |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "role-uuid-1",
      "name": "Super Admin",
      "description": "Full system access",
      "permission_count": 45,
      "user_count": 3,
      "is_system": true,
      "created_at": "2024-01-15T10:30:00Z"
    },
    {
      "id": "role-uuid-2",
      "name": "Organization Admin",
      "description": "Organization-level administration",
      "permission_count": 28,
      "user_count": 15,
      "is_system": true,
      "created_at": "2024-01-15T10:30:00Z"
    },
    {
      "id": "role-uuid-3",
      "name": "Support Admin",
      "description": "Customer support access",
      "permission_count": 12,
      "user_count": 8,
      "is_system": false,
      "created_at": "2024-02-20T14:15:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 3,
    "totalPages": 1
  }
}
```

---

#### GET /api/admin/roles/:id

Get a specific role with all its permissions.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Role ID |

**Response:** `200 OK`

```json
{
  "id": "role-uuid-1",
  "name": "Super Admin",
  "description": "Full system access with all permissions",
  "permissions": [
    {
      "id": "perm-uuid-1",
      "name": "users:create",
      "description": "Create new users",
      "resource": "users",
      "action": "create",
      "created_at": "2024-01-15T10:30:00Z"
    },
    {
      "id": "perm-uuid-2",
      "name": "users:read",
      "description": "View user information",
      "resource": "users",
      "action": "read",
      "created_at": "2024-01-15T10:30:00Z"
    },
    {
      "id": "perm-uuid-3",
      "name": "users:update",
      "description": "Update user information",
      "resource": "users",
      "action": "update",
      "created_at": "2024-01-15T10:30:00Z"
    },
    {
      "id": "perm-uuid-4",
      "name": "organizations:manage",
      "description": "Full organization management",
      "resource": "organizations",
      "action": "manage",
      "created_at": "2024-01-15T10:30:00Z"
    }
  ],
  "is_system": true,
  "user_count": 3,
  "created_at": "2024-01-15T10:30:00Z",
  "updated_at": "2024-01-15T10:30:00Z"
}
```

**Error Response:** `404 Not Found`

```json
{
  "error": {
    "code": "ROLE_NOT_FOUND",
    "message": "Role with ID 'role-uuid-invalid' not found"
  }
}
```

---

#### GET /api/admin/permissions

List all available permissions.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `resource` | string | Filter by resource (e.g., `users`, `organizations`) |
| `action` | string | Filter by action type |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "perm-uuid-1",
      "name": "users:create",
      "description": "Create new users",
      "resource": "users",
      "action": "create",
      "created_at": "2024-01-15T10:30:00Z"
    },
    {
      "id": "perm-uuid-2",
      "name": "users:read",
      "description": "View user information",
      "resource": "users",
      "action": "read",
      "created_at": "2024-01-15T10:30:00Z"
    },
    {
      "id": "perm-uuid-3",
      "name": "users:update",
      "description": "Update user information",
      "resource": "users",
      "action": "update",
      "created_at": "2024-01-15T10:30:00Z"
    },
    {
      "id": "perm-uuid-4",
      "name": "users:delete",
      "description": "Delete users",
      "resource": "users",
      "action": "delete",
      "created_at": "2024-01-15T10:30:00Z"
    },
    {
      "id": "perm-uuid-5",
      "name": "organizations:manage",
      "description": "Full organization management",
      "resource": "organizations",
      "action": "manage",
      "created_at": "2024-01-15T10:30:00Z"
    },
    {
      "id": "perm-uuid-6",
      "name": "devices:read",
      "description": "View device information",
      "resource": "devices",
      "action": "read",
      "created_at": "2024-01-15T10:30:00Z"
    },
    {
      "id": "perm-uuid-7",
      "name": "audit:read",
      "description": "View audit logs",
      "resource": "audit",
      "action": "read",
      "created_at": "2024-01-15T10:30:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "total": 45,
    "totalPages": 1
  }
}
```

---

## Epic AP-2: Organization Management

Endpoints for managing organizations, their features, and limits.

### Types

```typescript
interface Organization {
  id: UUID;
  name: string;
  slug: string;
  status: 'active' | 'suspended' | 'pending';
  plan: 'free' | 'starter' | 'professional' | 'enterprise';
  features: OrganizationFeatures;
  limits: OrganizationLimits;
  stats: OrganizationStats;
  billing_email: Email;
  contact_name: string;
  contact_email: Email;
  created_at: ISO8601DateTime;
  updated_at: ISO8601DateTime;
}

interface OrganizationFeatures {
  location_tracking: boolean;
  geofencing: boolean;
  app_usage_monitoring: boolean;
  webhooks: boolean;
  api_access: boolean;
  custom_branding: boolean;
  sso_enabled: boolean;
  advanced_analytics: boolean;
}

interface OrganizationLimits {
  max_users: number;
  max_devices: number;
  max_groups: number;
  max_geofences: number;
  max_webhooks: number;
  location_history_days: number;
  api_rate_limit: number;
}

interface OrganizationStats {
  user_count: number;
  device_count: number;
  active_device_count: number;
  group_count: number;
  storage_used_bytes: number;
}

interface OrganizationListItem {
  id: UUID;
  name: string;
  slug: string;
  status: 'active' | 'suspended' | 'pending';
  plan: string;
  user_count: number;
  device_count: number;
  created_at: ISO8601DateTime;
}

interface CreateOrganizationRequest {
  name: string;
  slug?: string;
  plan: 'free' | 'starter' | 'professional' | 'enterprise';
  billing_email: Email;
  contact_name: string;
  contact_email: Email;
  features?: Partial<OrganizationFeatures>;
  limits?: Partial<OrganizationLimits>;
}

interface UpdateOrganizationRequest {
  name?: string;
  status?: 'active' | 'suspended';
  plan?: 'free' | 'starter' | 'professional' | 'enterprise';
  billing_email?: Email;
  contact_name?: string;
  contact_email?: Email;
}
```

### Endpoints

#### GET /api/admin/organizations

List all organizations with pagination and filtering.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `search` | string | Search by name or slug |
| `status` | string | Filter by status |
| `plan` | string | Filter by plan type |
| `sort_by` | string | Sort field (name, created_at, user_count) |
| `sort_order` | string | asc or desc |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "org-uuid-1",
      "name": "Acme Corporation",
      "slug": "acme-corp",
      "status": "active",
      "plan": "enterprise",
      "user_count": 150,
      "device_count": 320,
      "created_at": "2023-06-15T09:00:00Z"
    },
    {
      "id": "org-uuid-2",
      "name": "Tech Startup Inc",
      "slug": "tech-startup",
      "status": "active",
      "plan": "professional",
      "user_count": 25,
      "device_count": 48,
      "created_at": "2024-01-20T14:30:00Z"
    },
    {
      "id": "org-uuid-3",
      "name": "Small Business LLC",
      "slug": "small-business",
      "status": "suspended",
      "plan": "starter",
      "user_count": 5,
      "device_count": 8,
      "created_at": "2024-03-10T11:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 45,
    "totalPages": 3
  }
}
```

---

#### POST /api/admin/organizations

Create a new organization.

**Request Body:**

```json
{
  "name": "New Organization",
  "slug": "new-org",
  "plan": "professional",
  "billing_email": "billing@neworg.com",
  "contact_name": "John Smith",
  "contact_email": "john.smith@neworg.com",
  "features": {
    "location_tracking": true,
    "geofencing": true,
    "app_usage_monitoring": true,
    "webhooks": true,
    "api_access": true,
    "custom_branding": false,
    "sso_enabled": false,
    "advanced_analytics": true
  },
  "limits": {
    "max_users": 50,
    "max_devices": 100,
    "max_groups": 20,
    "max_geofences": 50,
    "max_webhooks": 10,
    "location_history_days": 90,
    "api_rate_limit": 1000
  }
}
```

**Response:** `201 Created`

```json
{
  "id": "org-uuid-new",
  "name": "New Organization",
  "slug": "new-org",
  "status": "active",
  "plan": "professional",
  "features": {
    "location_tracking": true,
    "geofencing": true,
    "app_usage_monitoring": true,
    "webhooks": true,
    "api_access": true,
    "custom_branding": false,
    "sso_enabled": false,
    "advanced_analytics": true
  },
  "limits": {
    "max_users": 50,
    "max_devices": 100,
    "max_groups": 20,
    "max_geofences": 50,
    "max_webhooks": 10,
    "location_history_days": 90,
    "api_rate_limit": 1000
  },
  "stats": {
    "user_count": 0,
    "device_count": 0,
    "active_device_count": 0,
    "group_count": 0,
    "storage_used_bytes": 0
  },
  "billing_email": "billing@neworg.com",
  "contact_name": "John Smith",
  "contact_email": "john.smith@neworg.com",
  "created_at": "2024-12-10T15:00:00Z",
  "updated_at": "2024-12-10T15:00:00Z"
}
```

**Error Response:** `409 Conflict`

```json
{
  "error": {
    "code": "SLUG_EXISTS",
    "message": "Organization with slug 'new-org' already exists"
  }
}
```

---

#### GET /api/admin/organizations/:id

Get detailed organization information.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Organization ID |

**Response:** `200 OK`

```json
{
  "id": "org-uuid-1",
  "name": "Acme Corporation",
  "slug": "acme-corp",
  "status": "active",
  "plan": "enterprise",
  "features": {
    "location_tracking": true,
    "geofencing": true,
    "app_usage_monitoring": true,
    "webhooks": true,
    "api_access": true,
    "custom_branding": true,
    "sso_enabled": true,
    "advanced_analytics": true
  },
  "limits": {
    "max_users": 500,
    "max_devices": 1000,
    "max_groups": 100,
    "max_geofences": 200,
    "max_webhooks": 50,
    "location_history_days": 365,
    "api_rate_limit": 10000
  },
  "stats": {
    "user_count": 150,
    "device_count": 320,
    "active_device_count": 285,
    "group_count": 24,
    "storage_used_bytes": 5368709120
  },
  "billing_email": "billing@acme.com",
  "contact_name": "Jane Doe",
  "contact_email": "jane.doe@acme.com",
  "created_at": "2023-06-15T09:00:00Z",
  "updated_at": "2024-11-20T16:45:00Z"
}
```

---

#### PUT /api/admin/organizations/:id

Update organization details.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Organization ID |

**Request Body:**

```json
{
  "name": "Acme Corporation Updated",
  "plan": "enterprise",
  "billing_email": "new-billing@acme.com",
  "contact_name": "John Doe",
  "contact_email": "john.doe@acme.com"
}
```

**Response:** `200 OK`

```json
{
  "id": "org-uuid-1",
  "name": "Acme Corporation Updated",
  "slug": "acme-corp",
  "status": "active",
  "plan": "enterprise",
  "features": {
    "location_tracking": true,
    "geofencing": true,
    "app_usage_monitoring": true,
    "webhooks": true,
    "api_access": true,
    "custom_branding": true,
    "sso_enabled": true,
    "advanced_analytics": true
  },
  "limits": {
    "max_users": 500,
    "max_devices": 1000,
    "max_groups": 100,
    "max_geofences": 200,
    "max_webhooks": 50,
    "location_history_days": 365,
    "api_rate_limit": 10000
  },
  "stats": {
    "user_count": 150,
    "device_count": 320,
    "active_device_count": 285,
    "group_count": 24,
    "storage_used_bytes": 5368709120
  },
  "billing_email": "new-billing@acme.com",
  "contact_name": "John Doe",
  "contact_email": "john.doe@acme.com",
  "created_at": "2023-06-15T09:00:00Z",
  "updated_at": "2024-12-10T15:30:00Z"
}
```

---

#### DELETE /api/admin/organizations/:id

Delete an organization (soft delete).

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Organization ID |

**Response:** `204 No Content`

**Error Response:** `400 Bad Request`

```json
{
  "error": {
    "code": "ORGANIZATION_HAS_ACTIVE_RESOURCES",
    "message": "Cannot delete organization with active users or devices. Please remove all resources first."
  }
}
```

---

#### PUT /api/admin/organizations/:id/features

Update organization feature flags.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Organization ID |

**Request Body:**

```json
{
  "location_tracking": true,
  "geofencing": true,
  "app_usage_monitoring": true,
  "webhooks": true,
  "api_access": true,
  "custom_branding": true,
  "sso_enabled": false,
  "advanced_analytics": true
}
```

**Response:** `200 OK`

```json
{
  "id": "org-uuid-1",
  "features": {
    "location_tracking": true,
    "geofencing": true,
    "app_usage_monitoring": true,
    "webhooks": true,
    "api_access": true,
    "custom_branding": true,
    "sso_enabled": false,
    "advanced_analytics": true
  },
  "updated_at": "2024-12-10T15:45:00Z"
}
```

---

#### GET /api/admin/organizations/:id/stats

Get organization statistics and usage metrics.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Organization ID |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `period` | string | Time period: `day`, `week`, `month`, `year` |

**Response:** `200 OK`

```json
{
  "organization_id": "org-uuid-1",
  "period": "month",
  "current_usage": {
    "user_count": 150,
    "device_count": 320,
    "active_device_count": 285,
    "group_count": 24,
    "geofence_count": 45,
    "webhook_count": 12,
    "storage_used_bytes": 5368709120,
    "api_calls_count": 125000
  },
  "limits": {
    "max_users": 500,
    "max_devices": 1000,
    "max_groups": 100,
    "max_geofences": 200,
    "max_webhooks": 50,
    "storage_limit_bytes": 10737418240,
    "api_rate_limit": 10000
  },
  "usage_percentages": {
    "users": 30,
    "devices": 32,
    "groups": 24,
    "geofences": 22.5,
    "webhooks": 24,
    "storage": 50
  },
  "trends": {
    "user_growth": 5.2,
    "device_growth": 8.1,
    "api_usage_growth": 12.5
  },
  "generated_at": "2024-12-10T16:00:00Z"
}
```

---

## Epic AP-3: User Administration

Endpoints for managing users, sessions, and MFA.

### Types

```typescript
interface User {
  id: UUID;
  email: Email;
  name: string;
  status: 'active' | 'suspended' | 'pending' | 'deactivated';
  role: Role;
  organization_id: UUID;
  organization_name: string;
  mfa_enabled: boolean;
  mfa_methods: MfaMethod[];
  last_login_at: ISO8601DateTime | null;
  login_count: number;
  created_at: ISO8601DateTime;
  updated_at: ISO8601DateTime;
}

interface UserListItem {
  id: UUID;
  email: Email;
  name: string;
  status: 'active' | 'suspended' | 'pending' | 'deactivated';
  role_name: string;
  organization_name: string;
  mfa_enabled: boolean;
  last_login_at: ISO8601DateTime | null;
  created_at: ISO8601DateTime;
}

interface CreateUserRequest {
  email: Email;
  name: string;
  role_id: UUID;
  organization_id: UUID;
  send_invite: boolean;
  require_mfa?: boolean;
}

interface UpdateUserRequest {
  email?: Email;
  name?: string;
  role_id?: UUID;
  organization_id?: UUID;
}

interface UserSession {
  id: UUID;
  user_id: UUID;
  ip_address: string;
  user_agent: string;
  device_type: 'desktop' | 'mobile' | 'tablet' | 'unknown';
  location: string | null;
  created_at: ISO8601DateTime;
  last_active_at: ISO8601DateTime;
  expires_at: ISO8601DateTime;
  is_current: boolean;
}

interface MfaMethod {
  type: 'totp' | 'sms' | 'email' | 'security_key';
  enabled: boolean;
  verified_at: ISO8601DateTime | null;
  last_used_at: ISO8601DateTime | null;
}

interface MfaStatus {
  enabled: boolean;
  enforced: boolean;
  methods: MfaMethod[];
  backup_codes_remaining: number;
  last_verified_at: ISO8601DateTime | null;
}
```

### Endpoints

#### GET /api/admin/users

List all users with pagination and filtering.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `search` | string | Search by name or email |
| `status` | string | Filter by status |
| `organization_id` | UUID | Filter by organization |
| `role_id` | UUID | Filter by role |
| `mfa_enabled` | boolean | Filter by MFA status |
| `sort_by` | string | Sort field |
| `sort_order` | string | asc or desc |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "user-uuid-1",
      "email": "admin@acme.com",
      "name": "John Admin",
      "status": "active",
      "role_name": "Super Admin",
      "organization_name": "Acme Corporation",
      "mfa_enabled": true,
      "last_login_at": "2024-12-10T08:30:00Z",
      "created_at": "2023-06-15T09:00:00Z"
    },
    {
      "id": "user-uuid-2",
      "email": "jane@acme.com",
      "name": "Jane Doe",
      "status": "active",
      "role_name": "Organization Admin",
      "organization_name": "Acme Corporation",
      "mfa_enabled": true,
      "last_login_at": "2024-12-09T14:20:00Z",
      "created_at": "2023-08-20T11:30:00Z"
    },
    {
      "id": "user-uuid-3",
      "email": "suspended@example.com",
      "name": "Suspended User",
      "status": "suspended",
      "role_name": "Support Admin",
      "organization_name": "Tech Startup Inc",
      "mfa_enabled": false,
      "last_login_at": "2024-11-01T10:00:00Z",
      "created_at": "2024-02-15T16:45:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 156,
    "totalPages": 8
  }
}
```

---

#### POST /api/admin/users

Create a new user.

**Request Body:**

```json
{
  "email": "newuser@acme.com",
  "name": "New User",
  "role_id": "role-uuid-2",
  "organization_id": "org-uuid-1",
  "send_invite": true,
  "require_mfa": true
}
```

**Response:** `201 Created`

```json
{
  "id": "user-uuid-new",
  "email": "newuser@acme.com",
  "name": "New User",
  "status": "pending",
  "role": {
    "id": "role-uuid-2",
    "name": "Organization Admin",
    "description": "Organization-level administration"
  },
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "mfa_enabled": false,
  "mfa_methods": [],
  "last_login_at": null,
  "login_count": 0,
  "created_at": "2024-12-10T16:30:00Z",
  "updated_at": "2024-12-10T16:30:00Z",
  "invite_sent": true,
  "invite_expires_at": "2024-12-17T16:30:00Z"
}
```

**Error Response:** `409 Conflict`

```json
{
  "error": {
    "code": "USER_EXISTS",
    "message": "User with email 'newuser@acme.com' already exists"
  }
}
```

---

#### GET /api/admin/users/:id

Get detailed user information.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | User ID |

**Response:** `200 OK`

```json
{
  "id": "user-uuid-1",
  "email": "admin@acme.com",
  "name": "John Admin",
  "status": "active",
  "role": {
    "id": "role-uuid-1",
    "name": "Super Admin",
    "description": "Full system access",
    "permissions": [
      {
        "id": "perm-uuid-1",
        "name": "users:manage",
        "resource": "users",
        "action": "manage"
      }
    ]
  },
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "mfa_enabled": true,
  "mfa_methods": [
    {
      "type": "totp",
      "enabled": true,
      "verified_at": "2023-06-20T10:00:00Z",
      "last_used_at": "2024-12-10T08:30:00Z"
    }
  ],
  "last_login_at": "2024-12-10T08:30:00Z",
  "login_count": 523,
  "created_at": "2023-06-15T09:00:00Z",
  "updated_at": "2024-12-10T08:30:00Z"
}
```

---

#### PUT /api/admin/users/:id

Update user details.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | User ID |

**Request Body:**

```json
{
  "name": "John Admin Updated",
  "role_id": "role-uuid-1",
  "organization_id": "org-uuid-1"
}
```

**Response:** `200 OK`

```json
{
  "id": "user-uuid-1",
  "email": "admin@acme.com",
  "name": "John Admin Updated",
  "status": "active",
  "role": {
    "id": "role-uuid-1",
    "name": "Super Admin",
    "description": "Full system access"
  },
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "mfa_enabled": true,
  "mfa_methods": [
    {
      "type": "totp",
      "enabled": true,
      "verified_at": "2023-06-20T10:00:00Z",
      "last_used_at": "2024-12-10T08:30:00Z"
    }
  ],
  "last_login_at": "2024-12-10T08:30:00Z",
  "login_count": 523,
  "created_at": "2023-06-15T09:00:00Z",
  "updated_at": "2024-12-10T17:00:00Z"
}
```

---

#### POST /api/admin/users/:id/suspend

Suspend a user account.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | User ID |

**Request Body:**

```json
{
  "reason": "Policy violation - unauthorized access attempts",
  "notify_user": true
}
```

**Response:** `200 OK`

```json
{
  "id": "user-uuid-1",
  "email": "admin@acme.com",
  "name": "John Admin",
  "status": "suspended",
  "suspended_at": "2024-12-10T17:15:00Z",
  "suspended_reason": "Policy violation - unauthorized access attempts",
  "suspended_by": "admin-user-uuid"
}
```

---

#### POST /api/admin/users/:id/reactivate

Reactivate a suspended user account.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | User ID |

**Request Body:**

```json
{
  "notify_user": true
}
```

**Response:** `200 OK`

```json
{
  "id": "user-uuid-1",
  "email": "admin@acme.com",
  "name": "John Admin",
  "status": "active",
  "reactivated_at": "2024-12-10T17:30:00Z",
  "reactivated_by": "admin-user-uuid"
}
```

---

#### POST /api/admin/users/:id/reset-password

Trigger a password reset for a user.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | User ID |

**Request Body:**

```json
{
  "notify_user": true,
  "expire_sessions": true
}
```

**Response:** `200 OK`

```json
{
  "success": true,
  "message": "Password reset email sent",
  "reset_link_expires_at": "2024-12-11T17:45:00Z",
  "sessions_expired": 3
}
```

---

#### GET /api/admin/users/:id/sessions

Get all active sessions for a user.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | User ID |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "session-uuid-1",
      "user_id": "user-uuid-1",
      "ip_address": "192.168.1.100",
      "user_agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
      "device_type": "desktop",
      "location": "San Francisco, CA, US",
      "created_at": "2024-12-10T08:30:00Z",
      "last_active_at": "2024-12-10T17:45:00Z",
      "expires_at": "2024-12-11T08:30:00Z",
      "is_current": true
    },
    {
      "id": "session-uuid-2",
      "user_id": "user-uuid-1",
      "ip_address": "10.0.0.50",
      "user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)",
      "device_type": "mobile",
      "location": "San Francisco, CA, US",
      "created_at": "2024-12-09T14:20:00Z",
      "last_active_at": "2024-12-10T12:00:00Z",
      "expires_at": "2024-12-10T14:20:00Z",
      "is_current": false
    }
  ],
  "total": 2
}
```

---

#### DELETE /api/admin/users/:id/sessions/:sessionId

Revoke a specific user session.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | User ID |
| `sessionId` | UUID | Session ID |

**Response:** `204 No Content`

---

#### DELETE /api/admin/users/:id/sessions

Revoke all sessions for a user.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | User ID |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `exclude_current` | boolean | Exclude current session (default: false) |

**Response:** `200 OK`

```json
{
  "revoked_count": 3,
  "message": "All sessions revoked successfully"
}
```

---

#### GET /api/admin/users/:id/mfa

Get MFA status and configuration for a user.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | User ID |

**Response:** `200 OK`

```json
{
  "user_id": "user-uuid-1",
  "enabled": true,
  "enforced": true,
  "methods": [
    {
      "type": "totp",
      "enabled": true,
      "verified_at": "2023-06-20T10:00:00Z",
      "last_used_at": "2024-12-10T08:30:00Z"
    },
    {
      "type": "sms",
      "enabled": false,
      "verified_at": null,
      "last_used_at": null
    },
    {
      "type": "security_key",
      "enabled": true,
      "verified_at": "2024-01-15T09:00:00Z",
      "last_used_at": "2024-12-08T16:00:00Z"
    }
  ],
  "backup_codes_remaining": 7,
  "last_verified_at": "2024-12-10T08:30:00Z"
}
```

---

#### POST /api/admin/users/:id/mfa/force

Force MFA enrollment for a user.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | User ID |

**Request Body:**

```json
{
  "required_methods": ["totp"],
  "grace_period_hours": 48,
  "notify_user": true
}
```

**Response:** `200 OK`

```json
{
  "user_id": "user-uuid-1",
  "mfa_enforced": true,
  "required_methods": ["totp"],
  "enrollment_deadline": "2024-12-12T18:00:00Z",
  "notification_sent": true
}
```

---

#### POST /api/admin/users/:id/mfa/reset

Reset MFA for a user (removes all MFA methods).

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | User ID |

**Request Body:**

```json
{
  "reason": "User lost access to authenticator device",
  "require_re_enrollment": true,
  "notify_user": true
}
```

**Response:** `200 OK`

```json
{
  "user_id": "user-uuid-1",
  "mfa_reset": true,
  "methods_removed": ["totp", "security_key"],
  "re_enrollment_required": true,
  "notification_sent": true
}
```

---

## Epic AP-4: Device Fleet Administration

Endpoints for managing devices, bulk operations, and enrollment tokens.

### Types

```typescript
interface Device {
  id: UUID;
  name: string;
  identifier: string;
  platform: 'ios' | 'android';
  os_version: string;
  app_version: string;
  status: 'active' | 'suspended' | 'inactive' | 'pending';
  user_id: UUID;
  user_name: string;
  user_email: Email;
  organization_id: UUID;
  organization_name: string;
  last_seen_at: ISO8601DateTime | null;
  last_location: DeviceLocation | null;
  battery_level: number | null;
  enrolled_at: ISO8601DateTime;
  created_at: ISO8601DateTime;
  updated_at: ISO8601DateTime;
}

interface DeviceListItem {
  id: UUID;
  name: string;
  identifier: string;
  platform: 'ios' | 'android';
  status: 'active' | 'suspended' | 'inactive' | 'pending';
  user_name: string;
  organization_name: string;
  last_seen_at: ISO8601DateTime | null;
  battery_level: number | null;
}

interface DeviceLocation {
  latitude: number;
  longitude: number;
  accuracy: number;
  timestamp: ISO8601DateTime;
}

interface EnrollmentToken {
  id: UUID;
  token: string;
  name: string;
  organization_id: UUID;
  organization_name: string;
  max_uses: number | null;
  current_uses: number;
  expires_at: ISO8601DateTime | null;
  status: 'active' | 'expired' | 'revoked' | 'exhausted';
  created_by: UUID;
  created_at: ISO8601DateTime;
}

interface CreateEnrollmentTokenRequest {
  name: string;
  organization_id: UUID;
  max_uses?: number;
  expires_at?: ISO8601DateTime;
}

interface BulkOperationRequest {
  device_ids: UUID[];
  reason?: string;
  notify_users?: boolean;
}

interface BulkOperationResult {
  successful: UUID[];
  failed: Array<{
    id: UUID;
    error: string;
  }>;
  total: number;
  success_count: number;
  failure_count: number;
}

interface InactiveDevice {
  id: UUID;
  name: string;
  identifier: string;
  platform: 'ios' | 'android';
  user_name: string;
  user_email: Email;
  organization_name: string;
  last_seen_at: ISO8601DateTime;
  inactive_days: number;
}
```

### Endpoints

#### GET /api/admin/devices

List all devices with pagination and filtering.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `search` | string | Search by name, identifier, or user |
| `status` | string | Filter by status |
| `platform` | string | Filter by platform (ios, android) |
| `organization_id` | UUID | Filter by organization |
| `user_id` | UUID | Filter by user |
| `last_seen_after` | ISO8601DateTime | Filter by last seen date |
| `last_seen_before` | ISO8601DateTime | Filter by last seen date |
| `sort_by` | string | Sort field |
| `sort_order` | string | asc or desc |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "device-uuid-1",
      "name": "John's iPhone",
      "identifier": "ABC123DEF456",
      "platform": "ios",
      "status": "active",
      "user_name": "John Admin",
      "organization_name": "Acme Corporation",
      "last_seen_at": "2024-12-10T17:30:00Z",
      "battery_level": 85
    },
    {
      "id": "device-uuid-2",
      "name": "Work Android",
      "identifier": "XYZ789GHI012",
      "platform": "android",
      "status": "active",
      "user_name": "Jane Doe",
      "organization_name": "Acme Corporation",
      "last_seen_at": "2024-12-10T16:45:00Z",
      "battery_level": 42
    },
    {
      "id": "device-uuid-3",
      "name": "Old Device",
      "identifier": "OLD111222333",
      "platform": "android",
      "status": "inactive",
      "user_name": "Bob Smith",
      "organization_name": "Tech Startup Inc",
      "last_seen_at": "2024-11-01T10:00:00Z",
      "battery_level": null
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 320,
    "totalPages": 16
  }
}
```

---

#### GET /api/admin/devices/:id

Get detailed device information.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Device ID |

**Response:** `200 OK`

```json
{
  "id": "device-uuid-1",
  "name": "John's iPhone",
  "identifier": "ABC123DEF456",
  "platform": "ios",
  "os_version": "17.1.2",
  "app_version": "2.5.0",
  "status": "active",
  "user_id": "user-uuid-1",
  "user_name": "John Admin",
  "user_email": "admin@acme.com",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "last_seen_at": "2024-12-10T17:30:00Z",
  "last_location": {
    "latitude": 37.7749,
    "longitude": -122.4194,
    "accuracy": 10.5,
    "timestamp": "2024-12-10T17:30:00Z"
  },
  "battery_level": 85,
  "enrolled_at": "2023-06-15T10:00:00Z",
  "created_at": "2023-06-15T10:00:00Z",
  "updated_at": "2024-12-10T17:30:00Z"
}
```

---

#### POST /api/admin/devices/:id/suspend

Suspend a device.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Device ID |

**Request Body:**

```json
{
  "reason": "Device reported lost",
  "notify_user": true
}
```

**Response:** `200 OK`

```json
{
  "id": "device-uuid-1",
  "status": "suspended",
  "suspended_at": "2024-12-10T18:00:00Z",
  "suspended_reason": "Device reported lost",
  "suspended_by": "admin-user-uuid"
}
```

---

#### POST /api/admin/devices/:id/reactivate

Reactivate a suspended device.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Device ID |

**Request Body:**

```json
{
  "notify_user": true
}
```

**Response:** `200 OK`

```json
{
  "id": "device-uuid-1",
  "status": "active",
  "reactivated_at": "2024-12-10T18:15:00Z",
  "reactivated_by": "admin-user-uuid"
}
```

---

#### DELETE /api/admin/devices/:id

Delete a device.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Device ID |

**Response:** `204 No Content`

---

#### POST /api/admin/devices/bulk/suspend

Bulk suspend multiple devices.

**Request Body:**

```json
{
  "device_ids": ["device-uuid-1", "device-uuid-2", "device-uuid-3"],
  "reason": "Security incident - precautionary measure",
  "notify_users": true
}
```

**Response:** `200 OK`

```json
{
  "successful": ["device-uuid-1", "device-uuid-2"],
  "failed": [
    {
      "id": "device-uuid-3",
      "error": "Device already suspended"
    }
  ],
  "total": 3,
  "success_count": 2,
  "failure_count": 1
}
```

---

#### POST /api/admin/devices/bulk/reactivate

Bulk reactivate multiple devices.

**Request Body:**

```json
{
  "device_ids": ["device-uuid-1", "device-uuid-2"],
  "notify_users": true
}
```

**Response:** `200 OK`

```json
{
  "successful": ["device-uuid-1", "device-uuid-2"],
  "failed": [],
  "total": 2,
  "success_count": 2,
  "failure_count": 0
}
```

---

#### POST /api/admin/devices/bulk/delete

Bulk delete multiple devices.

**Request Body:**

```json
{
  "device_ids": ["device-uuid-1", "device-uuid-2", "device-uuid-3"],
  "reason": "Devices decommissioned"
}
```

**Response:** `200 OK`

```json
{
  "successful": ["device-uuid-1", "device-uuid-2", "device-uuid-3"],
  "failed": [],
  "total": 3,
  "success_count": 3,
  "failure_count": 0
}
```

---

#### GET /api/admin/devices/inactive

List inactive devices.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `inactive_days` | integer | Minimum days inactive (default: 30) |
| `organization_id` | UUID | Filter by organization |
| `platform` | string | Filter by platform |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "device-uuid-3",
      "name": "Old Device",
      "identifier": "OLD111222333",
      "platform": "android",
      "user_name": "Bob Smith",
      "user_email": "bob@techstartup.com",
      "organization_name": "Tech Startup Inc",
      "last_seen_at": "2024-11-01T10:00:00Z",
      "inactive_days": 39
    },
    {
      "id": "device-uuid-4",
      "name": "Abandoned Phone",
      "identifier": "ABN444555666",
      "platform": "ios",
      "user_name": "Alice Johnson",
      "user_email": "alice@acme.com",
      "organization_name": "Acme Corporation",
      "last_seen_at": "2024-10-15T08:00:00Z",
      "inactive_days": 56
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 15,
    "totalPages": 1
  }
}
```

---

#### POST /api/admin/devices/notify

Send notification to device owners.

**Request Body:**

```json
{
  "device_ids": ["device-uuid-3", "device-uuid-4"],
  "notification_type": "inactive_warning",
  "message": "Your device has been inactive for over 30 days. Please open the app to maintain connectivity.",
  "channels": ["email", "push"]
}
```

**Response:** `200 OK`

```json
{
  "notifications_sent": 4,
  "devices_notified": 2,
  "channels_used": ["email", "push"],
  "failed": []
}
```

---

#### GET /api/admin/enrollment/tokens

List enrollment tokens.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `status` | string | Filter by status |
| `organization_id` | UUID | Filter by organization |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "token-uuid-1",
      "token": "ENR-ABC123-XYZ789",
      "name": "Q4 2024 Onboarding",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "max_uses": 50,
      "current_uses": 23,
      "expires_at": "2024-12-31T23:59:59Z",
      "status": "active",
      "created_by": "user-uuid-1",
      "created_at": "2024-10-01T09:00:00Z"
    },
    {
      "id": "token-uuid-2",
      "token": "ENR-DEF456-UVW012",
      "name": "Unlimited Token",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "max_uses": null,
      "current_uses": 145,
      "expires_at": null,
      "status": "active",
      "created_by": "user-uuid-1",
      "created_at": "2023-06-15T09:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 8,
    "totalPages": 1
  }
}
```

---

#### POST /api/admin/enrollment/tokens

Create a new enrollment token.

**Request Body:**

```json
{
  "name": "2025 New Hires",
  "organization_id": "org-uuid-1",
  "max_uses": 100,
  "expires_at": "2025-03-31T23:59:59Z"
}
```

**Response:** `201 Created`

```json
{
  "id": "token-uuid-new",
  "token": "ENR-GHI789-RST345",
  "name": "2025 New Hires",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "max_uses": 100,
  "current_uses": 0,
  "expires_at": "2025-03-31T23:59:59Z",
  "status": "active",
  "created_by": "admin-user-uuid",
  "created_at": "2024-12-10T18:30:00Z"
}
```

---

#### GET /api/admin/enrollment/tokens/:id

Get enrollment token details.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Token ID |

**Response:** `200 OK`

```json
{
  "id": "token-uuid-1",
  "token": "ENR-ABC123-XYZ789",
  "name": "Q4 2024 Onboarding",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "max_uses": 50,
  "current_uses": 23,
  "expires_at": "2024-12-31T23:59:59Z",
  "status": "active",
  "created_by": "user-uuid-1",
  "created_at": "2024-10-01T09:00:00Z"
}
```

---

#### GET /api/admin/enrollment/tokens/:id/usage

Get enrollment token usage details.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Token ID |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |

**Response:** `200 OK`

```json
{
  "token_id": "token-uuid-1",
  "token_name": "Q4 2024 Onboarding",
  "total_uses": 23,
  "max_uses": 50,
  "remaining_uses": 27,
  "usage_history": [
    {
      "device_id": "device-uuid-10",
      "device_name": "New Employee iPhone",
      "user_id": "user-uuid-10",
      "user_name": "New Employee",
      "enrolled_at": "2024-12-09T14:30:00Z"
    },
    {
      "device_id": "device-uuid-9",
      "device_name": "Sales Team Android",
      "user_id": "user-uuid-9",
      "user_name": "Sales Rep",
      "enrolled_at": "2024-12-08T11:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 23,
    "totalPages": 2
  }
}
```

---

#### DELETE /api/admin/enrollment/tokens/:id

Revoke an enrollment token.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Token ID |

**Response:** `204 No Content`

---

## Epic AP-5: Groups Administration

Endpoints for managing groups, members, and invitations.

### Types

```typescript
interface Group {
  id: UUID;
  name: string;
  description: string;
  status: 'active' | 'suspended' | 'archived';
  organization_id: UUID;
  organization_name: string;
  owner_id: UUID;
  owner_name: string;
  owner_email: Email;
  member_count: number;
  device_count: number;
  created_at: ISO8601DateTime;
  updated_at: ISO8601DateTime;
}

interface GroupListItem {
  id: UUID;
  name: string;
  status: 'active' | 'suspended' | 'archived';
  organization_name: string;
  owner_name: string;
  member_count: number;
  device_count: number;
  created_at: ISO8601DateTime;
}

interface GroupMember {
  id: UUID;
  user_id: UUID;
  user_name: string;
  user_email: Email;
  role: 'owner' | 'admin' | 'member';
  joined_at: ISO8601DateTime;
  devices_in_group: number;
}

interface GroupInvite {
  id: UUID;
  group_id: UUID;
  group_name: string;
  email: Email;
  role: 'admin' | 'member';
  status: 'pending' | 'accepted' | 'expired' | 'revoked';
  invited_by: UUID;
  invited_by_name: string;
  created_at: ISO8601DateTime;
  expires_at: ISO8601DateTime;
}

interface CreateGroupInviteRequest {
  email: Email;
  role: 'admin' | 'member';
  message?: string;
}

interface TransferOwnershipRequest {
  new_owner_id: UUID;
  reason?: string;
  notify_members?: boolean;
}
```

### Endpoints

#### GET /api/admin/groups

List all groups with pagination and filtering.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `search` | string | Search by name |
| `status` | string | Filter by status |
| `organization_id` | UUID | Filter by organization |
| `owner_id` | UUID | Filter by owner |
| `sort_by` | string | Sort field |
| `sort_order` | string | asc or desc |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "group-uuid-1",
      "name": "Sales Team",
      "status": "active",
      "organization_name": "Acme Corporation",
      "owner_name": "John Admin",
      "member_count": 15,
      "device_count": 28,
      "created_at": "2023-08-01T10:00:00Z"
    },
    {
      "id": "group-uuid-2",
      "name": "Engineering",
      "status": "active",
      "organization_name": "Acme Corporation",
      "owner_name": "Jane Doe",
      "member_count": 42,
      "device_count": 65,
      "created_at": "2023-06-15T09:00:00Z"
    },
    {
      "id": "group-uuid-3",
      "name": "Old Project Team",
      "status": "archived",
      "organization_name": "Tech Startup Inc",
      "owner_name": "Bob Smith",
      "member_count": 5,
      "device_count": 0,
      "created_at": "2024-01-10T14:30:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 24,
    "totalPages": 2
  }
}
```

---

#### GET /api/admin/groups/:id

Get detailed group information.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Group ID |

**Response:** `200 OK`

```json
{
  "id": "group-uuid-1",
  "name": "Sales Team",
  "description": "Regional sales representatives and managers",
  "status": "active",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "owner_id": "user-uuid-1",
  "owner_name": "John Admin",
  "owner_email": "admin@acme.com",
  "member_count": 15,
  "device_count": 28,
  "created_at": "2023-08-01T10:00:00Z",
  "updated_at": "2024-12-05T11:30:00Z"
}
```

---

#### GET /api/admin/groups/:id/members

Get group members.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Group ID |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `role` | string | Filter by role (owner, admin, member) |

**Response:** `200 OK`

```json
{
  "group_id": "group-uuid-1",
  "group_name": "Sales Team",
  "data": [
    {
      "id": "member-uuid-1",
      "user_id": "user-uuid-1",
      "user_name": "John Admin",
      "user_email": "admin@acme.com",
      "role": "owner",
      "joined_at": "2023-08-01T10:00:00Z",
      "devices_in_group": 2
    },
    {
      "id": "member-uuid-2",
      "user_id": "user-uuid-5",
      "user_name": "Sales Manager",
      "user_email": "sales.manager@acme.com",
      "role": "admin",
      "joined_at": "2023-08-05T14:00:00Z",
      "devices_in_group": 3
    },
    {
      "id": "member-uuid-3",
      "user_id": "user-uuid-10",
      "user_name": "Sales Rep",
      "user_email": "sales.rep@acme.com",
      "role": "member",
      "joined_at": "2023-09-01T09:00:00Z",
      "devices_in_group": 1
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 15,
    "totalPages": 1
  }
}
```

---

#### POST /api/admin/groups/:id/suspend

Suspend a group.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Group ID |

**Request Body:**

```json
{
  "reason": "Policy violation investigation",
  "notify_members": true
}
```

**Response:** `200 OK`

```json
{
  "id": "group-uuid-1",
  "name": "Sales Team",
  "status": "suspended",
  "suspended_at": "2024-12-10T19:00:00Z",
  "suspended_reason": "Policy violation investigation",
  "suspended_by": "admin-user-uuid",
  "members_notified": 15
}
```

---

#### POST /api/admin/groups/:id/reactivate

Reactivate a suspended group.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Group ID |

**Request Body:**

```json
{
  "notify_members": true
}
```

**Response:** `200 OK`

```json
{
  "id": "group-uuid-1",
  "name": "Sales Team",
  "status": "active",
  "reactivated_at": "2024-12-10T19:15:00Z",
  "reactivated_by": "admin-user-uuid",
  "members_notified": 15
}
```

---

#### POST /api/admin/groups/:id/archive

Archive a group.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Group ID |

**Request Body:**

```json
{
  "reason": "Project completed",
  "notify_members": true,
  "remove_devices": false
}
```

**Response:** `200 OK`

```json
{
  "id": "group-uuid-1",
  "name": "Sales Team",
  "status": "archived",
  "archived_at": "2024-12-10T19:30:00Z",
  "archived_reason": "Project completed",
  "archived_by": "admin-user-uuid",
  "devices_removed": 0
}
```

---

#### POST /api/admin/groups/:id/transfer

Transfer group ownership.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Group ID |

**Request Body:**

```json
{
  "new_owner_id": "user-uuid-5",
  "reason": "Original owner leaving organization",
  "notify_members": true
}
```

**Response:** `200 OK`

```json
{
  "id": "group-uuid-1",
  "name": "Sales Team",
  "previous_owner_id": "user-uuid-1",
  "previous_owner_name": "John Admin",
  "new_owner_id": "user-uuid-5",
  "new_owner_name": "Sales Manager",
  "transferred_at": "2024-12-10T19:45:00Z",
  "transferred_by": "admin-user-uuid",
  "reason": "Original owner leaving organization"
}
```

---

#### GET /api/admin/groups/:id/invites

List group invitations.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Group ID |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | Filter by status |
| `page` | integer | Page number |
| `limit` | integer | Items per page |

**Response:** `200 OK`

```json
{
  "group_id": "group-uuid-1",
  "group_name": "Sales Team",
  "data": [
    {
      "id": "invite-uuid-1",
      "group_id": "group-uuid-1",
      "group_name": "Sales Team",
      "email": "new.sales@acme.com",
      "role": "member",
      "status": "pending",
      "invited_by": "user-uuid-1",
      "invited_by_name": "John Admin",
      "created_at": "2024-12-09T10:00:00Z",
      "expires_at": "2024-12-16T10:00:00Z"
    },
    {
      "id": "invite-uuid-2",
      "group_id": "group-uuid-1",
      "group_name": "Sales Team",
      "email": "team.lead@acme.com",
      "role": "admin",
      "status": "accepted",
      "invited_by": "user-uuid-1",
      "invited_by_name": "John Admin",
      "created_at": "2024-12-01T14:00:00Z",
      "expires_at": "2024-12-08T14:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 5,
    "totalPages": 1
  }
}
```

---

#### POST /api/admin/groups/:id/invites

Create a group invitation.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Group ID |

**Request Body:**

```json
{
  "email": "new.member@acme.com",
  "role": "member",
  "message": "You have been invited to join the Sales Team group."
}
```

**Response:** `201 Created`

```json
{
  "id": "invite-uuid-new",
  "group_id": "group-uuid-1",
  "group_name": "Sales Team",
  "email": "new.member@acme.com",
  "role": "member",
  "status": "pending",
  "invited_by": "admin-user-uuid",
  "invited_by_name": "Admin User",
  "created_at": "2024-12-10T20:00:00Z",
  "expires_at": "2024-12-17T20:00:00Z"
}
```

---

#### DELETE /api/admin/groups/:id/invites/:inviteId

Revoke a group invitation.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Group ID |
| `inviteId` | UUID | Invite ID |

**Response:** `204 No Content`

---

## Epic AP-6: Location & Geofence Administration

Endpoints for managing locations, geofences, proximity alerts, and retention policies.

### Types

```typescript
interface LocationRecord {
  id: UUID;
  device_id: UUID;
  device_name: string;
  user_id: UUID;
  user_name: string;
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude: number | null;
  speed: number | null;
  heading: number | null;
  battery_level: number | null;
  timestamp: ISO8601DateTime;
  created_at: ISO8601DateTime;
}

interface LatestDeviceLocation {
  device_id: UUID;
  device_name: string;
  user_id: UUID;
  user_name: string;
  organization_id: UUID;
  latitude: number;
  longitude: number;
  accuracy: number;
  battery_level: number | null;
  timestamp: ISO8601DateTime;
  status: 'active' | 'stale' | 'offline';
}

interface Geofence {
  id: UUID;
  name: string;
  description: string;
  organization_id: UUID;
  organization_name: string;
  geometry_type: 'circle' | 'polygon';
  center_latitude: number | null;
  center_longitude: number | null;
  radius_meters: number | null;
  polygon_coordinates: Array<[number, number]> | null;
  status: 'active' | 'inactive';
  trigger_on_enter: boolean;
  trigger_on_exit: boolean;
  trigger_on_dwell: boolean;
  dwell_time_seconds: number | null;
  schedule: GeofenceSchedule | null;
  device_count: number;
  created_at: ISO8601DateTime;
  updated_at: ISO8601DateTime;
}

interface GeofenceSchedule {
  timezone: string;
  days: Array<'mon' | 'tue' | 'wed' | 'thu' | 'fri' | 'sat' | 'sun'>;
  start_time: string;
  end_time: string;
}

interface CreateGeofenceRequest {
  name: string;
  description?: string;
  organization_id: UUID;
  geometry_type: 'circle' | 'polygon';
  center_latitude?: number;
  center_longitude?: number;
  radius_meters?: number;
  polygon_coordinates?: Array<[number, number]>;
  trigger_on_enter?: boolean;
  trigger_on_exit?: boolean;
  trigger_on_dwell?: boolean;
  dwell_time_seconds?: number;
  schedule?: GeofenceSchedule;
}

interface ProximityAlert {
  id: UUID;
  name: string;
  organization_id: UUID;
  organization_name: string;
  device_ids: UUID[];
  distance_meters: number;
  status: 'active' | 'inactive';
  alert_on_approach: boolean;
  alert_on_separation: boolean;
  cooldown_minutes: number;
  created_at: ISO8601DateTime;
  updated_at: ISO8601DateTime;
}

interface CreateProximityAlertRequest {
  name: string;
  organization_id: UUID;
  device_ids: UUID[];
  distance_meters: number;
  alert_on_approach?: boolean;
  alert_on_separation?: boolean;
  cooldown_minutes?: number;
}

interface RetentionPolicy {
  id: UUID;
  organization_id: UUID;
  organization_name: string;
  location_retention_days: number;
  audit_log_retention_days: number;
  app_usage_retention_days: number;
  auto_delete_enabled: boolean;
  last_cleanup_at: ISO8601DateTime | null;
  next_cleanup_at: ISO8601DateTime | null;
  updated_at: ISO8601DateTime;
}

interface LocationExportRequest {
  device_ids?: UUID[];
  organization_id?: UUID;
  start_date: ISO8601DateTime;
  end_date: ISO8601DateTime;
  format: 'csv' | 'json' | 'geojson';
  include_metadata?: boolean;
}

interface LocationExportResult {
  export_id: UUID;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  download_url: string | null;
  record_count: number;
  file_size_bytes: number | null;
  created_at: ISO8601DateTime;
  expires_at: ISO8601DateTime | null;
}
```

### Endpoints

#### GET /api/admin/locations

List location records with filtering.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `device_id` | UUID | Filter by device |
| `user_id` | UUID | Filter by user |
| `organization_id` | UUID | Filter by organization |
| `start_date` | ISO8601DateTime | Start of date range |
| `end_date` | ISO8601DateTime | End of date range |
| `bbox` | string | Bounding box: minLon,minLat,maxLon,maxLat |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "loc-uuid-1",
      "device_id": "device-uuid-1",
      "device_name": "John's iPhone",
      "user_id": "user-uuid-1",
      "user_name": "John Admin",
      "latitude": 37.7749,
      "longitude": -122.4194,
      "accuracy": 10.5,
      "altitude": 15.0,
      "speed": 0.0,
      "heading": null,
      "battery_level": 85,
      "timestamp": "2024-12-10T17:30:00Z",
      "created_at": "2024-12-10T17:30:01Z"
    },
    {
      "id": "loc-uuid-2",
      "device_id": "device-uuid-1",
      "device_name": "John's iPhone",
      "user_id": "user-uuid-1",
      "user_name": "John Admin",
      "latitude": 37.7751,
      "longitude": -122.4189,
      "accuracy": 8.2,
      "altitude": 16.0,
      "speed": 1.2,
      "heading": 45.0,
      "battery_level": 84,
      "timestamp": "2024-12-10T17:25:00Z",
      "created_at": "2024-12-10T17:25:01Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 1250,
    "totalPages": 63
  }
}
```

---

#### GET /api/admin/locations/latest

Get latest location for each device.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `organization_id` | UUID | Filter by organization |
| `status` | string | Filter by status (active, stale, offline) |
| `bbox` | string | Bounding box filter |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "device_id": "device-uuid-1",
      "device_name": "John's iPhone",
      "user_id": "user-uuid-1",
      "user_name": "John Admin",
      "organization_id": "org-uuid-1",
      "latitude": 37.7749,
      "longitude": -122.4194,
      "accuracy": 10.5,
      "battery_level": 85,
      "timestamp": "2024-12-10T17:30:00Z",
      "status": "active"
    },
    {
      "device_id": "device-uuid-2",
      "device_name": "Work Android",
      "user_id": "user-uuid-2",
      "user_name": "Jane Doe",
      "organization_id": "org-uuid-1",
      "latitude": 37.7855,
      "longitude": -122.4012,
      "accuracy": 15.0,
      "battery_level": 42,
      "timestamp": "2024-12-10T16:45:00Z",
      "status": "stale"
    }
  ],
  "total": 285,
  "active_count": 230,
  "stale_count": 40,
  "offline_count": 15
}
```

---

#### POST /api/admin/locations/export

Export location data.

**Request Body:**

```json
{
  "device_ids": ["device-uuid-1", "device-uuid-2"],
  "start_date": "2024-12-01T00:00:00Z",
  "end_date": "2024-12-10T23:59:59Z",
  "format": "csv",
  "include_metadata": true
}
```

**Response:** `202 Accepted`

```json
{
  "export_id": "export-uuid-1",
  "status": "pending",
  "download_url": null,
  "record_count": 0,
  "file_size_bytes": null,
  "created_at": "2024-12-10T20:00:00Z",
  "expires_at": null,
  "estimated_completion": "2024-12-10T20:05:00Z"
}
```

---

#### GET /api/admin/geofences

List all geofences.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `organization_id` | UUID | Filter by organization |
| `status` | string | Filter by status |
| `geometry_type` | string | Filter by type (circle, polygon) |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "geofence-uuid-1",
      "name": "Office HQ",
      "description": "Main office building perimeter",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "geometry_type": "circle",
      "center_latitude": 37.7749,
      "center_longitude": -122.4194,
      "radius_meters": 100,
      "polygon_coordinates": null,
      "status": "active",
      "trigger_on_enter": true,
      "trigger_on_exit": true,
      "trigger_on_dwell": false,
      "dwell_time_seconds": null,
      "schedule": {
        "timezone": "America/Los_Angeles",
        "days": ["mon", "tue", "wed", "thu", "fri"],
        "start_time": "08:00",
        "end_time": "18:00"
      },
      "device_count": 45,
      "created_at": "2023-06-20T10:00:00Z",
      "updated_at": "2024-11-15T14:30:00Z"
    },
    {
      "id": "geofence-uuid-2",
      "name": "Restricted Zone",
      "description": "Sensitive area - alerts only",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "geometry_type": "polygon",
      "center_latitude": null,
      "center_longitude": null,
      "radius_meters": null,
      "polygon_coordinates": [
        [-122.420, 37.775],
        [-122.418, 37.775],
        [-122.418, 37.773],
        [-122.420, 37.773],
        [-122.420, 37.775]
      ],
      "status": "active",
      "trigger_on_enter": true,
      "trigger_on_exit": false,
      "trigger_on_dwell": true,
      "dwell_time_seconds": 300,
      "schedule": null,
      "device_count": 0,
      "created_at": "2024-03-10T09:00:00Z",
      "updated_at": "2024-03-10T09:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 45,
    "totalPages": 3
  }
}
```

---

#### POST /api/admin/geofences

Create a new geofence.

**Request Body:**

```json
{
  "name": "Warehouse District",
  "description": "Warehouse and logistics area",
  "organization_id": "org-uuid-1",
  "geometry_type": "circle",
  "center_latitude": 37.7650,
  "center_longitude": -122.4100,
  "radius_meters": 250,
  "trigger_on_enter": true,
  "trigger_on_exit": true,
  "trigger_on_dwell": false,
  "schedule": {
    "timezone": "America/Los_Angeles",
    "days": ["mon", "tue", "wed", "thu", "fri", "sat"],
    "start_time": "06:00",
    "end_time": "22:00"
  }
}
```

**Response:** `201 Created`

```json
{
  "id": "geofence-uuid-new",
  "name": "Warehouse District",
  "description": "Warehouse and logistics area",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "geometry_type": "circle",
  "center_latitude": 37.7650,
  "center_longitude": -122.4100,
  "radius_meters": 250,
  "polygon_coordinates": null,
  "status": "active",
  "trigger_on_enter": true,
  "trigger_on_exit": true,
  "trigger_on_dwell": false,
  "dwell_time_seconds": null,
  "schedule": {
    "timezone": "America/Los_Angeles",
    "days": ["mon", "tue", "wed", "thu", "fri", "sat"],
    "start_time": "06:00",
    "end_time": "22:00"
  },
  "device_count": 0,
  "created_at": "2024-12-10T20:15:00Z",
  "updated_at": "2024-12-10T20:15:00Z"
}
```

---

#### PUT /api/admin/geofences/:id

Update a geofence.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Geofence ID |

**Request Body:**

```json
{
  "name": "Warehouse District Updated",
  "radius_meters": 300,
  "status": "active"
}
```

**Response:** `200 OK`

```json
{
  "id": "geofence-uuid-new",
  "name": "Warehouse District Updated",
  "description": "Warehouse and logistics area",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "geometry_type": "circle",
  "center_latitude": 37.7650,
  "center_longitude": -122.4100,
  "radius_meters": 300,
  "polygon_coordinates": null,
  "status": "active",
  "trigger_on_enter": true,
  "trigger_on_exit": true,
  "trigger_on_dwell": false,
  "dwell_time_seconds": null,
  "schedule": {
    "timezone": "America/Los_Angeles",
    "days": ["mon", "tue", "wed", "thu", "fri", "sat"],
    "start_time": "06:00",
    "end_time": "22:00"
  },
  "device_count": 0,
  "created_at": "2024-12-10T20:15:00Z",
  "updated_at": "2024-12-10T20:30:00Z"
}
```

---

#### DELETE /api/admin/geofences/:id

Delete a geofence.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Geofence ID |

**Response:** `204 No Content`

---

#### GET /api/admin/proximity-alerts

List proximity alerts.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `organization_id` | UUID | Filter by organization |
| `status` | string | Filter by status |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "prox-uuid-1",
      "name": "Field Team Proximity",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "device_ids": ["device-uuid-1", "device-uuid-2", "device-uuid-3"],
      "distance_meters": 50,
      "status": "active",
      "alert_on_approach": true,
      "alert_on_separation": false,
      "cooldown_minutes": 15,
      "created_at": "2024-06-01T10:00:00Z",
      "updated_at": "2024-06-01T10:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 8,
    "totalPages": 1
  }
}
```

---

#### POST /api/admin/proximity-alerts

Create a proximity alert.

**Request Body:**

```json
{
  "name": "Executive Protection",
  "organization_id": "org-uuid-1",
  "device_ids": ["device-uuid-10", "device-uuid-11"],
  "distance_meters": 100,
  "alert_on_approach": true,
  "alert_on_separation": true,
  "cooldown_minutes": 10
}
```

**Response:** `201 Created`

```json
{
  "id": "prox-uuid-new",
  "name": "Executive Protection",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "device_ids": ["device-uuid-10", "device-uuid-11"],
  "distance_meters": 100,
  "status": "active",
  "alert_on_approach": true,
  "alert_on_separation": true,
  "cooldown_minutes": 10,
  "created_at": "2024-12-10T20:45:00Z",
  "updated_at": "2024-12-10T20:45:00Z"
}
```

---

#### PUT /api/admin/proximity-alerts/:id

Update a proximity alert.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Proximity alert ID |

**Request Body:**

```json
{
  "distance_meters": 75,
  "status": "inactive"
}
```

**Response:** `200 OK`

```json
{
  "id": "prox-uuid-new",
  "name": "Executive Protection",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "device_ids": ["device-uuid-10", "device-uuid-11"],
  "distance_meters": 75,
  "status": "inactive",
  "alert_on_approach": true,
  "alert_on_separation": true,
  "cooldown_minutes": 10,
  "created_at": "2024-12-10T20:45:00Z",
  "updated_at": "2024-12-10T21:00:00Z"
}
```

---

#### DELETE /api/admin/proximity-alerts/:id

Delete a proximity alert.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Proximity alert ID |

**Response:** `204 No Content`

---

#### GET /api/admin/retention-policies

Get data retention policies.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `organization_id` | UUID | Filter by organization |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "retention-uuid-1",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "location_retention_days": 365,
      "audit_log_retention_days": 730,
      "app_usage_retention_days": 90,
      "auto_delete_enabled": true,
      "last_cleanup_at": "2024-12-01T03:00:00Z",
      "next_cleanup_at": "2024-12-15T03:00:00Z",
      "updated_at": "2024-11-20T10:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 45,
    "totalPages": 3
  }
}
```

---

#### PUT /api/admin/retention-policies/:id

Update a retention policy.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Retention policy ID |

**Request Body:**

```json
{
  "location_retention_days": 180,
  "audit_log_retention_days": 365,
  "app_usage_retention_days": 60,
  "auto_delete_enabled": true
}
```

**Response:** `200 OK`

```json
{
  "id": "retention-uuid-1",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "location_retention_days": 180,
  "audit_log_retention_days": 365,
  "app_usage_retention_days": 60,
  "auto_delete_enabled": true,
  "last_cleanup_at": "2024-12-01T03:00:00Z",
  "next_cleanup_at": "2024-12-15T03:00:00Z",
  "updated_at": "2024-12-10T21:15:00Z"
}
```

---

## Epic AP-7: Webhook Administration

Endpoints for managing webhooks, testing, and delivery logs.

### Types

```typescript
interface Webhook {
  id: UUID;
  name: string;
  url: string;
  organization_id: UUID;
  organization_name: string;
  events: WebhookEvent[];
  status: 'active' | 'inactive' | 'failing';
  secret: string;
  headers: Record<string, string>;
  retry_config: WebhookRetryConfig;
  success_rate: number;
  last_triggered_at: ISO8601DateTime | null;
  last_success_at: ISO8601DateTime | null;
  last_failure_at: ISO8601DateTime | null;
  created_at: ISO8601DateTime;
  updated_at: ISO8601DateTime;
}

type WebhookEvent =
  | 'device.enrolled'
  | 'device.unenrolled'
  | 'device.status_changed'
  | 'location.updated'
  | 'geofence.entered'
  | 'geofence.exited'
  | 'geofence.dwell'
  | 'proximity.alert'
  | 'user.created'
  | 'user.updated'
  | 'user.suspended'
  | 'group.created'
  | 'group.member_added'
  | 'group.member_removed'
  | 'unlock_request.created'
  | 'unlock_request.approved'
  | 'unlock_request.denied';

interface WebhookRetryConfig {
  max_retries: number;
  initial_delay_seconds: number;
  max_delay_seconds: number;
  backoff_multiplier: number;
}

interface CreateWebhookRequest {
  name: string;
  url: string;
  organization_id: UUID;
  events: WebhookEvent[];
  headers?: Record<string, string>;
  retry_config?: Partial<WebhookRetryConfig>;
}

interface WebhookDelivery {
  id: UUID;
  webhook_id: UUID;
  webhook_name: string;
  event: WebhookEvent;
  status: 'pending' | 'delivered' | 'failed' | 'retrying';
  request_body: object;
  response_status: number | null;
  response_body: string | null;
  attempt_count: number;
  max_attempts: number;
  next_retry_at: ISO8601DateTime | null;
  duration_ms: number | null;
  created_at: ISO8601DateTime;
  completed_at: ISO8601DateTime | null;
}

interface WebhookTestRequest {
  event: WebhookEvent;
  payload?: object;
}

interface WebhookTestResult {
  success: boolean;
  status_code: number;
  response_body: string;
  duration_ms: number;
  error: string | null;
}
```

### Endpoints

#### GET /api/admin/webhooks

List all webhooks.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `organization_id` | UUID | Filter by organization |
| `status` | string | Filter by status |
| `event` | string | Filter by subscribed event |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "webhook-uuid-1",
      "name": "Location Updates",
      "url": "https://api.example.com/webhooks/locations",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "events": ["location.updated", "geofence.entered", "geofence.exited"],
      "status": "active",
      "success_rate": 98.5,
      "last_triggered_at": "2024-12-10T21:00:00Z",
      "created_at": "2023-07-15T10:00:00Z"
    },
    {
      "id": "webhook-uuid-2",
      "name": "Device Events",
      "url": "https://api.example.com/webhooks/devices",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "events": ["device.enrolled", "device.unenrolled", "device.status_changed"],
      "status": "failing",
      "success_rate": 45.2,
      "last_triggered_at": "2024-12-10T20:30:00Z",
      "created_at": "2023-08-20T14:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 12,
    "totalPages": 1
  }
}
```

---

#### POST /api/admin/webhooks

Create a new webhook.

**Request Body:**

```json
{
  "name": "User Events Webhook",
  "url": "https://api.example.com/webhooks/users",
  "organization_id": "org-uuid-1",
  "events": ["user.created", "user.updated", "user.suspended"],
  "headers": {
    "X-Custom-Header": "custom-value"
  },
  "retry_config": {
    "max_retries": 5,
    "initial_delay_seconds": 30,
    "max_delay_seconds": 3600,
    "backoff_multiplier": 2
  }
}
```

**Response:** `201 Created`

```json
{
  "id": "webhook-uuid-new",
  "name": "User Events Webhook",
  "url": "https://api.example.com/webhooks/users",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "events": ["user.created", "user.updated", "user.suspended"],
  "status": "active",
  "secret": "whsec_abc123def456ghi789",
  "headers": {
    "X-Custom-Header": "custom-value"
  },
  "retry_config": {
    "max_retries": 5,
    "initial_delay_seconds": 30,
    "max_delay_seconds": 3600,
    "backoff_multiplier": 2
  },
  "success_rate": 0,
  "last_triggered_at": null,
  "last_success_at": null,
  "last_failure_at": null,
  "created_at": "2024-12-10T21:30:00Z",
  "updated_at": "2024-12-10T21:30:00Z"
}
```

---

#### GET /api/admin/webhooks/:id

Get webhook details.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Webhook ID |

**Response:** `200 OK`

```json
{
  "id": "webhook-uuid-1",
  "name": "Location Updates",
  "url": "https://api.example.com/webhooks/locations",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "events": ["location.updated", "geofence.entered", "geofence.exited"],
  "status": "active",
  "secret": "whsec_xyz789abc123def456",
  "headers": {},
  "retry_config": {
    "max_retries": 3,
    "initial_delay_seconds": 60,
    "max_delay_seconds": 3600,
    "backoff_multiplier": 2
  },
  "success_rate": 98.5,
  "last_triggered_at": "2024-12-10T21:00:00Z",
  "last_success_at": "2024-12-10T21:00:00Z",
  "last_failure_at": "2024-12-08T15:30:00Z",
  "created_at": "2023-07-15T10:00:00Z",
  "updated_at": "2024-11-20T09:00:00Z"
}
```

---

#### PUT /api/admin/webhooks/:id

Update a webhook.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Webhook ID |

**Request Body:**

```json
{
  "name": "Location Updates v2",
  "events": ["location.updated", "geofence.entered", "geofence.exited", "geofence.dwell"]
}
```

**Response:** `200 OK`

```json
{
  "id": "webhook-uuid-1",
  "name": "Location Updates v2",
  "url": "https://api.example.com/webhooks/locations",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "events": ["location.updated", "geofence.entered", "geofence.exited", "geofence.dwell"],
  "status": "active",
  "secret": "whsec_xyz789abc123def456",
  "headers": {},
  "retry_config": {
    "max_retries": 3,
    "initial_delay_seconds": 60,
    "max_delay_seconds": 3600,
    "backoff_multiplier": 2
  },
  "success_rate": 98.5,
  "last_triggered_at": "2024-12-10T21:00:00Z",
  "last_success_at": "2024-12-10T21:00:00Z",
  "last_failure_at": "2024-12-08T15:30:00Z",
  "created_at": "2023-07-15T10:00:00Z",
  "updated_at": "2024-12-10T21:45:00Z"
}
```

---

#### DELETE /api/admin/webhooks/:id

Delete a webhook.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Webhook ID |

**Response:** `204 No Content`

---

#### PATCH /api/admin/webhooks/:id/toggle

Enable or disable a webhook.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Webhook ID |

**Request Body:**

```json
{
  "enabled": false
}
```

**Response:** `200 OK`

```json
{
  "id": "webhook-uuid-1",
  "status": "inactive",
  "updated_at": "2024-12-10T22:00:00Z"
}
```

---

#### POST /api/admin/webhooks/:id/test

Test a webhook with a sample payload.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Webhook ID |

**Request Body:**

```json
{
  "event": "location.updated",
  "payload": {
    "device_id": "test-device-uuid",
    "latitude": 37.7749,
    "longitude": -122.4194,
    "timestamp": "2024-12-10T22:00:00Z"
  }
}
```

**Response:** `200 OK`

```json
{
  "success": true,
  "status_code": 200,
  "response_body": "{\"received\": true}",
  "duration_ms": 145,
  "error": null
}
```

**Error Response (webhook failure):** `200 OK`

```json
{
  "success": false,
  "status_code": 500,
  "response_body": "Internal Server Error",
  "duration_ms": 2500,
  "error": "Webhook endpoint returned 500 status"
}
```

---

#### GET /api/admin/webhooks/:id/deliveries

Get webhook delivery logs.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Webhook ID |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `status` | string | Filter by status |
| `event` | string | Filter by event type |
| `start_date` | ISO8601DateTime | Start of date range |
| `end_date` | ISO8601DateTime | End of date range |

**Response:** `200 OK`

```json
{
  "webhook_id": "webhook-uuid-1",
  "webhook_name": "Location Updates",
  "data": [
    {
      "id": "delivery-uuid-1",
      "webhook_id": "webhook-uuid-1",
      "webhook_name": "Location Updates",
      "event": "location.updated",
      "status": "delivered",
      "request_body": {
        "event": "location.updated",
        "data": {
          "device_id": "device-uuid-1",
          "latitude": 37.7749,
          "longitude": -122.4194
        }
      },
      "response_status": 200,
      "response_body": "{\"received\": true}",
      "attempt_count": 1,
      "max_attempts": 3,
      "next_retry_at": null,
      "duration_ms": 125,
      "created_at": "2024-12-10T21:00:00Z",
      "completed_at": "2024-12-10T21:00:00Z"
    },
    {
      "id": "delivery-uuid-2",
      "webhook_id": "webhook-uuid-1",
      "webhook_name": "Location Updates",
      "event": "geofence.entered",
      "status": "failed",
      "request_body": {
        "event": "geofence.entered",
        "data": {
          "device_id": "device-uuid-2",
          "geofence_id": "geofence-uuid-1"
        }
      },
      "response_status": 503,
      "response_body": "Service Unavailable",
      "attempt_count": 3,
      "max_attempts": 3,
      "next_retry_at": null,
      "duration_ms": 30000,
      "created_at": "2024-12-08T15:30:00Z",
      "completed_at": "2024-12-08T16:30:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 1250,
    "totalPages": 63
  }
}
```

---

#### POST /api/admin/webhooks/:id/deliveries/:deliveryId/retry

Retry a failed webhook delivery.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Webhook ID |
| `deliveryId` | UUID | Delivery ID |

**Response:** `200 OK`

```json
{
  "id": "delivery-uuid-2",
  "status": "retrying",
  "attempt_count": 4,
  "next_retry_at": "2024-12-10T22:15:00Z",
  "message": "Delivery queued for retry"
}
```

---

## Epic AP-8: App Usage & Unlock Requests

Endpoints for managing app usage statistics, unlock requests, and app limits.

### Types

```typescript
interface AppUsageStats {
  organization_id: UUID;
  organization_name: string;
  period: 'day' | 'week' | 'month';
  start_date: ISO8601DateTime;
  end_date: ISO8601DateTime;
  total_screen_time_minutes: number;
  total_app_opens: number;
  unique_devices: number;
  top_apps: AppUsageItem[];
  usage_by_category: CategoryUsage[];
}

interface AppUsageItem {
  package_name: string;
  app_name: string;
  category: string;
  total_time_minutes: number;
  open_count: number;
  device_count: number;
  percentage_of_total: number;
}

interface CategoryUsage {
  category: string;
  total_time_minutes: number;
  app_count: number;
  percentage_of_total: number;
}

interface UnlockRequest {
  id: UUID;
  device_id: UUID;
  device_name: string;
  user_id: UUID;
  user_name: string;
  user_email: Email;
  organization_id: UUID;
  organization_name: string;
  app_package: string;
  app_name: string;
  reason: string;
  duration_minutes: number | null;
  status: 'pending' | 'approved' | 'denied' | 'expired';
  reviewed_by: UUID | null;
  reviewed_by_name: string | null;
  reviewed_at: ISO8601DateTime | null;
  review_note: string | null;
  expires_at: ISO8601DateTime | null;
  created_at: ISO8601DateTime;
}

interface UnlockRequestListItem {
  id: UUID;
  device_name: string;
  user_name: string;
  organization_name: string;
  app_name: string;
  status: 'pending' | 'approved' | 'denied' | 'expired';
  created_at: ISO8601DateTime;
}

interface ApproveUnlockRequest {
  duration_minutes: number;
  note?: string;
}

interface DenyUnlockRequest {
  note?: string;
}

interface BulkUnlockRequest {
  request_ids: UUID[];
  duration_minutes?: number;
  note?: string;
}

interface AppLimitConfig {
  id: UUID;
  organization_id: UUID;
  organization_name: string;
  name: string;
  description: string;
  app_packages: string[];
  categories: string[];
  daily_limit_minutes: number | null;
  schedule: AppLimitSchedule | null;
  blocked: boolean;
  applies_to: 'all' | 'groups' | 'devices';
  group_ids: UUID[];
  device_ids: UUID[];
  status: 'active' | 'inactive';
  created_at: ISO8601DateTime;
  updated_at: ISO8601DateTime;
}

interface AppLimitSchedule {
  timezone: string;
  rules: Array<{
    days: Array<'mon' | 'tue' | 'wed' | 'thu' | 'fri' | 'sat' | 'sun'>;
    start_time: string;
    end_time: string;
    action: 'block' | 'limit';
    limit_minutes?: number;
  }>;
}

interface CreateAppLimitRequest {
  organization_id: UUID;
  name: string;
  description?: string;
  app_packages?: string[];
  categories?: string[];
  daily_limit_minutes?: number;
  schedule?: AppLimitSchedule;
  blocked?: boolean;
  applies_to: 'all' | 'groups' | 'devices';
  group_ids?: UUID[];
  device_ids?: UUID[];
}
```

### Endpoints

#### GET /api/admin/app-usage

Get app usage statistics.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `organization_id` | UUID | Filter by organization |
| `period` | string | Time period (day, week, month) |
| `start_date` | ISO8601DateTime | Start of date range |
| `end_date` | ISO8601DateTime | End of date range |
| `device_id` | UUID | Filter by device |
| `group_id` | UUID | Filter by group |

**Response:** `200 OK`

```json
{
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "period": "week",
  "start_date": "2024-12-02T00:00:00Z",
  "end_date": "2024-12-08T23:59:59Z",
  "total_screen_time_minutes": 125000,
  "total_app_opens": 45000,
  "unique_devices": 285,
  "top_apps": [
    {
      "package_name": "com.slack",
      "app_name": "Slack",
      "category": "Productivity",
      "total_time_minutes": 18500,
      "open_count": 8500,
      "device_count": 245,
      "percentage_of_total": 14.8
    },
    {
      "package_name": "com.google.android.gm",
      "app_name": "Gmail",
      "category": "Communication",
      "total_time_minutes": 15200,
      "open_count": 12000,
      "device_count": 280,
      "percentage_of_total": 12.16
    }
  ],
  "usage_by_category": [
    {
      "category": "Productivity",
      "total_time_minutes": 45000,
      "app_count": 12,
      "percentage_of_total": 36
    },
    {
      "category": "Communication",
      "total_time_minutes": 32000,
      "app_count": 8,
      "percentage_of_total": 25.6
    }
  ]
}
```

---

#### GET /api/admin/app-usage/categories

Get usage breakdown by category.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `organization_id` | UUID | Filter by organization |
| `period` | string | Time period |
| `start_date` | ISO8601DateTime | Start of date range |
| `end_date` | ISO8601DateTime | End of date range |

**Response:** `200 OK`

```json
{
  "organization_id": "org-uuid-1",
  "period": "month",
  "categories": [
    {
      "category": "Productivity",
      "total_time_minutes": 180000,
      "app_count": 15,
      "device_count": 280,
      "percentage_of_total": 36,
      "trend": 5.2
    },
    {
      "category": "Communication",
      "total_time_minutes": 128000,
      "app_count": 10,
      "device_count": 285,
      "percentage_of_total": 25.6,
      "trend": -2.1
    },
    {
      "category": "Social",
      "total_time_minutes": 45000,
      "app_count": 8,
      "device_count": 150,
      "percentage_of_total": 9,
      "trend": 12.5
    }
  ]
}
```

---

#### GET /api/admin/app-usage/top-apps

Get top apps by usage.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `organization_id` | UUID | Filter by organization |
| `period` | string | Time period |
| `limit` | integer | Number of apps to return (default: 10) |
| `category` | string | Filter by category |

**Response:** `200 OK`

```json
{
  "organization_id": "org-uuid-1",
  "period": "week",
  "data": [
    {
      "rank": 1,
      "package_name": "com.slack",
      "app_name": "Slack",
      "category": "Productivity",
      "total_time_minutes": 18500,
      "open_count": 8500,
      "device_count": 245,
      "avg_session_minutes": 12.5
    },
    {
      "rank": 2,
      "package_name": "com.google.android.gm",
      "app_name": "Gmail",
      "category": "Communication",
      "total_time_minutes": 15200,
      "open_count": 12000,
      "device_count": 280,
      "avg_session_minutes": 4.2
    }
  ]
}
```

---

#### GET /api/admin/unlock-requests

List unlock requests.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `status` | string | Filter by status |
| `organization_id` | UUID | Filter by organization |
| `user_id` | UUID | Filter by user |
| `device_id` | UUID | Filter by device |
| `sort_by` | string | Sort field |
| `sort_order` | string | asc or desc |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "unlock-uuid-1",
      "device_name": "Sales Rep iPhone",
      "user_name": "Sales Rep",
      "organization_name": "Acme Corporation",
      "app_name": "Instagram",
      "status": "pending",
      "created_at": "2024-12-10T20:00:00Z"
    },
    {
      "id": "unlock-uuid-2",
      "device_name": "Manager Android",
      "user_name": "Manager",
      "organization_name": "Acme Corporation",
      "app_name": "TikTok",
      "status": "denied",
      "created_at": "2024-12-09T15:30:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 45,
    "totalPages": 3
  },
  "summary": {
    "pending": 12,
    "approved": 28,
    "denied": 5,
    "expired": 0
  }
}
```

---

#### GET /api/admin/unlock-requests/:id

Get unlock request details.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Unlock request ID |

**Response:** `200 OK`

```json
{
  "id": "unlock-uuid-1",
  "device_id": "device-uuid-15",
  "device_name": "Sales Rep iPhone",
  "user_id": "user-uuid-15",
  "user_name": "Sales Rep",
  "user_email": "sales.rep@acme.com",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "app_package": "com.instagram.android",
  "app_name": "Instagram",
  "reason": "Need to manage company Instagram account for marketing campaign",
  "duration_minutes": null,
  "status": "pending",
  "reviewed_by": null,
  "reviewed_by_name": null,
  "reviewed_at": null,
  "review_note": null,
  "expires_at": null,
  "created_at": "2024-12-10T20:00:00Z"
}
```

---

#### POST /api/admin/unlock-requests/:id/approve

Approve an unlock request.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Unlock request ID |

**Request Body:**

```json
{
  "duration_minutes": 60,
  "note": "Approved for marketing campaign work"
}
```

**Response:** `200 OK`

```json
{
  "id": "unlock-uuid-1",
  "status": "approved",
  "duration_minutes": 60,
  "reviewed_by": "admin-user-uuid",
  "reviewed_by_name": "Admin User",
  "reviewed_at": "2024-12-10T22:30:00Z",
  "review_note": "Approved for marketing campaign work",
  "expires_at": "2024-12-10T23:30:00Z"
}
```

---

#### POST /api/admin/unlock-requests/:id/deny

Deny an unlock request.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Unlock request ID |

**Request Body:**

```json
{
  "note": "Social media access not permitted during work hours"
}
```

**Response:** `200 OK`

```json
{
  "id": "unlock-uuid-1",
  "status": "denied",
  "reviewed_by": "admin-user-uuid",
  "reviewed_by_name": "Admin User",
  "reviewed_at": "2024-12-10T22:30:00Z",
  "review_note": "Social media access not permitted during work hours"
}
```

---

#### POST /api/admin/unlock-requests/bulk/approve

Bulk approve unlock requests.

**Request Body:**

```json
{
  "request_ids": ["unlock-uuid-1", "unlock-uuid-3", "unlock-uuid-5"],
  "duration_minutes": 30,
  "note": "Bulk approved for special event"
}
```

**Response:** `200 OK`

```json
{
  "successful": ["unlock-uuid-1", "unlock-uuid-3", "unlock-uuid-5"],
  "failed": [],
  "total": 3,
  "success_count": 3,
  "failure_count": 0
}
```

---

#### POST /api/admin/unlock-requests/bulk/deny

Bulk deny unlock requests.

**Request Body:**

```json
{
  "request_ids": ["unlock-uuid-2", "unlock-uuid-4"],
  "note": "Denied per policy"
}
```

**Response:** `200 OK`

```json
{
  "successful": ["unlock-uuid-2", "unlock-uuid-4"],
  "failed": [],
  "total": 2,
  "success_count": 2,
  "failure_count": 0
}
```

---

#### GET /api/admin/app-limits

List app limit configurations.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `organization_id` | UUID | Filter by organization |
| `status` | string | Filter by status |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "limit-uuid-1",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "name": "Social Media Limits",
      "description": "Limit social media during work hours",
      "app_packages": [],
      "categories": ["Social", "Entertainment"],
      "daily_limit_minutes": 30,
      "schedule": {
        "timezone": "America/Los_Angeles",
        "rules": [
          {
            "days": ["mon", "tue", "wed", "thu", "fri"],
            "start_time": "09:00",
            "end_time": "17:00",
            "action": "limit",
            "limit_minutes": 15
          }
        ]
      },
      "blocked": false,
      "applies_to": "all",
      "group_ids": [],
      "device_ids": [],
      "status": "active",
      "created_at": "2024-01-15T10:00:00Z",
      "updated_at": "2024-11-20T14:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 8,
    "totalPages": 1
  }
}
```

---

#### POST /api/admin/app-limits

Create an app limit configuration.

**Request Body:**

```json
{
  "organization_id": "org-uuid-1",
  "name": "Gaming Block",
  "description": "Block gaming apps during work hours",
  "categories": ["Games"],
  "blocked": true,
  "schedule": {
    "timezone": "America/Los_Angeles",
    "rules": [
      {
        "days": ["mon", "tue", "wed", "thu", "fri"],
        "start_time": "08:00",
        "end_time": "18:00",
        "action": "block"
      }
    ]
  },
  "applies_to": "all"
}
```

**Response:** `201 Created`

```json
{
  "id": "limit-uuid-new",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "name": "Gaming Block",
  "description": "Block gaming apps during work hours",
  "app_packages": [],
  "categories": ["Games"],
  "daily_limit_minutes": null,
  "schedule": {
    "timezone": "America/Los_Angeles",
    "rules": [
      {
        "days": ["mon", "tue", "wed", "thu", "fri"],
        "start_time": "08:00",
        "end_time": "18:00",
        "action": "block"
      }
    ]
  },
  "blocked": true,
  "applies_to": "all",
  "group_ids": [],
  "device_ids": [],
  "status": "active",
  "created_at": "2024-12-10T23:00:00Z",
  "updated_at": "2024-12-10T23:00:00Z"
}
```

---

#### PUT /api/admin/app-limits/:id

Update an app limit configuration.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | App limit ID |

**Request Body:**

```json
{
  "name": "Gaming Block v2",
  "daily_limit_minutes": 15,
  "blocked": false
}
```

**Response:** `200 OK`

```json
{
  "id": "limit-uuid-new",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "name": "Gaming Block v2",
  "description": "Block gaming apps during work hours",
  "app_packages": [],
  "categories": ["Games"],
  "daily_limit_minutes": 15,
  "schedule": {
    "timezone": "America/Los_Angeles",
    "rules": [
      {
        "days": ["mon", "tue", "wed", "thu", "fri"],
        "start_time": "08:00",
        "end_time": "18:00",
        "action": "block"
      }
    ]
  },
  "blocked": false,
  "applies_to": "all",
  "group_ids": [],
  "device_ids": [],
  "status": "active",
  "created_at": "2024-12-10T23:00:00Z",
  "updated_at": "2024-12-10T23:15:00Z"
}
```

---

#### DELETE /api/admin/app-limits/:id

Delete an app limit configuration.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | App limit ID |

**Response:** `204 No Content`

---

## Epic AP-9: System Configuration

Endpoints for managing authentication settings, OAuth providers, feature flags, rate limits, and API keys.

### Types

```typescript
interface AuthSettings {
  password_policy: PasswordPolicy;
  session_config: SessionConfig;
  mfa_config: MfaConfig;
  lockout_policy: LockoutPolicy;
  updated_at: ISO8601DateTime;
}

interface PasswordPolicy {
  min_length: number;
  require_uppercase: boolean;
  require_lowercase: boolean;
  require_numbers: boolean;
  require_special_chars: boolean;
  max_age_days: number | null;
  history_count: number;
}

interface SessionConfig {
  session_timeout_minutes: number;
  idle_timeout_minutes: number;
  max_concurrent_sessions: number;
  remember_me_duration_days: number;
}

interface MfaConfig {
  enabled: boolean;
  enforced: boolean;
  allowed_methods: Array<'totp' | 'sms' | 'email' | 'security_key'>;
  grace_period_days: number;
}

interface LockoutPolicy {
  max_attempts: number;
  lockout_duration_minutes: number;
  reset_after_minutes: number;
}

interface OAuthProvider {
  provider: 'google' | 'microsoft' | 'okta' | 'github';
  enabled: boolean;
  client_id: string;
  tenant_id: string | null;
  allowed_domains: string[];
  auto_provision_users: boolean;
  default_role_id: UUID | null;
  updated_at: ISO8601DateTime;
}

interface FeatureFlag {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
  rollout_percentage: number;
  organization_ids: UUID[];
  user_ids: UUID[];
  metadata: Record<string, any>;
  created_at: ISO8601DateTime;
  updated_at: ISO8601DateTime;
}

interface RateLimitConfig {
  endpoint_pattern: string;
  requests_per_minute: number;
  requests_per_hour: number;
  requests_per_day: number;
  burst_limit: number;
  enabled: boolean;
}

interface DataRetentionConfig {
  location_data_days: number;
  audit_logs_days: number;
  app_usage_days: number;
  session_data_days: number;
  deleted_user_data_days: number;
  auto_cleanup_enabled: boolean;
  next_cleanup_at: ISO8601DateTime | null;
}

interface ApiKey {
  id: UUID;
  name: string;
  key_prefix: string;
  organization_id: UUID;
  organization_name: string;
  permissions: string[];
  rate_limit: number;
  expires_at: ISO8601DateTime | null;
  last_used_at: ISO8601DateTime | null;
  status: 'active' | 'revoked' | 'expired';
  created_by: UUID;
  created_at: ISO8601DateTime;
}

interface CreateApiKeyRequest {
  name: string;
  organization_id: UUID;
  permissions: string[];
  rate_limit?: number;
  expires_at?: ISO8601DateTime;
}

interface CreateApiKeyResponse {
  id: UUID;
  name: string;
  key: string;
  key_prefix: string;
  organization_id: UUID;
  permissions: string[];
  rate_limit: number;
  expires_at: ISO8601DateTime | null;
  created_at: ISO8601DateTime;
}
```

### Endpoints

#### GET /api/admin/system-config/auth

Get authentication settings.

**Response:** `200 OK`

```json
{
  "password_policy": {
    "min_length": 12,
    "require_uppercase": true,
    "require_lowercase": true,
    "require_numbers": true,
    "require_special_chars": true,
    "max_age_days": 90,
    "history_count": 5
  },
  "session_config": {
    "session_timeout_minutes": 480,
    "idle_timeout_minutes": 30,
    "max_concurrent_sessions": 5,
    "remember_me_duration_days": 30
  },
  "mfa_config": {
    "enabled": true,
    "enforced": true,
    "allowed_methods": ["totp", "security_key"],
    "grace_period_days": 7
  },
  "lockout_policy": {
    "max_attempts": 5,
    "lockout_duration_minutes": 30,
    "reset_after_minutes": 15
  },
  "updated_at": "2024-11-15T10:00:00Z"
}
```

---

#### PUT /api/admin/system-config/auth

Update authentication settings.

**Request Body:**

```json
{
  "password_policy": {
    "min_length": 14,
    "max_age_days": 60
  },
  "session_config": {
    "idle_timeout_minutes": 15
  },
  "mfa_config": {
    "enforced": true,
    "allowed_methods": ["totp", "security_key", "sms"]
  }
}
```

**Response:** `200 OK`

```json
{
  "password_policy": {
    "min_length": 14,
    "require_uppercase": true,
    "require_lowercase": true,
    "require_numbers": true,
    "require_special_chars": true,
    "max_age_days": 60,
    "history_count": 5
  },
  "session_config": {
    "session_timeout_minutes": 480,
    "idle_timeout_minutes": 15,
    "max_concurrent_sessions": 5,
    "remember_me_duration_days": 30
  },
  "mfa_config": {
    "enabled": true,
    "enforced": true,
    "allowed_methods": ["totp", "security_key", "sms"],
    "grace_period_days": 7
  },
  "lockout_policy": {
    "max_attempts": 5,
    "lockout_duration_minutes": 30,
    "reset_after_minutes": 15
  },
  "updated_at": "2024-12-10T23:30:00Z"
}
```

---

#### GET /api/admin/system-config/oauth-providers

List OAuth provider configurations.

**Response:** `200 OK`

```json
{
  "data": [
    {
      "provider": "google",
      "enabled": true,
      "client_id": "123456789-abc.apps.googleusercontent.com",
      "tenant_id": null,
      "allowed_domains": ["acme.com", "acme.io"],
      "auto_provision_users": true,
      "default_role_id": "role-uuid-2",
      "updated_at": "2024-10-01T09:00:00Z"
    },
    {
      "provider": "microsoft",
      "enabled": true,
      "client_id": "abcd1234-5678-90ef-ghij-klmnopqrstuv",
      "tenant_id": "tenant-uuid-xyz",
      "allowed_domains": ["acme.com"],
      "auto_provision_users": false,
      "default_role_id": null,
      "updated_at": "2024-09-15T14:00:00Z"
    },
    {
      "provider": "okta",
      "enabled": false,
      "client_id": "",
      "tenant_id": null,
      "allowed_domains": [],
      "auto_provision_users": false,
      "default_role_id": null,
      "updated_at": "2024-01-15T10:00:00Z"
    }
  ]
}
```

---

#### PUT /api/admin/system-config/oauth-providers/:provider

Update an OAuth provider configuration.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `provider` | string | Provider name (google, microsoft, okta, github) |

**Request Body:**

```json
{
  "enabled": true,
  "client_id": "new-client-id",
  "client_secret": "new-client-secret",
  "allowed_domains": ["acme.com", "acme.io", "acme.co"],
  "auto_provision_users": true,
  "default_role_id": "role-uuid-3"
}
```

**Response:** `200 OK`

```json
{
  "provider": "google",
  "enabled": true,
  "client_id": "new-client-id",
  "tenant_id": null,
  "allowed_domains": ["acme.com", "acme.io", "acme.co"],
  "auto_provision_users": true,
  "default_role_id": "role-uuid-3",
  "updated_at": "2024-12-10T23:45:00Z"
}
```

---

#### GET /api/admin/system-config/feature-flags

List feature flags.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `enabled` | boolean | Filter by enabled status |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "new-dashboard",
      "name": "New Dashboard",
      "description": "Enable the redesigned admin dashboard",
      "enabled": true,
      "rollout_percentage": 100,
      "organization_ids": [],
      "user_ids": [],
      "metadata": {
        "version": "2.0"
      },
      "created_at": "2024-06-01T10:00:00Z",
      "updated_at": "2024-11-01T09:00:00Z"
    },
    {
      "id": "advanced-analytics",
      "name": "Advanced Analytics",
      "description": "Enable advanced analytics features",
      "enabled": true,
      "rollout_percentage": 50,
      "organization_ids": ["org-uuid-1", "org-uuid-2"],
      "user_ids": [],
      "metadata": {},
      "created_at": "2024-09-15T14:00:00Z",
      "updated_at": "2024-12-01T10:00:00Z"
    },
    {
      "id": "beta-api-v2",
      "name": "Beta API v2",
      "description": "Enable beta access to API v2 endpoints",
      "enabled": false,
      "rollout_percentage": 0,
      "organization_ids": [],
      "user_ids": ["user-uuid-1"],
      "metadata": {
        "api_version": "2.0.0-beta"
      },
      "created_at": "2024-11-01T09:00:00Z",
      "updated_at": "2024-11-01T09:00:00Z"
    }
  ]
}
```

---

#### PUT /api/admin/system-config/feature-flags/:flag

Update a feature flag.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `flag` | string | Feature flag ID |

**Request Body:**

```json
{
  "enabled": true,
  "rollout_percentage": 75,
  "organization_ids": ["org-uuid-1", "org-uuid-2", "org-uuid-3"]
}
```

**Response:** `200 OK`

```json
{
  "id": "advanced-analytics",
  "name": "Advanced Analytics",
  "description": "Enable advanced analytics features",
  "enabled": true,
  "rollout_percentage": 75,
  "organization_ids": ["org-uuid-1", "org-uuid-2", "org-uuid-3"],
  "user_ids": [],
  "metadata": {},
  "created_at": "2024-09-15T14:00:00Z",
  "updated_at": "2024-12-11T00:00:00Z"
}
```

---

#### GET /api/admin/system-config/rate-limits

Get rate limit configurations.

**Response:** `200 OK`

```json
{
  "data": [
    {
      "endpoint_pattern": "/api/admin/*",
      "requests_per_minute": 60,
      "requests_per_hour": 1000,
      "requests_per_day": 10000,
      "burst_limit": 100,
      "enabled": true
    },
    {
      "endpoint_pattern": "/api/admin/locations/*",
      "requests_per_minute": 120,
      "requests_per_hour": 5000,
      "requests_per_day": 50000,
      "burst_limit": 200,
      "enabled": true
    },
    {
      "endpoint_pattern": "/api/admin/webhooks/*/test",
      "requests_per_minute": 5,
      "requests_per_hour": 20,
      "requests_per_day": 100,
      "burst_limit": 10,
      "enabled": true
    }
  ]
}
```

---

#### PUT /api/admin/system-config/rate-limits

Update rate limit configurations.

**Request Body:**

```json
{
  "endpoint_pattern": "/api/admin/*",
  "requests_per_minute": 100,
  "requests_per_hour": 2000,
  "burst_limit": 150
}
```

**Response:** `200 OK`

```json
{
  "endpoint_pattern": "/api/admin/*",
  "requests_per_minute": 100,
  "requests_per_hour": 2000,
  "requests_per_day": 10000,
  "burst_limit": 150,
  "enabled": true,
  "updated_at": "2024-12-11T00:15:00Z"
}
```

---

#### GET /api/admin/system-config/retention

Get data retention configuration.

**Response:** `200 OK`

```json
{
  "location_data_days": 365,
  "audit_logs_days": 730,
  "app_usage_days": 90,
  "session_data_days": 30,
  "deleted_user_data_days": 90,
  "auto_cleanup_enabled": true,
  "next_cleanup_at": "2024-12-15T03:00:00Z",
  "updated_at": "2024-11-01T10:00:00Z"
}
```

---

#### PUT /api/admin/system-config/retention

Update data retention configuration.

**Request Body:**

```json
{
  "location_data_days": 180,
  "audit_logs_days": 365,
  "auto_cleanup_enabled": true
}
```

**Response:** `200 OK`

```json
{
  "location_data_days": 180,
  "audit_logs_days": 365,
  "app_usage_days": 90,
  "session_data_days": 30,
  "deleted_user_data_days": 90,
  "auto_cleanup_enabled": true,
  "next_cleanup_at": "2024-12-15T03:00:00Z",
  "updated_at": "2024-12-11T00:30:00Z"
}
```

---

#### GET /api/admin/api-keys

List API keys.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `organization_id` | UUID | Filter by organization |
| `status` | string | Filter by status |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "apikey-uuid-1",
      "name": "Production API Key",
      "key_prefix": "pm_live_abc1",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "permissions": ["devices:read", "locations:read", "webhooks:manage"],
      "rate_limit": 1000,
      "expires_at": null,
      "last_used_at": "2024-12-10T23:45:00Z",
      "status": "active",
      "created_by": "user-uuid-1",
      "created_at": "2024-01-15T10:00:00Z"
    },
    {
      "id": "apikey-uuid-2",
      "name": "Development Key",
      "key_prefix": "pm_test_xyz2",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "permissions": ["*"],
      "rate_limit": 100,
      "expires_at": "2025-01-15T10:00:00Z",
      "last_used_at": "2024-12-08T14:30:00Z",
      "status": "active",
      "created_by": "user-uuid-1",
      "created_at": "2024-10-15T09:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 5,
    "totalPages": 1
  }
}
```

---

#### POST /api/admin/api-keys

Create a new API key.

**Request Body:**

```json
{
  "name": "Integration Key",
  "organization_id": "org-uuid-1",
  "permissions": ["devices:read", "locations:read"],
  "rate_limit": 500,
  "expires_at": "2025-06-30T23:59:59Z"
}
```

**Response:** `201 Created`

```json
{
  "id": "apikey-uuid-new",
  "name": "Integration Key",
  "key": "pm_live_newkey123456789abcdefghijklmnop",
  "key_prefix": "pm_live_newk",
  "organization_id": "org-uuid-1",
  "permissions": ["devices:read", "locations:read"],
  "rate_limit": 500,
  "expires_at": "2025-06-30T23:59:59Z",
  "created_at": "2024-12-11T00:45:00Z"
}
```

**Note:** The full API key is only returned once during creation. Store it securely.

---

#### DELETE /api/admin/api-keys/:id

Delete (revoke) an API key.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | API key ID |

**Response:** `204 No Content`

---

#### POST /api/admin/api-keys/:id/rotate

Rotate an API key.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | API key ID |

**Request Body:**

```json
{
  "revoke_old_after_hours": 24
}
```

**Response:** `200 OK`

```json
{
  "id": "apikey-uuid-1",
  "name": "Production API Key",
  "new_key": "pm_live_rotatedkey789xyz123456789abc",
  "new_key_prefix": "pm_live_rota",
  "old_key_valid_until": "2024-12-12T00:45:00Z",
  "created_at": "2024-12-11T00:45:00Z"
}
```

---

## Epic AP-10: Dashboard & Analytics

Endpoints for dashboard metrics, analytics, and report generation.

### Types

```typescript
interface DashboardMetrics {
  period: string;
  users: {
    total: number;
    active: number;
    new_this_period: number;
    growth_percentage: number;
  };
  devices: {
    total: number;
    active: number;
    inactive: number;
    new_this_period: number;
  };
  organizations: {
    total: number;
    active: number;
    new_this_period: number;
  };
  api: {
    total_requests: number;
    success_rate: number;
    avg_response_time_ms: number;
  };
  generated_at: ISO8601DateTime;
}

interface ActivitySummary {
  period: string;
  timeline: Array<{
    timestamp: ISO8601DateTime;
    event_type: string;
    count: number;
  }>;
  top_actions: Array<{
    action: string;
    count: number;
    percentage: number;
  }>;
  active_users: number;
  total_events: number;
}

interface AlertIndicator {
  id: string;
  type: 'warning' | 'error' | 'info';
  category: string;
  title: string;
  message: string;
  count: number;
  severity: 'low' | 'medium' | 'high' | 'critical';
  action_url: string | null;
  created_at: ISO8601DateTime;
}

interface UserAnalytics {
  period: string;
  total_users: number;
  active_users: number;
  new_users: number;
  churned_users: number;
  retention_rate: number;
  avg_session_duration_minutes: number;
  users_by_role: Array<{
    role: string;
    count: number;
    percentage: number;
  }>;
  users_by_organization: Array<{
    organization_id: UUID;
    organization_name: string;
    count: number;
  }>;
  login_trends: Array<{
    date: string;
    count: number;
  }>;
}

interface DeviceAnalytics {
  period: string;
  total_devices: number;
  active_devices: number;
  inactive_devices: number;
  new_enrollments: number;
  unenrollments: number;
  devices_by_platform: Array<{
    platform: string;
    count: number;
    percentage: number;
  }>;
  devices_by_status: Array<{
    status: string;
    count: number;
    percentage: number;
  }>;
  enrollment_trends: Array<{
    date: string;
    enrollments: number;
    unenrollments: number;
  }>;
}

interface ApiAnalytics {
  period: string;
  total_requests: number;
  successful_requests: number;
  failed_requests: number;
  success_rate: number;
  avg_response_time_ms: number;
  p95_response_time_ms: number;
  p99_response_time_ms: number;
  requests_by_endpoint: Array<{
    endpoint: string;
    count: number;
    avg_response_time_ms: number;
    error_rate: number;
  }>;
  requests_by_status: Array<{
    status_code: number;
    count: number;
    percentage: number;
  }>;
  hourly_trends: Array<{
    hour: string;
    count: number;
    avg_response_time_ms: number;
  }>;
}

interface Report {
  id: UUID;
  name: string;
  type: 'users' | 'devices' | 'locations' | 'app_usage' | 'audit' | 'custom';
  status: 'pending' | 'generating' | 'completed' | 'failed';
  parameters: Record<string, any>;
  format: 'pdf' | 'csv' | 'xlsx';
  file_size_bytes: number | null;
  download_url: string | null;
  expires_at: ISO8601DateTime | null;
  created_by: UUID;
  created_at: ISO8601DateTime;
  completed_at: ISO8601DateTime | null;
}

interface GenerateReportRequest {
  name: string;
  type: 'users' | 'devices' | 'locations' | 'app_usage' | 'audit' | 'custom';
  parameters: {
    organization_id?: UUID;
    start_date: ISO8601DateTime;
    end_date: ISO8601DateTime;
    filters?: Record<string, any>;
  };
  format: 'pdf' | 'csv' | 'xlsx';
}
```

### Endpoints

#### GET /api/admin/dashboard/metrics

Get key dashboard metrics.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `period` | string | Time period (day, week, month) |
| `organization_id` | UUID | Filter by organization |

**Response:** `200 OK`

```json
{
  "period": "month",
  "users": {
    "total": 1250,
    "active": 1100,
    "new_this_period": 85,
    "growth_percentage": 7.3
  },
  "devices": {
    "total": 2800,
    "active": 2450,
    "inactive": 350,
    "new_this_period": 120
  },
  "organizations": {
    "total": 45,
    "active": 42,
    "new_this_period": 3
  },
  "api": {
    "total_requests": 5250000,
    "success_rate": 99.2,
    "avg_response_time_ms": 145
  },
  "generated_at": "2024-12-11T01:00:00Z"
}
```

---

#### GET /api/admin/dashboard/activity

Get activity summary.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `period` | string | Time period |
| `organization_id` | UUID | Filter by organization |

**Response:** `200 OK`

```json
{
  "period": "week",
  "timeline": [
    {
      "timestamp": "2024-12-10T00:00:00Z",
      "event_type": "user.login",
      "count": 450
    },
    {
      "timestamp": "2024-12-10T00:00:00Z",
      "event_type": "device.location_update",
      "count": 12500
    },
    {
      "timestamp": "2024-12-10T00:00:00Z",
      "event_type": "geofence.event",
      "count": 85
    }
  ],
  "top_actions": [
    {
      "action": "device.location_update",
      "count": 85000,
      "percentage": 75.2
    },
    {
      "action": "user.login",
      "count": 3200,
      "percentage": 2.8
    },
    {
      "action": "api.request",
      "count": 25000,
      "percentage": 22.0
    }
  ],
  "active_users": 950,
  "total_events": 113200
}
```

---

#### GET /api/admin/dashboard/alerts

Get alert indicators.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `severity` | string | Filter by severity |
| `category` | string | Filter by category |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "alert-1",
      "type": "warning",
      "category": "devices",
      "title": "High Inactive Device Count",
      "message": "45 devices have been inactive for more than 30 days",
      "count": 45,
      "severity": "medium",
      "action_url": "/admin/devices/inactive",
      "created_at": "2024-12-10T08:00:00Z"
    },
    {
      "id": "alert-2",
      "type": "error",
      "category": "webhooks",
      "title": "Webhook Delivery Failures",
      "message": "3 webhooks have failure rates above 50%",
      "count": 3,
      "severity": "high",
      "action_url": "/admin/webhooks?status=failing",
      "created_at": "2024-12-10T10:30:00Z"
    },
    {
      "id": "alert-3",
      "type": "info",
      "category": "users",
      "title": "Pending MFA Enrollments",
      "message": "12 users have not completed required MFA enrollment",
      "count": 12,
      "severity": "low",
      "action_url": "/admin/users?mfa_pending=true",
      "created_at": "2024-12-10T12:00:00Z"
    }
  ],
  "summary": {
    "critical": 0,
    "high": 1,
    "medium": 1,
    "low": 1
  }
}
```

---

#### GET /api/admin/analytics/users

Get user analytics.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `period` | string | Time period |
| `organization_id` | UUID | Filter by organization |
| `start_date` | ISO8601DateTime | Start of date range |
| `end_date` | ISO8601DateTime | End of date range |

**Response:** `200 OK`

```json
{
  "period": "month",
  "total_users": 1250,
  "active_users": 1100,
  "new_users": 85,
  "churned_users": 12,
  "retention_rate": 96.5,
  "avg_session_duration_minutes": 45,
  "users_by_role": [
    {
      "role": "Organization Admin",
      "count": 45,
      "percentage": 3.6
    },
    {
      "role": "Support Admin",
      "count": 25,
      "percentage": 2.0
    },
    {
      "role": "Standard User",
      "count": 1180,
      "percentage": 94.4
    }
  ],
  "users_by_organization": [
    {
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "count": 150
    },
    {
      "organization_id": "org-uuid-2",
      "organization_name": "Tech Startup Inc",
      "count": 48
    }
  ],
  "login_trends": [
    {
      "date": "2024-12-01",
      "count": 850
    },
    {
      "date": "2024-12-02",
      "count": 920
    }
  ]
}
```

---

#### GET /api/admin/analytics/devices

Get device analytics.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `period` | string | Time period |
| `organization_id` | UUID | Filter by organization |
| `start_date` | ISO8601DateTime | Start date |
| `end_date` | ISO8601DateTime | End date |

**Response:** `200 OK`

```json
{
  "period": "month",
  "total_devices": 2800,
  "active_devices": 2450,
  "inactive_devices": 350,
  "new_enrollments": 120,
  "unenrollments": 35,
  "devices_by_platform": [
    {
      "platform": "ios",
      "count": 1680,
      "percentage": 60.0
    },
    {
      "platform": "android",
      "count": 1120,
      "percentage": 40.0
    }
  ],
  "devices_by_status": [
    {
      "status": "active",
      "count": 2450,
      "percentage": 87.5
    },
    {
      "status": "inactive",
      "count": 350,
      "percentage": 12.5
    }
  ],
  "enrollment_trends": [
    {
      "date": "2024-12-01",
      "enrollments": 15,
      "unenrollments": 3
    },
    {
      "date": "2024-12-02",
      "enrollments": 12,
      "unenrollments": 2
    }
  ]
}
```

---

#### GET /api/admin/analytics/api

Get API usage analytics.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `period` | string | Time period |
| `organization_id` | UUID | Filter by organization |
| `start_date` | ISO8601DateTime | Start date |
| `end_date` | ISO8601DateTime | End date |

**Response:** `200 OK`

```json
{
  "period": "week",
  "total_requests": 1250000,
  "successful_requests": 1237500,
  "failed_requests": 12500,
  "success_rate": 99.0,
  "avg_response_time_ms": 145,
  "p95_response_time_ms": 320,
  "p99_response_time_ms": 580,
  "requests_by_endpoint": [
    {
      "endpoint": "/api/admin/locations",
      "count": 450000,
      "avg_response_time_ms": 120,
      "error_rate": 0.5
    },
    {
      "endpoint": "/api/admin/devices",
      "count": 250000,
      "avg_response_time_ms": 95,
      "error_rate": 0.3
    }
  ],
  "requests_by_status": [
    {
      "status_code": 200,
      "count": 1200000,
      "percentage": 96.0
    },
    {
      "status_code": 201,
      "count": 37500,
      "percentage": 3.0
    },
    {
      "status_code": 400,
      "count": 8000,
      "percentage": 0.64
    },
    {
      "status_code": 500,
      "count": 4500,
      "percentage": 0.36
    }
  ],
  "hourly_trends": [
    {
      "hour": "2024-12-10T09:00:00Z",
      "count": 85000,
      "avg_response_time_ms": 130
    },
    {
      "hour": "2024-12-10T10:00:00Z",
      "count": 92000,
      "avg_response_time_ms": 145
    }
  ]
}
```

---

#### GET /api/admin/reports

List generated reports.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `type` | string | Filter by report type |
| `status` | string | Filter by status |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "report-uuid-1",
      "name": "Monthly User Report - December 2024",
      "type": "users",
      "status": "completed",
      "parameters": {
        "start_date": "2024-12-01T00:00:00Z",
        "end_date": "2024-12-31T23:59:59Z"
      },
      "format": "pdf",
      "file_size_bytes": 1250000,
      "download_url": "https://storage.example.com/reports/report-uuid-1.pdf",
      "expires_at": "2025-01-10T01:30:00Z",
      "created_by": "user-uuid-1",
      "created_at": "2024-12-10T01:30:00Z",
      "completed_at": "2024-12-10T01:32:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 15,
    "totalPages": 1
  }
}
```

---

#### POST /api/admin/reports

Generate a new report.

**Request Body:**

```json
{
  "name": "Device Activity Report - Q4 2024",
  "type": "devices",
  "parameters": {
    "organization_id": "org-uuid-1",
    "start_date": "2024-10-01T00:00:00Z",
    "end_date": "2024-12-31T23:59:59Z",
    "filters": {
      "platform": "ios"
    }
  },
  "format": "xlsx"
}
```

**Response:** `202 Accepted`

```json
{
  "id": "report-uuid-new",
  "name": "Device Activity Report - Q4 2024",
  "type": "devices",
  "status": "pending",
  "parameters": {
    "organization_id": "org-uuid-1",
    "start_date": "2024-10-01T00:00:00Z",
    "end_date": "2024-12-31T23:59:59Z",
    "filters": {
      "platform": "ios"
    }
  },
  "format": "xlsx",
  "file_size_bytes": null,
  "download_url": null,
  "expires_at": null,
  "created_by": "admin-user-uuid",
  "created_at": "2024-12-11T01:30:00Z",
  "completed_at": null,
  "estimated_completion": "2024-12-11T01:35:00Z"
}
```

---

#### GET /api/admin/reports/:id

Get report details.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Report ID |

**Response:** `200 OK`

```json
{
  "id": "report-uuid-1",
  "name": "Monthly User Report - December 2024",
  "type": "users",
  "status": "completed",
  "parameters": {
    "start_date": "2024-12-01T00:00:00Z",
    "end_date": "2024-12-31T23:59:59Z"
  },
  "format": "pdf",
  "file_size_bytes": 1250000,
  "download_url": "https://storage.example.com/reports/report-uuid-1.pdf",
  "expires_at": "2025-01-10T01:30:00Z",
  "created_by": "user-uuid-1",
  "created_at": "2024-12-10T01:30:00Z",
  "completed_at": "2024-12-10T01:32:00Z"
}
```

---

#### GET /api/admin/reports/:id/download

Download a report file.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Report ID |

**Response:** `302 Found`

Redirects to the signed download URL.

**Error Response:** `404 Not Found`

```json
{
  "error": {
    "code": "REPORT_NOT_READY",
    "message": "Report is still being generated"
  }
}
```

---

## Epic AP-11: Audit & Compliance

Endpoints for audit logs, GDPR requests, and compliance status.

### Types

```typescript
interface AuditLogEntry {
  id: UUID;
  timestamp: ISO8601DateTime;
  actor_id: UUID;
  actor_type: 'user' | 'api_key' | 'system';
  actor_name: string;
  actor_email: Email | null;
  action: string;
  resource_type: string;
  resource_id: UUID | null;
  resource_name: string | null;
  organization_id: UUID | null;
  organization_name: string | null;
  ip_address: string;
  user_agent: string;
  status: 'success' | 'failure';
  error_message: string | null;
  changes: AuditChanges | null;
  metadata: Record<string, any>;
}

interface AuditChanges {
  before: Record<string, any>;
  after: Record<string, any>;
  fields_changed: string[];
}

interface AuditAction {
  action: string;
  category: string;
  description: string;
  count: number;
}

interface AuditExportRequest {
  start_date: ISO8601DateTime;
  end_date: ISO8601DateTime;
  organization_id?: UUID;
  actor_id?: UUID;
  actions?: string[];
  resource_types?: string[];
  format: 'csv' | 'json';
}

interface GdprRequest {
  id: UUID;
  type: 'access' | 'deletion' | 'rectification' | 'portability';
  status: 'pending' | 'in_progress' | 'completed' | 'rejected';
  requester_email: Email;
  requester_name: string;
  subject_user_id: UUID | null;
  subject_email: Email;
  organization_id: UUID | null;
  organization_name: string | null;
  reason: string;
  notes: string | null;
  processed_by: UUID | null;
  processed_by_name: string | null;
  processed_at: ISO8601DateTime | null;
  download_url: string | null;
  download_expires_at: ISO8601DateTime | null;
  created_at: ISO8601DateTime;
  due_date: ISO8601DateTime;
}

interface ProcessGdprRequest {
  action: 'approve' | 'reject';
  notes?: string;
}

interface ComplianceStatus {
  overall_status: 'compliant' | 'at_risk' | 'non_compliant';
  last_assessment_at: ISO8601DateTime;
  next_assessment_at: ISO8601DateTime;
  frameworks: Array<{
    name: string;
    status: 'compliant' | 'at_risk' | 'non_compliant';
    score: number;
    last_audit_at: ISO8601DateTime;
  }>;
  checks: Array<{
    category: string;
    name: string;
    status: 'passed' | 'warning' | 'failed';
    message: string;
    last_checked_at: ISO8601DateTime;
  }>;
  pending_actions: Array<{
    id: string;
    priority: 'low' | 'medium' | 'high' | 'critical';
    title: string;
    description: string;
    due_date: ISO8601DateTime | null;
  }>;
}
```

### Endpoints

#### GET /api/admin/audit/logs

List audit log entries.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `start_date` | ISO8601DateTime | Start of date range |
| `end_date` | ISO8601DateTime | End of date range |
| `actor_id` | UUID | Filter by actor |
| `action` | string | Filter by action |
| `resource_type` | string | Filter by resource type |
| `resource_id` | UUID | Filter by resource ID |
| `organization_id` | UUID | Filter by organization |
| `status` | string | Filter by status |
| `sort_order` | string | asc or desc (default: desc) |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "audit-uuid-1",
      "timestamp": "2024-12-10T23:45:00Z",
      "actor_id": "user-uuid-1",
      "actor_type": "user",
      "actor_name": "John Admin",
      "actor_email": "admin@acme.com",
      "action": "user.update",
      "resource_type": "user",
      "resource_id": "user-uuid-5",
      "resource_name": "Jane Doe",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "ip_address": "192.168.1.100",
      "user_agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
      "status": "success",
      "error_message": null,
      "changes": {
        "before": {
          "role_id": "role-uuid-2"
        },
        "after": {
          "role_id": "role-uuid-3"
        },
        "fields_changed": ["role_id"]
      },
      "metadata": {
        "reason": "Promotion to admin role"
      }
    },
    {
      "id": "audit-uuid-2",
      "timestamp": "2024-12-10T23:30:00Z",
      "actor_id": "apikey-uuid-1",
      "actor_type": "api_key",
      "actor_name": "Production API Key",
      "actor_email": null,
      "action": "device.create",
      "resource_type": "device",
      "resource_id": "device-uuid-100",
      "resource_name": "New Device",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "ip_address": "10.0.0.50",
      "user_agent": "PhoneManager-SDK/2.5.0",
      "status": "success",
      "error_message": null,
      "changes": null,
      "metadata": {}
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 15000,
    "totalPages": 750
  }
}
```

---

#### GET /api/admin/audit/logs/:id

Get a specific audit log entry.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Audit log entry ID |

**Response:** `200 OK`

```json
{
  "id": "audit-uuid-1",
  "timestamp": "2024-12-10T23:45:00Z",
  "actor_id": "user-uuid-1",
  "actor_type": "user",
  "actor_name": "John Admin",
  "actor_email": "admin@acme.com",
  "action": "user.update",
  "resource_type": "user",
  "resource_id": "user-uuid-5",
  "resource_name": "Jane Doe",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "ip_address": "192.168.1.100",
  "user_agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
  "status": "success",
  "error_message": null,
  "changes": {
    "before": {
      "role_id": "role-uuid-2",
      "name": "Jane Doe"
    },
    "after": {
      "role_id": "role-uuid-3",
      "name": "Jane Doe"
    },
    "fields_changed": ["role_id"]
  },
  "metadata": {
    "reason": "Promotion to admin role",
    "session_id": "session-uuid-xyz"
  }
}
```

---

#### GET /api/admin/audit/actions

List available audit actions.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `category` | string | Filter by category |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "action": "user.create",
      "category": "users",
      "description": "User account created",
      "count": 125
    },
    {
      "action": "user.update",
      "category": "users",
      "description": "User account updated",
      "count": 450
    },
    {
      "action": "user.delete",
      "category": "users",
      "description": "User account deleted",
      "count": 25
    },
    {
      "action": "user.login",
      "category": "authentication",
      "description": "User logged in",
      "count": 8500
    },
    {
      "action": "user.logout",
      "category": "authentication",
      "description": "User logged out",
      "count": 7200
    },
    {
      "action": "device.enroll",
      "category": "devices",
      "description": "Device enrolled",
      "count": 320
    },
    {
      "action": "geofence.event",
      "category": "locations",
      "description": "Geofence event triggered",
      "count": 5600
    }
  ],
  "categories": ["users", "authentication", "devices", "locations", "organizations", "webhooks", "system"]
}
```

---

#### POST /api/admin/audit/export

Export audit logs.

**Request Body:**

```json
{
  "start_date": "2024-11-01T00:00:00Z",
  "end_date": "2024-11-30T23:59:59Z",
  "organization_id": "org-uuid-1",
  "actions": ["user.create", "user.update", "user.delete"],
  "format": "csv"
}
```

**Response:** `202 Accepted`

```json
{
  "export_id": "export-uuid-audit",
  "status": "pending",
  "parameters": {
    "start_date": "2024-11-01T00:00:00Z",
    "end_date": "2024-11-30T23:59:59Z",
    "organization_id": "org-uuid-1",
    "actions": ["user.create", "user.update", "user.delete"]
  },
  "format": "csv",
  "estimated_record_count": 600,
  "download_url": null,
  "created_at": "2024-12-11T02:00:00Z",
  "estimated_completion": "2024-12-11T02:05:00Z"
}
```

---

#### GET /api/admin/gdpr/requests

List GDPR requests.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | integer | Page number |
| `limit` | integer | Items per page |
| `type` | string | Filter by request type |
| `status` | string | Filter by status |
| `organization_id` | UUID | Filter by organization |

**Response:** `200 OK`

```json
{
  "data": [
    {
      "id": "gdpr-uuid-1",
      "type": "access",
      "status": "pending",
      "requester_email": "user@example.com",
      "requester_name": "John User",
      "subject_user_id": "user-uuid-25",
      "subject_email": "user@example.com",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "reason": "Request for personal data under GDPR Article 15",
      "notes": null,
      "processed_by": null,
      "processed_by_name": null,
      "processed_at": null,
      "download_url": null,
      "download_expires_at": null,
      "created_at": "2024-12-08T10:00:00Z",
      "due_date": "2024-01-07T10:00:00Z"
    },
    {
      "id": "gdpr-uuid-2",
      "type": "deletion",
      "status": "completed",
      "requester_email": "former@acme.com",
      "requester_name": "Former Employee",
      "subject_user_id": "user-uuid-deleted",
      "subject_email": "former@acme.com",
      "organization_id": "org-uuid-1",
      "organization_name": "Acme Corporation",
      "reason": "Right to be forgotten request",
      "notes": "All personal data deleted per GDPR Article 17",
      "processed_by": "user-uuid-1",
      "processed_by_name": "John Admin",
      "processed_at": "2024-12-05T14:30:00Z",
      "download_url": null,
      "download_expires_at": null,
      "created_at": "2024-11-28T09:00:00Z",
      "due_date": "2024-12-28T09:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 8,
    "totalPages": 1
  },
  "summary": {
    "pending": 2,
    "in_progress": 1,
    "completed": 4,
    "rejected": 1
  }
}
```

---

#### GET /api/admin/gdpr/requests/:id

Get GDPR request details.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | GDPR request ID |

**Response:** `200 OK`

```json
{
  "id": "gdpr-uuid-1",
  "type": "access",
  "status": "pending",
  "requester_email": "user@example.com",
  "requester_name": "John User",
  "subject_user_id": "user-uuid-25",
  "subject_email": "user@example.com",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "reason": "Request for personal data under GDPR Article 15",
  "notes": null,
  "processed_by": null,
  "processed_by_name": null,
  "processed_at": null,
  "download_url": null,
  "download_expires_at": null,
  "created_at": "2024-12-08T10:00:00Z",
  "due_date": "2024-01-07T10:00:00Z"
}
```

---

#### POST /api/admin/gdpr/requests/:id/process

Process a GDPR request.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | GDPR request ID |

**Request Body:**

```json
{
  "action": "approve",
  "notes": "Personal data export generated and sent to requester"
}
```

**Response:** `200 OK`

```json
{
  "id": "gdpr-uuid-1",
  "type": "access",
  "status": "completed",
  "requester_email": "user@example.com",
  "requester_name": "John User",
  "subject_user_id": "user-uuid-25",
  "subject_email": "user@example.com",
  "organization_id": "org-uuid-1",
  "organization_name": "Acme Corporation",
  "reason": "Request for personal data under GDPR Article 15",
  "notes": "Personal data export generated and sent to requester",
  "processed_by": "admin-user-uuid",
  "processed_by_name": "Admin User",
  "processed_at": "2024-12-11T02:30:00Z",
  "download_url": "https://storage.example.com/gdpr/gdpr-uuid-1-export.zip",
  "download_expires_at": "2024-12-18T02:30:00Z",
  "created_at": "2024-12-08T10:00:00Z",
  "due_date": "2024-01-07T10:00:00Z"
}
```

---

#### GET /api/admin/compliance/status

Get compliance status overview.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `organization_id` | UUID | Filter by organization |

**Response:** `200 OK`

```json
{
  "overall_status": "compliant",
  "last_assessment_at": "2024-12-01T03:00:00Z",
  "next_assessment_at": "2025-01-01T03:00:00Z",
  "frameworks": [
    {
      "name": "GDPR",
      "status": "compliant",
      "score": 95,
      "last_audit_at": "2024-11-15T10:00:00Z"
    },
    {
      "name": "SOC 2 Type II",
      "status": "compliant",
      "score": 98,
      "last_audit_at": "2024-10-01T10:00:00Z"
    },
    {
      "name": "HIPAA",
      "status": "at_risk",
      "score": 82,
      "last_audit_at": "2024-09-01T10:00:00Z"
    }
  ],
  "checks": [
    {
      "category": "data_protection",
      "name": "Encryption at Rest",
      "status": "passed",
      "message": "All data encrypted with AES-256",
      "last_checked_at": "2024-12-10T03:00:00Z"
    },
    {
      "category": "data_protection",
      "name": "Encryption in Transit",
      "status": "passed",
      "message": "TLS 1.3 enforced on all endpoints",
      "last_checked_at": "2024-12-10T03:00:00Z"
    },
    {
      "category": "access_control",
      "name": "MFA Enforcement",
      "status": "warning",
      "message": "12 admin users without MFA enabled",
      "last_checked_at": "2024-12-10T03:00:00Z"
    },
    {
      "category": "audit",
      "name": "Audit Log Retention",
      "status": "passed",
      "message": "Audit logs retained for 730 days",
      "last_checked_at": "2024-12-10T03:00:00Z"
    }
  ],
  "pending_actions": [
    {
      "id": "action-1",
      "priority": "high",
      "title": "Enable MFA for Admin Users",
      "description": "12 admin users do not have MFA enabled. Enable MFA to meet compliance requirements.",
      "due_date": "2024-12-31T23:59:59Z"
    },
    {
      "id": "action-2",
      "priority": "medium",
      "title": "Review HIPAA Training Records",
      "description": "Verify all employees with PHI access have completed HIPAA training within the last 12 months.",
      "due_date": "2025-01-15T23:59:59Z"
    }
  ]
}
```

---

## Error Handling

### Standard Error Response

All API errors follow a consistent format:

```typescript
interface ApiError {
  error: {
    code: string;
    message: string;
    details?: Record<string, string[]>;
    request_id?: string;
  };
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `UNAUTHORIZED` | 401 | Missing or invalid authentication |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `VALIDATION_ERROR` | 422 | Request validation failed |
| `CONFLICT` | 409 | Resource conflict (e.g., duplicate) |
| `RATE_LIMITED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Internal server error |

### Error Examples

**401 Unauthorized:**
```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid or expired access token",
    "request_id": "req-uuid-123"
  }
}
```

**403 Forbidden:**
```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "You do not have permission to access this resource",
    "request_id": "req-uuid-124"
  }
}
```

**404 Not Found:**
```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "User with ID 'user-uuid-invalid' not found",
    "request_id": "req-uuid-125"
  }
}
```

**422 Validation Error:**
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": {
      "email": ["Invalid email format"],
      "name": ["Name is required", "Name must be at least 2 characters"]
    },
    "request_id": "req-uuid-126"
  }
}
```

**429 Rate Limited:**
```json
{
  "error": {
    "code": "RATE_LIMITED",
    "message": "Too many requests. Please try again in 60 seconds.",
    "request_id": "req-uuid-127"
  }
}
```

---

## Rate Limiting

### Rate Limit Headers

All API responses include rate limiting information:

```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1702260000
```

### Default Limits

| Endpoint Pattern | Requests/Minute | Requests/Hour | Burst |
|------------------|-----------------|---------------|-------|
| `/api/admin/*` | 60 | 1000 | 100 |
| `/api/admin/locations/*` | 120 | 5000 | 200 |
| `/api/admin/webhooks/*/test` | 5 | 20 | 10 |
| `/api/admin/reports` | 10 | 50 | 20 |
| `/api/admin/audit/export` | 5 | 20 | 10 |

### Rate Limit Exceeded Response

When rate limits are exceeded:

```
HTTP/1.1 429 Too Many Requests
Retry-After: 60
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1702260060
```

```json
{
  "error": {
    "code": "RATE_LIMITED",
    "message": "Rate limit exceeded. Please retry after 60 seconds."
  }
}
```

---

## Appendix: Endpoint Summary

### Total Endpoints: 115

| Epic | Endpoints | Description |
|------|-----------|-------------|
| AP-1 | 3 | RBAC & Access Control |
| AP-2 | 7 | Organization Management |
| AP-3 | 13 | User Administration |
| AP-4 | 15 | Device Fleet Administration |
| AP-5 | 10 | Groups Administration |
| AP-6 | 13 | Location & Geofence Administration |
| AP-7 | 9 | Webhook Administration |
| AP-8 | 13 | App Usage & Unlock Requests |
| AP-9 | 14 | System Configuration |
| AP-10 | 10 | Dashboard & Analytics |
| AP-11 | 8 | Audit & Compliance |
| AP-12 | 0 | Admin Portal UI Shell (frontend only) |
