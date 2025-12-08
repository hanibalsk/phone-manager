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
