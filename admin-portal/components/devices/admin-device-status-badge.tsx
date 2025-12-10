"use client";

import type { AdminDeviceStatus } from "@/types";

interface AdminDeviceStatusBadgeProps {
  status: AdminDeviceStatus;
}

export function AdminDeviceStatusBadge({ status }: AdminDeviceStatusBadgeProps) {
  const config: Record<AdminDeviceStatus, { label: string; className: string }> = {
    active: {
      label: "Active",
      className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
    },
    suspended: {
      label: "Suspended",
      className: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
    },
    offline: {
      label: "Offline",
      className: "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400",
    },
    pending: {
      label: "Pending",
      className: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
    },
  };

  const { label, className } = config[status];

  return (
    <span
      className={`inline-flex items-center px-2 py-1 text-xs font-medium rounded-full ${className}`}
    >
      {label}
    </span>
  );
}
