"use client";

import type { WebhookStatus } from "@/types";
import { CheckCircle2, PauseCircle, XCircle } from "lucide-react";

interface WebhookStatusBadgeProps {
  status: WebhookStatus;
}

const statusConfig: Record<WebhookStatus, { label: string; icon: typeof CheckCircle2; className: string }> = {
  active: {
    label: "Active",
    icon: CheckCircle2,
    className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  },
  paused: {
    label: "Paused",
    icon: PauseCircle,
    className: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
  },
  failed: {
    label: "Failed",
    icon: XCircle,
    className: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
  },
};

export function WebhookStatusBadge({ status }: WebhookStatusBadgeProps) {
  const config = statusConfig[status];
  const Icon = config.icon;

  return (
    <span
      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${config.className}`}
    >
      <Icon className="h-3 w-3" />
      {config.label}
    </span>
  );
}
