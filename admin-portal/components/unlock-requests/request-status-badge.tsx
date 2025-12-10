"use client";

import type { AdminUnlockRequestStatus } from "@/types";
import { Clock, CheckCircle, XCircle, Timer, Ban } from "lucide-react";

const statusConfig: Record<
  AdminUnlockRequestStatus,
  { label: string; className: string; Icon: React.ComponentType<{ className?: string }> }
> = {
  pending: {
    label: "Pending",
    className: "bg-yellow-100 text-yellow-800",
    Icon: Clock,
  },
  approved: {
    label: "Approved",
    className: "bg-green-100 text-green-800",
    Icon: CheckCircle,
  },
  denied: {
    label: "Denied",
    className: "bg-red-100 text-red-800",
    Icon: XCircle,
  },
  expired: {
    label: "Expired",
    className: "bg-gray-100 text-gray-800",
    Icon: Timer,
  },
  cancelled: {
    label: "Cancelled",
    className: "bg-gray-100 text-gray-600",
    Icon: Ban,
  },
};

interface RequestStatusBadgeProps {
  status: AdminUnlockRequestStatus;
}

export function RequestStatusBadge({ status }: RequestStatusBadgeProps) {
  const config = statusConfig[status] || statusConfig.pending;
  const { Icon } = config;

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ${config.className}`}
    >
      <Icon className="h-3 w-3" />
      {config.label}
    </span>
  );
}
