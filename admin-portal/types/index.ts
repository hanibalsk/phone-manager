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

// Epic AP-6: Location & Geofence Administration

// Story AP-6.1: Location Map View
export interface DeviceLocation {
  id: string;
  device_id: string;
  device_name: string;
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude: number | null;
  speed: number | null;
  bearing: number | null;
  battery_level: number | null;
  timestamp: string;
  organization_id: string;
  organization_name: string;
}

export interface LocationFilter {
  device_id?: string;
  organization_id?: string;
  from?: string;
  to?: string;
  bbox?: {
    north: number;
    south: number;
    east: number;
    west: number;
  };
}

export interface LatestDeviceLocation {
  device_id: string;
  device_name: string;
  organization_id: string;
  organization_name: string;
  latitude: number;
  longitude: number;
  accuracy: number;
  battery_level: number | null;
  timestamp: string;
  status: AdminDeviceStatus;
}

// Story AP-6.3: Geofence Management
export type GeofenceShape = "circle" | "polygon";

export interface Geofence {
  id: string;
  name: string;
  device_id: string;
  device_name: string;
  organization_id: string;
  organization_name: string;
  shape: GeofenceShape;
  center_latitude: number | null;
  center_longitude: number | null;
  radius_meters: number | null;
  polygon_coordinates: Array<{ latitude: number; longitude: number }> | null;
  enabled: boolean;
  trigger_on_enter: boolean;
  trigger_on_exit: boolean;
  trigger_on_dwell: boolean;
  dwell_time_seconds: number | null;
  created_at: string;
  updated_at: string;
}

export interface GeofenceListParams {
  device_id?: string;
  organization_id?: string;
  enabled?: boolean;
  search?: string;
  page?: number;
  limit?: number;
}

export interface CreateGeofenceRequest {
  name: string;
  device_id: string;
  shape: GeofenceShape;
  center_latitude?: number;
  center_longitude?: number;
  radius_meters?: number;
  polygon_coordinates?: Array<{ latitude: number; longitude: number }>;
  trigger_on_enter?: boolean;
  trigger_on_exit?: boolean;
  trigger_on_dwell?: boolean;
  dwell_time_seconds?: number;
}

// Story AP-6.4: Geofence Events
export type GeofenceEventType = "ENTER" | "EXIT" | "DWELL";

export interface GeofenceEvent {
  id: string;
  geofence_id: string;
  geofence_name: string;
  device_id: string;
  device_name: string;
  event_type: GeofenceEventType;
  latitude: number;
  longitude: number;
  triggered_at: string;
  dwell_time_seconds: number | null;
}

export interface GeofenceEventFilter {
  geofence_id?: string;
  device_id?: string;
  event_type?: GeofenceEventType;
  from?: string;
  to?: string;
  page?: number;
  limit?: number;
}

// Story AP-6.5: Proximity Alerts
export interface ProximityAlert {
  id: string;
  name: string;
  device_a_id: string;
  device_a_name: string;
  device_b_id: string;
  device_b_name: string;
  organization_id: string;
  organization_name: string;
  trigger_distance_meters: number;
  cooldown_seconds: number;
  enabled: boolean;
  last_triggered_at: string | null;
  trigger_count: number;
  created_at: string;
  updated_at: string;
}

export interface ProximityAlertTrigger {
  id: string;
  alert_id: string;
  distance_meters: number;
  device_a_latitude: number;
  device_a_longitude: number;
  device_b_latitude: number;
  device_b_longitude: number;
  triggered_at: string;
}

export interface CreateProximityAlertRequest {
  name: string;
  device_a_id: string;
  device_b_id: string;
  trigger_distance_meters: number;
  cooldown_seconds?: number;
}

export interface ProximityAlertListParams {
  device_id?: string;
  organization_id?: string;
  enabled?: boolean;
  search?: string;
  page?: number;
  limit?: number;
}

// Story AP-6.6: Data Retention
export interface RetentionPolicy {
  organization_id: string;
  organization_name: string;
  location_retention_days: number;
  event_retention_days: number;
  trip_retention_days: number;
  auto_delete_enabled: boolean;
  last_purge_at: string | null;
  storage_used_mb: number;
  updated_at: string;
}

export interface UpdateRetentionPolicyRequest {
  location_retention_days?: number;
  event_retention_days?: number;
  trip_retention_days?: number;
  auto_delete_enabled?: boolean;
}

export interface PurgeResult {
  locations_deleted: number;
  events_deleted: number;
  trips_deleted: number;
  storage_freed_mb: number;
}

// Epic AP-7: Webhooks & Trips Administration

// Story AP-7.1: Webhooks
export type WebhookEventType =
  | "location_update"
  | "geofence_event"
  | "proximity_alert"
  | "trip_complete"
  | "device_status";

export type WebhookStatus = "active" | "paused" | "failed";

export interface Webhook {
  id: string;
  name: string;
  url: string;
  organization_id: string;
  organization_name: string;
  status: WebhookStatus;
  event_types: WebhookEventType[];
  secret: string | null;
  success_count: number;
  failure_count: number;
  last_delivery_at: string | null;
  last_delivery_status: "success" | "failed" | null;
  created_at: string;
  updated_at: string;
}

export interface WebhookListParams {
  organization_id?: string;
  status?: WebhookStatus;
  event_type?: WebhookEventType;
  search?: string;
  page?: number;
  limit?: number;
}

export interface CreateWebhookRequest {
  name: string;
  url: string;
  event_types: WebhookEventType[];
  secret?: string;
}

export interface UpdateWebhookRequest {
  name?: string;
  url?: string;
  event_types?: WebhookEventType[];
  secret?: string;
}

// Story AP-7.2: Webhook Deliveries
export type DeliveryStatus = "success" | "failed" | "pending";

export interface WebhookDelivery {
  id: string;
  webhook_id: string;
  event_type: WebhookEventType;
  status: DeliveryStatus;
  request_payload: string;
  response_status: number | null;
  response_body: string | null;
  response_headers: Record<string, string> | null;
  duration_ms: number | null;
  retry_count: number;
  error_message: string | null;
  created_at: string;
  completed_at: string | null;
}

export interface WebhookDeliveryListParams {
  status?: DeliveryStatus;
  from?: string;
  to?: string;
  page?: number;
  limit?: number;
}

// Story AP-7.3: Webhook Test
export interface WebhookTestResult {
  success: boolean;
  status_code: number | null;
  response_body: string | null;
  response_time_ms: number | null;
  error_message: string | null;
  accessibility: "reachable" | "unreachable" | "timeout";
}

// Story AP-7.4: Trips
export type TripStatus = "in_progress" | "completed" | "paused";

export interface Trip {
  id: string;
  device_id: string;
  device_name: string;
  organization_id: string;
  organization_name: string;
  status: TripStatus;
  start_time: string;
  end_time: string | null;
  duration_seconds: number;
  distance_meters: number;
  start_latitude: number;
  start_longitude: number;
  end_latitude: number | null;
  end_longitude: number | null;
  point_count: number;
  created_at: string;
}

export interface TripPoint {
  latitude: number;
  longitude: number;
  altitude: number | null;
  speed: number | null;
  bearing: number | null;
  accuracy: number;
  timestamp: string;
}

export type TripEventType = "trip_start" | "stop_detected" | "resumed" | "trip_end";

export interface TripEvent {
  id: string;
  trip_id: string;
  event_type: TripEventType;
  latitude: number;
  longitude: number;
  timestamp: string;
  metadata: Record<string, unknown> | null;
}

export interface TripListParams {
  device_id?: string;
  organization_id?: string;
  status?: TripStatus;
  from?: string;
  to?: string;
  page?: number;
  limit?: number;
}

// Story AP-7.5: Trip Export
export interface TripExportRequest {
  device_id?: string;
  organization_id?: string;
  from?: string;
  to?: string;
  format: "csv" | "json";
  include_path: boolean;
}

// Epic AP-8: App Usage & Unlock Requests

// Story AP-8.1: App Usage Statistics
export type AppCategory = "social" | "games" | "productivity" | "entertainment" | "education" | "communication" | "other";

export interface AdminAppUsage {
  id: string;
  device_id: string;
  device_name: string;
  organization_id: string;
  organization_name: string;
  package_name: string;
  app_name: string;
  category: AppCategory;
  usage_minutes: number;
  date: string;
}

export interface AppUsageByCategory {
  category: AppCategory;
  total_minutes: number;
  app_count: number;
  percentage: number;
}

export interface TopApp {
  package_name: string;
  app_name: string;
  category: AppCategory;
  total_minutes: number;
  device_count: number;
}

export interface DeviceAppUsage {
  device_id: string;
  device_name: string;
  apps: {
    package_name: string;
    app_name: string;
    category: AppCategory;
    usage_minutes: number;
  }[];
  total_minutes: number;
}

export interface AppUsageParams {
  device_id?: string;
  organization_id?: string;
  from?: string;
  to?: string;
  category?: AppCategory;
}

// Story AP-8.2: App Limits
export interface TimeWindow {
  start_time: string; // HH:MM format (24h)
  end_time: string;
  days?: ("mon" | "tue" | "wed" | "thu" | "fri" | "sat" | "sun")[];
  days_of_week?: number[]; // 0-6 (Sunday-Saturday) - for auto-approval rules
}

export interface AppLimit {
  id: string;
  name: string;
  target_type: "app" | "category";
  target_value: string; // package_name or category
  target_display: string; // app name or category name
  limit_type: "time" | "blocked";
  daily_limit_minutes: number | null;
  weekly_limit_minutes: number | null;
  time_windows: TimeWindow[];
  device_id: string | null;
  device_name: string | null;
  group_id: string | null;
  group_name: string | null;
  organization_id: string;
  organization_name: string;
  enabled: boolean;
  created_at: string;
  updated_at: string;
}

export interface CreateAppLimitRequest {
  name: string;
  target_type: "app" | "category";
  target_value: string;
  limit_type: "time" | "blocked";
  daily_limit_minutes?: number;
  weekly_limit_minutes?: number;
  time_windows?: TimeWindow[];
  device_id?: string;
  group_id?: string;
}

export interface AppLimitListParams {
  device_id?: string;
  group_id?: string;
  organization_id?: string;
  target_type?: "app" | "category";
  enabled?: boolean;
  page?: number;
  limit?: number;
}

// Story AP-8.3: Limit Templates
export interface LimitTemplate {
  id: string;
  name: string;
  description: string | null;
  organization_id: string;
  organization_name: string;
  rules: {
    target_type: "app" | "category";
    target_value: string;
    target_display: string;
    limit_type: "time" | "blocked";
    daily_limit_minutes: number | null;
    weekly_limit_minutes: number | null;
    time_windows: TimeWindow[];
  }[];
  linked_device_count: number;
  linked_group_count: number;
  created_at: string;
  updated_at: string;
}

export interface CreateLimitTemplateRequest {
  name: string;
  description?: string;
  rules: {
    target_type: "app" | "category";
    target_value: string;
    limit_type: "time" | "blocked";
    daily_limit_minutes?: number;
    weekly_limit_minutes?: number;
    time_windows?: TimeWindow[];
  }[];
}

// Story AP-8.4: Unlock Requests (Admin)
export type AdminUnlockRequestStatus = "pending" | "approved" | "denied" | "expired" | "cancelled";

export interface AdminUnlockRequest {
  id: string;
  device_id: string;
  device_name: string;
  user_id: string;
  user_name: string;
  user_email: string;
  organization_id: string;
  organization_name: string;
  reason: string;
  requested_duration_minutes: number;
  status: AdminUnlockRequestStatus;
  approved_duration_minutes: number | null;
  deny_note: string | null;
  actioned_by: string | null;
  actioned_by_name: string | null;
  actioned_at: string | null;
  expires_at: string | null;
  auto_approved: boolean;
  auto_approval_rule_id: string | null;
  created_at: string;
}

export interface UnlockRequestListParams {
  device_id?: string;
  organization_id?: string;
  status?: AdminUnlockRequestStatus;
  from?: string;
  to?: string;
  page?: number;
  limit?: number;
}

export interface ApproveUnlockRequest {
  duration_minutes: number;
}

export interface DenyUnlockRequest {
  note: string;
}

// Story AP-8.5: Auto-Approval Rules
export interface AutoApprovalConditions {
  time_window?: TimeWindow;
  user_ids?: string[];
  device_ids?: string[];
  group_ids?: string[];
  max_daily_requests?: number;
}

export interface AutoApprovalRule {
  id: string;
  name: string;
  description: string | null;
  organization_id: string;
  organization_name: string;
  priority: number;
  conditions: AutoApprovalConditions;
  max_duration_minutes: number;
  enabled: boolean;
  approval_count: number;
  last_triggered_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface CreateAutoApprovalRuleRequest {
  name: string;
  description?: string;
  organization_id: string;
  conditions: AutoApprovalConditions;
  max_duration_minutes: number;
  enabled?: boolean;
}

export interface AutoApprovalLogEntry {
  id: string;
  request_id: string;
  rule_id: string;
  rule_name: string;
  organization_id: string;
  device_id: string;
  device_name: string;
  user_id: string;
  user_name: string;
  requested_duration_minutes: number;
  approved_duration_minutes: number;
  approved_at: string;
}

// ==============================================
// Epic AP-9: System Configuration
// ==============================================

// Story AP-9.1: Authentication Settings
export type RegistrationMode = "open" | "invite_only" | "oauth_only" | "disabled";

export interface OAuthProviderConfig {
  provider: "google" | "apple";
  enabled: boolean;
  client_id: string;
  client_secret_set: boolean; // True if secret is configured (not shown)
  allowed_domains?: string[]; // Optional domain restrictions
}

export interface AuthConfig {
  registration_mode: RegistrationMode;
  oauth_providers: OAuthProviderConfig[];
  session_timeout_minutes: number;
  max_login_attempts: number;
  lockout_duration_minutes: number;
  require_mfa: boolean;
  password_min_length: number;
  password_require_special: boolean;
  updated_at: string;
  updated_by: string | null;
}

export interface UpdateAuthConfigRequest {
  registration_mode?: RegistrationMode;
  session_timeout_minutes?: number;
  max_login_attempts?: number;
  lockout_duration_minutes?: number;
  require_mfa?: boolean;
  password_min_length?: number;
  password_require_special?: boolean;
}

export interface UpdateOAuthProviderRequest {
  enabled?: boolean;
  client_id?: string;
  client_secret?: string;
  allowed_domains?: string[];
}

// Story AP-9.2: Feature Flags
export interface FeatureFlag {
  id: string;
  name: string;
  key: string;
  description: string;
  enabled: boolean;
  dependencies?: string[]; // Other feature keys this depends on
  dependents?: string[]; // Features that depend on this
  category: "core" | "tracking" | "communication" | "analytics";
  updated_at: string;
  updated_by: string | null;
}

// Story AP-9.3: Rate Limits
export interface RateLimitConfig {
  id: string;
  endpoint_category: string;
  description: string;
  requests_per_minute: number;
  requests_per_hour: number;
  requests_per_day: number;
  enabled: boolean;
}

export interface RateLimitOverride {
  id: string;
  organization_id: string;
  organization_name: string;
  endpoint_category: string;
  requests_per_minute: number;
  requests_per_hour: number;
  requests_per_day: number;
  reason: string;
  created_at: string;
  created_by: string;
}

export interface CreateRateLimitOverrideRequest {
  organization_id: string;
  endpoint_category: string;
  requests_per_minute: number;
  requests_per_hour: number;
  requests_per_day: number;
  reason: string;
}

export interface RateLimitMetrics {
  endpoint_category: string;
  total_requests_today: number;
  rate_limited_requests_today: number;
  peak_requests_per_minute: number;
  last_rate_limited_at: string | null;
}

// Story AP-9.4: API Keys
export type ApiKeyPermission = "read" | "write" | "admin";
export type ApiKeyScope = "devices" | "locations" | "users" | "organizations" | "webhooks" | "all";

export interface ApiKeyPermissionSet {
  scope: ApiKeyScope;
  permission: ApiKeyPermission;
}

export interface ApiKey {
  id: string;
  name: string;
  description: string | null;
  key_prefix: string; // First 8 chars of key for identification
  permissions: ApiKeyPermissionSet[];
  rate_limit_per_minute: number | null;
  expires_at: string | null;
  last_used_at: string | null;
  total_requests: number;
  created_at: string;
  created_by: string;
  is_active: boolean;
}

export interface CreateApiKeyRequest {
  name: string;
  description?: string;
  permissions: ApiKeyPermissionSet[];
  rate_limit_per_minute?: number;
  expires_at?: string;
}

export interface CreateApiKeyResponse {
  api_key: ApiKey;
  secret_key: string; // Only shown once at creation
}

export interface ApiKeyUsageStats {
  total_requests: number;
  requests_last_24h: number;
  requests_last_7d: number;
  error_count_last_24h: number;
  last_used_at: string | null;
  most_used_endpoints: { endpoint: string; count: number }[];
}

// Story AP-9.5: Data Retention
export type RetentionPeriod = "30d" | "60d" | "90d" | "180d" | "365d" | "unlimited";

export interface DataTypeRetention {
  data_type: "locations" | "audit_logs" | "trips" | "alerts" | "device_events";
  retention_period: RetentionPeriod;
  description: string;
}

export interface RetentionConfig {
  default_retention_period: RetentionPeriod;
  data_types: DataTypeRetention[];
  inactive_device_threshold_days: number;
  auto_cleanup_enabled: boolean;
  last_cleanup_at: string | null;
  next_cleanup_at: string | null;
  updated_at: string;
  updated_by: string | null;
}

export interface UpdateRetentionConfigRequest {
  default_retention_period?: RetentionPeriod;
  data_types?: { data_type: string; retention_period: RetentionPeriod }[];
  inactive_device_threshold_days?: number;
  auto_cleanup_enabled?: boolean;
}

export interface RetentionStats {
  data_type: string;
  total_records: number;
  records_to_delete: number;
  oldest_record_date: string | null;
  storage_used_mb: number;
}

// ============================================
// Epic AP-10: Dashboard & Analytics Types
// ============================================

// Story AP-10.1: Overview Dashboard
export interface DashboardMetrics {
  users: {
    total: number;
    active: number;
    new_today: number;
    active_today: number;
  };
  devices: {
    total: number;
    online: number;
    offline: number;
    new_today: number;
  };
  organizations: {
    total: number;
    active: number;
    new_today: number;
  };
  groups: {
    total: number;
    active: number;
    new_today: number;
  };
}

export interface AlertIndicators {
  pending_unlock_requests: number;
  pending_registrations: number;
  failed_webhooks: number;
  system_alerts: number;
  expiring_api_keys: number;
}

export interface QuickAction {
  id: string;
  label: string;
  icon: string;
  href: string;
  badge?: number;
}

// Story AP-10.2: User Analytics
export interface UserGrowthData {
  date: string;
  total_users: number;
  new_users: number;
  active_users: number;
}

export interface UserRetentionData {
  cohort_date: string;
  users: number;
  day_1: number;
  day_7: number;
  day_14: number;
  day_30: number;
}

export interface UserSegmentData {
  segment: "new" | "returning" | "inactive";
  count: number;
  percentage: number;
}

export interface UserAnalytics {
  growth: UserGrowthData[];
  retention: UserRetentionData[];
  segments: UserSegmentData[];
  total_users: number;
  active_users: number;
  churn_rate: number;
}

// Story AP-10.3: Device Analytics
export interface DeviceDistribution {
  platform: string;
  count: number;
  percentage: number;
}

export interface DeviceStatusDistribution {
  status: string;
  count: number;
  percentage: number;
}

export interface DeviceConnectivityData {
  date: string;
  online: number;
  offline: number;
}

export interface LocationVolumeData {
  date: string;
  uploads: number;
  data_points: number;
}

export interface DeviceActivityHeatmap {
  hour: number;
  day: number;
  activity: number;
}

export interface DeviceAnalytics {
  platform_distribution: DeviceDistribution[];
  status_distribution: DeviceStatusDistribution[];
  connectivity: DeviceConnectivityData[];
  location_volume: LocationVolumeData[];
  heatmap: DeviceActivityHeatmap[];
  total_devices: number;
  online_devices: number;
}

// Story AP-10.4: API Analytics
export interface EndpointMetrics {
  endpoint: string;
  method: string;
  request_count: number;
  avg_response_time_ms: number;
  error_count: number;
  error_rate: number;
}

export interface ResponseTimeData {
  date: string;
  p50: number;
  p90: number;
  p95: number;
  p99: number;
}

export interface ErrorRateData {
  date: string;
  total_requests: number;
  errors: number;
  error_rate: number;
}

export interface ApiConsumer {
  id: string;
  name: string;
  type: "api_key" | "user" | "organization";
  request_count: number;
  last_request: string;
}

export interface ApiAnalytics {
  endpoints: EndpointMetrics[];
  response_times: ResponseTimeData[];
  error_rates: ErrorRateData[];
  top_consumers: ApiConsumer[];
  total_requests: number;
  avg_response_time_ms: number;
  overall_error_rate: number;
}

// Story AP-10.5: Custom Reports
export type ReportMetricType =
  | "users"
  | "devices"
  | "organizations"
  | "locations"
  | "api_calls"
  | "errors"
  | "retention";

export interface ReportMetric {
  type: ReportMetricType;
  aggregation: "count" | "sum" | "average" | "min" | "max";
  label: string;
}

export interface ReportFilter {
  field: string;
  operator: "eq" | "ne" | "gt" | "lt" | "gte" | "lte" | "in" | "contains";
  value: string | number | string[];
}

export interface ReportConfig {
  id: string;
  name: string;
  description?: string;
  metrics: ReportMetric[];
  filters: ReportFilter[];
  date_range: {
    start: string;
    end: string;
  };
  group_by?: string;
  created_at: string;
  updated_at: string;
  created_by: string;
}

export interface ReportResult {
  config: ReportConfig;
  data: Record<string, unknown>[];
  generated_at: string;
  row_count: number;
}

export interface SavedReport {
  id: string;
  name: string;
  description?: string;
  config: Omit<ReportConfig, "id" | "created_at" | "updated_at" | "created_by">;
  created_at: string;
  updated_at: string;
  last_run?: string;
}

// ============================================
// Epic AP-11: Audit & Compliance Types
// ============================================

export type AuditActionType =
  | "create"
  | "update"
  | "delete"
  | "login"
  | "logout"
  | "view"
  | "export"
  | "import"
  | "approve"
  | "reject"
  | "suspend"
  | "restore"
  | "archive";

export type AuditResourceType =
  | "user"
  | "organization"
  | "device"
  | "group"
  | "role"
  | "permission"
  | "location"
  | "geofence"
  | "webhook"
  | "trip"
  | "app_restriction"
  | "unlock_request"
  | "config"
  | "report"
  | "audit_log";

export interface AuditLogEntry {
  id: string;
  actor_id: string;
  actor_email: string;
  actor_name: string;
  action: AuditActionType;
  resource_type: AuditResourceType;
  resource_id: string;
  resource_name?: string;
  organization_id?: string;
  organization_name?: string;
  ip_address: string;
  user_agent: string;
  before_state?: Record<string, unknown>;
  after_state?: Record<string, unknown>;
  metadata?: Record<string, unknown>;
  timestamp: string;
  hash?: string; // For tamper-evident chain
  previous_hash?: string;
}

export interface AuditLogFilter {
  actor_id?: string;
  action?: AuditActionType;
  resource_type?: AuditResourceType;
  resource_id?: string;
  organization_id?: string;
  date_from?: string;
  date_to?: string;
  search?: string;
}

export interface AuditLogStats {
  total_entries: number;
  entries_by_action: Record<AuditActionType, number>;
  entries_by_resource: Record<AuditResourceType, number>;
  top_actors: Array<{
    actor_id: string;
    actor_name: string;
    action_count: number;
  }>;
}

export interface UserActivityReport {
  user_id: string;
  user_name: string;
  user_email: string;
  period_start: string;
  period_end: string;
  total_actions: number;
  actions_by_type: Record<AuditActionType, number>;
  actions_by_resource: Record<AuditResourceType, number>;
  timeline: Array<{
    date: string;
    action: AuditActionType;
    resource_type: AuditResourceType;
    resource_name?: string;
    timestamp: string;
  }>;
}

export interface OrgActivityReport {
  organization_id: string;
  organization_name: string;
  period_start: string;
  period_end: string;
  total_actions: number;
  user_action_counts: Array<{
    user_id: string;
    user_name: string;
    action_count: number;
  }>;
  resource_changes: Array<{
    resource_type: AuditResourceType;
    created: number;
    updated: number;
    deleted: number;
  }>;
  anomalies: Array<{
    type: string;
    description: string;
    severity: "low" | "medium" | "high";
    timestamp: string;
  }>;
}

export interface GDPRDataExportRequest {
  id: string;
  user_id: string;
  user_email: string;
  requested_by: string;
  status: "pending" | "processing" | "completed" | "failed";
  data_types: string[];
  download_url?: string;
  expires_at?: string;
  created_at: string;
  completed_at?: string;
}

export interface GDPRDeletionRequest {
  id: string;
  user_id: string;
  user_email: string;
  requested_by: string;
  status: "pending" | "processing" | "completed" | "failed";
  data_types_deleted: string[];
  verification_report?: {
    deleted_at: string;
    deleted_by: string;
    categories: Array<{
      name: string;
      count: number;
      status: "deleted" | "retained" | "anonymized";
    }>;
  };
  created_at: string;
  completed_at?: string;
}

export interface AuditIntegrityStatus {
  status: "healthy" | "warning" | "error";
  last_verified: string;
  chain_length: number;
  broken_links: number;
  alerts: Array<{
    id: string;
    type: "tampering" | "gap" | "corruption";
    description: string;
    severity: "low" | "medium" | "high" | "critical";
    detected_at: string;
    resolved?: boolean;
  }>;
  retention_policy: {
    days: number;
    auto_archive: boolean;
    archive_location?: string;
  };
  storage_usage: {
    total_entries: number;
    size_bytes: number;
    oldest_entry: string;
  };
}

// ============================================
// Epic AP-12: Notification Types
// ============================================

export type NotificationType =
  | "info"
  | "success"
  | "warning"
  | "error"
  | "system";

export type NotificationCategory =
  | "user"
  | "organization"
  | "device"
  | "security"
  | "system"
  | "audit";

export interface Notification {
  id: string;
  type: NotificationType;
  category: NotificationCategory;
  title: string;
  message: string;
  link?: string;
  read: boolean;
  created_at: string;
  expires_at?: string;
}

export interface NotificationPreferences {
  email_enabled: boolean;
  email_digest: "realtime" | "daily" | "weekly" | "never";
  in_app_enabled: boolean;
  categories: Record<NotificationCategory, boolean>;
}
