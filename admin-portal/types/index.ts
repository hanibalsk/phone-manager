// Common types for the admin portal

export interface Device {
  id: string;
  name: string;
  androidId: string;
  enrolledAt: string;
  lastSeen: string;
  status: "active" | "inactive" | "pending";
}

export interface UnlockRequest {
  id: string;
  deviceId: string;
  deviceName: string;
  reason: string;
  requestedAt: string;
  status: "pending" | "approved" | "denied";
  requestedDuration: number;
  adminResponse?: string;
}

export interface AppUsage {
  packageName: string;
  appName: string;
  usageTimeMinutes: number;
  lastUsed: string;
}

export interface DailyLimit {
  id: string;
  packageName: string;
  appName: string;
  dailyLimitMinutes: number;
  enabled: boolean;
}

export interface AdminSettings {
  unlockPin: string;
  defaultDailyLimitMinutes: number;
  notificationsEnabled: boolean;
  autoApproveUnlockRequests: boolean;
}

export interface ApiResponse<T> {
  data?: T;
  error?: string;
  status: number;
}
