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
  if (includeAuth) {
    const token = getAccessToken();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
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

  refresh: (refreshToken: string) =>
    request<RefreshResponse>("/api/v1/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refresh_token: refreshToken }),
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
  list: () => request<Device[]>("/api/admin/devices"),

  get: (id: string) => request<Device>(`/api/admin/devices/${id}`),

  getUsage: (id: string, date?: string) => {
    const params = date ? `?date=${date}` : "";
    return request<AppUsage[]>(`/api/admin/devices/${id}/usage${params}`);
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
    const endpoint = `/api/admin/devices/fleet${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<AdminDevice>>(endpoint);
  },

  // Story AP-4.2: Get device details
  get: (id: string) => request<DeviceDetails>(`/api/admin/devices/fleet/${id}`),

  // Story AP-4.2: Suspend device
  suspend: (id: string, reason?: string) =>
    request<DeviceDetails>(`/api/admin/devices/fleet/${id}/suspend`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    }),

  // Story AP-4.2: Reactivate device
  reactivate: (id: string) =>
    request<DeviceDetails>(`/api/admin/devices/fleet/${id}/reactivate`, {
      method: "POST",
    }),

  // Story AP-4.2: Delete device
  delete: (id: string) =>
    request<{ success: boolean }>(`/api/admin/devices/fleet/${id}`, {
      method: "DELETE",
    }),

  // Story AP-4.4: Bulk suspend
  bulkSuspend: (deviceIds: string[]) =>
    request<BulkOperationResult>("/api/admin/devices/fleet/bulk/suspend", {
      method: "POST",
      body: JSON.stringify({ device_ids: deviceIds }),
    }),

  // Story AP-4.4: Bulk reactivate
  bulkReactivate: (deviceIds: string[]) =>
    request<BulkOperationResult>("/api/admin/devices/fleet/bulk/reactivate", {
      method: "POST",
      body: JSON.stringify({ device_ids: deviceIds }),
    }),

  // Story AP-4.4: Bulk delete
  bulkDelete: (deviceIds: string[]) =>
    request<BulkOperationResult>("/api/admin/devices/fleet/bulk/delete", {
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
    const endpoint = `/api/admin/devices/fleet/inactive${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<InactiveDevice>>(endpoint);
  },

  // Story AP-4.5: Send notification to device owners
  notifyOwners: (deviceIds: string[], messageTemplate?: string) =>
    request<NotifyOwnersResult>("/api/admin/devices/fleet/notify", {
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
    const endpoint = `/api/admin/enrollment/tokens${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<EnrollmentToken>>(endpoint);
  },

  // Create new token
  create: (data: CreateEnrollmentTokenRequest) =>
    request<EnrollmentToken>("/api/admin/enrollment/tokens", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Get token details
  get: (id: string) => request<EnrollmentToken>(`/api/admin/enrollment/tokens/${id}`),

  // Get token usage history
  getUsage: (id: string) =>
    request<{ enrollments: TokenUsage[] }>(`/api/admin/enrollment/tokens/${id}/usage`),

  // Revoke token
  revoke: (id: string) =>
    request<{ success: boolean }>(`/api/admin/enrollment/tokens/${id}`, {
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
    const endpoint = `/api/admin/users${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<AdminUser>>(endpoint);
  },

  get: (id: string) => request<AdminUser>(`/api/admin/users/${id}`),

  create: (data: CreateUserRequest) =>
    request<AdminUser>("/api/admin/users", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  suspend: (id: string, reason: string) =>
    request<AdminUser>(`/api/admin/users/${id}/suspend`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    }),

  reactivate: (id: string) =>
    request<AdminUser>(`/api/admin/users/${id}/reactivate`, {
      method: "POST",
    }),

  // Story AP-3.4: Password Reset
  resetPassword: (id: string, forceChange: boolean = true) =>
    request<{ success: boolean }>(`/api/admin/users/${id}/reset-password`, {
      method: "POST",
      body: JSON.stringify({ force_change_on_login: forceChange }),
    }),

  // Story AP-3.5: Session Management
  getSessions: (id: string) =>
    request<UserSession[]>(`/api/admin/users/${id}/sessions`),

  revokeSession: (userId: string, sessionId: string) =>
    request<{ success: boolean }>(`/api/admin/users/${userId}/sessions/${sessionId}`, {
      method: "DELETE",
    }),

  revokeAllSessions: (id: string) =>
    request<{ success: boolean }>(`/api/admin/users/${id}/sessions`, {
      method: "DELETE",
    }),

  // Story AP-3.6: MFA Management
  getMfaStatus: (id: string) =>
    request<MfaStatus>(`/api/admin/users/${id}/mfa`),

  forceMfaEnrollment: (id: string) =>
    request<{ success: boolean }>(`/api/admin/users/${id}/mfa/force`, {
      method: "POST",
    }),

  resetMfa: (id: string) =>
    request<{ success: boolean }>(`/api/admin/users/${id}/mfa`, {
      method: "DELETE",
    }),
};

// Unlock Requests
export const unlockApi = {
  list: (status?: string) => {
    const params = status ? `?status=${status}` : "";
    return request<UnlockRequest[]>(`/api/admin/unlock-requests${params}`);
  },

  approve: (id: string, response?: string) =>
    request<UnlockRequest>(`/api/admin/unlock-requests/${id}/approve`, {
      method: "POST",
      body: JSON.stringify({ response }),
    }),

  deny: (id: string, response?: string) =>
    request<UnlockRequest>(`/api/admin/unlock-requests/${id}/deny`, {
      method: "POST",
      body: JSON.stringify({ response }),
    }),
};

// App Limits
export const limitsApi = {
  list: (deviceId: string) =>
    request<DailyLimit[]>(`/api/admin/devices/${deviceId}/limits`),

  set: (deviceId: string, limit: Omit<DailyLimit, "id">) =>
    request<DailyLimit>(`/api/admin/devices/${deviceId}/limits`, {
      method: "POST",
      body: JSON.stringify(limit),
    }),

  update: (deviceId: string, limitId: string, limit: Partial<DailyLimit>) =>
    request<DailyLimit>(`/api/admin/devices/${deviceId}/limits/${limitId}`, {
      method: "PUT",
      body: JSON.stringify(limit),
    }),

  delete: (deviceId: string, limitId: string) =>
    request<void>(`/api/admin/devices/${deviceId}/limits/${limitId}`, {
      method: "DELETE",
    }),
};

// Admin Settings
export const settingsApi = {
  get: () => request<AdminSettings>("/api/admin/settings"),

  update: (settings: Partial<AdminSettings>) =>
    request<AdminSettings>("/api/admin/settings", {
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
    const endpoint = `/api/admin/organizations${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<Organization>>(endpoint);
  },

  get: (id: string) => request<Organization>(`/api/admin/organizations/${id}`),

  create: (data: CreateOrganizationRequest) =>
    request<Organization>("/api/admin/organizations", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  update: (id: string, data: UpdateOrganizationRequest) =>
    request<Organization>(`/api/admin/organizations/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Story AP-2.2: Update organization limits
  updateLimits: (id: string, limits: { max_devices?: number; max_users?: number; max_groups?: number }) =>
    request<Organization>(`/api/admin/organizations/${id}/limits`, {
      method: "PUT",
      body: JSON.stringify(limits),
    }),

  // Story AP-2.3: Status management
  suspend: (id: string, reason: string) =>
    request<Organization>(`/api/admin/organizations/${id}/suspend`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    }),

  reactivate: (id: string) =>
    request<Organization>(`/api/admin/organizations/${id}/reactivate`, {
      method: "POST",
    }),

  archive: (id: string) =>
    request<Organization>(`/api/admin/organizations/${id}/archive`, {
      method: "POST",
    }),

  // Story AP-2.4: Feature flags
  updateFeatures: (id: string, features: Partial<Organization["features"]>) =>
    request<Organization>(`/api/admin/organizations/${id}/features`, {
      method: "PUT",
      body: JSON.stringify(features),
    }),

  // Story AP-2.5: Statistics
  getStats: (id: string) =>
    request<OrganizationStats>(`/api/admin/organizations/${id}/stats`),
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
  list: () => request<Role[]>("/api/admin/roles"),

  // Story AP-1.1: Get role details
  get: (id: string) => request<Role>(`/api/admin/roles/${id}`),

  // Story AP-1.3: Create custom role
  create: (data: CreateRoleRequest) =>
    request<Role>("/api/admin/roles", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Story AP-1.3: Update role
  update: (id: string, data: UpdateRoleRequest) =>
    request<Role>(`/api/admin/roles/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Story AP-1.3: Delete custom role
  delete: (id: string) =>
    request<void>(`/api/admin/roles/${id}`, {
      method: "DELETE",
    }),

  // Story AP-1.1: List all permissions
  listPermissions: () => request<Permission[]>("/api/admin/permissions"),

  // Story AP-1.2: Get user role assignments
  getUserAssignments: (userId: string) =>
    request<UserRoleAssignment[]>(`/api/admin/users/${userId}/roles`),

  // Story AP-1.2: Assign role to user
  assignRole: (userId: string, roleId: string, organizationId?: string) =>
    request<UserRoleAssignment>(`/api/admin/users/${userId}/roles`, {
      method: "POST",
      body: JSON.stringify({ role_id: roleId, organization_id: organizationId }),
    }),

  // Story AP-1.2: Remove role from user
  removeRole: (userId: string, assignmentId: string) =>
    request<void>(`/api/admin/users/${userId}/roles/${assignmentId}`, {
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
    const endpoint = `/api/admin/groups${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<AdminGroup>>(endpoint);
  },

  // Story AP-5.1: Get group details
  get: (id: string) => request<AdminGroup>(`/api/admin/groups/${id}`),

  // Story AP-5.1: Suspend group
  suspend: (id: string, reason?: string) =>
    request<AdminGroup>(`/api/admin/groups/${id}/suspend`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    }),

  // Story AP-5.1: Reactivate group
  reactivate: (id: string) =>
    request<AdminGroup>(`/api/admin/groups/${id}/reactivate`, {
      method: "POST",
    }),

  // Story AP-5.1: Archive group
  archive: (id: string) =>
    request<AdminGroup>(`/api/admin/groups/${id}/archive`, {
      method: "POST",
    }),

  // Story AP-5.2: Get group members
  getMembers: (groupId: string) =>
    request<GroupMember[]>(`/api/admin/groups/${groupId}/members`),

  // Story AP-5.2: Change member role
  changeMemberRole: (groupId: string, memberId: string, role: GroupMemberRole) =>
    request<GroupMember>(`/api/admin/groups/${groupId}/members/${memberId}`, {
      method: "PUT",
      body: JSON.stringify({ role }),
    }),

  // Story AP-5.2: Remove member from group
  removeMember: (groupId: string, memberId: string) =>
    request<void>(`/api/admin/groups/${groupId}/members/${memberId}`, {
      method: "DELETE",
    }),

  // Story AP-5.2: Add user to group
  addMember: (groupId: string, userId: string, role: GroupMemberRole = "member") =>
    request<GroupMember>(`/api/admin/groups/${groupId}/members`, {
      method: "POST",
      body: JSON.stringify({ user_id: userId, role }),
    }),

  // Story AP-5.3: Transfer ownership
  transferOwnership: (groupId: string, newOwnerId: string) =>
    request<AdminGroup>(`/api/admin/groups/${groupId}/transfer`, {
      method: "POST",
      body: JSON.stringify({ new_owner_id: newOwnerId }),
    }),

  // Story AP-5.4: Get group invites
  getInvites: (groupId: string, status?: string) => {
    const queryString = status ? `?status=${status}` : "";
    return request<GroupInvite[]>(`/api/admin/groups/${groupId}/invites${queryString}`);
  },

  // Story AP-5.4: Revoke invite
  revokeInvite: (groupId: string, inviteId: string) =>
    request<void>(`/api/admin/groups/${groupId}/invites/${inviteId}`, {
      method: "DELETE",
    }),

  // Story AP-5.4: Revoke all invites for group
  revokeAllInvites: (groupId: string) =>
    request<{ revoked_count: number }>(`/api/admin/groups/${groupId}/invites`, {
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
    const endpoint = `/api/admin/locations/latest${queryString ? `?${queryString}` : ""}`;
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
    const endpoint = `/api/admin/locations/history${queryString ? `?${queryString}` : ""}`;
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
    const endpoint = `/api/admin/geofences${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<Geofence>>(endpoint);
  },

  // Get geofence details
  get: (id: string) => request<Geofence>(`/api/admin/geofences/${id}`),

  // Create geofence
  create: (data: CreateGeofenceRequest) =>
    request<Geofence>("/api/admin/geofences", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Update geofence
  update: (id: string, data: Partial<CreateGeofenceRequest>) =>
    request<Geofence>(`/api/admin/geofences/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Delete geofence
  delete: (id: string) =>
    request<void>(`/api/admin/geofences/${id}`, {
      method: "DELETE",
    }),

  // Toggle geofence enabled/disabled
  toggle: (id: string) =>
    request<Geofence>(`/api/admin/geofences/${id}/toggle`, {
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
    const endpoint = `/api/admin/geofences/events${queryString ? `?${queryString}` : ""}`;
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
    const endpoint = `/api/admin/proximity-alerts${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<ProximityAlert>>(endpoint);
  },

  // Get proximity alert details
  get: (id: string) => request<ProximityAlert>(`/api/admin/proximity-alerts/${id}`),

  // Create proximity alert
  create: (data: CreateProximityAlertRequest) =>
    request<ProximityAlert>("/api/admin/proximity-alerts", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Update proximity alert
  update: (id: string, data: Partial<CreateProximityAlertRequest>) =>
    request<ProximityAlert>(`/api/admin/proximity-alerts/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Delete proximity alert
  delete: (id: string) =>
    request<void>(`/api/admin/proximity-alerts/${id}`, {
      method: "DELETE",
    }),

  // Toggle proximity alert enabled/disabled
  toggle: (id: string) =>
    request<ProximityAlert>(`/api/admin/proximity-alerts/${id}/toggle`, {
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
    const endpoint = `/api/admin/proximity-alerts/${alertId}/triggers${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<ProximityAlertTrigger>>(endpoint);
  },
};

// Story AP-6.6: Data Retention API
export const retentionApi = {
  // List all retention policies
  list: () => request<RetentionPolicy[]>("/api/admin/retention-policies"),

  // Get retention policy for organization
  get: (orgId: string) => request<RetentionPolicy>(`/api/admin/retention-policies/${orgId}`),

  // Update retention policy
  update: (orgId: string, data: UpdateRetentionPolicyRequest) =>
    request<RetentionPolicy>(`/api/admin/retention-policies/${orgId}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Manual purge for organization
  purge: (orgId: string) =>
    request<PurgeResult>(`/api/admin/retention-policies/${orgId}/purge`, {
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
    const endpoint = `/api/admin/webhooks${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<Webhook>>(endpoint);
  },

  // Get webhook details
  get: (id: string) => request<Webhook>(`/api/admin/webhooks/${id}`),

  // Create webhook
  create: (data: CreateWebhookRequest) =>
    request<Webhook>("/api/admin/webhooks", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Update webhook
  update: (id: string, data: UpdateWebhookRequest) =>
    request<Webhook>(`/api/admin/webhooks/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  // Delete webhook
  delete: (id: string) =>
    request<void>(`/api/admin/webhooks/${id}`, {
      method: "DELETE",
    }),

  // Toggle webhook enabled/paused
  toggle: (id: string) =>
    request<Webhook>(`/api/admin/webhooks/${id}/toggle`, {
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
    const endpoint = `/api/admin/webhooks/${webhookId}/deliveries${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<WebhookDelivery>>(endpoint);
  },

  // Get delivery details
  getDelivery: (webhookId: string, deliveryId: string) =>
    request<WebhookDelivery>(`/api/admin/webhooks/${webhookId}/deliveries/${deliveryId}`),

  // Resend failed delivery
  resendDelivery: (webhookId: string, deliveryId: string) =>
    request<WebhookDelivery>(`/api/admin/webhooks/${webhookId}/deliveries/${deliveryId}/resend`, {
      method: "POST",
    }),

  // Story AP-7.3: Test webhook
  test: (id: string, eventType?: WebhookEventType) => {
    const queryString = eventType ? `?event_type=${eventType}` : "";
    return request<WebhookTestResult>(`/api/admin/webhooks/${id}/test${queryString}`, {
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
    const endpoint = `/api/admin/trips${queryString ? `?${queryString}` : ""}`;
    return request<PaginatedResponse<Trip>>(endpoint);
  },

  // Get trip details
  get: (id: string) => request<Trip>(`/api/admin/trips/${id}`),

  // Get trip path (coordinates)
  getPath: (id: string) => request<{ points: TripPoint[] }>(`/api/admin/trips/${id}/path`),

  // Get trip events
  getEvents: (id: string) => request<{ events: TripEvent[] }>(`/api/admin/trips/${id}/events`),
};
