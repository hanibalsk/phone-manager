"use client";

import { Clock, Ban } from "lucide-react";

interface LimitTypeBadgeProps {
  type: "time" | "blocked";
}

export function LimitTypeBadge({ type }: LimitTypeBadgeProps) {
  if (type === "blocked") {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2 py-1 text-xs font-medium text-red-800">
        <Ban className="h-3 w-3" />
        Blocked
      </span>
    );
  }

  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-2 py-1 text-xs font-medium text-blue-800">
      <Clock className="h-3 w-3" />
      Time Limit
    </span>
  );
}
