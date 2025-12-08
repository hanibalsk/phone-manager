"use client";

import { Badge } from "@/components/ui/badge";
import type { UserRole } from "@/types";

interface UserRoleBadgeProps {
  role: UserRole;
}

const roleConfig: Record<UserRole, { label: string; variant: "default" | "secondary" | "outline" }> = {
  super_admin: { label: "Super Admin", variant: "default" },
  org_admin: { label: "Org Admin", variant: "default" },
  org_manager: { label: "Manager", variant: "secondary" },
  support: { label: "Support", variant: "secondary" },
  viewer: { label: "Viewer", variant: "outline" },
};

export function UserRoleBadge({ role }: UserRoleBadgeProps) {
  const config = roleConfig[role];

  return (
    <Badge variant={config.variant}>
      {config.label}
    </Badge>
  );
}
