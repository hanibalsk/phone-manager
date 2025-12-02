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
  AuthState,
} from "@/types/auth";
import { authApi } from "@/lib/api-client";

const TOKEN_STORAGE_KEY = "auth_tokens";

interface AuthContextValue extends AuthState {
  login: (credentials: LoginCredentials) => Promise<{ success: boolean; error?: string }>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function getStoredTokens(): Tokens | null {
  if (typeof window === "undefined") return null;
  try {
    const stored = localStorage.getItem(TOKEN_STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch {
    return null;
  }
}

function setStoredTokens(tokens: Tokens | null): void {
  if (typeof window === "undefined") return;
  if (tokens) {
    localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(tokens));
  } else {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  }
}

export function getAccessToken(): string | null {
  const tokens = getStoredTokens();
  return tokens?.access_token ?? null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [tokens, setTokens] = useState<Tokens | null>(null);

  const isAuthenticated = !!user && !!tokens;

  // Initialize auth state from stored tokens
  useEffect(() => {
    const initAuth = async () => {
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
      setIsLoading(false);
    };
    initAuth();
  }, []);

  const refreshTokenInternal = async (refreshTokenValue: string): Promise<boolean> => {
    const response = await authApi.refresh(refreshTokenValue);
    if (response.data) {
      const newTokens = response.data.tokens;
      setTokens(newTokens);
      setStoredTokens(newTokens);

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
    if (!tokens?.refresh_token) return false;
    return refreshTokenInternal(tokens.refresh_token);
  }, [tokens]);

  const login = useCallback(async (credentials: LoginCredentials): Promise<{ success: boolean; error?: string }> => {
    const response = await authApi.login(credentials);
    if (response.data) {
      const { user: userData, tokens: newTokens } = response.data;
      setUser(userData);
      setTokens(newTokens);
      setStoredTokens(newTokens);
      return { success: true };
    }
    return { success: false, error: response.error || "Login failed" };
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    if (tokens?.access_token) {
      await authApi.logout();
    }
    setUser(null);
    setTokens(null);
    setStoredTokens(null);
    router.push("/login");
  }, [tokens, router]);

  const value: AuthContextValue = {
    user,
    isAuthenticated,
    isLoading,
    login,
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
