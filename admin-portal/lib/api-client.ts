import type {
  ApiResponse,
  Device,
  UnlockRequest,
  AppUsage,
  DailyLimit,
  AdminSettings,
  PublicConfig,
  AdminUser,
  UserListParams,
  PaginatedResponse,
  CreateUserRequest,
  UserSession,
  MfaStatus,
  Organization,
  OrganizationListParams,
  CreateOrganizationRequest,
  UpdateOrganizationRequest,
  Role,
  Permission,
  CreateRoleRequest,
  UpdateRoleRequest,
  UserRoleAssignment,
  AdminDevice,
  DeviceListParams,
  DeviceDetails,
  BulkOperationResult,
  EnrollmentToken,
  CreateEnrollmentTokenRequest,
  TokenUsage,
  InactiveDevice,
  NotifyOwnersResult,
  AdminGroup,
  GroupListParams,
  GroupMember,
  GroupMemberRole,
  GroupInvite,
  // Epic AP-6: Location & Geofence
  DeviceLocation,
  LocationFilter,
  LatestDeviceLocation,
  Geofence,
  GeofenceListParams,
  CreateGeofenceRequest,
  GeofenceEvent,
  GeofenceEventFilter,
  ProximityAlert,
  ProximityAlertTrigger,
  CreateProximityAlertRequest,
  ProximityAlertListParams,
  RetentionPolicy,
  UpdateRetentionPolicyRequest,
  PurgeResult,
  // Epic AP-7: Webhooks & Trips
  Webhook,
  WebhookListParams,
  CreateWebhookRequest,
  UpdateWebhookRequest,
  WebhookDelivery,
  WebhookDeliveryListParams,
  WebhookTestResult,
  WebhookEventType,
  Trip,
  TripPoint,
  TripEvent,
  TripListParams,
  ExportJob,
  ExportJobStatus,
  CreateExportJobRequest,
  // Epic AP-8: App Usage & Unlock Requests
  AdminAppUsage,
  AppUsageByCategory,
  TopApp,
  DeviceAppUsage,
  AppUsageParams,
  AppLimit,
  CreateAppLimitRequest,
  AppLimitListParams,
  LimitTemplate,
  CreateLimitTemplateRequest,
  AdminUnlockRequest,
  UnlockRequestListParams,
  ApproveUnlockRequest,
  DenyUnlockRequest,
  AutoApprovalRule,
  CreateAutoApprovalRuleRequest,
  AutoApprovalLogEntry,
  AutoApprovalConditions,
  // Epic AP-9: System Configuration
  AuthConfig,
  UpdateAuthConfigRequest,
  OAuthProviderConfig,
  UpdateOAuthProviderRequest,
  FeatureFlag,
  RateLimitConfig,
  RateLimitOverride,
  CreateRateLimitOverrideRequest,
  RateLimitMetrics,
  ApiKey,
  CreateApiKeyRequest,
  CreateApiKeyResponse,
  ApiKeyUsageStats,
  RetentionConfig,
  UpdateRetentionConfigRequest,
  RetentionStats,
  // Epic AP-10: Dashboard & Analytics
  DashboardMetrics,
  AlertIndicators,
  UserAnalytics,
  UserGrowthData,
  UserRetentionData,
  UserSegmentData,
  DeviceAnalytics,
  DeviceDistribution,
  DeviceConnectivityData,
  LocationVolumeData,
  DeviceActivityHeatmap,
  ApiAnalytics,
  EndpointMetrics,
  ResponseTimeData,
  ErrorRateData,
  ApiConsumer,
  ReportConfig,
  ReportResult,
  SavedReport,
  // Epic AP-11: Audit & Compliance
  AuditLogEntry,
  AuditLogFilter,
  AuditLogStats,
  UserActivityReport,
  OrgActivityReport,
  GDPRDataExportRequest,
  GDPRDeletionRequest,
  AuditIntegrityStatus,
  // Epic AP-12: Notifications
  Notification,
  NotificationPreferences,
} from "@/types";
import type {
  LoginResponse,
  RefreshResponse,
  LoginCredentials,
  RegisterCredentials,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  User,
} from "@/types/auth";
import { env } from "./env";
import { getAccessToken } from "@/contexts/auth-context";
import { isLocalStorageMode } from "./auth-mode";

const API_BASE_URL = env.NEXT_PUBLIC_API_URL;

async function request<T>(
  endpoint: string,
  options: RequestInit = {},
  includeAuth = true
): Promise<ApiResponse<T>> {
  const url = `${API_BASE_URL}${endpoint}`;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };

  // Add auth header if we have a token and auth is requested
  // Only add header in localStorage mode; httpOnly mode uses cookies automatically
  if (includeAuth && isLocalStorageMode()) {
    const token = getAccessToken();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
      credentials: "include", // Always include cookies for httpOnly mode support
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return {
        status: response.status,
        error: errorData.message || `HTTP error ${response.status}`,
      };
    }

    const data = await response.json();
    return { data, status: response.status };
  } catch (error) {
    return {
      status: 0,
      error: error instanceof Error ? error.message : "Network error",
    };
  }
}

// Blob request helper for binary downloads (reports, exports, etc.)
async function requestBlob(
  endpoint: string,
  options: RequestInit = {},
  includeAuth = true
): Promise<ApiResponse<Blob>> {
  const url = `${API_BASE_URL}${endpoint}`;

  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };

  // Add auth header if we have a token and auth is requested
  // Only add header in localStorage mode; httpOnly mode uses cookies automatically
  if (includeAuth && isLocalStorageMode()) {
    const token = getAccessToken();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
      credentials: "include", // Always include cookies for httpOnly mode support
    });

    if (!response.ok) {
      // Try to parse error as JSON, fallback to status text
      const errorData = await response.json().catch(() => ({}));
      return {
        status: response.status,
        error: errorData.message || `HTTP error ${response.status}`,
      };
    }

    const data = await response.blob();
    return { data, status: response.status };
  } catch (error) {
    return {
      status: 0,
      error: error instanceof Error ? error.message : "Network error",
    };
  }
}

// Auth API (no auth header required for most endpoints)
export const authApi = {
  login: (credentials: LoginCredentials) =>
    request<LoginResponse>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(credentials),
    }, false),

  register: (credentials: RegisterCredentials) =>
    request<LoginResponse>("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify(credentials),
    }, false),

  refresh: (refreshToken?: string) =>
    request<RefreshResponse>("/api/v1/auth/refresh", {
      method: "POST",
      // In localStorage mode, pass refresh token in body
      // In httpOnly mode, backend reads from cookie (no body needed)
      body: isLocalStorageMode() && refreshToken
        ? JSON.stringify({ refresh_token: refreshToken })
        : undefined,
    }, false),

  getCurrentUser: () =>
    request<User>("/api/v1/auth/me", {
      method: "GET",
    }, true),

  logout: () =>
    request<void>("/api/v1/auth/logout", {
      method: "POST",
    }, true),

  forgotPassword: (data: ForgotPasswordRequest) =>
    request<void>("/api/v1/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify(data),
    }, false),

  resetPassword: (data: ResetPasswordRequest) =>
    request<void>("/api/v1/auth/reset-password", {
      method: "POST",
      body: JSON.stringify(data),
    }, false),
};

// Device Management (Legacy - simple list)
export const deviceApi = {
  list: () => request<Device[]>("/api/admin/v1/devices"),

  get: (id: string) => request<Device>(`/api/admin/v1/devices/${id}`),

  getUsage: (id: string, date?: string) => {
    const params = date ? `?date=${date}` : "";
    return request<AppUsage[]>(`/api/admin/v1/devices/${id}/usage${params}`);
  },
};

// Epic AP-4: Admin Device Fleet Management
export const adminDevicesApi = {
  // Story AP-4.1: List devices with filtering
  list: (params?: DeviceListParams) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
      if (params.search) searchParams.set("search", params.search);
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.group_id) searchParams.set("group_id", params.group_id);
      if (params.status) searchParams.set("status", params.status);
      if (params.platform) searchParams.set("platform", params.platform);
      if (params.sort_by) searchParams.set("sort_by", params.sort_by);
      if (params.sort_order) searchParams.set("sort_order", params.sort_order);
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/devices/fleet${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<AdminDevice>>(endpoint);
  },

  // Story AP-4.2: Get device details
  get: (id: string) => request<DeviceDetails>(`/api/admin/v1/devices/fleet/${id}`),

  // Story AP-4.2: Suspend device
  suspend: (id: string, reason?: string) =>
    request<DeviceDetails>(`/api/admin/v1/devices/fleet/${id}/suspend`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    }),

  // Story AP-4.2: Reactivate device
  reactivate: (id: string) =>
    request<DeviceDetails>(`/api/admin/v1/devices/fleet/${id}/reactivate`, {
      method: "POST",
    }),

  // Story AP-4.2: Delete device
  delete: (id: string) =>
    request<{ success: boolean }>(`/api/admin/v1/devices/fleet/${id}`, {
      method: "DELETE",
    }),

  // Story AP-4.4: Bulk suspend
  bulkSuspend: (deviceIds: string[]) =>
    request<BulkOperationResult>("/api/admin/v1/devices/fleet/bulk/suspend", {
      method: "POST",
      body: JSON.stringify({ device_ids: deviceIds }),
    }),

  // Story AP-4.4: Bulk reactivate
  bulkReactivate: (deviceIds: string[]) =>
    request<BulkOperationResult>("/api/admin/v1/devices/fleet/bulk/reactivate", {
      method: "POST",
      body: JSON.stringify({ device_ids: deviceIds }),
    }),

  // Story AP-4.4: Bulk delete
  bulkDelete: (deviceIds: string[]) =>
    request<BulkOperationResult>("/api/admin/v1/devices/fleet/bulk/delete", {
      method: "POST",
      body: JSON.stringify({ device_ids: deviceIds }),
    }),

  // Story AP-4.5: Get inactive devices
  getInactive: (params?: { days?: number; page?: number; limit?: number }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.days) searchParams.set("days", String(params.days));
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/devices/fleet/inactive${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<InactiveDevice>>(endpoint);
  },

  // Story AP-4.5: Send notification to device owners
  notifyOwners: (deviceIds: string[], messageTemplate?: string) =>
    request<NotifyOwnersResult>("/api/admin/v1/devices/fleet/notify", {
      method: "POST",
      body: JSON.stringify({ device_ids: deviceIds, message_template: messageTemplate }),
    }),
};

// Story AP-4.3: Enrollment Token Management
export const enrollmentApi = {
  // List all tokens
  list: (params?: { page?: number; limit?: number }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/enrollment/tokens${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<EnrollmentToken>>(endpoint);
  },

  // Create new token
  create: (data: CreateEnrollmentTokenRequest) =>
    request<EnrollmentToken>("/api/admin/v1/enrollment/tokens", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Get token details
  get: (id: string) => request<EnrollmentToken>(`/api/admin/v1/enrollment/tokens/${id}`),

  // Get token usage history
  getUsage: (id: string) =>
    request<{ enrollments: TokenUsage[] }>(`/api/admin/v1/enrollment/tokens/${id}/usage`),

  // Revoke token
  revoke: (id: string) =>
    request<{ success: boolean }>(`/api/admin/v1/enrollment/tokens/${id}`, {
      method: "DELETE",
    }),
};

// User Administration (Story AP-3.1)
export const usersApi = {
  list: (params?: UserListParams) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
      if (params.search) searchParams.set("search", params.search);
      if (params.status) searchParams.set("status", params.status);
      if (params.role) searchParams.set("role", params.role);
      if (params.sort_by) searchParams.set("sort_by", params.sort_by);
      if (params.sort_order) searchParams.set("sort_order", params.sort_order);
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/users${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<AdminUser>>(endpoint);
  },

  get: (id: string) => request<AdminUser>(`/api/admin/v1/users/${id}`),

  create: (data: CreateUserRequest) =>
    request<AdminUser>("/api/admin/v1/users", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  suspend: (id: string, reason: string) =>
    request<AdminUser>(`/api/admin/v1/users/${id}/suspend`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    }),

  reactivate: (id: string) =>
    request<AdminUser>(`/api/admin/v1/users/${id}/reactivate`, {
      method: "POST",
    }),

  // Story AP-3.4: Password Reset
  resetPassword: (id: string, forceChange: boolean = true) =>
    request<{ success: boolean }>(`/api/admin/v1/users/${id}/reset-password`, {
      method: "POST",
      body: JSON.stringify({ force_change_on_login: forceChange }),
    }),

  // Story AP-3.5: Session Management
  getSessions: (id: string) =>
    request<UserSession[]>(`/api/admin/v1/users/${id}/sessions`),

  revokeSession: (userId: string, sessionId: string) =>
    request<{ success: boolean }>(`/api/admin/v1/users/${userId}/sessions/${sessionId}`, {
      method: "DELETE",
    }),

  revokeAllSessions: (id: string) =>
    request<{ success: boolean }>(`/api/admin/v1/users/${id}/sessions`, {
      method: "DELETE",
    }),

  // Story AP-3.6: MFA Management
  getMfaStatus: (id: string) =>
    request<MfaStatus>(`/api/admin/v1/users/${id}/mfa`),

  forceMfaEnrollment: (id: string) =>
    request<{ success: boolean }>(`/api/admin/v1/users/${id}/mfa/force`, {
      method: "POST",
    }),

  resetMfa: (id: string) =>
    request<{ success: boolean }>(`/api/admin/v1/users/${id}/mfa`, {
      method: "DELETE",
    }),
};

// Unlock Requests
export const unlockApi = {
  list: (status?: string) => {
    const params = status ? `?status=${status}` : "";
    return request<UnlockRequest[]>(`/api/admin/v1/unlock-requests${params}`);
  },

  approve: (id: string, response?: string) =>
    request<UnlockRequest>(`/api/admin/v1/unlock-requests/${id}/approve`, {
      method: "POST",
      body: JSON.stringify({ response }),
    }),

  deny: (id: string, response?: string) =>
    request<UnlockRequest>(`/api/admin/v1/unlock-requests/${id}/deny`, {
      method: "POST",
      body: JSON.stringify({ response }),
    }),
};

// App Limits
export const limitsApi = {
  list: (deviceId: string) =>
    request<DailyLimit[]>(`/api/admin/v1/devices/${deviceId}/limits`),

  set: (deviceId: string, limit: Omit<DailyLimit, "id">) =>
    request<DailyLimit>(`/api/admin/v1/devices/${deviceId}/limits`, {
      method: "POST",
      body: JSON.stringify(limit),
    }),

  update: (deviceId: string, limitId: string, limit: Partial<DailyLimit>) =>
    request<DailyLimit>(`/api/admin/v1/devices/${deviceId}/limits/${limitId}`, {
      method: "PUT",
      body: JSON.stringify(limit),
    }),

  delete: (deviceId: string, limitId: string) =>
    request<void>(`/api/admin/v1/devices/${deviceId}/limits/${limitId}`, {
      method: "DELETE",
    }),
};

// Admin Settings
export const settingsApi = {
  get: () => request<AdminSettings>("/api/admin/v1/settings"),

  update: (settings: Partial<AdminSettings>) =>
    request<AdminSettings>("/api/admin/v1/settings", {
      method: "PUT",
      body: JSON.stringify(settings),
    }),
};

// Health Check
export const healthApi = {
  check: () => request<{ status: string }>("/api/health"),
};

// Organization Management (Epic AP-2)
export const organizationsApi = {
  list: (params?: OrganizationListParams) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
      if (params.search) searchParams.set("search", params.search);
      if (params.status) searchParams.set("status", params.status);
      if (params.type) searchParams.set("type", params.type);
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/organizations${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<Organization>>(endpoint);
  },

  get: (id: string) => request<Organization>(`/api/admin/v1/organizations/${id}`),

  create: (data: CreateOrganizationRequest) =>
    request<Organization>("/api/admin/v1/organizations", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  update: (id: string, data: UpdateOrganizationRequest) =>
    request<Organization>(`/api/admin/v1/organizations/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Story AP-2.2: Update organization limits
  updateLimits: (id: string, limits: { max_devices?: number; max_users?: number; max_groups?: number }) =>
    request<Organization>(`/api/admin/v1/organizations/${id}/limits`, {
      method: "PUT",
      body: JSON.stringify(limits),
    }),

  // Story AP-2.3: Status management
  suspend: (id: string, reason: string) =>
    request<Organization>(`/api/admin/v1/organizations/${id}/suspend`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    }),

  reactivate: (id: string) =>
    request<Organization>(`/api/admin/v1/organizations/${id}/reactivate`, {
      method: "POST",
    }),

  archive: (id: string) =>
    request<Organization>(`/api/admin/v1/organizations/${id}/archive`, {
      method: "POST",
    }),

  // Story AP-2.4: Feature flags
  updateFeatures: (id: string, features: Partial<Organization["features"]>) =>
    request<Organization>(`/api/admin/v1/organizations/${id}/features`, {
      method: "PUT",
      body: JSON.stringify(features),
    }),

  // Story AP-2.5: Statistics
  getStats: (id: string) =>
    request<OrganizationStats>(`/api/admin/v1/organizations/${id}/stats`),
};

export interface OrganizationStats {
  users_count: number;
  devices_count: number;
  groups_count: number;
  storage_used_mb: number;
  usage_trends: {
    period: string;
    users: number;
    devices: number;
  }[];
}

// Public Configuration (feature flags & auth config)
export const configApi = {
  getPublic: () => request<PublicConfig>("/api/v1/config/public", {}, false),
};

// Epic AP-1: Roles & Permissions Management
export const rolesApi = {
  // Story AP-1.1: List all roles
  list: () => request<Role[]>("/api/admin/v1/roles"),

  // Story AP-1.1: Get role details
  get: (id: string) => request<Role>(`/api/admin/v1/roles/${id}`),

  // Story AP-1.3: Create custom role
  create: (data: CreateRoleRequest) =>
    request<Role>("/api/admin/v1/roles", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Story AP-1.3: Update role
  update: (id: string, data: UpdateRoleRequest) =>
    request<Role>(`/api/admin/v1/roles/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Story AP-1.3: Delete custom role
  delete: (id: string) =>
    request<void>(`/api/admin/v1/roles/${id}`, {
      method: "DELETE",
    }),

  // Story AP-1.1: List all permissions
  listPermissions: () => request<Permission[]>("/api/admin/v1/permissions"),

  // Story AP-1.2: Get user role assignments
  getUserAssignments: (userId: string) =>
    request<UserRoleAssignment[]>(`/api/admin/v1/users/${userId}/roles`),

  // Story AP-1.2: Assign role to user
  assignRole: (userId: string, roleId: string, organizationId?: string) =>
    request<UserRoleAssignment>(`/api/admin/v1/users/${userId}/roles`, {
      method: "POST",
      body: JSON.stringify({ role_id: roleId, organization_id: organizationId }),
    }),

  // Story AP-1.2: Remove role from user
  removeRole: (userId: string, assignmentId: string) =>
    request<void>(`/api/admin/v1/users/${userId}/roles/${assignmentId}`, {
      method: "DELETE",
    }),
};

// Epic AP-5: Groups Administration
export const adminGroupsApi = {
  // Story AP-5.1: List groups with filtering
  list: (params?: GroupListParams) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
      if (params.search) searchParams.set("search", params.search);
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.status) searchParams.set("status", params.status);
      if (params.sort_by) searchParams.set("sort_by", params.sort_by);
      if (params.sort_order) searchParams.set("sort_order", params.sort_order);
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/groups${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<AdminGroup>>(endpoint);
  },

  // Story AP-5.1: Get group details
  get: (id: string) => request<AdminGroup>(`/api/admin/v1/groups/${id}`),

  // Story AP-5.1: Suspend group
  suspend: (id: string, reason?: string) =>
    request<AdminGroup>(`/api/admin/v1/groups/${id}/suspend`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    }),

  // Story AP-5.1: Reactivate group
  reactivate: (id: string) =>
    request<AdminGroup>(`/api/admin/v1/groups/${id}/reactivate`, {
      method: "POST",
    }),

  // Story AP-5.1: Archive group
  archive: (id: string) =>
    request<AdminGroup>(`/api/admin/v1/groups/${id}/archive`, {
      method: "POST",
    }),

  // Story AP-5.2: Get group members
  getMembers: (groupId: string) =>
    request<GroupMember[]>(`/api/admin/v1/groups/${groupId}/members`),

  // Story AP-5.2: Change member role
  changeMemberRole: (groupId: string, memberId: string, role: GroupMemberRole) =>
    request<GroupMember>(`/api/admin/v1/groups/${groupId}/members/${memberId}`, {
      method: "PUT",
      body: JSON.stringify({ role }),
    }),

  // Story AP-5.2: Remove member from group
  removeMember: (groupId: string, memberId: string) =>
    request<void>(`/api/admin/v1/groups/${groupId}/members/${memberId}`, {
      method: "DELETE",
    }),

  // Story AP-5.2: Add user to group
  addMember: (groupId: string, userId: string, role: GroupMemberRole = "member") =>
    request<GroupMember>(`/api/admin/v1/groups/${groupId}/members`, {
      method: "POST",
      body: JSON.stringify({ user_id: userId, role }),
    }),

  // Story AP-5.3: Transfer ownership
  transferOwnership: (groupId: string, newOwnerId: string) =>
    request<AdminGroup>(`/api/admin/v1/groups/${groupId}/transfer`, {
      method: "POST",
      body: JSON.stringify({ new_owner_id: newOwnerId }),
    }),

  // Story AP-5.4: Get group invites
  getInvites: (groupId: string, status?: string) => {
    const queryString = status ? `?status=${status}` : "";
    return request<GroupInvite[]>(`/api/admin/v1/groups/${groupId}/invites${queryString}`);
  },

  // Story AP-5.4: Revoke invite
  revokeInvite: (groupId: string, inviteId: string) =>
    request<void>(`/api/admin/v1/groups/${groupId}/invites/${inviteId}`, {
      method: "DELETE",
    }),

  // Story AP-5.4: Revoke all invites for group
  revokeAllInvites: (groupId: string) =>
    request<{ revoked_count: number }>(`/api/admin/v1/groups/${groupId}/invites`, {
      method: "DELETE",
    }),
};

// Epic AP-6: Location & Geofence Administration

// Story AP-6.1 & AP-6.2: Locations API
export const locationsApi = {
  // Get latest location for all devices
  getLatest: (params?: { organization_id?: string }) => {
    const searchParams = new URLSearchParams();
    if (params?.organization_id) searchParams.set("organization_id", params.organization_id);
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/locations/latest${queryString ? `?${queryString}` : ""}`;
    return request<LatestDeviceLocation[]>(endpoint);
  },

  // Query location history
  query: (params: LocationFilter & { limit?: number }) => {
    const searchParams = new URLSearchParams();
    if (params.device_id) searchParams.set("device_id", params.device_id);
    if (params.organization_id) searchParams.set("organization_id", params.organization_id);
    if (params.from) searchParams.set("from", params.from);
    if (params.to) searchParams.set("to", params.to);
    if (params.limit) searchParams.set("limit", String(params.limit));
    if (params.bbox) {
      searchParams.set("north", String(params.bbox.north));
      searchParams.set("south", String(params.bbox.south));
      searchParams.set("east", String(params.bbox.east));
      searchParams.set("west", String(params.bbox.west));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/locations/history${queryString ? `?${queryString}` : ""}`;
    return request<{ locations: DeviceLocation[]; total: number; truncated: boolean }>(endpoint);
  },
};

// Story AP-6.3 & AP-6.4: Geofences API
export const geofencesApi = {
  // List geofences
  list: (params?: GeofenceListParams) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.device_id) searchParams.set("device_id", params.device_id);
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.enabled !== undefined) searchParams.set("enabled", String(params.enabled));
      if (params.search) searchParams.set("search", params.search);
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/geofences${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<Geofence>>(endpoint);
  },

  // Get geofence details
  get: (id: string) => request<Geofence>(`/api/admin/v1/geofences/${id}`),

  // Create geofence
  create: (data: CreateGeofenceRequest) =>
    request<Geofence>("/api/admin/v1/geofences", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Update geofence
  update: (id: string, data: Partial<CreateGeofenceRequest>) =>
    request<Geofence>(`/api/admin/v1/geofences/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Delete geofence
  delete: (id: string) =>
    request<void>(`/api/admin/v1/geofences/${id}`, {
      method: "DELETE",
    }),

  // Toggle geofence enabled/disabled
  toggle: (id: string) =>
    request<Geofence>(`/api/admin/v1/geofences/${id}/toggle`, {
      method: "POST",
    }),

  // Get geofence events
  getEvents: (params?: GeofenceEventFilter) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.geofence_id) searchParams.set("geofence_id", params.geofence_id);
      if (params.device_id) searchParams.set("device_id", params.device_id);
      if (params.event_type) searchParams.set("event_type", params.event_type);
      if (params.from) searchParams.set("from", params.from);
      if (params.to) searchParams.set("to", params.to);
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/geofences/events${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<GeofenceEvent>>(endpoint);
  },
};

// Story AP-6.5: Proximity Alerts API
export const proximityAlertsApi = {
  // List proximity alerts
  list: (params?: ProximityAlertListParams) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.device_id) searchParams.set("device_id", params.device_id);
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.enabled !== undefined) searchParams.set("enabled", String(params.enabled));
      if (params.search) searchParams.set("search", params.search);
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/proximity-alerts${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<ProximityAlert>>(endpoint);
  },

  // Get proximity alert details
  get: (id: string) => request<ProximityAlert>(`/api/admin/v1/proximity-alerts/${id}`),

  // Create proximity alert
  create: (data: CreateProximityAlertRequest) =>
    request<ProximityAlert>("/api/admin/v1/proximity-alerts", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Update proximity alert
  update: (id: string, data: Partial<CreateProximityAlertRequest>) =>
    request<ProximityAlert>(`/api/admin/v1/proximity-alerts/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Delete proximity alert
  delete: (id: string) =>
    request<void>(`/api/admin/v1/proximity-alerts/${id}`, {
      method: "DELETE",
    }),

  // Toggle proximity alert enabled/disabled
  toggle: (id: string) =>
    request<ProximityAlert>(`/api/admin/v1/proximity-alerts/${id}/toggle`, {
      method: "POST",
    }),

  // Get trigger history
  getTriggers: (alertId: string, params?: { page?: number; limit?: number }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/proximity-alerts/${alertId}/triggers${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<ProximityAlertTrigger>>(endpoint);
  },
};

// Story AP-6.6: Data Retention API
export const retentionApi = {
  // List all retention policies
  list: () => request<RetentionPolicy[]>("/api/admin/v1/retention-policies"),

  // Get retention policy for organization
  get: (orgId: string) => request<RetentionPolicy>(`/api/admin/v1/retention-policies/${orgId}`),

  // Update retention policy
  update: (orgId: string, data: UpdateRetentionPolicyRequest) =>
    request<RetentionPolicy>(`/api/admin/v1/retention-policies/${orgId}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Manual purge for organization
  purge: (orgId: string) =>
    request<PurgeResult>(`/api/admin/v1/retention-policies/${orgId}/purge`, {
      method: "POST",
    }),
};

// Epic AP-7: Webhooks & Trips Administration

// Story AP-7.1, AP-7.2, AP-7.3: Webhooks API
export const webhooksApi = {
  // List webhooks with filtering
  list: (params?: WebhookListParams) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.status) searchParams.set("status", params.status);
      if (params.event_type) searchParams.set("event_type", params.event_type);
      if (params.search) searchParams.set("search", params.search);
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/webhooks${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<Webhook>>(endpoint);
  },

  // Get webhook details
  get: (id: string) => request<Webhook>(`/api/admin/v1/webhooks/${id}`),

  // Create webhook
  create: (data: CreateWebhookRequest) =>
    request<Webhook>("/api/admin/v1/webhooks", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Update webhook
  update: (id: string, data: UpdateWebhookRequest) =>
    request<Webhook>(`/api/admin/v1/webhooks/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Delete webhook
  delete: (id: string) =>
    request<void>(`/api/admin/v1/webhooks/${id}`, {
      method: "DELETE",
    }),

  // Toggle webhook enabled/paused
  toggle: (id: string) =>
    request<Webhook>(`/api/admin/v1/webhooks/${id}/toggle`, {
      method: "POST",
    }),

  // Story AP-7.2: Get delivery logs
  getDeliveries: (webhookId: string, params?: WebhookDeliveryListParams) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.status) searchParams.set("status", params.status);
      if (params.from) searchParams.set("from", params.from);
      if (params.to) searchParams.set("to", params.to);
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/webhooks/${webhookId}/deliveries${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<WebhookDelivery>>(endpoint);
  },

  // Get delivery details
  getDelivery: (webhookId: string, deliveryId: string) =>
    request<WebhookDelivery>(`/api/admin/v1/webhooks/${webhookId}/deliveries/${deliveryId}`),

  // Resend failed delivery
  resendDelivery: (webhookId: string, deliveryId: string) =>
    request<WebhookDelivery>(`/api/admin/v1/webhooks/${webhookId}/deliveries/${deliveryId}/resend`, {
      method: "POST",
    }),

  // Story AP-7.3: Test webhook
  test: (id: string, eventType?: WebhookEventType) => {
    const queryString = eventType ? `?event_type=${eventType}` : "";
    return request<WebhookTestResult>(`/api/admin/v1/webhooks/${id}/test${queryString}`, {
      method: "POST",
    });
  },
};

// Story AP-7.4, AP-7.5: Trips API
export const tripsApi = {
  // List trips with filtering
  list: (params?: TripListParams) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.device_id) searchParams.set("device_id", params.device_id);
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.status) searchParams.set("status", params.status);
      if (params.from) searchParams.set("from", params.from);
      if (params.to) searchParams.set("to", params.to);
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/trips${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<Trip>>(endpoint);
  },

  // Get trip details
  get: (id: string) => request<Trip>(`/api/admin/v1/trips/${id}`),

  // Get trip path (coordinates)
  getPath: (id: string) => request<{ points: TripPoint[] }>(`/api/admin/v1/trips/${id}/path`),

  // Get trip events
  getEvents: (id: string) => request<{ events: TripEvent[] }>(`/api/admin/v1/trips/${id}/events`),

  // Count trips for export (to determine if async is needed)
  count: (params?: { device_id?: string; organization_id?: string; from?: string; to?: string }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.device_id) searchParams.set("device_id", params.device_id);
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.from) searchParams.set("from", params.from);
      if (params.to) searchParams.set("to", params.to);
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/trips/count${queryString ? `?${queryString}` : ""}`;
    return request<{ count: number }>(endpoint);
  },
};

// Story AP-7.5.4: Async Export Jobs API
export const exportJobsApi = {
  // Create a new async export job
  create: (data: CreateExportJobRequest) =>
    request<ExportJob>("/api/admin/v1/export-jobs", { method: "POST", body: JSON.stringify(data) }),

  // List export jobs for the current user
  list: (params?: { status?: ExportJobStatus; type?: string; page?: number; limit?: number }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.status) searchParams.set("status", params.status);
      if (params.type) searchParams.set("type", params.type);
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/export-jobs${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<ExportJob>>(endpoint);
  },

  // Get export job status
  get: (id: string) => request<ExportJob>(`/api/admin/v1/export-jobs/${id}`),

  // Cancel an export job
  cancel: (id: string) =>
    request<ExportJob>(`/api/admin/v1/export-jobs/${id}/cancel`, { method: "POST" }),

  // Delete an export job (and its file)
  delete: (id: string) =>
    request<void>(`/api/admin/v1/export-jobs/${id}`, { method: "DELETE" }),
};

// Epic AP-8: App Usage & Unlock Requests API

// Story AP-8.1: App Usage Statistics API
export const appUsageApi = {
  // Get app usage data
  list: (params?: AppUsageParams) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.device_id) searchParams.set("device_id", params.device_id);
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.from) searchParams.set("from", params.from);
      if (params.to) searchParams.set("to", params.to);
      if (params.category) searchParams.set("category", params.category);
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/app-usage${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<AdminAppUsage>>(endpoint);
  },

  // Get usage by category
  getByCategory: (params?: { organization_id?: string; from?: string; to?: string }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.from) searchParams.set("from", params.from);
      if (params.to) searchParams.set("to", params.to);
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/app-usage/categories${queryString ? `?${queryString}` : ""}`;
    return request<{ items: AppUsageByCategory[] }>(endpoint);
  },

  // Get top apps
  getTopApps: (params?: { organization_id?: string; from?: string; to?: string; limit?: number }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.from) searchParams.set("from", params.from);
      if (params.to) searchParams.set("to", params.to);
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/app-usage/top-apps${queryString ? `?${queryString}` : ""}`;
    return request<{ items: TopApp[] }>(endpoint);
  },

  // Get device usage breakdown
  getDeviceUsage: (deviceId: string, params?: { from?: string; to?: string }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.from) searchParams.set("from", params.from);
      if (params.to) searchParams.set("to", params.to);
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/app-usage/device/${deviceId}${queryString ? `?${queryString}` : ""}`;
    return request<DeviceAppUsage>(endpoint);
  },
};

// Story AP-8.2 & AP-8.3: App Limits & Templates API
export const appLimitsApi = {
  // List app limits
  list: (params?: AppLimitListParams) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.device_id) searchParams.set("device_id", params.device_id);
      if (params.group_id) searchParams.set("group_id", params.group_id);
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.target_type) searchParams.set("target_type", params.target_type);
      if (params.enabled !== undefined) searchParams.set("enabled", String(params.enabled));
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/app-limits${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<AppLimit>>(endpoint);
  },

  // Get single limit
  get: (id: string) => request<AppLimit>(`/api/admin/v1/app-limits/${id}`),

  // Create limit
  create: (data: CreateAppLimitRequest) =>
    request<AppLimit>("/api/admin/v1/app-limits", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Update limit
  update: (id: string, data: Partial<CreateAppLimitRequest>) =>
    request<AppLimit>(`/api/admin/v1/app-limits/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Delete limit
  delete: (id: string) =>
    request<void>(`/api/admin/v1/app-limits/${id}`, {
      method: "DELETE",
    }),

  // Toggle enabled
  toggle: (id: string) =>
    request<AppLimit>(`/api/admin/v1/app-limits/${id}/toggle`, {
      method: "POST",
    }),

  // Templates
  listTemplates: (params?: { organization_id?: string; page?: number; limit?: number }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/limit-templates${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<LimitTemplate>>(endpoint);
  },

  getTemplate: (id: string) => request<LimitTemplate>(`/api/admin/v1/limit-templates/${id}`),

  createTemplate: (data: CreateLimitTemplateRequest) =>
    request<LimitTemplate>("/api/admin/v1/limit-templates", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  updateTemplate: (id: string, data: Partial<CreateLimitTemplateRequest>) =>
    request<LimitTemplate>(`/api/admin/v1/limit-templates/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  deleteTemplate: (id: string, replacementTemplateId?: string) => {
    const searchParams = new URLSearchParams();
    if (replacementTemplateId) searchParams.set("replacement_template_id", replacementTemplateId);
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/limit-templates/${id}${queryString ? `?${queryString}` : ""}`;
    return request<void>(endpoint, { method: "DELETE" });
  },

  applyTemplate: (id: string, data: { device_ids?: string[]; group_ids?: string[] }) =>
    request<{ applied_count: number }>(`/api/admin/v1/limit-templates/${id}/apply`, {
      method: "POST",
      body: JSON.stringify(data),
    }),
};

// Story AP-8.4 & AP-8.5: Unlock Requests & Auto-Approval API
export const unlockRequestsApi = {
  // List unlock requests
  list: (params?: UnlockRequestListParams) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.device_id) searchParams.set("device_id", params.device_id);
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.status) searchParams.set("status", params.status);
      if (params.from) searchParams.set("from", params.from);
      if (params.to) searchParams.set("to", params.to);
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/unlock-requests${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<AdminUnlockRequest>>(endpoint);
  },

  // Get single request
  get: (id: string) => request<AdminUnlockRequest>(`/api/admin/v1/unlock-requests/${id}`),

  // Approve request
  approve: (id: string, data: ApproveUnlockRequest) =>
    request<AdminUnlockRequest>(`/api/admin/v1/unlock-requests/${id}/approve`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Deny request
  deny: (id: string, data: DenyUnlockRequest) =>
    request<AdminUnlockRequest>(`/api/admin/v1/unlock-requests/${id}/deny`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Get request history for device
  getDeviceHistory: (deviceId: string, params?: { page?: number; limit?: number }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/unlock-requests/history/${deviceId}${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<AdminUnlockRequest>>(endpoint);
  },

  // Auto-approval rules
  listRules: (params?: { organization_id?: string; enabled?: boolean }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.enabled !== undefined) searchParams.set("enabled", String(params.enabled));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/auto-approval-rules${queryString ? `?${queryString}` : ""}`;
    return request<{ items: AutoApprovalRule[] }>(endpoint);
  },

  // Alias for consistency with component usage
  listAutoApprovalRules: (params?: { organization_id?: string; enabled?: boolean }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.enabled !== undefined) searchParams.set("enabled", String(params.enabled));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/auto-approval-rules${queryString ? `?${queryString}` : ""}`;
    return request<{ items: AutoApprovalRule[] }>(endpoint);
  },

  getRule: (id: string) => request<AutoApprovalRule>(`/api/admin/v1/auto-approval-rules/${id}`),

  getAutoApprovalRule: (id: string) => request<AutoApprovalRule>(`/api/admin/v1/auto-approval-rules/${id}`),

  createRule: (data: CreateAutoApprovalRuleRequest) =>
    request<AutoApprovalRule>("/api/admin/v1/auto-approval-rules", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  createAutoApprovalRule: (data: CreateAutoApprovalRuleRequest) =>
    request<AutoApprovalRule>("/api/admin/v1/auto-approval-rules", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  updateRule: (id: string, data: Partial<CreateAutoApprovalRuleRequest>) =>
    request<AutoApprovalRule>(`/api/admin/v1/auto-approval-rules/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  updateAutoApprovalRule: (id: string, data: Partial<CreateAutoApprovalRuleRequest>) =>
    request<AutoApprovalRule>(`/api/admin/v1/auto-approval-rules/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  deleteRule: (id: string) =>
    request<void>(`/api/admin/v1/auto-approval-rules/${id}`, {
      method: "DELETE",
    }),

  deleteAutoApprovalRule: (id: string) =>
    request<void>(`/api/admin/v1/auto-approval-rules/${id}`, {
      method: "DELETE",
    }),

  toggleRule: (id: string) =>
    request<AutoApprovalRule>(`/api/admin/v1/auto-approval-rules/${id}/toggle`, {
      method: "POST",
    }),

  reorderRules: (ruleIds: string[]) =>
    request<void>("/api/admin/v1/auto-approval-rules/reorder", {
      method: "PUT",
      body: JSON.stringify({ rule_ids: ruleIds }),
    }),

  reorderAutoApprovalRules: (data: { rule_ids: string[] }) =>
    request<void>("/api/admin/v1/auto-approval-rules/reorder", {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Auto-approval log
  getAutoApprovalLog: (params?: { from?: string; to?: string; organization_id?: string; rule_id?: string; page?: number; limit?: number }) => {
    const searchParams = new URLSearchParams();
    if (params) {
      if (params.from) searchParams.set("from", params.from);
      if (params.to) searchParams.set("to", params.to);
      if (params.organization_id) searchParams.set("organization_id", params.organization_id);
      if (params.rule_id) searchParams.set("rule_id", params.rule_id);
      if (params.page) searchParams.set("page", String(params.page));
      if (params.limit) searchParams.set("limit", String(params.limit));
    }
    const queryString = searchParams.toString();
    const endpoint = `/api/admin/v1/auto-approval-log${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<AutoApprovalLogEntry>>(endpoint);
  },
};

// ==============================================
// Epic AP-9: System Configuration API
// ==============================================

// Story AP-9.1: Authentication Settings
export const authConfigApi = {
  get: () => request<AuthConfig>("/api/admin/v1/system-config/auth"),
  update: (data: UpdateAuthConfigRequest) =>
    request<AuthConfig>("/api/admin/v1/system-config/auth", {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  getOAuthProviders: () =>
    request<OAuthProviderConfig[]>("/api/admin/v1/system-config/oauth-providers"),
  updateOAuthProvider: (provider: string, data: UpdateOAuthProviderRequest) =>
    request<OAuthProviderConfig>(
      `/api/admin/v1/system-config/oauth-providers/${provider}`,
      {
        method: "PUT",
        body: JSON.stringify(data),
      }
    ),
};

// Story AP-9.2: Feature Flags
export const featureFlagsApi = {
  list: () => request<FeatureFlag[]>("/api/admin/v1/system-config/features"),
  toggle: (featureId: string, enabled: boolean) =>
    request<FeatureFlag>(`/api/admin/v1/system-config/features/${featureId}`, {
      method: "PUT",
      body: JSON.stringify({ enabled }),
    }),
};

// Story AP-9.3: Rate Limits
export const rateLimitsApi = {
  list: () => request<RateLimitConfig[]>("/api/admin/v1/system-config/rate-limits"),
  update: (id: string, data: Partial<RateLimitConfig>) =>
    request<RateLimitConfig>(`/api/admin/v1/system-config/rate-limits/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  listOverrides: () =>
    request<RateLimitOverride[]>(
      "/api/admin/v1/system-config/rate-limits/overrides"
    ),
  createOverride: (data: CreateRateLimitOverrideRequest) =>
    request<RateLimitOverride>(
      "/api/admin/v1/system-config/rate-limits/overrides",
      {
        method: "POST",
        body: JSON.stringify(data),
      }
    ),
  updateOverride: (id: string, data: Partial<CreateRateLimitOverrideRequest>) =>
    request<RateLimitOverride>(
      `/api/admin/v1/system-config/rate-limits/overrides/${id}`,
      {
        method: "PUT",
        body: JSON.stringify(data),
      }
    ),
  deleteOverride: (id: string) =>
    request<void>(`/api/admin/v1/system-config/rate-limits/overrides/${id}`, {
      method: "DELETE",
    }),
  getMetrics: () =>
    request<RateLimitMetrics[]>(
      "/api/admin/v1/system-config/rate-limits/metrics"
    ),
};

// Story AP-9.4: API Keys
export const apiKeysApi = {
  list: () => request<ApiKey[]>("/api/admin/v1/system-config/api-keys"),
  get: (id: string) =>
    request<ApiKey>(`/api/admin/v1/system-config/api-keys/${id}`),
  create: (data: CreateApiKeyRequest) =>
    request<CreateApiKeyResponse>("/api/admin/v1/system-config/api-keys", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  update: (id: string, data: Partial<CreateApiKeyRequest>) =>
    request<ApiKey>(`/api/admin/v1/system-config/api-keys/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  delete: (id: string) =>
    request<void>(`/api/admin/v1/system-config/api-keys/${id}`, {
      method: "DELETE",
    }),
  rotate: (id: string, gracePeriodHours?: number) =>
    request<CreateApiKeyResponse>(
      `/api/admin/v1/system-config/api-keys/${id}/rotate`,
      {
        method: "POST",
        body: JSON.stringify({ grace_period_hours: gracePeriodHours }),
      }
    ),
  getUsage: (id: string) =>
    request<ApiKeyUsageStats>(
      `/api/admin/v1/system-config/api-keys/${id}/usage`
    ),
  toggleActive: (id: string, active: boolean) =>
    request<ApiKey>(`/api/admin/v1/system-config/api-keys/${id}`, {
      method: "PUT",
      body: JSON.stringify({ is_active: active }),
    }),
};

// Story AP-9.5: System-wide Data Retention Config
export const systemRetentionApi = {
  get: () => request<RetentionConfig>("/api/admin/v1/system-config/retention"),
  update: (data: UpdateRetentionConfigRequest) =>
    request<RetentionConfig>("/api/admin/v1/system-config/retention", {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  getStats: () =>
    request<RetentionStats[]>("/api/admin/v1/system-config/retention/stats"),
  preview: () =>
    request<RetentionStats[]>("/api/admin/v1/system-config/retention/preview"),
};

// ============================================
// Epic AP-10: Dashboard & Analytics APIs
// ============================================

// Story AP-10.1: Overview Dashboard
export const dashboardApi = {
  getMetrics: () => request<DashboardMetrics>("/api/admin/v1/dashboard/metrics"),
  getAlerts: () => request<AlertIndicators>("/api/admin/v1/dashboard/alerts"),
  refreshMetrics: () =>
    request<DashboardMetrics>("/api/admin/v1/dashboard/metrics/refresh", {
      method: "POST",
    }),
};

// Story AP-10.2: User Analytics
export const userAnalyticsApi = {
  getAll: (params?: { period?: string; start?: string; end?: string }) =>
    request<UserAnalytics>(
      `/api/admin/v1/analytics/users${params ? `?${new URLSearchParams(params as Record<string, string>)}` : ""}`
    ),
  getGrowth: (params?: { period?: string; start?: string; end?: string }) =>
    request<UserGrowthData[]>(
      `/api/admin/v1/analytics/users/growth${params ? `?${new URLSearchParams(params as Record<string, string>)}` : ""}`
    ),
  getActive: (params?: { period?: string }) =>
    request<UserGrowthData[]>(
      `/api/admin/v1/analytics/users/active${params ? `?${new URLSearchParams(params as Record<string, string>)}` : ""}`
    ),
  getRetention: (params?: { cohort_count?: string }) =>
    request<UserRetentionData[]>(
      `/api/admin/v1/analytics/users/retention${params ? `?${new URLSearchParams(params as Record<string, string>)}` : ""}`
    ),
  getSegments: () =>
    request<UserSegmentData[]>("/api/admin/v1/analytics/users/segments"),
};

// Story AP-10.3: Device Analytics
export const deviceAnalyticsApi = {
  getAll: (params?: { period?: string; start?: string; end?: string }) =>
    request<DeviceAnalytics>(
      `/api/admin/v1/analytics/devices${params ? `?${new URLSearchParams(params as Record<string, string>)}` : ""}`
    ),
  getDistribution: () =>
    request<DeviceDistribution[]>("/api/admin/v1/analytics/devices/distribution"),
  getConnectivity: (params?: { period?: string }) =>
    request<DeviceConnectivityData[]>(
      `/api/admin/v1/analytics/devices/connectivity${params ? `?${new URLSearchParams(params as Record<string, string>)}` : ""}`
    ),
  getVolume: (params?: { period?: string }) =>
    request<LocationVolumeData[]>(
      `/api/admin/v1/analytics/devices/volume${params ? `?${new URLSearchParams(params as Record<string, string>)}` : ""}`
    ),
  getHeatmap: () =>
    request<DeviceActivityHeatmap[]>("/api/admin/v1/analytics/devices/heatmap"),
};

// Story AP-10.4: API Analytics
export const apiAnalyticsApi = {
  getAll: (params?: { period?: string; start?: string; end?: string }) =>
    request<ApiAnalytics>(
      `/api/admin/v1/analytics/api${params ? `?${new URLSearchParams(params as Record<string, string>)}` : ""}`
    ),
  getEndpoints: (params?: { limit?: string }) =>
    request<EndpointMetrics[]>(
      `/api/admin/v1/analytics/api/endpoints${params ? `?${new URLSearchParams(params as Record<string, string>)}` : ""}`
    ),
  getLatency: (params?: { period?: string }) =>
    request<ResponseTimeData[]>(
      `/api/admin/v1/analytics/api/latency${params ? `?${new URLSearchParams(params as Record<string, string>)}` : ""}`
    ),
  getErrors: (params?: { period?: string }) =>
    request<ErrorRateData[]>(
      `/api/admin/v1/analytics/api/errors${params ? `?${new URLSearchParams(params as Record<string, string>)}` : ""}`
    ),
  getConsumers: (params?: { limit?: string }) =>
    request<ApiConsumer[]>(
      `/api/admin/v1/analytics/api/consumers${params ? `?${new URLSearchParams(params as Record<string, string>)}` : ""}`
    ),
};

// Story AP-10.5: Custom Reports
export const reportsApi = {
  generate: (config: Omit<ReportConfig, "id" | "created_at" | "updated_at" | "created_by">) =>
    request<ReportResult>("/api/admin/v1/reports/generate", {
      method: "POST",
      body: JSON.stringify(config),
    }),
  getSaved: () => request<SavedReport[]>("/api/admin/v1/reports/saved"),
  createSaved: (data: Omit<SavedReport, "id" | "created_at" | "updated_at" | "last_run">) =>
    request<SavedReport>("/api/admin/v1/reports/saved", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  updateSaved: (id: string, data: Partial<Omit<SavedReport, "id" | "created_at" | "updated_at">>) =>
    request<SavedReport>(`/api/admin/v1/reports/saved/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  deleteSaved: (id: string) =>
    request<void>(`/api/admin/v1/reports/saved/${id}`, { method: "DELETE" }),
  runSaved: (id: string) =>
    request<ReportResult>(`/api/admin/v1/reports/saved/${id}/run`, { method: "POST" }),
  export: (id: string, format: "pdf" | "csv") =>
    requestBlob(`/api/admin/v1/reports/${id}/export?format=${format}`),
};

// ============================================
// Epic AP-11: Audit & Compliance API
// ============================================

export const auditApi = {
  // Audit log entries
  getLogs: (filters?: AuditLogFilter, page = 1, limit = 50) =>
    request<{ data: AuditLogEntry[]; total: number; page: number; limit: number }>(
      `/api/admin/v1/audit/logs?page=${page}&limit=${limit}${filters ? `&${new URLSearchParams(filters as Record<string, string>).toString()}` : ""}`
    ),
  getLog: (id: string) => request<AuditLogEntry>(`/api/admin/v1/audit/logs/${id}`),
  getStats: (filters?: AuditLogFilter) =>
    request<AuditLogStats>(
      `/api/admin/v1/audit/stats${filters ? `?${new URLSearchParams(filters as Record<string, string>).toString()}` : ""}`
    ),
  exportLogs: (filters?: AuditLogFilter, format: "csv" | "json" = "csv") =>
    requestBlob(
      `/api/admin/v1/audit/logs/export?format=${format}${filters ? `&${new URLSearchParams(filters as Record<string, string>).toString()}` : ""}`
    ),

  // User activity reports
  getUserActivity: (userId: string, dateFrom?: string, dateTo?: string) =>
    request<UserActivityReport>(
      `/api/admin/v1/audit/users/${userId}/activity${dateFrom ? `?date_from=${dateFrom}` : ""}${dateTo ? `&date_to=${dateTo}` : ""}`
    ),
  exportUserActivity: (userId: string, format: "pdf" | "csv" = "csv") =>
    requestBlob(`/api/admin/v1/audit/users/${userId}/export?format=${format}`),

  // Organization activity reports
  getOrgActivity: (orgId: string, dateFrom?: string, dateTo?: string) =>
    request<OrgActivityReport>(
      `/api/admin/v1/audit/organizations/${orgId}/activity${dateFrom ? `?date_from=${dateFrom}` : ""}${dateTo ? `&date_to=${dateTo}` : ""}`
    ),
  getOrgAnomalies: (orgId: string) =>
    request<OrgActivityReport["anomalies"]>(`/api/admin/v1/audit/organizations/${orgId}/anomalies`),

  // GDPR compliance
  createDataExport: (userId: string, dataTypes: string[]) =>
    request<GDPRDataExportRequest>("/api/admin/v1/gdpr/export", {
      method: "POST",
      body: JSON.stringify({ user_id: userId, data_types: dataTypes }),
    }),
  getDataExports: () => request<GDPRDataExportRequest[]>("/api/admin/v1/gdpr/exports"),
  getDataExport: (id: string) => request<GDPRDataExportRequest>(`/api/admin/v1/gdpr/exports/${id}`),
  createDeletionRequest: (userId: string, dataTypes: string[]) =>
    request<GDPRDeletionRequest>("/api/admin/v1/gdpr/delete", {
      method: "POST",
      body: JSON.stringify({ user_id: userId, data_types: dataTypes }),
    }),
  getDeletionRequests: () => request<GDPRDeletionRequest[]>("/api/admin/v1/gdpr/deletions"),
  getDeletionRequest: (id: string) => request<GDPRDeletionRequest>(`/api/admin/v1/gdpr/deletions/${id}`),

  // Integrity & tamper-evidence
  getIntegrityStatus: () => request<AuditIntegrityStatus>("/api/admin/v1/audit/integrity/status"),
  verifyIntegrity: () =>
    request<{ success: boolean; issues: string[] }>("/api/admin/v1/audit/integrity/verify", {
      method: "POST",
    }),
  getIntegrityAlerts: () =>
    request<AuditIntegrityStatus["alerts"]>("/api/admin/v1/audit/integrity/alerts"),
  resolveAlert: (alertId: string) =>
    request<void>(`/api/admin/v1/audit/integrity/alerts/${alertId}/resolve`, { method: "POST" }),
};

// ============================================
// Epic AP-12: Notifications API
// ============================================

export const notificationsApi = {
  getAll: (unreadOnly = false) =>
    request<Notification[]>(`/api/admin/v1/notifications${unreadOnly ? "?unread=true" : ""}`),
  getUnreadCount: () => request<{ count: number }>("/api/admin/v1/notifications/unread-count"),
  markAsRead: (id: string) =>
    request<void>(`/api/admin/v1/notifications/${id}/read`, { method: "POST" }),
  markAllAsRead: () =>
    request<void>("/api/admin/v1/notifications/read-all", { method: "POST" }),
  delete: (id: string) =>
    request<void>(`/api/admin/v1/notifications/${id}`, { method: "DELETE" }),
  getPreferences: () =>
    request<NotificationPreferences>("/api/admin/v1/notifications/preferences"),
  updatePreferences: (prefs: Partial<NotificationPreferences>) =>
    request<NotificationPreferences>("/api/admin/v1/notifications/preferences", {
      method: "PUT",
      body: JSON.stringify(prefs),
    }),
};
