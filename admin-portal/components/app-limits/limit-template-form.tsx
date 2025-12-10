"use client";

import { useState, useEffect } from "react";
import type { LimitTemplate, CreateLimitTemplateRequest, Organization, AdminDevice, AdminGroup, TimeWindow, AppCategory } from "@/types";
import { LimitTypeBadge } from "./limit-type-badge";
import { AppCategoryBadge } from "@/components/app-usage/app-category-badge";
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
  FileText,
  Save,
  ArrowLeft,
  Plus,
  Trash2,
  Clock,
  Ban,
  Copy,
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

interface TemplateRule {
  target_type: "app" | "category";
  target_value: string;
  target_display: string;
  limit_type: "time" | "blocked";
  daily_limit_minutes: number | null;
  weekly_limit_minutes: number | null;
  time_windows: TimeWindow[];
}

interface LimitTemplateFormProps {
  template?: LimitTemplate;
}

export function LimitTemplateForm({ template }: LimitTemplateFormProps) {
  const router = useRouter();
  const isEditing = !!template;

  const [name, setName] = useState(template?.name || "");
  const [description, setDescription] = useState(template?.description || "");
  const [organizationId, setOrganizationId] = useState(template?.organization_id || "");
  const [rules, setRules] = useState<TemplateRule[]>(
    template?.rules || []
  );
  const [showApplyModal, setShowApplyModal] = useState(false);
  const [applyDeviceIds, setApplyDeviceIds] = useState<string[]>([]);
  const [applyGroupIds, setApplyGroupIds] = useState<string[]>([]);
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();
  const { data: groupsData, execute: fetchGroups } = useApi<{ items: AdminGroup[] }>();
  const { execute: saveTemplate, loading: saving } = useApi<LimitTemplate>();
  const { execute: applyTemplate, loading: applying } = useApi<{ applied_count: number }>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
    fetchDevices(() => adminDevicesApi.list({ limit: 100 }));
    fetchGroups(() => adminGroupsApi.list({ limit: 100 }));
  }, [fetchOrgs, fetchDevices, fetchGroups]);

  // Filter by organization
  const filteredDevices = organizationId
    ? devicesData?.items?.filter((d) => d.organization_id === organizationId)
    : devicesData?.items;

  const filteredGroups = organizationId
    ? groupsData?.items?.filter((g) => g.organization_id === organizationId)
    : groupsData?.items;

  const addRule = () => {
    setRules([
      ...rules,
      {
        target_type: "category",
        target_value: "social",
        target_display: "Social",
        limit_type: "time",
        daily_limit_minutes: 60,
        weekly_limit_minutes: null,
        time_windows: [],
      },
    ]);
  };

  const removeRule = (index: number) => {
    setRules(rules.filter((_, i) => i !== index));
  };

  const updateRule = (index: number, updates: Partial<TemplateRule>) => {
    const updated = [...rules];
    updated[index] = { ...updated[index], ...updates };

    // Update display name when target changes
    if (updates.target_value !== undefined && updates.target_type === "category") {
      const category = CATEGORIES.find((c) => c.value === updates.target_value);
      if (category) {
        updated[index].target_display = category.label;
      }
    }

    setRules(updated);
  };

  const duplicateRule = (index: number) => {
    const rule = rules[index];
    setRules([...rules, { ...rule }]);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!name.trim()) {
      setNotification({ type: "error", message: "Name is required" });
      return;
    }

    if (rules.length === 0) {
      setNotification({ type: "error", message: "At least one rule is required" });
      return;
    }

    const data: CreateLimitTemplateRequest = {
      name: name.trim(),
      description: description.trim() || undefined,
      rules: rules.map((rule) => ({
        target_type: rule.target_type,
        target_value: rule.target_value,
        limit_type: rule.limit_type,
        daily_limit_minutes: rule.daily_limit_minutes || undefined,
        weekly_limit_minutes: rule.weekly_limit_minutes || undefined,
        time_windows: rule.time_windows.length > 0 ? rule.time_windows : undefined,
      })),
    };

    const result = await saveTemplate(() =>
      isEditing
        ? appLimitsApi.updateTemplate(template.id, data)
        : appLimitsApi.createTemplate(data)
    );

    if (result) {
      router.push("/app-limits/templates");
    } else {
      setNotification({ type: "error", message: "Failed to save template" });
    }
  };

  const handleApply = async () => {
    if (!template) return;
    if (applyDeviceIds.length === 0 && applyGroupIds.length === 0) {
      setNotification({ type: "error", message: "Select at least one device or group" });
      return;
    }

    const result = await applyTemplate(() =>
      appLimitsApi.applyTemplate(template.id, {
        device_ids: applyDeviceIds.length > 0 ? applyDeviceIds : undefined,
        group_ids: applyGroupIds.length > 0 ? applyGroupIds : undefined,
      })
    );

    if (result) {
      setNotification({ type: "success", message: `Template applied to ${result.applied_count} item(s)` });
      setShowApplyModal(false);
      setApplyDeviceIds([]);
      setApplyGroupIds([]);
    } else {
      setNotification({ type: "error", message: "Failed to apply template" });
    }
  };

  const toggleDevice = (deviceId: string) => {
    if (applyDeviceIds.includes(deviceId)) {
      setApplyDeviceIds(applyDeviceIds.filter((id) => id !== deviceId));
    } else {
      setApplyDeviceIds([...applyDeviceIds, deviceId]);
    }
  };

  const toggleGroup = (groupId: string) => {
    if (applyGroupIds.includes(groupId)) {
      setApplyGroupIds(applyGroupIds.filter((id) => id !== groupId));
    } else {
      setApplyGroupIds([...applyGroupIds, groupId]);
    }
  };

  return (
    <>
      <form onSubmit={handleSubmit}>
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <FileText className="h-6 w-6" />
                <div>
                  <CardTitle>{isEditing ? "Edit Template" : "Create Template"}</CardTitle>
                  <CardDescription>
                    Create reusable limit configurations
                  </CardDescription>
                </div>
              </div>
              <div className="flex gap-2">
                {isEditing && (
                  <Button type="button" variant="outline" onClick={() => setShowApplyModal(true)}>
                    Apply to Devices
                  </Button>
                )}
                <Button variant="outline" type="button" onClick={() => router.back()}>
                  <ArrowLeft className="mr-2 h-4 w-4" />
                  Back
                </Button>
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
              <h3 className="font-medium">Template Information</h3>

              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <label className="text-sm font-medium">Name *</label>
                  <Input
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="e.g., Kids Weekday Rules"
                    className="mt-1"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Organization</label>
                  <select
                    className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={organizationId}
                    onChange={(e) => setOrganizationId(e.target.value)}
                    disabled={isEditing}
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

              <div>
                <label className="text-sm font-medium">Description</label>
                <Input
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Describe what this template is for..."
                  className="mt-1"
                />
              </div>
            </div>

            {/* Rules */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="font-medium">Limit Rules</h3>
                <Button type="button" variant="outline" size="sm" onClick={addRule}>
                  <Plus className="mr-2 h-4 w-4" />
                  Add Rule
                </Button>
              </div>

              {rules.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground border-2 border-dashed rounded-lg">
                  <p>No rules added yet</p>
                  <Button type="button" variant="outline" className="mt-4" onClick={addRule}>
                    <Plus className="mr-2 h-4 w-4" />
                    Add First Rule
                  </Button>
                </div>
              ) : (
                <div className="space-y-4">
                  {rules.map((rule, index) => (
                    <div key={index} className="p-4 border rounded-lg space-y-4">
                      <div className="flex items-center justify-between">
                        <span className="font-medium text-sm">Rule {index + 1}</span>
                        <div className="flex gap-2">
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => duplicateRule(index)}
                          >
                            <Copy className="h-4 w-4" />
                          </Button>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => removeRule(index)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>

                      <div className="grid gap-4 md:grid-cols-3">
                        <div>
                          <label className="text-sm font-medium">Target Type</label>
                          <select
                            className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                            value={rule.target_type}
                            onChange={(e) =>
                              updateRule(index, {
                                target_type: e.target.value as "app" | "category",
                                target_value: "",
                                target_display: "",
                              })
                            }
                          >
                            <option value="app">Specific App</option>
                            <option value="category">Category</option>
                          </select>
                        </div>

                        <div>
                          <label className="text-sm font-medium">
                            {rule.target_type === "app" ? "Package Name" : "Category"}
                          </label>
                          {rule.target_type === "category" ? (
                            <select
                              className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                              value={rule.target_value}
                              onChange={(e) =>
                                updateRule(index, {
                                  target_value: e.target.value,
                                  target_type: "category",
                                })
                              }
                            >
                              <option value="">Select...</option>
                              {CATEGORIES.map((cat) => (
                                <option key={cat.value} value={cat.value}>
                                  {cat.label}
                                </option>
                              ))}
                            </select>
                          ) : (
                            <Input
                              value={rule.target_value}
                              onChange={(e) =>
                                updateRule(index, {
                                  target_value: e.target.value,
                                  target_display: e.target.value,
                                })
                              }
                              placeholder="com.example.app"
                              className="mt-1"
                            />
                          )}
                        </div>

                        <div>
                          <label className="text-sm font-medium">Limit Type</label>
                          <select
                            className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                            value={rule.limit_type}
                            onChange={(e) =>
                              updateRule(index, {
                                limit_type: e.target.value as "time" | "blocked",
                              })
                            }
                          >
                            <option value="time">Time Limit</option>
                            <option value="blocked">Blocked</option>
                          </select>
                        </div>
                      </div>

                      {rule.limit_type === "time" && (
                        <div className="grid gap-4 md:grid-cols-2">
                          <div>
                            <label className="text-sm font-medium">Daily Limit (minutes)</label>
                            <Input
                              type="number"
                              min="1"
                              value={rule.daily_limit_minutes || ""}
                              onChange={(e) =>
                                updateRule(index, {
                                  daily_limit_minutes: e.target.value ? parseInt(e.target.value) : null,
                                })
                              }
                              placeholder="e.g., 60"
                              className="mt-1"
                            />
                          </div>
                          <div>
                            <label className="text-sm font-medium">Weekly Limit (minutes)</label>
                            <Input
                              type="number"
                              min="1"
                              value={rule.weekly_limit_minutes || ""}
                              onChange={(e) =>
                                updateRule(index, {
                                  weekly_limit_minutes: e.target.value ? parseInt(e.target.value) : null,
                                })
                              }
                              placeholder="e.g., 420"
                              className="mt-1"
                            />
                          </div>
                        </div>
                      )}

                      {/* Rule preview */}
                      <div className="flex items-center gap-2 p-2 bg-secondary/50 rounded text-sm">
                        {rule.target_type === "category" && rule.target_value ? (
                          <AppCategoryBadge
                            category={rule.target_value as AppCategory}
                          />
                        ) : (
                          <span>{rule.target_display || "Select target"}</span>
                        )}
                        <span>→</span>
                        <LimitTypeBadge type={rule.limit_type} />
                        {rule.limit_type === "time" && (
                          <span className="text-muted-foreground">
                            {rule.daily_limit_minutes && `${rule.daily_limit_minutes}m/day`}
                            {rule.daily_limit_minutes && rule.weekly_limit_minutes && " • "}
                            {rule.weekly_limit_minutes && `${rule.weekly_limit_minutes}m/week`}
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Submit */}
            <div className="flex justify-end gap-3 pt-4">
              <Button type="button" variant="outline" onClick={() => router.back()}>
                Cancel
              </Button>
              <Button type="submit" disabled={saving}>
                <Save className="mr-2 h-4 w-4" />
                {saving ? "Saving..." : isEditing ? "Update Template" : "Create Template"}
              </Button>
            </div>
          </CardContent>
        </Card>
      </form>

      {/* Apply Modal */}
      {showApplyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center" role="dialog" aria-modal="true">
          <div className="absolute inset-0 bg-black/50" onClick={() => setShowApplyModal(false)} />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-lg mx-4 p-6 max-h-[80vh] overflow-y-auto">
            <h3 className="text-lg font-semibold mb-4">Apply Template</h3>
            <p className="text-sm text-muted-foreground mb-4">
              Select devices or groups to apply this template to.
            </p>

            {/* Devices */}
            <div className="mb-4">
              <label className="text-sm font-medium">Devices</label>
              <div className="mt-2 max-h-40 overflow-y-auto border rounded p-2 space-y-1">
                {filteredDevices?.map((device) => (
                  <label key={device.id} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={applyDeviceIds.includes(device.id)}
                      onChange={() => toggleDevice(device.id)}
                      className="rounded"
                    />
                    <span className="text-sm">{device.display_name}</span>
                  </label>
                ))}
                {(!filteredDevices || filteredDevices.length === 0) && (
                  <div className="text-sm text-muted-foreground">No devices available</div>
                )}
              </div>
            </div>

            {/* Groups */}
            <div className="mb-6">
              <label className="text-sm font-medium">Groups</label>
              <div className="mt-2 max-h-40 overflow-y-auto border rounded p-2 space-y-1">
                {filteredGroups?.map((group) => (
                  <label key={group.id} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={applyGroupIds.includes(group.id)}
                      onChange={() => toggleGroup(group.id)}
                      className="rounded"
                    />
                    <span className="text-sm">{group.name}</span>
                  </label>
                ))}
                {(!filteredGroups || filteredGroups.length === 0) && (
                  <div className="text-sm text-muted-foreground">No groups available</div>
                )}
              </div>
            </div>

            <div className="flex justify-end gap-3">
              <Button variant="outline" onClick={() => setShowApplyModal(false)}>
                Cancel
              </Button>
              <Button onClick={handleApply} disabled={applying}>
                {applying ? "Applying..." : "Apply Template"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
