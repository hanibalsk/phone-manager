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
  days: ("mon" | "tue" | "wed" | "thu" | "fri" | "sat" | "sun")[];
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
export interface AutoApprovalCondition {
  type: "time_window" | "user" | "device" | "group";
  time_window?: TimeWindow;
  user_ids?: string[];
  device_ids?: string[];
  group_ids?: string[];
}

export interface AutoApprovalRule {
  id: string;
  name: string;
  description: string | null;
  organization_id: string;
  organization_name: string;
  priority: number;
  conditions: AutoApprovalCondition[];
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
  conditions: AutoApprovalCondition[];
  max_duration_minutes: number;
}

export interface AutoApprovalLogEntry {
  id: string;
  request_id: string;
  rule_id: string;
  rule_name: string;
  device_id: string;
  device_name: string;
  user_id: string;
  user_name: string;
  approved_duration_minutes: number;
  triggered_at: string;
}
