// Authentication types for the admin portal

export interface UserRole {
  role_code: string;
  role_name: string;
  organization_id: string | null;
}

export interface User {
  id: string;
  email: string;
  display_name: string;
  avatar_url: string | null;
  email_verified: boolean;
  auth_provider: string;
  organization_id: string | null;
  created_at: string;
  roles?: UserRole[];
  permissions?: string[]; // Array of permission codes like "users.read", "devices.create"
}

export interface Tokens {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
}

export interface LoginResponse {
  user: User;
  tokens?: Tokens; // Optional for httpOnly cookie mode (tokens set via cookies)
}

export interface RefreshResponse {
  tokens?: Tokens; // Optional for httpOnly cookie mode (tokens set via cookies)
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterCredentials {
  email: string;
  password: string;
  display_name: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  new_password: string;
}

export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
