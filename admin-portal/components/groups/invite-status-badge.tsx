"use client";

import type { InviteStatus } from "@/types";

interface InviteStatusBadgeProps {
  status: InviteStatus;
}

const statusConfig: Record<InviteStatus, { label: string; className: string }> = {
  pending: {
    label: "Pending",
    className: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  },
  accepted: {
    label: "Accepted",
    className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  },
  expired: {
    label: "Expired",
    className: "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400",
  },
  revoked: {
    label: "Revoked",
    className: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
  },
};

export function InviteStatusBadge({ status }: InviteStatusBadgeProps) {
  const config = statusConfig[status];

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.className}`}
    >
      {config.label}
    </span>
  );
}
