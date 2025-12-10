// Common types for the admin portal
// Note: Field names use snake_case to match backend JSON serialization

export interface Device {
  id: string;
  name: string;
  android_id: string;
  enrolled_at: string;
  last_seen: string;
  status: "active" | "inactive" | "pending";
}

export interface UnlockRequest {
  id: string;
  device_id: string;
  device_name: string;
  reason: string;
  requested_at: string;
  status: "pending" | "approved" | "denied";
  requested_duration: number;
  admin_response?: string;
}

export interface AppUsage {
  package_name: string;
  app_name: string;
  usage_time_minutes: number;
  last_used: string;
}

export interface DailyLimit {
  id: string;
  package_name: string;
  app_name: string;
  daily_limit_minutes: number;
  enabled: boolean;
}

export interface AdminSettings {
  unlock_pin: string;
  default_daily_limit_minutes: number;
  notifications_enabled: boolean;
  auto_approve_unlock_requests: boolean;
}

export interface ApiResponse<T> {
  data?: T;
  error?: string;
  status: number;
}

// Public configuration types
export interface PublicConfig {
  auth: AuthConfig;
  features: FeaturesConfig;
}

export interface AuthConfig {
  registration_enabled: boolean;
  invite_only: boolean;
  oauth_only: boolean;
  google_enabled: boolean;
  apple_enabled: boolean;
}

export interface FeaturesConfig {
  geofences: boolean;
  proximity_alerts: boolean;
  webhooks: boolean;
  movement_tracking: boolean;
  b2b: boolean;
  geofence_events: boolean;
}

// User Administration types (Story AP-3.1)
export type UserStatus = "active" | "suspended" | "pending_verification" | "locked";
export type UserRole = "super_admin" | "org_admin" | "org_manager" | "support" | "viewer";

export interface AdminUser {
  id: string;
  email: string;
  display_name: string;
  avatar_url: string | null;
  status: UserStatus;
  role: UserRole;
  organization_id: string | null;
  organization_name: string | null;
  mfa_enabled: boolean;
  created_at: string;
  last_login: string | null;
}

export interface UserListParams {
  page?: number;
  limit?: number;
  search?: string;
  status?: UserStatus;
  role?: UserRole;
  sort_by?: "email" | "display_name" | "created_at" | "last_login";
  sort_order?: "asc" | "desc";
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  limit: number;
}

export interface CreateUserRequest {
  email: string;
  display_name: string;
  organization_id?: string;
  role: UserRole;
  send_welcome_email?: boolean;
}

// Story AP-3.5: Session Management
export interface UserSession {
  id: string;
  device: string;
  ip_address: string;
  user_agent: string;
  last_activity: string;
  created_at: string;
  is_current: boolean;
}

// Story AP-3.6: MFA Management
export interface MfaStatus {
  enabled: boolean;
  method: "totp" | "sms" | "email" | null;
  enrolled_at: string | null;
  backup_codes_remaining: number;
}

// Epic AP-2: Organization Management
export type OrganizationType = "enterprise" | "smb" | "startup" | "personal";
export type OrganizationStatus = "active" | "suspended" | "pending" | "archived";

export interface Organization {
  id: string;
  name: string;
  slug: string;
  type: OrganizationType;
  status: OrganizationStatus;
  contact_email: string;
  max_devices: number;
  max_users: number;
  max_groups: number;
  features: OrganizationFeatures;
  created_at: string;
  updated_at: string;
}

export interface OrganizationFeatures {
  geofences: boolean;
  proximity_alerts: boolean;
  webhooks: boolean;
  trips: boolean;
  movement_tracking: boolean;
}

export interface CreateOrganizationRequest {
  name: string;
  slug: string;
  type: OrganizationType;
  contact_email: string;
}

export interface UpdateOrganizationRequest {
  name?: string;
  type?: OrganizationType;
  contact_email?: string;
}

export interface OrganizationListParams {
  page?: number;
  limit?: number;
  search?: string;
  status?: OrganizationStatus;
  type?: OrganizationType;
}

// Epic AP-1: RBAC & Access Control
export type SystemRole = "super_admin" | "org_admin" | "org_manager" | "support" | "viewer";

export type PermissionCategory =
  | "users"
  | "organizations"
  | "devices"
  | "locations"
  | "geofences"
  | "alerts"
  | "webhooks"
  | "trips"
  | "groups"
  | "enrollment"
  | "audit"
  | "config"
  | "reports"
  | "api_keys";

export interface Permission {
  id: string;
  code: string; // e.g., "USERS.CREATE", "DEVICES.READ"
  name: string;
  description: string;
  category: PermissionCategory;
}

export interface Role {
  id: string;
  name: string;
  code: SystemRole;
  description: string;
  is_system: boolean; // true for predefined roles
  permissions: Permission[];
  user_count: number;
  created_at: string;
  updated_at: string;
}

export interface CreateRoleRequest {
  name: string;
  code: string;
  description: string;
  permission_ids: string[];
}

export interface UpdateRoleRequest {
  name?: string;
  description?: string;
  permission_ids?: string[];
}

// Story AP-1.2: User Role Assignment
export interface UserRoleAssignment {
  id: string;
  user_id: string;
  user_email: string;
  user_name: string;
  role_id: string;
  role_code: SystemRole;
  role_name: string;
  organization_id: string | null;
  organization_name: string | null;
  assigned_at: string;
  assigned_by: string;
}

// Epic AP-4: Device Fleet Administration
export type DevicePlatform = "android" | "ios";
export type AdminDeviceStatus = "active" | "suspended" | "offline" | "pending";

export interface AdminDevice {
  id: string;
  device_id: string;
  display_name: string;
  platform: DevicePlatform;
  status: AdminDeviceStatus;
  owner_id: string;
  owner_email: string;
  organization_id: string;
  organization_name: string;
  group_id: string | null;
  group_name: string | null;
  last_seen: string | null;
  location_count: number;
  trip_count: number;
  created_at: string;
}

export interface DeviceListParams {
  page?: number;
  limit?: number;
  search?: string;
  organization_id?: string;
  group_id?: string;
  status?: AdminDeviceStatus;
  platform?: DevicePlatform;
  sort_by?: "display_name" | "last_seen" | "location_count" | "created_at";
  sort_order?: "asc" | "desc";
}

// Story AP-4.2: Device Details
export interface DeviceDetails extends AdminDevice {
  enrollment_status: "enrolled" | "pending" | "expired";
  policy_id: string | null;
  policy_name: string | null;
  policy_compliant: boolean;
  compliance_issues: string[];
  last_location: {
    latitude: number;
    longitude: number;
    timestamp: string;
  } | null;
}

// Story AP-4.4: Bulk Operations
export interface BulkOperationResult {
  total: number;
  success_count: number;
  failure_count: number;
  failures: {
    device_id: string;
    device_name: string;
    error: string;
  }[];
}

// Story AP-4.3: Enrollment Token Management
export type EnrollmentTokenStatus = "active" | "expired" | "revoked" | "exhausted";

export interface EnrollmentToken {
  id: string;
  name: string;
  code: string;
  max_uses: number | null;
  uses_count: number;
  status: EnrollmentTokenStatus;
  expires_at: string | null;
  policy_id: string | null;
  policy_name: string | null;
  created_at: string;
  created_by: string;
}

export interface CreateEnrollmentTokenRequest {
  name: string;
  max_uses?: number;
  expires_at?: string;
  policy_id?: string;
}

export interface TokenUsage {
  device_id: string;
  device_name: string;
  enrolled_at: string;
}

// Story AP-4.5: Inactive Device Management
export interface InactiveDevice extends AdminDevice {
  days_inactive: number;
  last_activity_type: "location" | "sync" | "login";
}

export interface DataRetentionPolicy {
  locations_retained_days: number;
  trips_retained: boolean;
  user_data_deleted: boolean;
}

export interface NotifyOwnersResult {
  sent: number;
  failed: number;
}

// Epic AP-5: Groups Administration
export type GroupStatus = "active" | "suspended" | "archived";
export type GroupMemberRole = "admin" | "member";

export interface AdminGroup {
  id: string;
  name: string;
  description: string | null;
  owner_id: string;
  owner_name: string;
  owner_email: string;
  organization_id: string;
  organization_name: string;
  member_count: number;
  device_count: number;
  status: GroupStatus;
  invite_code: string | null;
  created_at: string;
  updated_at: string;
}

export interface GroupListParams {
  page?: number;
  limit?: number;
  search?: string;
  organization_id?: string;
  status?: GroupStatus;
  sort_by?: "name" | "member_count" | "device_count" | "created_at";
  sort_order?: "asc" | "desc";
}

// Story AP-5.2: Group Membership
export interface GroupMember {
  id: string;
  user_id: string;
  user_name: string;
  user_email: string;
  role: GroupMemberRole;
  joined_at: string;
  device_count: number;
}

// Story AP-5.4: Group Invites
export type InviteStatus = "pending" | "accepted" | "expired" | "revoked";

export interface GroupInvite {
  id: string;
  group_id: string;
  group_name: string;
  code: string;
  created_by: string;
  created_by_email: string;
  status: InviteStatus;
  used_by: string | null;
  used_by_email: string | null;
  expires_at: string;
  created_at: string;
  used_at: string | null;
}
