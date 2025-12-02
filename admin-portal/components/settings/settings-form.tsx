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

  useEffect(() => {
    if (settings) {
      setFormData({
        unlockPin: settings.unlockPin,
        defaultDailyLimitMinutes: settings.defaultDailyLimitMinutes,
        notificationsEnabled: settings.notificationsEnabled,
        autoApproveUnlockRequests: settings.autoApproveUnlockRequests,
      });
    }
  }, [settings]);

  const handleChange = (field: keyof AdminSettings, value: string | number | boolean) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setHasChanges(true);
  };

  const handleSubmit = async () => {
    setSaving(true);
    try {
      await onSave(formData);
      setHasChanges(false);
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
              <Label htmlFor="unlockPin">Unlock PIN</Label>
              <Input
                id="unlockPin"
                type="password"
                placeholder="Enter PIN"
                value={formData.unlockPin || ""}
                onChange={(e) => handleChange("unlockPin", e.target.value)}
              />
              <p className="text-xs text-muted-foreground">
                PIN required to access admin features on the device
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="defaultLimit">Default Daily Limit (minutes)</Label>
              <Input
                id="defaultLimit"
                type="number"
                min={1}
                max={1440}
                value={formData.defaultDailyLimitMinutes || 60}
                onChange={(e) =>
                  handleChange("defaultDailyLimitMinutes", parseInt(e.target.value))
                }
              />
              <p className="text-xs text-muted-foreground">
                Default time limit applied to new apps
              </p>
            </div>

            <div className="flex items-center justify-between border rounded-lg p-4">
              <div>
                <Label htmlFor="notifications">Push Notifications</Label>
                <p className="text-sm text-muted-foreground">
                  Receive notifications for unlock requests
                </p>
              </div>
              <input
                id="notifications"
                type="checkbox"
                className="h-4 w-4"
                checked={formData.notificationsEnabled || false}
                onChange={(e) =>
                  handleChange("notificationsEnabled", e.target.checked)
                }
              />
            </div>

            <div className="flex items-center justify-between border rounded-lg p-4">
              <div>
                <Label htmlFor="autoApprove">Auto-Approve Requests</Label>
                <p className="text-sm text-muted-foreground">
                  Automatically approve all unlock requests
                </p>
              </div>
              <input
                id="autoApprove"
                type="checkbox"
                className="h-4 w-4"
                checked={formData.autoApproveUnlockRequests || false}
                onChange={(e) =>
                  handleChange("autoApproveUnlockRequests", e.target.checked)
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
