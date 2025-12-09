"use client";

import { Badge } from "@/components/ui/badge";
import type { OrganizationStatus } from "@/types";

interface OrganizationStatusBadgeProps {
  status: OrganizationStatus;
}

const statusConfig: Record<OrganizationStatus, { label: string; variant: "success" | "destructive" | "warning" | "secondary" }> = {
  active: { label: "Active", variant: "success" },
  suspended: { label: "Suspended", variant: "destructive" },
  pending: { label: "Pending", variant: "warning" },
  archived: { label: "Archived", variant: "secondary" },
};

export function OrganizationStatusBadge({ status }: OrganizationStatusBadgeProps) {
  const config = statusConfig[status];

  return (
    <Badge variant={config.variant}>
      {config.label}
    </Badge>
  );
}
