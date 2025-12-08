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

// Device Management
export const deviceApi = {
  list: () => request<Device[]>("/api/admin/devices"),

  get: (id: string) => request<Device>(`/api/admin/devices/${id}`),

  getUsage: (id: string, date?: string) => {
    const params = date ? `?date=${date}` : "";
    return request<AppUsage[]>(`/api/admin/devices/${id}/usage${params}`);
  },
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

// Public Configuration (feature flags & auth config)
export const configApi = {
  getPublic: () => request<PublicConfig>("/api/v1/config/public", {}, false),
};
