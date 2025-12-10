"use client";

import type { DevicePlatform } from "@/types";
import { Smartphone } from "lucide-react";

interface DevicePlatformBadgeProps {
  platform: DevicePlatform;
}

export function DevicePlatformBadge({ platform }: DevicePlatformBadgeProps) {
  const config = {
    android: {
      label: "Android",
      className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
    },
    ios: {
      label: "iOS",
      className: "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400",
    },
  };

  const { label, className } = config[platform];

  return (
    <span
      className={`inline-flex items-center gap-1 px-2 py-1 text-xs font-medium rounded-full ${className}`}
    >
      <Smartphone className="h-3 w-3" />
      {label}
    </span>
  );
}
