"use client";

import type { WebhookEventType } from "@/types";
import { MapPin, Navigation, Users, Route, Smartphone } from "lucide-react";

interface WebhookEventBadgeProps {
  eventType: WebhookEventType;
  compact?: boolean;
}

const eventConfig: Record<WebhookEventType, { label: string; icon: typeof MapPin; className: string }> = {
  location_update: {
    label: "Location",
    icon: MapPin,
    className: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  },
  geofence_event: {
    label: "Geofence",
    icon: Navigation,
    className: "bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400",
  },
  proximity_alert: {
    label: "Proximity",
    icon: Users,
    className: "bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400",
  },
  trip_complete: {
    label: "Trip",
    icon: Route,
    className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  },
  device_status: {
    label: "Device",
    icon: Smartphone,
    className: "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400",
  },
};

export function WebhookEventBadge({ eventType, compact = false }: WebhookEventBadgeProps) {
  const config = eventConfig[eventType];
  const Icon = config.icon;

  if (compact) {
    return (
      <span
        className={`inline-flex items-center justify-center px-1.5 py-0.5 rounded text-xs ${config.className}`}
        title={config.label}
      >
        <Icon className="h-3 w-3" />
      </span>
    );
  }

  return (
    <span
      className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium ${config.className}`}
    >
      <Icon className="h-3 w-3" />
      {config.label}
    </span>
  );
}
