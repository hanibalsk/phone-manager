/**
 * Permission codes for RBAC system.
 * These match the permission codes defined in the backend.
 * Format: CATEGORY.ACTION (e.g., USERS.CREATE, DEVICES.READ)
 */

// User permissions
export const USERS_READ = "users.read";
export const USERS_CREATE = "users.create";
export const USERS_UPDATE = "users.update";
export const USERS_DELETE = "users.delete";
export const USERS_SUSPEND = "users.suspend";
export const USERS_MANAGE_MFA = "users.manage_mfa";
export const USERS_MANAGE_SESSIONS = "users.manage_sessions";
export const USERS_ASSIGN_ROLES = "users.assign_roles";

// Organization permissions
export const ORGANIZATIONS_READ = "organizations.read";
export const ORGANIZATIONS_CREATE = "organizations.create";
export const ORGANIZATIONS_UPDATE = "organizations.update";
export const ORGANIZATIONS_DELETE = "organizations.delete";
export const ORGANIZATIONS_MANAGE_LIMITS = "organizations.manage_limits";
export const ORGANIZATIONS_MANAGE_FEATURES = "organizations.manage_features";

// Device permissions
export const DEVICES_READ = "devices.read";
export const DEVICES_CREATE = "devices.create";
export const DEVICES_UPDATE = "devices.update";
export const DEVICES_DELETE = "devices.delete";
export const DEVICES_MANAGE_POLICIES = "devices.manage_policies";

// Location permissions
export const LOCATIONS_READ = "locations.read";
export const LOCATIONS_EXPORT = "locations.export";

// Geofence permissions
export const GEOFENCES_READ = "geofences.read";
export const GEOFENCES_CREATE = "geofences.create";
export const GEOFENCES_UPDATE = "geofences.update";
export const GEOFENCES_DELETE = "geofences.delete";

// Alert permissions
export const ALERTS_READ = "alerts.read";
export const ALERTS_CREATE = "alerts.create";
export const ALERTS_UPDATE = "alerts.update";
export const ALERTS_DELETE = "alerts.delete";

// Webhook permissions
export const WEBHOOKS_READ = "webhooks.read";
export const WEBHOOKS_CREATE = "webhooks.create";
export const WEBHOOKS_UPDATE = "webhooks.update";
export const WEBHOOKS_DELETE = "webhooks.delete";
export const WEBHOOKS_MANAGE = "webhooks.manage";

// Trip permissions
export const TRIPS_READ = "trips.read";
export const TRIPS_EXPORT = "trips.export";

// Group permissions
export const GROUPS_READ = "groups.read";
export const GROUPS_CREATE = "groups.create";
export const GROUPS_UPDATE = "groups.update";
export const GROUPS_DELETE = "groups.delete";
export const GROUPS_MANAGE_MEMBERS = "groups.manage_members";

// Enrollment permissions
export const ENROLLMENT_READ = "enrollment.read";
export const ENROLLMENT_CREATE = "enrollment.create";
export const ENROLLMENT_MANAGE = "enrollment.manage";

// Audit permissions
export const AUDIT_READ = "audit.read";
export const AUDIT_EXPORT = "audit.export";

// Config permissions
export const CONFIG_READ = "config.read";
export const CONFIG_UPDATE = "config.update";

// Reports permissions
export const REPORTS_READ = "reports.read";
export const REPORTS_CREATE = "reports.create";
export const REPORTS_EXPORT = "reports.export";

// API Keys permissions
export const API_KEYS_READ = "api_keys.read";
export const API_KEYS_CREATE = "api_keys.create";
export const API_KEYS_DELETE = "api_keys.delete";
export const API_KEYS_ROTATE = "api_keys.rotate";

// Roles permissions (for RBAC management)
export const ROLES_READ = "roles.read";
export const ROLES_CREATE = "roles.create";
export const ROLES_UPDATE = "roles.update";
export const ROLES_DELETE = "roles.delete";

/**
 * Human-readable labels for permission categories.
 * Used in role creation/editing dialogs and permission displays.
 */
export const CATEGORY_LABELS: Record<string, string> = {
  users: "Users",
  organizations: "Organizations",
  devices: "Devices",
  locations: "Locations",
  geofences: "Geofences",
  alerts: "Alerts",
  webhooks: "Webhooks",
  trips: "Trips",
  groups: "Groups",
  enrollment: "Enrollment",
  audit: "Audit",
  config: "Configuration",
  reports: "Reports",
  api_keys: "API Keys",
  roles: "Roles",
};

/**
 * Permission groups for common use cases
 */
export const PERMISSION_GROUPS = {
  // Full user management
  USER_MANAGEMENT: [
    USERS_READ,
    USERS_CREATE,
    USERS_UPDATE,
    USERS_DELETE,
    USERS_SUSPEND,
    USERS_MANAGE_MFA,
    USERS_MANAGE_SESSIONS,
    USERS_ASSIGN_ROLES,
  ],

  // Full organization management
  ORG_MANAGEMENT: [
    ORGANIZATIONS_READ,
    ORGANIZATIONS_CREATE,
    ORGANIZATIONS_UPDATE,
    ORGANIZATIONS_DELETE,
    ORGANIZATIONS_MANAGE_LIMITS,
    ORGANIZATIONS_MANAGE_FEATURES,
  ],

  // Full device management
  DEVICE_MANAGEMENT: [
    DEVICES_READ,
    DEVICES_CREATE,
    DEVICES_UPDATE,
    DEVICES_DELETE,
    DEVICES_MANAGE_POLICIES,
  ],

  // Full role management
  ROLE_MANAGEMENT: [
    ROLES_READ,
    ROLES_CREATE,
    ROLES_UPDATE,
    ROLES_DELETE,
  ],

  // Read-only access
  READ_ONLY: [
    USERS_READ,
    ORGANIZATIONS_READ,
    DEVICES_READ,
    LOCATIONS_READ,
    GEOFENCES_READ,
    ALERTS_READ,
    WEBHOOKS_READ,
    TRIPS_READ,
    GROUPS_READ,
    ENROLLMENT_READ,
    AUDIT_READ,
    CONFIG_READ,
    REPORTS_READ,
    API_KEYS_READ,
    ROLES_READ,
  ],
} as const;

/**
 * Navigation items and their required permissions
 */
export const NAVIGATION_PERMISSIONS = {
  dashboard: [], // Everyone can see dashboard
  organizations: [ORGANIZATIONS_READ],
  users: [USERS_READ],
  roles: [ROLES_READ],
  devices: [DEVICES_READ],
  unlockRequests: [DEVICES_READ],
  limits: [DEVICES_UPDATE],
  settings: [CONFIG_READ],
  configuration: [CONFIG_UPDATE],
} as const;
