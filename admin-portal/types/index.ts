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
