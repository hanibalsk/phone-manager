"use client";

import { useMemo, useCallback } from "react";
import { useAuth } from "@/contexts/auth-context";

/**
 * Hook for checking user permissions.
 * Permissions are derived from the user's roles and stored in the auth context.
 */
export function usePermissions() {
  const { user, isAuthenticated } = useAuth();

  // Get the user's permissions as a Set for efficient lookups
  const permissionSet = useMemo(() => {
    if (!user?.permissions) {
      return new Set<string>();
    }
    return new Set(user.permissions.map(p => p.toLowerCase()));
  }, [user?.permissions]);

  /**
   * Check if user has a specific permission
   */
  const hasPermission = useCallback(
    (permissionCode: string): boolean => {
      if (!isAuthenticated || !user) {
        return false;
      }

      // Super admin has all permissions
      if (user.roles?.some(r => r.role_code === "super_admin")) {
        return true;
      }

      return permissionSet.has(permissionCode.toLowerCase());
    },
    [isAuthenticated, user, permissionSet]
  );

  /**
   * Check if user has any of the specified permissions
   */
  const hasAnyPermission = useCallback(
    (permissionCodes: string[]): boolean => {
      if (!isAuthenticated || !user) {
        return false;
      }

      // Super admin has all permissions
      if (user.roles?.some(r => r.role_code === "super_admin")) {
        return true;
      }

      return permissionCodes.some(code =>
        permissionSet.has(code.toLowerCase())
      );
    },
    [isAuthenticated, user, permissionSet]
  );

  /**
   * Check if user has all of the specified permissions
   */
  const hasAllPermissions = useCallback(
    (permissionCodes: string[]): boolean => {
      if (!isAuthenticated || !user) {
        return false;
      }

      // Super admin has all permissions
      if (user.roles?.some(r => r.role_code === "super_admin")) {
        return true;
      }

      return permissionCodes.every(code =>
        permissionSet.has(code.toLowerCase())
      );
    },
    [isAuthenticated, user, permissionSet]
  );

  /**
   * Check if user has a specific role
   */
  const hasRole = useCallback(
    (roleCode: string): boolean => {
      if (!isAuthenticated || !user?.roles) {
        return false;
      }
      return user.roles.some(r => r.role_code === roleCode);
    },
    [isAuthenticated, user?.roles]
  );

  /**
   * Check if user is a super admin
   */
  const isSuperAdmin = useMemo(() => {
    return hasRole("super_admin");
  }, [hasRole]);

  /**
   * Check if user is an org admin (either super_admin or org_admin)
   */
  const isOrgAdmin = useMemo(() => {
    return hasRole("super_admin") || hasRole("org_admin");
  }, [hasRole]);

  /**
   * Get all user's permissions
   */
  const permissions = useMemo(() => {
    return user?.permissions || [];
  }, [user?.permissions]);

  /**
   * Get all user's roles
   */
  const roles = useMemo(() => {
    return user?.roles || [];
  }, [user?.roles]);

  return {
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    hasRole,
    isSuperAdmin,
    isOrgAdmin,
    permissions,
    roles,
    isAuthenticated,
  };
}
