"use client";

import type { GroupStatus } from "@/types";

interface GroupStatusBadgeProps {
  status: GroupStatus;
}

const statusConfig: Record<GroupStatus, { label: string; className: string }> = {
  active: {
    label: "Active",
    className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  },
  suspended: {
    label: "Suspended",
    className: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
  },
  archived: {
    label: "Archived",
    className: "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400",
  },
};

export function GroupStatusBadge({ status }: GroupStatusBadgeProps) {
  const config = statusConfig[status];

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.className}`}
    >
      {config.label}
    </span>
  );
}
