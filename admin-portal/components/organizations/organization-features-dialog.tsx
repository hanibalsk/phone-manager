"use client";

import { useState, useId } from "react";
import type { Organization, OrganizationFeatures } from "@/types";
import { organizationsApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { X, ToggleLeft, AlertCircle } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface OrganizationFeaturesDialogProps {
  organization: Organization;
  onSuccess: () => void;
  onCancel: () => void;
}

interface FeatureConfig {
  key: keyof OrganizationFeatures;
  label: string;
  description: string;
}

const FEATURE_CONFIGS: FeatureConfig[] = [
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

export function OrganizationFeaturesDialog({ organization, onSuccess, onCancel }: OrganizationFeaturesDialogProps) {
  const [features, setFeatures] = useState<OrganizationFeatures>({ ...organization.features });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onCancel });
  const titleId = useId();
  const descriptionId = useId();

  const handleToggle = (key: keyof OrganizationFeatures) => {
    setFeatures((prev) => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Check if any features changed
    const changes: Partial<OrganizationFeatures> = {};
    for (const config of FEATURE_CONFIGS) {
      if (features[config.key] !== organization.features[config.key]) {
        changes[config.key] = features[config.key];
      }
    }

    if (Object.keys(changes).length === 0) {
      onSuccess();
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const result = await organizationsApi.updateFeatures(organization.id, changes);

      if (result.error) {
        setError(result.error);
        return;
      }

      onSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update features");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
    >
      <Card ref={dialogRef} className="w-full max-w-md mx-4">
        <form onSubmit={handleSubmit}>
          <CardHeader className="relative">
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="absolute right-4 top-4"
              onClick={onCancel}
              aria-label="Close dialog"
            >
              <X className="h-4 w-4" aria-hidden="true" />
            </Button>
            <CardTitle id={titleId} className="flex items-center gap-2">
              <ToggleLeft className="h-5 w-5" aria-hidden="true" />
              Manage Features
            </CardTitle>
            <CardDescription id={descriptionId}>
              Enable or disable features for {organization.name}
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            {error && (
              <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
                <AlertCircle className="h-4 w-4" />
                {error}
              </div>
            )}

            <div className="space-y-4">
              {FEATURE_CONFIGS.map((config) => (
                <div
                  key={config.key}
                  className="flex items-start justify-between gap-4 p-3 border rounded-lg"
                >
                  <div className="space-y-1">
                    <Label htmlFor={config.key} className="font-medium">
                      {config.label}
                    </Label>
                    <p className="text-xs text-muted-foreground">
                      {config.description}
                    </p>
                  </div>
                  <Switch
                    id={config.key}
                    checked={features[config.key]}
                    onCheckedChange={() => handleToggle(config.key)}
                  />
                </div>
              ))}
            </div>
          </CardContent>

          <CardFooter className="flex justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={onCancel}
              disabled={submitting}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Saving..." : "Save Features"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
