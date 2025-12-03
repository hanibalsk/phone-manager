"use client";

import { useState, useEffect } from "react";
import type { AdminSettings } from "@/types";
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
import { Button } from "@/components/ui/button";
import { RefreshCw, Save } from "lucide-react";
import { adminSettingsUpdateSchema, getFieldErrors } from "@/lib/schemas";

interface SettingsFormProps {
  settings: AdminSettings | null;
  loading: boolean;
  onRefresh: () => void;
  onSave: (settings: Partial<AdminSettings>) => Promise<void>;
}

export function SettingsForm({
  settings,
  loading,
  onRefresh,
  onSave,
}: SettingsFormProps) {
  const [formData, setFormData] = useState<Partial<AdminSettings>>({});
  const [saving, setSaving] = useState(false);
  const [hasChanges, setHasChanges] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (settings) {
      setFormData({
        unlock_pin: settings.unlock_pin,
        default_daily_limit_minutes: settings.default_daily_limit_minutes,
        notifications_enabled: settings.notifications_enabled,
        auto_approve_unlock_requests: settings.auto_approve_unlock_requests,
      });
    }
  }, [settings]);

  const handleChange = (field: keyof AdminSettings, value: string | number | boolean) => {
    const newData = { ...formData, [field]: value };
    setFormData(newData);
    setHasChanges(true);
    // Clear field error on change
    if (errors[field]) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next[field];
        return next;
      });
    }
  };

  const handleSubmit = async () => {
    // Validate before submitting
    const validationErrors = getFieldErrors(adminSettingsUpdateSchema, formData);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setSaving(true);
    try {
      await onSave(formData);
      setHasChanges(false);
      setErrors({});
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Admin Settings</CardTitle>
            <CardDescription>
              Configure global settings for parental controls
            </CardDescription>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={onRefresh}
            disabled={loading}
          >
            <RefreshCw
              className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="flex items-center justify-center py-8">
            <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <div className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="unlock_pin">Unlock PIN</Label>
              <Input
                id="unlock_pin"
                type="password"
                placeholder="Enter PIN"
                value={formData.unlock_pin || ""}
                onChange={(e) => handleChange("unlock_pin", e.target.value)}
                aria-invalid={!!errors.unlock_pin}
                aria-describedby={errors.unlock_pin ? "unlock_pin-error" : undefined}
              />
              {errors.unlock_pin ? (
                <p id="unlock_pin-error" className="text-xs text-destructive">
                  {errors.unlock_pin}
                </p>
              ) : (
                <p className="text-xs text-muted-foreground">
                  PIN required to access admin features on the device
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="default_daily_limit_minutes">Default Daily Limit (minutes)</Label>
              <Input
                id="default_daily_limit_minutes"
                type="number"
                min={1}
                max={1440}
                value={formData.default_daily_limit_minutes || 60}
                onChange={(e) =>
                  handleChange("default_daily_limit_minutes", parseInt(e.target.value))
                }
                aria-invalid={!!errors.default_daily_limit_minutes}
                aria-describedby={errors.default_daily_limit_minutes ? "default_daily_limit_minutes-error" : undefined}
              />
              {errors.default_daily_limit_minutes ? (
                <p id="default_daily_limit_minutes-error" className="text-xs text-destructive">
                  {errors.default_daily_limit_minutes}
                </p>
              ) : (
                <p className="text-xs text-muted-foreground">
                  Default time limit applied to new apps
                </p>
              )}
            </div>

            <div className="flex items-center justify-between border rounded-lg p-4">
              <div>
                <Label htmlFor="notifications_enabled">Push Notifications</Label>
                <p className="text-sm text-muted-foreground">
                  Receive notifications for unlock requests
                </p>
              </div>
              <input
                id="notifications_enabled"
                type="checkbox"
                className="h-4 w-4"
                checked={formData.notifications_enabled || false}
                onChange={(e) =>
                  handleChange("notifications_enabled", e.target.checked)
                }
              />
            </div>

            <div className="flex items-center justify-between border rounded-lg p-4">
              <div>
                <Label htmlFor="auto_approve_unlock_requests">Auto-Approve Requests</Label>
                <p className="text-sm text-muted-foreground">
                  Automatically approve all unlock requests
                </p>
              </div>
              <input
                id="auto_approve_unlock_requests"
                type="checkbox"
                className="h-4 w-4"
                checked={formData.auto_approve_unlock_requests || false}
                onChange={(e) =>
                  handleChange("auto_approve_unlock_requests", e.target.checked)
                }
              />
            </div>
          </div>
        )}
      </CardContent>
      <CardFooter>
        <Button
          onClick={handleSubmit}
          disabled={saving || !hasChanges}
          className="ml-auto"
        >
          <Save className="h-4 w-4 mr-2" />
          {saving ? "Saving..." : "Save Changes"}
        </Button>
      </CardFooter>
    </Card>
  );
}
