"use client";

import type { GeofenceEventType } from "@/types";
import { ArrowDownToLine, ArrowUpFromLine, Clock } from "lucide-react";

interface EventTypeBadgeProps {
  type: GeofenceEventType;
}

const typeConfig: Record<GeofenceEventType, { label: string; icon: typeof ArrowDownToLine; className: string }> = {
  ENTER: {
    label: "Enter",
    icon: ArrowDownToLine,
    className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  },
  EXIT: {
    label: "Exit",
    icon: ArrowUpFromLine,
    className: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
  },
  DWELL: {
    label: "Dwell",
    icon: Clock,
    className: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  },
};

export function EventTypeBadge({ type }: EventTypeBadgeProps) {
  const config = typeConfig[type];
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
