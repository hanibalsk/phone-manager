"use client";

import type { Role, PermissionCategory } from "@/types";
import { Badge } from "@/components/ui/badge";
import { Check, X } from "lucide-react";

interface PermissionMatrixProps {
  roles: Role[];
}

const CATEGORY_LABELS: Record<PermissionCategory, string> = {
  users: "Users",
  organizations: "Organizations",
  devices: "Devices",
  locations: "Locations",
  geofences: "Geofences",
  alerts: "Alerts",
  webhooks: "Webhooks",
  trips: "Trips",
  groups: "Groups",
  enrollment: "Enrollment",
  audit: "Audit",
  config: "Configuration",
  reports: "Reports",
  api_keys: "API Keys",
};

const PERMISSION_ACTIONS = ["read", "create", "update", "delete"] as const;

export function PermissionMatrix({ roles }: PermissionMatrixProps) {
  // Get all unique categories across all roles
  const allCategories = Array.from(
    new Set(
      roles.flatMap((role) => role.permissions.map((p) => p.category))
    )
  ).sort((a, b) => {
    const keys = Object.keys(CATEGORY_LABELS) as PermissionCategory[];
    return keys.indexOf(a) - keys.indexOf(b);
  });

  // Check if a role has a specific permission
  const hasPermission = (role: Role, category: PermissionCategory, action: string): boolean => {
    return role.permissions.some(
      (p) => p.category === category && p.code.toLowerCase().includes(action)
    );
  };

  if (roles.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        No roles to compare
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="border-b">
            <th className="text-left py-3 px-4 font-medium sticky left-0 bg-background">
              Category / Action
            </th>
            {roles.map((role) => (
              <th key={role.id} className="text-center py-3 px-2 font-medium min-w-[100px]">
                <div className="flex flex-col items-center gap-1">
                  <span>{role.name}</span>
                  <Badge variant={role.is_system ? "default" : "outline"} className="text-xs">
                    {role.is_system ? "System" : "Custom"}
                  </Badge>
                </div>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {allCategories.map((category) => (
            <>
              <tr key={`${category}-header`} className="bg-muted/50">
                <td
                  colSpan={roles.length + 1}
                  className="py-2 px-4 font-medium text-muted-foreground sticky left-0 bg-muted/50"
                >
                  {CATEGORY_LABELS[category] || category}
                </td>
              </tr>
              {PERMISSION_ACTIONS.map((action) => (
                <tr key={`${category}-${action}`} className="border-b hover:bg-muted/30">
                  <td className="py-2 px-4 pl-8 capitalize sticky left-0 bg-background">
                    {action}
                  </td>
                  {roles.map((role) => {
                    const has = hasPermission(role, category, action);
                    return (
                      <td key={`${role.id}-${category}-${action}`} className="text-center py-2 px-2">
                        {has ? (
                          <Check className="h-4 w-4 text-green-500 mx-auto" aria-label="Has permission" />
                        ) : (
                          <X className="h-4 w-4 text-muted-foreground/30 mx-auto" aria-label="No permission" />
                        )}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </>
          ))}
        </tbody>
      </table>
    </div>
  );
}
