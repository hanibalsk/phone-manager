"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  type ReactNode,
} from "react";
import { useRouter } from "next/navigation";
import type {
  User,
  Tokens,
  LoginCredentials,
  RegisterCredentials,
  AuthState,
} from "@/types/auth";
import { authApi } from "@/lib/api-client";
import { isLocalStorageMode, isHttpOnlyMode } from "@/lib/auth-mode";

const TOKEN_STORAGE_KEY = "auth_tokens";

interface AuthContextValue extends AuthState {
  login: (credentials: LoginCredentials) => Promise<{ success: boolean; error?: string }>;
  register: (credentials: RegisterCredentials) => Promise<{ success: boolean; error?: string }>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * Get tokens from localStorage (only in localStorage mode).
 * In httpOnly mode, returns null since tokens are managed via cookies.
 */
function getStoredTokens(): Tokens | null {
  // In httpOnly mode, tokens are in cookies - not accessible from JS
  if (isHttpOnlyMode()) return null;
  if (typeof window === "undefined") return null;
  try {
    const stored = localStorage.getItem(TOKEN_STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch {
    return null;
  }
}

/**
 * Store tokens in localStorage (only in localStorage mode).
 * In httpOnly mode, this is a no-op since backend sets cookies.
 */
function setStoredTokens(tokens: Tokens | null): void {
  // In httpOnly mode, backend handles cookie storage
  if (isHttpOnlyMode()) return;
  if (typeof window === "undefined") return;
  if (tokens) {
    localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(tokens));
  } else {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  }
}

/**
 * Get the current access token (only available in localStorage mode).
 * In httpOnly mode, returns null since tokens are in httpOnly cookies.
 */
export function getAccessToken(): string | null {
  // In httpOnly mode, tokens are in cookies - not accessible from JS
  if (isHttpOnlyMode()) return null;
  const tokens = getStoredTokens();
  return tokens?.access_token ?? null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [tokens, setTokens] = useState<Tokens | null>(null);

  // In httpOnly mode, tokens are in cookies (not in state), so just check user
  // In localStorage mode, require both user and tokens
  const isAuthenticated = isHttpOnlyMode() ? !!user : (!!user && !!tokens);

  // Initialize auth state from stored tokens or cookies
  useEffect(() => {
    const initAuth = async () => {
      if (isHttpOnlyMode()) {
        // In httpOnly mode, validate session by calling /api/v1/auth/me
        // The cookies are sent automatically with the request
        const userResponse = await authApi.getCurrentUser();
        if (userResponse.data) {
          setUser(userResponse.data);
        }
        // If no user data, user is not authenticated (no redirect here, let route guards handle it)
      } else {
        // In localStorage mode, try to restore from stored tokens
        const storedTokens = getStoredTokens();
        if (storedTokens) {
          setTokens(storedTokens);
          // Attempt to refresh token to validate and get user info
          const success = await refreshTokenInternal(storedTokens.refresh_token);
          if (!success) {
            // Token refresh failed, clear stored tokens
            setStoredTokens(null);
            setTokens(null);
          }
        }
      }
      setIsLoading(false);
    };
    initAuth();
  }, []);

  const refreshTokenInternal = async (refreshTokenValue?: string): Promise<boolean> => {
    // In httpOnly mode, no token value needed (backend reads from cookie)
    // In localStorage mode, refresh token is required
    const response = await authApi.refresh(refreshTokenValue);
    if (response.data) {
      // In localStorage mode, store the new tokens
      // In httpOnly mode, tokens are set via cookies by backend
      if (isLocalStorageMode() && response.data.tokens) {
        const newTokens = response.data.tokens;
        setTokens(newTokens);
        setStoredTokens(newTokens);
      }

      // Fetch user data after successful token refresh
      const userResponse = await authApi.getCurrentUser();
      if (userResponse.data) {
        setUser(userResponse.data);
        return true;
      }
    }
    return false;
  };

  const refreshToken = useCallback(async (): Promise<boolean> => {
    // In httpOnly mode, no token value needed (backend reads from cookie)
    if (isHttpOnlyMode()) {
      return refreshTokenInternal();
    }
    // In localStorage mode, need refresh token from state
    if (!tokens?.refresh_token) return false;
    return refreshTokenInternal(tokens.refresh_token);
  }, [tokens]);

  const login = useCallback(async (credentials: LoginCredentials): Promise<{ success: boolean; error?: string }> => {
    const response = await authApi.login(credentials);
    if (response.data) {
      const { user: userData, tokens: newTokens } = response.data;
      setUser(userData);
      // In localStorage mode, store tokens in localStorage
      // In httpOnly mode, tokens are set via cookies by backend
      if (isLocalStorageMode() && newTokens) {
        setTokens(newTokens);
        setStoredTokens(newTokens);
      }
      return { success: true };
    }
    return { success: false, error: response.error || "Login failed" };
  }, []);

  const register = useCallback(async (credentials: RegisterCredentials): Promise<{ success: boolean; error?: string }> => {
    const response = await authApi.register(credentials);
    if (response.data) {
      const { user: userData, tokens: newTokens } = response.data;
      setUser(userData);
      // In localStorage mode, store tokens in localStorage
      // In httpOnly mode, tokens are set via cookies by backend
      if (isLocalStorageMode() && newTokens) {
        setTokens(newTokens);
        setStoredTokens(newTokens);
      }
      return { success: true };
    }
    return { success: false, error: response.error || "Registration failed" };
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    // Always call logout API to clear cookies/session on backend
    // In httpOnly mode, this clears the httpOnly cookies
    // In localStorage mode, this invalidates the session
    if (isHttpOnlyMode() || tokens?.access_token) {
      await authApi.logout();
    }
    setUser(null);
    setTokens(null);
    // In localStorage mode, clear stored tokens
    if (isLocalStorageMode()) {
      setStoredTokens(null);
    }
    router.push("/login");
  }, [tokens, router]);

  const value: AuthContextValue = {
    user,
    isAuthenticated,
    isLoading,
    login,
    register,
    logout,
    refreshToken,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
