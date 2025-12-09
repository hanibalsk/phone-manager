import type { OrganizationFeatures } from "@/types";

/**
 * Feature configuration for organizations
 */
export interface FeatureConfig {
  key: keyof OrganizationFeatures;
  label: string;
  description: string;
}

/**
 * Configuration for all available organization features
 */
export const FEATURE_CONFIGS: FeatureConfig[] = [
  {
    key: "geofences",
    label: "Geofences",
    description: "Enable geofence-based location tracking and alerts",
  },
  {
    key: "proximity_alerts",
    label: "Proximity Alerts",
    description: "Receive alerts when devices enter or leave specified areas",
  },
  {
    key: "webhooks",
    label: "Webhooks",
    description: "Allow integration with external services via webhooks",
  },
  {
    key: "trips",
    label: "Trips",
    description: "Enable trip tracking and history",
  },
  {
    key: "movement_tracking",
    label: "Movement Tracking",
    description: "Track device movement patterns and history",
  },
];

/**
 * Organization status options
 */
export const ORGANIZATION_STATUSES = [
  "active",
  "suspended",
  "pending",
  "archived",
] as const;

/**
 * Organization type options
 */
export const ORGANIZATION_TYPES = [
  "enterprise",
  "small_business",
  "personal",
] as const;
