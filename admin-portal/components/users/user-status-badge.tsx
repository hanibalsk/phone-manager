"use client";

import { Badge } from "@/components/ui/badge";
import type { UserStatus } from "@/types";

interface UserStatusBadgeProps {
  status: UserStatus;
}

const statusConfig: Record<UserStatus, { label: string; variant: "success" | "destructive" | "warning" | "secondary" }> = {
  active: { label: "Active", variant: "success" },
  suspended: { label: "Suspended", variant: "destructive" },
  pending_verification: { label: "Pending", variant: "warning" },
  locked: { label: "Locked", variant: "secondary" },
};

export function UserStatusBadge({ status }: UserStatusBadgeProps) {
  const config = statusConfig[status];

  return (
    <Badge variant={config.variant}>
      {config.label}
    </Badge>
  );
}
