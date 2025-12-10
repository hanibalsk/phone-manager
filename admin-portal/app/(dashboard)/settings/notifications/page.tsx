"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Bell,
  Mail,
  Monitor,
  User,
  Building,
  Smartphone,
  Shield,
  Settings,
  Clock,
  Save,
  RefreshCw,
  ArrowLeft,
} from "lucide-react";
import Link from "next/link";
import { useApi } from "@/hooks/use-api";
import { notificationsApi } from "@/lib/api-client";
import type { NotificationPreferences, NotificationCategory } from "@/types";

const CATEGORY_CONFIG: Record<
  NotificationCategory,
  { icon: React.ComponentType<{ className?: string }>; label: string; description: string }
> = {
  user: {
    icon: User,
    label: "User Events",
    description: "User creation, updates, role changes",
  },
  organization: {
    icon: Building,
    label: "Organization Events",
    description: "Organization updates, membership changes",
  },
  device: {
    icon: Smartphone,
    label: "Device Events",
    description: "Device registration, status changes, alerts",
  },
  security: {
    icon: Shield,
    label: "Security Events",
    description: "Login attempts, permission changes, security alerts",
  },
  system: {
    icon: Settings,
    label: "System Events",
    description: "System updates, maintenance, configuration changes",
  },
  audit: {
    icon: Clock,
    label: "Audit Events",
    description: "Compliance reports, audit log alerts",
  },
};

const DIGEST_OPTIONS = [
  { value: "realtime", label: "Real-time" },
  { value: "daily", label: "Daily digest" },
  { value: "weekly", label: "Weekly digest" },
  { value: "never", label: "Never" },
];

const DEFAULT_PREFERENCES: NotificationPreferences = {
  email_enabled: true,
  email_digest: "daily",
  in_app_enabled: true,
  categories: {
    user: true,
    organization: true,
    device: true,
    security: true,
    system: false,
    audit: true,
  },
};

export default function NotificationSettingsPage() {
  const [preferences, setPreferences] = useState<NotificationPreferences>(DEFAULT_PREFERENCES);
  const [hasChanges, setHasChanges] = useState(false);

  const { execute: fetchPreferences, loading } = useApi<NotificationPreferences>();
  const { execute: savePreferences, loading: saving } = useApi<NotificationPreferences>();

  const loadPreferences = async () => {
    const result = await fetchPreferences(() => notificationsApi.getPreferences());
    if (result) {
      setPreferences(result);
      setHasChanges(false);
    }
  };

  useEffect(() => {
    loadPreferences();
  }, []);

  const handleSave = async () => {
    const result = await savePreferences(() =>
      notificationsApi.updatePreferences(preferences)
    );
    if (result) {
      setPreferences(result);
      setHasChanges(false);
    }
  };

  const updatePreference = <K extends keyof NotificationPreferences>(
    key: K,
    value: NotificationPreferences[K]
  ) => {
    setPreferences((prev) => ({ ...prev, [key]: value }));
    setHasChanges(true);
  };

  const updateCategoryPreference = (
    category: NotificationCategory,
    value: boolean
  ) => {
    setPreferences((prev) => ({
      ...prev,
      categories: {
        ...prev.categories,
        [category]: value,
      },
    }));
    setHasChanges(true);
  };

  const toggleAllCategories = (enabled: boolean) => {
    const newCategories: Record<NotificationCategory, boolean> = {
      user: enabled,
      organization: enabled,
      device: enabled,
      security: enabled,
      system: enabled,
      audit: enabled,
    };
    setPreferences((prev) => ({ ...prev, categories: newCategories }));
    setHasChanges(true);
  };

  return (
    <div className="space-y-6" data-testid="notification-settings-page">
      {/* Header */}
      <div className="flex items-center justify-between" data-testid="notification-settings-header">
        <div>
          <div className="flex items-center gap-2 text-sm text-muted-foreground mb-2">
            <Link href="/" className="hover:text-foreground">
              Dashboard
            </Link>
            <span>/</span>
            <Link href="/settings" className="hover:text-foreground">
              Settings
            </Link>
            <span>/</span>
            <span>Notifications</span>
          </div>
          <h1 className="text-3xl font-bold">Notification Settings</h1>
          <p className="text-muted-foreground mt-1">
            Configure how and when you receive notifications
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link href="/settings">
            <Button variant="outline" data-testid="notification-settings-back-button">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Settings
            </Button>
          </Link>
          <Button onClick={handleSave} disabled={!hasChanges || saving} data-testid="notification-settings-save-button">
            {saving ? (
              <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
            ) : (
              <Save className="h-4 w-4 mr-2" />
            )}
            Save Changes
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-12" data-testid="notification-settings-loading">
          <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <div className="grid gap-6" data-testid="notification-settings-content">
          {/* Global Email Settings */}
          <Card data-testid="notification-settings-email-card">
            <CardHeader>
              <div className="flex items-center gap-2">
                <Mail className="h-5 w-5 text-muted-foreground" />
                <CardTitle>Email Notifications</CardTitle>
              </div>
              <CardDescription>
                Configure email notification delivery
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>Enable Email Notifications</Label>
                  <p className="text-sm text-muted-foreground">
                    Receive notifications via email
                  </p>
                </div>
                <Switch
                  checked={preferences.email_enabled}
                  onCheckedChange={(checked) =>
                    updatePreference("email_enabled", checked)
                  }
                  data-testid="notification-settings-email-toggle"
                />
              </div>

              {preferences.email_enabled && (
                <div className="space-y-2" data-testid="notification-settings-email-digest">
                  <Label>Digest Frequency</Label>
                  <Select
                    value={preferences.email_digest}
                    onValueChange={(value) =>
                      updatePreference("email_digest", value as NotificationPreferences["email_digest"])
                    }
                  >
                    <SelectTrigger className="w-48" data-testid="notification-settings-digest-select">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {DIGEST_OPTIONS.map((option) => (
                        <SelectItem key={option.value} value={option.value} data-testid={`notification-settings-digest-${option.value}`}>
                          {option.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <p className="text-sm text-muted-foreground">
                    Choose how often to receive email notifications
                  </p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Global In-App Settings */}
          <Card data-testid="notification-settings-inapp-card">
            <CardHeader>
              <div className="flex items-center gap-2">
                <Monitor className="h-5 w-5 text-muted-foreground" />
                <CardTitle>In-App Notifications</CardTitle>
              </div>
              <CardDescription>
                Configure in-app notification delivery
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>Enable In-App Notifications</Label>
                  <p className="text-sm text-muted-foreground">
                    Show notifications in the admin portal
                  </p>
                </div>
                <Switch
                  checked={preferences.in_app_enabled}
                  onCheckedChange={(checked) =>
                    updatePreference("in_app_enabled", checked)
                  }
                  data-testid="notification-settings-inapp-toggle"
                />
              </div>
            </CardContent>
          </Card>

          {/* Per-Category Settings */}
          <Card data-testid="notification-settings-categories-card">
            <CardHeader>
              <div className="flex items-center gap-2">
                <Bell className="h-5 w-5 text-muted-foreground" />
                <CardTitle>Category Preferences</CardTitle>
              </div>
              <CardDescription>
                Enable or disable notifications by category
              </CardDescription>
            </CardHeader>
            <CardContent>
              {/* Quick toggles */}
              <div className="flex items-center gap-4 pb-4 mb-4 border-b" data-testid="notification-settings-quick-actions">
                <span className="text-sm font-medium">Quick actions:</span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => toggleAllCategories(true)}
                  data-testid="notification-settings-enable-all"
                >
                  Enable all
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => toggleAllCategories(false)}
                  data-testid="notification-settings-disable-all"
                >
                  Disable all
                </Button>
              </div>

              {/* Category list */}
              <div className="space-y-4" data-testid="notification-settings-category-list">
                {(Object.keys(CATEGORY_CONFIG) as NotificationCategory[]).map(
                  (category) => {
                    const config = CATEGORY_CONFIG[category];
                    const Icon = config.icon;
                    const isEnabled = preferences.categories[category];

                    return (
                      <div
                        key={category}
                        className="flex items-center justify-between py-3 border-b last:border-0"
                        data-testid={`notification-settings-category-${category}`}
                      >
                        <div className="flex items-center gap-3">
                          <div className="p-2 bg-muted rounded-lg">
                            <Icon className="h-5 w-5 text-muted-foreground" />
                          </div>
                          <div>
                            <p className="font-medium">{config.label}</p>
                            <p className="text-sm text-muted-foreground">
                              {config.description}
                            </p>
                          </div>
                        </div>
                        <Switch
                          checked={isEnabled}
                          onCheckedChange={(checked) =>
                            updateCategoryPreference(category, checked)
                          }
                          data-testid={`notification-settings-category-${category}-toggle`}
                        />
                      </div>
                    );
                  }
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
