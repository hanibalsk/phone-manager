"use client";

import type { TripStatus } from "@/types";
import { Play, CheckCircle2, Pause } from "lucide-react";

interface TripStatusBadgeProps {
  status: TripStatus;
}

const statusConfig: Record<TripStatus, { label: string; icon: typeof Play; className: string }> = {
  in_progress: {
    label: "In Progress",
    icon: Play,
    className: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  },
  completed: {
    label: "Completed",
    icon: CheckCircle2,
    className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  },
  paused: {
    label: "Paused",
    icon: Pause,
    className: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
  },
};

export function TripStatusBadge({ status }: TripStatusBadgeProps) {
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
