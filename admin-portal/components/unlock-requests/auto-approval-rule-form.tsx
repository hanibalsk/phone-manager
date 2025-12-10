"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import type { AutoApprovalRule, Organization, AdminDevice, AdminUser, AdminGroup, CreateAutoApprovalRuleRequest } from "@/types";
import { unlockRequestsApi, organizationsApi, adminDevicesApi, usersApi, adminGroupsApi } from "@/lib/api-client";
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
import { Zap, ArrowLeft, Plus, X } from "lucide-react";
import Link from "next/link";

const DAY_OPTIONS = [
  { value: 0, label: "Sun" },
  { value: 1, label: "Mon" },
  { value: 2, label: "Tue" },
  { value: 3, label: "Wed" },
  { value: 4, label: "Thu" },
  { value: 5, label: "Fri" },
  { value: 6, label: "Sat" },
];

const DURATION_PRESETS = [
  { value: 15, label: "15 min" },
  { value: 30, label: "30 min" },
  { value: 60, label: "1 hour" },
  { value: 120, label: "2 hours" },
];

interface AutoApprovalRuleFormProps {
  rule?: AutoApprovalRule;
}

export function AutoApprovalRuleForm({ rule }: AutoApprovalRuleFormProps) {
  const router = useRouter();
  const isEdit = !!rule;

  // Form state
  const [name, setName] = useState(rule?.name || "");
  const [description, setDescription] = useState(rule?.description || "");
  const [organizationId, setOrganizationId] = useState(rule?.organization_id || "");
  const [maxDuration, setMaxDuration] = useState(rule?.max_duration_minutes || 30);
  const [enabled, setEnabled] = useState(rule?.enabled ?? true);

  // Conditions
  const [useTimeWindow, setUseTimeWindow] = useState(!!rule?.conditions.time_window);
  const [startTime, setStartTime] = useState(rule?.conditions.time_window?.start_time || "09:00");
  const [endTime, setEndTime] = useState(rule?.conditions.time_window?.end_time || "17:00");
  const [selectedDays, setSelectedDays] = useState<number[]>(rule?.conditions.time_window?.days_of_week || [1, 2, 3, 4, 5]);

  const [useUsers, setUseUsers] = useState(!!rule?.conditions.user_ids?.length);
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>(rule?.conditions.user_ids || []);

  const [useDevices, setUseDevices] = useState(!!rule?.conditions.device_ids?.length);
  const [selectedDeviceIds, setSelectedDeviceIds] = useState<string[]>(rule?.conditions.device_ids || []);

  const [useGroups, setUseGroups] = useState(!!rule?.conditions.group_ids?.length);
  const [selectedGroupIds, setSelectedGroupIds] = useState<string[]>(rule?.conditions.group_ids || []);

  const [maxDailyRequests, setMaxDailyRequests] = useState<number | undefined>(rule?.conditions.max_daily_requests);

  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  // API hooks
  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: usersData, execute: fetchUsers } = useApi<{ items: AdminUser[] }>();
  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();
  const { data: groupsData, execute: fetchGroups } = useApi<{ items: AdminGroup[] }>();
  const { execute: createRule, loading: creating } = useApi<AutoApprovalRule>();
  const { execute: updateRule, loading: updating } = useApi<AutoApprovalRule>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
  }, [fetchOrgs]);

  // Fetch users/devices/groups when organization changes
  useEffect(() => {
    if (organizationId) {
      fetchUsers(() => usersApi.list({ limit: 100 }));
      fetchDevices(() => adminDevicesApi.list({ organization_id: organizationId, limit: 100 }));
      fetchGroups(() => adminGroupsApi.list({ organization_id: organizationId, limit: 100 }));
    }
  }, [organizationId, fetchUsers, fetchDevices, fetchGroups]);

  // Clear notification after 3 seconds
  useEffect(() => {
    if (notification) {
      const timer = setTimeout(() => setNotification(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [notification]);

  const toggleDay = (day: number) => {
    if (selectedDays.includes(day)) {
      setSelectedDays(selectedDays.filter(d => d !== day));
    } else {
      setSelectedDays([...selectedDays, day].sort());
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!name.trim()) {
      setNotification({ type: "error", message: "Please enter a rule name" });
      return;
    }

    if (!organizationId) {
      setNotification({ type: "error", message: "Please select an organization" });
      return;
    }

    if (useTimeWindow && selectedDays.length === 0) {
      setNotification({ type: "error", message: "Please select at least one day for the time window" });
      return;
    }

    const conditions: CreateAutoApprovalRuleRequest["conditions"] = {};

    if (useTimeWindow) {
      conditions.time_window = {
        start_time: startTime,
        end_time: endTime,
        days_of_week: selectedDays,
      };
    }

    if (useUsers && selectedUserIds.length > 0) {
      conditions.user_ids = selectedUserIds;
    }

    if (useDevices && selectedDeviceIds.length > 0) {
      conditions.device_ids = selectedDeviceIds;
    }

    if (useGroups && selectedGroupIds.length > 0) {
      conditions.group_ids = selectedGroupIds;
    }

    if (maxDailyRequests) {
      conditions.max_daily_requests = maxDailyRequests;
    }

    const data: CreateAutoApprovalRuleRequest = {
      name: name.trim(),
      description: description.trim() || undefined,
      organization_id: organizationId,
      conditions,
      max_duration_minutes: maxDuration,
      enabled,
    };

    let result;
    if (isEdit) {
      result = await updateRule(() =>
        unlockRequestsApi.updateAutoApprovalRule(rule.id, data)
      );
    } else {
      result = await createRule(() =>
        unlockRequestsApi.createAutoApprovalRule(data)
      );
    }

    if (result) {
      router.push("/unlock-requests/rules");
    } else {
      setNotification({ type: "error", message: `Failed to ${isEdit ? "update" : "create"} rule` });
    }
  };

  const loading = creating || updating;

  return (
    <form onSubmit={handleSubmit}>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-4">
            <Link href="/unlock-requests/rules">
              <Button variant="ghost" size="icon">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <div className="flex items-center gap-2">
              <Zap className="h-6 w-6" />
              <div>
                <CardTitle>{isEdit ? "Edit" : "Create"} Auto-Approval Rule</CardTitle>
                <CardDescription>
                  Configure conditions for automatic request approval
                </CardDescription>
              </div>
            </div>
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

            <div>
              <label className="text-sm font-medium">Rule Name *</label>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g., Weekday School Hours"
                className="mt-1"
              />
            </div>

            <div>
              <label className="text-sm font-medium">Description</label>
              <Input
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Brief description of the rule..."
                className="mt-1"
              />
            </div>

            <div>
              <label className="text-sm font-medium">Organization *</label>
              <select
                className="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={organizationId}
                onChange={(e) => {
                  setOrganizationId(e.target.value);
                  setSelectedUserIds([]);
                  setSelectedDeviceIds([]);
                  setSelectedGroupIds([]);
                }}
              >
                <option value="">Select organization...</option>
                {orgsData?.items?.map((org) => (
                  <option key={org.id} value={org.id}>
                    {org.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-sm font-medium">Maximum Auto-Approval Duration *</label>
              <div className="mt-2 flex flex-wrap gap-2">
                {DURATION_PRESETS.map((preset) => (
                  <button
                    key={preset.value}
                    type="button"
                    className={`px-3 py-2 text-sm rounded border transition-colors ${
                      maxDuration === preset.value
                        ? "border-primary bg-primary/10"
                        : "border-border hover:border-primary/50"
                    }`}
                    onClick={() => setMaxDuration(preset.value)}
                  >
                    {preset.label}
                  </button>
                ))}
              </div>
              <div className="mt-2 flex items-center gap-2">
                <span className="text-sm text-muted-foreground">Or custom:</span>
                <Input
                  type="number"
                  min="1"
                  max="480"
                  value={maxDuration}
                  onChange={(e) => setMaxDuration(parseInt(e.target.value) || 30)}
                  className="w-24"
                />
                <span className="text-sm text-muted-foreground">minutes</span>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="enabled"
                checked={enabled}
                onChange={(e) => setEnabled(e.target.checked)}
                className="rounded"
              />
              <label htmlFor="enabled" className="text-sm font-medium">
                Enable rule immediately
              </label>
            </div>
          </div>

          {/* Conditions */}
          <div className="space-y-4 pt-4 border-t">
            <h3 className="font-medium">Conditions</h3>
            <p className="text-sm text-muted-foreground">
              Configure when this rule should apply. All enabled conditions must match for auto-approval.
            </p>

            {/* Time Window */}
            <div className="border rounded-lg p-4">
              <div className="flex items-center gap-2 mb-3">
                <input
                  type="checkbox"
                  id="useTimeWindow"
                  checked={useTimeWindow}
                  onChange={(e) => setUseTimeWindow(e.target.checked)}
                  className="rounded"
                />
                <label htmlFor="useTimeWindow" className="text-sm font-medium">
                  Time Window
                </label>
              </div>

              {useTimeWindow && (
                <div className="space-y-3 ml-6">
                  <div className="flex items-center gap-2">
                    <Input
                      type="time"
                      value={startTime}
                      onChange={(e) => setStartTime(e.target.value)}
                      className="w-32"
                    />
                    <span className="text-muted-foreground">to</span>
                    <Input
                      type="time"
                      value={endTime}
                      onChange={(e) => setEndTime(e.target.value)}
                      className="w-32"
                    />
                  </div>

                  <div className="flex gap-2">
                    {DAY_OPTIONS.map((day) => (
                      <button
                        key={day.value}
                        type="button"
                        className={`px-3 py-1 text-xs rounded border transition-colors ${
                          selectedDays.includes(day.value)
                            ? "border-primary bg-primary text-primary-foreground"
                            : "border-border hover:border-primary/50"
                        }`}
                        onClick={() => toggleDay(day.value)}
                      >
                        {day.label}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Users */}
            {organizationId && (
              <div className="border rounded-lg p-4">
                <div className="flex items-center gap-2 mb-3">
                  <input
                    type="checkbox"
                    id="useUsers"
                    checked={useUsers}
                    onChange={(e) => setUseUsers(e.target.checked)}
                    className="rounded"
                  />
                  <label htmlFor="useUsers" className="text-sm font-medium">
                    Specific Users Only
                  </label>
                </div>

                {useUsers && (
                  <div className="ml-6">
                    <select
                      className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                      value=""
                      onChange={(e) => {
                        if (e.target.value && !selectedUserIds.includes(e.target.value)) {
                          setSelectedUserIds([...selectedUserIds, e.target.value]);
                        }
                      }}
                    >
                      <option value="">Add user...</option>
                      {usersData?.items?.filter(u => !selectedUserIds.includes(u.id)).map((user) => (
                        <option key={user.id} value={user.id}>
                          {user.display_name} ({user.email})
                        </option>
                      ))}
                    </select>

                    {selectedUserIds.length > 0 && (
                      <div className="mt-2 flex flex-wrap gap-2">
                        {selectedUserIds.map((userId) => {
                          const user = usersData?.items?.find(u => u.id === userId);
                          return (
                            <span
                              key={userId}
                              className="inline-flex items-center gap-1 bg-secondary px-2 py-1 rounded text-sm"
                            >
                              {user?.display_name || userId}
                              <button
                                type="button"
                                onClick={() => setSelectedUserIds(selectedUserIds.filter(id => id !== userId))}
                              >
                                <X className="h-3 w-3" />
                              </button>
                            </span>
                          );
                        })}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Devices */}
            {organizationId && (
              <div className="border rounded-lg p-4">
                <div className="flex items-center gap-2 mb-3">
                  <input
                    type="checkbox"
                    id="useDevices"
                    checked={useDevices}
                    onChange={(e) => setUseDevices(e.target.checked)}
                    className="rounded"
                  />
                  <label htmlFor="useDevices" className="text-sm font-medium">
                    Specific Devices Only
                  </label>
                </div>

                {useDevices && (
                  <div className="ml-6">
                    <select
                      className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                      value=""
                      onChange={(e) => {
                        if (e.target.value && !selectedDeviceIds.includes(e.target.value)) {
                          setSelectedDeviceIds([...selectedDeviceIds, e.target.value]);
                        }
                      }}
                    >
                      <option value="">Add device...</option>
                      {devicesData?.items?.filter(d => !selectedDeviceIds.includes(d.id)).map((device) => (
                        <option key={device.id} value={device.id}>
                          {device.display_name}
                        </option>
                      ))}
                    </select>

                    {selectedDeviceIds.length > 0 && (
                      <div className="mt-2 flex flex-wrap gap-2">
                        {selectedDeviceIds.map((deviceId) => {
                          const device = devicesData?.items?.find(d => d.id === deviceId);
                          return (
                            <span
                              key={deviceId}
                              className="inline-flex items-center gap-1 bg-secondary px-2 py-1 rounded text-sm"
                            >
                              {device?.display_name || deviceId}
                              <button
                                type="button"
                                onClick={() => setSelectedDeviceIds(selectedDeviceIds.filter(id => id !== deviceId))}
                              >
                                <X className="h-3 w-3" />
                              </button>
                            </span>
                          );
                        })}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Groups */}
            {organizationId && (
              <div className="border rounded-lg p-4">
                <div className="flex items-center gap-2 mb-3">
                  <input
                    type="checkbox"
                    id="useGroups"
                    checked={useGroups}
                    onChange={(e) => setUseGroups(e.target.checked)}
                    className="rounded"
                  />
                  <label htmlFor="useGroups" className="text-sm font-medium">
                    Specific Groups Only
                  </label>
                </div>

                {useGroups && (
                  <div className="ml-6">
                    <select
                      className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                      value=""
                      onChange={(e) => {
                        if (e.target.value && !selectedGroupIds.includes(e.target.value)) {
                          setSelectedGroupIds([...selectedGroupIds, e.target.value]);
                        }
                      }}
                    >
                      <option value="">Add group...</option>
                      {groupsData?.items?.filter(g => !selectedGroupIds.includes(g.id)).map((group) => (
                        <option key={group.id} value={group.id}>
                          {group.name}
                        </option>
                      ))}
                    </select>

                    {selectedGroupIds.length > 0 && (
                      <div className="mt-2 flex flex-wrap gap-2">
                        {selectedGroupIds.map((groupId) => {
                          const group = groupsData?.items?.find(g => g.id === groupId);
                          return (
                            <span
                              key={groupId}
                              className="inline-flex items-center gap-1 bg-secondary px-2 py-1 rounded text-sm"
                            >
                              {group?.name || groupId}
                              <button
                                type="button"
                                onClick={() => setSelectedGroupIds(selectedGroupIds.filter(id => id !== groupId))}
                              >
                                <X className="h-3 w-3" />
                              </button>
                            </span>
                          );
                        })}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Max Daily Requests */}
            <div className="border rounded-lg p-4">
              <div className="flex items-center gap-2 mb-3">
                <input
                  type="checkbox"
                  id="useMaxDaily"
                  checked={maxDailyRequests !== undefined}
                  onChange={(e) => setMaxDailyRequests(e.target.checked ? 5 : undefined)}
                  className="rounded"
                />
                <label htmlFor="useMaxDaily" className="text-sm font-medium">
                  Maximum Daily Requests Limit
                </label>
              </div>

              {maxDailyRequests !== undefined && (
                <div className="ml-6 flex items-center gap-2">
                  <Input
                    type="number"
                    min="1"
                    max="100"
                    value={maxDailyRequests}
                    onChange={(e) => setMaxDailyRequests(parseInt(e.target.value) || 1)}
                    className="w-24"
                  />
                  <span className="text-sm text-muted-foreground">requests per user per day</span>
                </div>
              )}
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <Link href="/unlock-requests/rules">
              <Button variant="outline" type="button">
                Cancel
              </Button>
            </Link>
            <Button type="submit" disabled={loading}>
              {loading ? "Saving..." : isEdit ? "Update Rule" : "Create Rule"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </form>
  );
}
