"use client";

import type { EnrollmentTokenStatus } from "@/types";

interface TokenStatusBadgeProps {
  status: EnrollmentTokenStatus;
}

export function TokenStatusBadge({ status }: TokenStatusBadgeProps) {
  const config: Record<EnrollmentTokenStatus, { label: string; className: string }> = {
    active: {
      label: "Active",
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
    exhausted: {
      label: "Exhausted",
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
