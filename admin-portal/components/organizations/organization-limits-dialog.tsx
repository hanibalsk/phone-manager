"use client";

import { useState, useId } from "react";
import type { Organization } from "@/types";
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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { X, Settings, AlertCircle } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface OrganizationLimitsDialogProps {
  organization: Organization;
  onSuccess: () => void;
  onCancel: () => void;
}

interface FormErrors {
  max_users?: string;
  max_devices?: string;
  max_groups?: string;
  general?: string;
}

export function OrganizationLimitsDialog({ organization, onSuccess, onCancel }: OrganizationLimitsDialogProps) {
  const [maxUsers, setMaxUsers] = useState(organization.max_users.toString());
  const [maxDevices, setMaxDevices] = useState(organization.max_devices.toString());
  const [maxGroups, setMaxGroups] = useState(organization.max_groups.toString());
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<FormErrors>({});

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onCancel });
  const titleId = useId();
  const descriptionId = useId();

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    const maxUsersNum = parseInt(maxUsers, 10);
    const maxDevicesNum = parseInt(maxDevices, 10);
    const maxGroupsNum = parseInt(maxGroups, 10);

    if (isNaN(maxUsersNum) || maxUsersNum < 1) {
      newErrors.max_users = "Must be a positive number";
    }

    if (isNaN(maxDevicesNum) || maxDevicesNum < 1) {
      newErrors.max_devices = "Must be a positive number";
    }

    if (isNaN(maxGroupsNum) || maxGroupsNum < 0) {
      newErrors.max_groups = "Must be a non-negative number";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    setErrors({});

    try {
      const limits: { max_users?: number; max_devices?: number; max_groups?: number } = {};

      const maxUsersNum = parseInt(maxUsers, 10);
      const maxDevicesNum = parseInt(maxDevices, 10);
      const maxGroupsNum = parseInt(maxGroups, 10);

      if (maxUsersNum !== organization.max_users) {
        limits.max_users = maxUsersNum;
      }
      if (maxDevicesNum !== organization.max_devices) {
        limits.max_devices = maxDevicesNum;
      }
      if (maxGroupsNum !== organization.max_groups) {
        limits.max_groups = maxGroupsNum;
      }

      // Only call API if there are changes
      if (Object.keys(limits).length === 0) {
        onSuccess();
        return;
      }

      const result = await organizationsApi.updateLimits(organization.id, limits);

      if (result.error) {
        setErrors({ general: result.error });
        return;
      }

      onSuccess();
    } catch (error) {
      setErrors({
        general: error instanceof Error ? error.message : "Failed to update limits"
      });
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
              <Settings className="h-5 w-5" aria-hidden="true" />
              Configure Limits
            </CardTitle>
            <CardDescription id={descriptionId}>
              Set resource limits for {organization.name}
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            {errors.general && (
              <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
                <AlertCircle className="h-4 w-4" />
                {errors.general}
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="maxUsers">Maximum Users</Label>
              <Input
                id="maxUsers"
                type="number"
                min="1"
                value={maxUsers}
                onChange={(e) => {
                  setMaxUsers(e.target.value);
                  if (errors.max_users) setErrors({ ...errors, max_users: undefined });
                }}
                className={errors.max_users ? "border-destructive" : ""}
              />
              {errors.max_users && (
                <p className="text-sm text-destructive">{errors.max_users}</p>
              )}
              <p className="text-xs text-muted-foreground">
                Maximum number of users that can be added to this organization
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="maxDevices">Maximum Devices</Label>
              <Input
                id="maxDevices"
                type="number"
                min="1"
                value={maxDevices}
                onChange={(e) => {
                  setMaxDevices(e.target.value);
                  if (errors.max_devices) setErrors({ ...errors, max_devices: undefined });
                }}
                className={errors.max_devices ? "border-destructive" : ""}
              />
              {errors.max_devices && (
                <p className="text-sm text-destructive">{errors.max_devices}</p>
              )}
              <p className="text-xs text-muted-foreground">
                Maximum number of devices that can be enrolled
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="maxGroups">Maximum Groups</Label>
              <Input
                id="maxGroups"
                type="number"
                min="0"
                value={maxGroups}
                onChange={(e) => {
                  setMaxGroups(e.target.value);
                  if (errors.max_groups) setErrors({ ...errors, max_groups: undefined });
                }}
                className={errors.max_groups ? "border-destructive" : ""}
              />
              {errors.max_groups && (
                <p className="text-sm text-destructive">{errors.max_groups}</p>
              )}
              <p className="text-xs text-muted-foreground">
                Maximum number of device groups (0 = unlimited)
              </p>
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
              {submitting ? "Saving..." : "Save Limits"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
