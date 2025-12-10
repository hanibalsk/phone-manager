"use client";

import type { GeofenceShape } from "@/types";
import { Circle, Pentagon } from "lucide-react";

interface GeofenceShapeBadgeProps {
  shape: GeofenceShape;
}

const shapeConfig: Record<GeofenceShape, { label: string; icon: typeof Circle; className: string }> = {
  circle: {
    label: "Circle",
    icon: Circle,
    className: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  },
  polygon: {
    label: "Polygon",
    icon: Pentagon,
    className: "bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400",
  },
};

export function GeofenceShapeBadge({ shape }: GeofenceShapeBadgeProps) {
  const config = shapeConfig[shape];
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
