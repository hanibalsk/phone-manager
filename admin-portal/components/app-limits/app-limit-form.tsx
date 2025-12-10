"use client";

import { useState, useEffect } from "react";
import type { AppLimit, CreateAppLimitRequest, Organization, AdminDevice, AdminGroup, TimeWindow, AppCategory } from "@/types";
import { appLimitsApi, organizationsApi, adminDevicesApi, adminGroupsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useRouter } from "next/navigation";
import {
  Shield,
  Save,
  ArrowLeft,
  Plus,
  Trash2,
  Clock,
  Ban,
} from "lucide-react";

const CATEGORIES: { value: AppCategory; label: string }[] = [
  { value: "social", label: "Social" },
  { value: "games", label: "Games" },
  { value: "productivity", label: "Productivity" },
  { value: "entertainment", label: "Entertainment" },
  { value: "education", label: "Education" },
  { value: "communication", label: "Communication" },
  { value: "other", label: "Other" },
];

const DAYS = [
  { value: "mon" as const, label: "Mon" },
  { value: "tue" as const, label: "Tue" },
  { value: "wed" as const, label: "Wed" },
  { value: "thu" as const, label: "Thu" },
  { value: "fri" as const, label: "Fri" },
  { value: "sat" as const, label: "Sat" },
  { value: "sun" as const, label: "Sun" },
];

interface AppLimitFormProps {
  limit?: AppLimit;
}

export function AppLimitForm({ limit }: AppLimitFormProps) {
  const router = useRouter();
  const isEditing = !!limit;

  const [name, setName] = useState(limit?.name || "");
  const [targetType, setTargetType] = useState<"app" | "category">(limit?.target_type || "app");
  const [targetValue, setTargetValue] = useState(limit?.target_value || "");
  const [limitType, setLimitType] = useState<"time" | "blocked">(limit?.limit_type || "time");
  const [dailyLimitMinutes, setDailyLimitMinutes] = useState<string>(
    limit?.daily_limit_minutes?.toString() || ""
  );
  const [weeklyLimitMinutes, setWeeklyLimitMinutes] = useState<string>(
    limit?.weekly_limit_minutes?.toString() || ""
  );
  const [timeWindows, setTimeWindows] = useState<TimeWindow[]>(
    limit?.time_windows || []
  );
  const [deviceId, setDeviceId] = useState(limit?.device_id || "");
  const [groupId, setGroupId] = useState(limit?.group_id || "");
  const [organizationId, setOrganizationId] = useState(limit?.organization_id || "");
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();
  const { data: groupsData, execute: fetchGroups } = useApi<{ items: AdminGroup[] }>();
  const { execute: saveLimit, loading: saving } = useApi<AppLimit>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
    fetchDevices(() => adminDevicesApi.list({ limit: 100 }));
    fetchGroups(() => adminGroupsApi.list({ limit: 100 }));
  }, [fetchOrgs, fetchDevices, fetchGroups]);

  // Filter devices and groups by organization
  const filteredDevices = organizationId
    ? devicesData?.items?.filter((d) => d.organization_id === organizationId)
    : devicesData?.items;

  const filteredGroups = organizationId
    ? groupsData?.items?.filter((g) => g.organization_id === organizationId)
    : groupsData?.items;

  const addTimeWindow = () => {
    setTimeWindows([
      ...timeWindows,
      {
        start_time: "09:00",
        end_time: "17:00",
        days: ["mon", "tue", "wed", "thu", "fri"],
      },
    ]);
  };

  const removeTimeWindow = (index: number) => {
    setTimeWindows(timeWindows.filter((_, i) => i !== index));
  };

  const updateTimeWindow = (index: number, field: keyof TimeWindow, value: string | string[]) => {
    const updated = [...timeWindows];
    updated[index] = { ...updated[index], [field]: value };
    setTimeWindows(updated);
  };

  const toggleDay = (windowIndex: number, day: "mon" | "tue" | "wed" | "thu" | "fri" | "sat" | "sun") => {
    const updated = [...timeWindows];
    const window = updated[windowIndex];
    const days = window.days || [];
    if (days.includes(day)) {
      window.days = days.filter((d) => d !== day);
    } else {
      window.days = [...days, day];
    }
    setTimeWindows(updated);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!name.trim()) {
      setNotification({ type: "error", message: "Name is required" });
      return;
    }

    if (!targetValue.trim()) {
      setNotification({ type: "error", message: "Target is required" });
      return;
    }

    if (limitType === "time" && !dailyLimitMinutes && !weeklyLimitMinutes) {
      setNotification({ type: "error", message: "At least one time limit is required" });
      return;
    }

    const data: CreateAppLimitRequest = {
      name: name.trim(),
      target_type: targetType,
      target_value: targetValue.trim(),
      limit_type: limitType,
      daily_limit_minutes: dailyLimitMinutes ? parseInt(dailyLimitMinutes) : undefined,
      weekly_limit_minutes: weeklyLimitMinutes ? parseInt(weeklyLimitMinutes) : undefined,
      time_windows: timeWindows.length > 0 ? timeWindows : undefined,
      device_id: deviceId || undefined,
      group_id: groupId || undefined,
    };

    const result = await saveLimit(() =>
      isEditing
        ? appLimitsApi.update(limit.id, data)
        : appLimitsApi.create(data)
    );

    if (result) {
      router.push("/app-limits");
    } else {
      setNotification({ type: "error", message: "Failed to save limit" });
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Shield className="h-6 w-6" />
              <div>
                <CardTitle>{isEditing ? "Edit App Limit" : "Create App Limit"}</CardTitle>
                <CardDescription>
                  Configure time limits and restrictions for apps
                </CardDescription>
              </div>
            </div>
            <Button variant="outline" type="button" onClick={() => router.back()}>
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Notification */}
          {notification && (
            <div
              className={`p-3 rounded-md ${
                notification.type === "success"
                  ? "bg-green-100 text-green-800"
                  : "bg-red-100 text-red-800"
              }`}
            >
              {notification.message}
            </div>
          )}

          {/* Basic Info */}
          <div className="space-y-4">
            <h3 className="font-medium">Basic Information</h3>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="text-sm font-medium">Name *</label>
                <Input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g., Block Social Media"
                  className="mt-1"
                />
              </div>

              <div>
                <label className="text-sm font-medium">Organization</label>
                <select
                  className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={organizationId}
                  onChange={(e) => {
                    setOrganizationId(e.target.value);
                    setDeviceId("");
                    setGroupId("");
                  }}
                >
                  <option value="">Select Organization</option>
                  {orgsData?.items?.map((org) => (
                    <option key={org.id} value={org.id}>
                      {org.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {/* Target Selection */}
          <div className="space-y-4">
            <h3 className="font-medium">Target</h3>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="text-sm font-medium">Target Type *</label>
                <select
                  className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={targetType}
                  onChange={(e) => {
                    setTargetType(e.target.value as "app" | "category");
                    setTargetValue("");
                  }}
                >
                  <option value="app">Specific App</option>
                  <option value="category">App Category</option>
                </select>
              </div>

              <div>
                <label className="text-sm font-medium">
                  {targetType === "app" ? "Package Name *" : "Category *"}
                </label>
                {targetType === "category" ? (
                  <select
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={targetValue}
                    onChange={(e) => setTargetValue(e.target.value)}
                  >
                    <option value="">Select Category</option>
                    {CATEGORIES.map((cat) => (
                      <option key={cat.value} value={cat.value}>
                        {cat.label}
                      </option>
                    ))}
                  </select>
                ) : (
                  <Input
                    value={targetValue}
                    onChange={(e) => setTargetValue(e.target.value)}
                    placeholder="e.g., com.facebook.katana"
                    className="mt-1"
                  />
                )}
              </div>
            </div>
          </div>

          {/* Limit Type */}
          <div className="space-y-4">
            <h3 className="font-medium">Limit Type</h3>

            <div className="flex gap-4">
              <button
                type="button"
                className={`flex-1 p-4 rounded-lg border-2 transition-colors ${
                  limitType === "time"
                    ? "border-primary bg-primary/5"
                    : "border-border hover:border-primary/50"
                }`}
                onClick={() => setLimitType("time")}
              >
                <div className="flex items-center gap-3">
                  <Clock className="h-6 w-6" />
                  <div className="text-left">
                    <div className="font-medium">Time Limit</div>
                    <div className="text-sm text-muted-foreground">
                      Set daily/weekly usage limits
                    </div>
                  </div>
                </div>
              </button>

              <button
                type="button"
                className={`flex-1 p-4 rounded-lg border-2 transition-colors ${
                  limitType === "blocked"
                    ? "border-primary bg-primary/5"
                    : "border-border hover:border-primary/50"
                }`}
                onClick={() => setLimitType("blocked")}
              >
                <div className="flex items-center gap-3">
                  <Ban className="h-6 w-6" />
                  <div className="text-left">
                    <div className="font-medium">Block Completely</div>
                    <div className="text-sm text-muted-foreground">
                      Block app entirely
                    </div>
                  </div>
                </div>
              </button>
            </div>
          </div>

          {/* Time Limits */}
          {limitType === "time" && (
            <div className="space-y-4">
              <h3 className="font-medium">Time Limits</h3>

              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <label className="text-sm font-medium">Daily Limit (minutes)</label>
                  <Input
                    type="number"
                    min="1"
                    value={dailyLimitMinutes}
                    onChange={(e) => setDailyLimitMinutes(e.target.value)}
                    placeholder="e.g., 60"
                    className="mt-1"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Weekly Limit (minutes)</label>
                  <Input
                    type="number"
                    min="1"
                    value={weeklyLimitMinutes}
                    onChange={(e) => setWeeklyLimitMinutes(e.target.value)}
                    placeholder="e.g., 420"
                    className="mt-1"
                  />
                </div>
              </div>

              {/* Quick presets */}
              <div className="flex flex-wrap gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setDailyLimitMinutes("30")}
                >
                  30min/day
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setDailyLimitMinutes("60")}
                >
                  1hr/day
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setDailyLimitMinutes("120")}
                >
                  2hr/day
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setWeeklyLimitMinutes("420")}
                >
                  7hr/week
                </Button>
              </div>
            </div>
          )}

          {/* Time Windows */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-medium">Allowed Time Windows (Optional)</h3>
              <Button type="button" variant="outline" size="sm" onClick={addTimeWindow}>
                <Plus className="mr-2 h-4 w-4" />
                Add Window
              </Button>
            </div>
            <p className="text-sm text-muted-foreground">
              Define when the app can be used. Leave empty to apply limits all day.
            </p>

            {timeWindows.map((window, index) => (
              <div key={index} className="p-4 rounded-lg border space-y-3">
                <div className="flex items-center justify-between">
                  <span className="font-medium">Window {index + 1}</span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => removeTimeWindow(index)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>

                <div className="flex gap-4">
                  <div>
                    <label className="text-sm font-medium">Start Time</label>
                    <Input
                      type="time"
                      value={window.start_time}
                      onChange={(e) => updateTimeWindow(index, "start_time", e.target.value)}
                      className="mt-1"
                    />
                  </div>
                  <div>
                    <label className="text-sm font-medium">End Time</label>
                    <Input
                      type="time"
                      value={window.end_time}
                      onChange={(e) => updateTimeWindow(index, "end_time", e.target.value)}
                      className="mt-1"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-sm font-medium">Days</label>
                  <div className="flex gap-2 mt-1">
                    {DAYS.map((day) => (
                      <button
                        key={day.value}
                        type="button"
                        className={`px-3 py-1 rounded text-sm ${
                          window.days?.includes(day.value)
                            ? "bg-primary text-primary-foreground"
                            : "bg-secondary text-secondary-foreground"
                        }`}
                        onClick={() => toggleDay(index, day.value)}
                      >
                        {day.label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Apply To */}
          <div className="space-y-4">
            <h3 className="font-medium">Apply To</h3>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="text-sm font-medium">Device (Optional)</label>
                <select
                  className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={deviceId}
                  onChange={(e) => {
                    setDeviceId(e.target.value);
                    if (e.target.value) setGroupId("");
                  }}
                  disabled={!!groupId}
                >
                  <option value="">All Devices</option>
                  {filteredDevices?.map((device) => (
                    <option key={device.id} value={device.id}>
                      {device.display_name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-sm font-medium">Group (Optional)</label>
                <select
                  className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={groupId}
                  onChange={(e) => {
                    setGroupId(e.target.value);
                    if (e.target.value) setDeviceId("");
                  }}
                  disabled={!!deviceId}
                >
                  <option value="">No Group</option>
                  {filteredGroups?.map((group) => (
                    <option key={group.id} value={group.id}>
                      {group.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <p className="text-sm text-muted-foreground">
              Select a device or group to apply this limit to. Leave empty to apply to all devices in the organization.
            </p>
          </div>

          {/* Submit */}
          <div className="flex justify-end gap-3 pt-4">
            <Button type="button" variant="outline" onClick={() => router.back()}>
              Cancel
            </Button>
            <Button type="submit" disabled={saving}>
              <Save className="mr-2 h-4 w-4" />
              {saving ? "Saving..." : isEditing ? "Update Limit" : "Create Limit"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </form>
  );
}
