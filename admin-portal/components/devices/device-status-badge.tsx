"use client";

import { Badge } from "@/components/ui/badge";

type DeviceStatus = "active" | "inactive" | "offline";

interface DeviceStatusBadgeProps {
  lastSeen: string;
}

function calculateStatus(lastSeen: string): DeviceStatus {
  const lastSeenDate = new Date(lastSeen);
  const now = new Date();
  const diffMs = now.getTime() - lastSeenDate.getTime();
  const diffHours = diffMs / (1000 * 60 * 60);

  if (diffHours < 1) {
    return "active";
  } else if (diffHours < 24) {
    return "inactive";
  } else {
    return "offline";
  }
}

const statusConfig: Record<
  DeviceStatus,
  { label: string; variant: "success" | "warning" | "destructive" }
> = {
  active: { label: "Active", variant: "success" },
  inactive: { label: "Inactive", variant: "warning" },
  offline: { label: "Offline", variant: "destructive" },
};

export function DeviceStatusBadge({ lastSeen }: DeviceStatusBadgeProps) {
  const status = calculateStatus(lastSeen);
  const config = statusConfig[status];

  return <Badge variant={config.variant}>{config.label}</Badge>;
}

export { calculateStatus };
