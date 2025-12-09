"use client";

import type { ReactNode } from "react";
import { usePermissions } from "@/hooks/use-permissions";
import { ShieldX } from "lucide-react";

interface PermissionGuardProps {
  /**
   * Single permission code or array of permission codes to check.
   * If array, behavior depends on `requireAll` prop.
   */
  permission: string | string[];

  /**
   * When true, user must have ALL permissions in the array.
   * When false (default), user needs ANY of the permissions.
   */
  requireAll?: boolean;

  /**
   * Content to render when user has permission
   */
  children: ReactNode;

  /**
   * Content to render when user lacks permission.
   * If not provided, renders nothing.
   */
  fallback?: ReactNode;

  /**
   * When true, shows a permission denied message instead of nothing.
   * Ignored if fallback is provided.
   */
  showDenied?: boolean;
}

/**
 * Conditionally renders children based on user permissions.
 *
 * @example
 * // Single permission
 * <PermissionGuard permission="users.create">
 *   <CreateUserButton />
 * </PermissionGuard>
 *
 * @example
 * // Any of multiple permissions
 * <PermissionGuard permission={["users.update", "users.delete"]}>
 *   <UserActionsMenu />
 * </PermissionGuard>
 *
 * @example
 * // All permissions required
 * <PermissionGuard permission={["users.read", "users.update"]} requireAll>
 *   <UserEditForm />
 * </PermissionGuard>
 *
 * @example
 * // With fallback content
 * <PermissionGuard permission="admin.access" fallback={<UpgradePrompt />}>
 *   <AdminPanel />
 * </PermissionGuard>
 */
export function PermissionGuard({
  permission,
  requireAll = false,
  children,
  fallback,
  showDenied = false,
}: PermissionGuardProps) {
  const { hasPermission, hasAnyPermission, hasAllPermissions } = usePermissions();

  // Check permissions
  let hasAccess: boolean;

  if (typeof permission === "string") {
    hasAccess = hasPermission(permission);
  } else if (requireAll) {
    hasAccess = hasAllPermissions(permission);
  } else {
    hasAccess = hasAnyPermission(permission);
  }

  // User has access - render children
  if (hasAccess) {
    return <>{children}</>;
  }

  // User lacks access - render fallback or denied message
  if (fallback) {
    return <>{fallback}</>;
  }

  if (showDenied) {
    return <PermissionDenied />;
  }

  // Render nothing
  return null;
}

/**
 * Standard permission denied component
 */
function PermissionDenied() {
  return (
    <div className="flex flex-col items-center justify-center py-8 text-center">
      <div className="p-3 bg-destructive/10 rounded-full mb-4">
        <ShieldX className="h-8 w-8 text-destructive" />
      </div>
      <h3 className="text-lg font-medium mb-1">Access Denied</h3>
      <p className="text-sm text-muted-foreground max-w-sm">
        You do not have permission to access this content.
        Contact your administrator if you believe this is an error.
      </p>
    </div>
  );
}

/**
 * Higher-order component version of PermissionGuard for wrapping components
 */
export function withPermission<P extends object>(
  WrappedComponent: React.ComponentType<P>,
  permission: string | string[],
  options?: { requireAll?: boolean; fallback?: ReactNode }
) {
  const WithPermissionComponent = (props: P) => (
    <PermissionGuard
      permission={permission}
      requireAll={options?.requireAll}
      fallback={options?.fallback}
    >
      <WrappedComponent {...props} />
    </PermissionGuard>
  );

  WithPermissionComponent.displayName = `WithPermission(${WrappedComponent.displayName || WrappedComponent.name || "Component"})`;

  return WithPermissionComponent;
}
