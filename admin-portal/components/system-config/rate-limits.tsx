"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useApi } from "@/hooks/use-api";
import { rateLimitsApi, organizationsApi } from "@/lib/api-client";
import type {
  RateLimitConfig,
  RateLimitOverride,
  RateLimitMetrics,
  CreateRateLimitOverrideRequest,
  Organization,
} from "@/types";
import {
  Loader2,
  Gauge,
  AlertCircle,
  Check,
  Plus,
  Trash2,
  Edit,
  Save,
  X,
  Building,
  Activity,
  BarChart3,
} from "lucide-react";

export function RateLimits() {
  const [notification, setNotification] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // State
  const [limits, setLimits] = useState<RateLimitConfig[]>([]);
  const [overrides, setOverrides] = useState<RateLimitOverride[]>([]);
  const [metrics, setMetrics] = useState<RateLimitMetrics[]>([]);
  const [organizations, setOrganizations] = useState<Organization[]>([]);

  // View state
  const [activeView, setActiveView] = useState<"limits" | "overrides" | "metrics">("limits");
  const [editingLimit, setEditingLimit] = useState<RateLimitConfig | null>(null);
  const [editedLimitData, setEditedLimitData] = useState<Partial<RateLimitConfig>>({});

  // Override form
  const [showOverrideForm, setShowOverrideForm] = useState(false);
  const [editingOverride, setEditingOverride] = useState<RateLimitOverride | null>(null);
  const [overrideFormData, setOverrideFormData] = useState<CreateRateLimitOverrideRequest>({
    organization_id: "",
    endpoint_category: "",
    requests_per_minute: 60,
    requests_per_hour: 1000,
    requests_per_day: 10000,
    reason: "",
  });

  // Delete confirmation
  const [overrideToDelete, setOverrideToDelete] = useState<RateLimitOverride | null>(null);

  // API hooks
  const { loading: loadingLimits, execute: fetchLimits } = useApi<RateLimitConfig[]>();
  const { loading: savingLimit, execute: saveLimit } = useApi<RateLimitConfig>();
  const { loading: loadingOverrides, execute: fetchOverrides } = useApi<RateLimitOverride[]>();
  const { loading: savingOverride, execute: saveOverride } = useApi<RateLimitOverride>();
  const { loading: deletingOverride, execute: deleteOverride } = useApi<void>();
  const { loading: loadingMetrics, execute: fetchMetrics } = useApi<RateLimitMetrics[]>();
  const { execute: fetchOrgs } = useApi<{ items: Organization[] }>();

  // Load data
  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadData = async () => {
    const [limitsResult, overridesResult, metricsResult, orgsResult] = await Promise.all([
      fetchLimits(() => rateLimitsApi.list()),
      fetchOverrides(() => rateLimitsApi.listOverrides()),
      fetchMetrics(() => rateLimitsApi.getMetrics()),
      fetchOrgs(() => organizationsApi.list({})),
    ]);

    if (limitsResult) setLimits(limitsResult);
    if (overridesResult) setOverrides(overridesResult);
    if (metricsResult) setMetrics(metricsResult);
    if (orgsResult?.items) setOrganizations(orgsResult.items);
  };

  const startEditingLimit = (limit: RateLimitConfig) => {
    setEditingLimit(limit);
    setEditedLimitData({
      requests_per_minute: limit.requests_per_minute,
      requests_per_hour: limit.requests_per_hour,
      requests_per_day: limit.requests_per_day,
      enabled: limit.enabled,
    });
  };

  const handleSaveLimit = async () => {
    if (!editingLimit) return;

    const result = await saveLimit(() =>
      rateLimitsApi.update(editingLimit.id, editedLimitData)
    );

    if (result) {
      setLimits((prev) =>
        prev.map((l) => (l.id === result.id ? result : l))
      );
      setEditingLimit(null);
      setNotification({ type: "success", message: "Rate limit updated" });
      setTimeout(() => setNotification(null), 3000);
    } else {
      setNotification({ type: "error", message: "Failed to update rate limit" });
      setTimeout(() => setNotification(null), 3000);
    }
  };

  const handleSaveOverride = async () => {
    let success = false;

    if (editingOverride) {
      const result = await saveOverride(() =>
        rateLimitsApi.updateOverride(editingOverride.id, overrideFormData)
      );
      if (result) {
        setOverrides((prev) =>
          prev.map((o) => (o.id === result.id ? result : o))
        );
        setNotification({ type: "success", message: "Override updated" });
        success = true;
      } else {
        setNotification({ type: "error", message: "Failed to update override" });
      }
    } else {
      const result = await saveOverride(() =>
        rateLimitsApi.createOverride(overrideFormData)
      );
      if (result) {
        setOverrides((prev) => [...prev, result]);
        setNotification({ type: "success", message: "Override created" });
        success = true;
      } else {
        setNotification({ type: "error", message: "Failed to create override" });
      }
    }

    // Only close the form and reset state on success
    if (success) {
      setShowOverrideForm(false);
      setEditingOverride(null);
      setOverrideFormData({
        organization_id: "",
        endpoint_category: "",
        requests_per_minute: 60,
        requests_per_hour: 1000,
        requests_per_day: 10000,
        reason: "",
      });
    }
    setTimeout(() => setNotification(null), 3000);
  };

  const handleDeleteOverride = async () => {
    if (!overrideToDelete) return;

    await deleteOverride(() => rateLimitsApi.deleteOverride(overrideToDelete.id));
    setOverrides((prev) => prev.filter((o) => o.id !== overrideToDelete.id));
    setOverrideToDelete(null);
    setNotification({ type: "success", message: "Override deleted" });
    setTimeout(() => setNotification(null), 3000);
  };

  const startEditingOverride = (override: RateLimitOverride) => {
    setEditingOverride(override);
    setOverrideFormData({
      organization_id: override.organization_id,
      endpoint_category: override.endpoint_category,
      requests_per_minute: override.requests_per_minute,
      requests_per_hour: override.requests_per_hour,
      requests_per_day: override.requests_per_day,
      reason: override.reason,
    });
    setShowOverrideForm(true);
  };

  if (loadingLimits && limits.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6" data-testid="rate-limits-container">
      {/* Notification */}
      {notification && (
        <div
          className={`p-4 rounded-lg flex items-center gap-2 ${
            notification.type === "success"
              ? "bg-green-50 text-green-800 border border-green-200"
              : "bg-red-50 text-red-800 border border-red-200"
          }`}
        >
          {notification.type === "success" ? (
            <Check className="h-4 w-4" />
          ) : (
            <AlertCircle className="h-4 w-4" />
          )}
          {notification.message}
        </div>
      )}

      {/* View Tabs */}
      <div className="flex gap-2">
        <Button
          variant={activeView === "limits" ? "default" : "outline"}
          onClick={() => setActiveView("limits")}
          className="flex items-center gap-2"
          data-testid="rate-limits-tab-limits"
        >
          <Gauge className="h-4 w-4" />
          Default Limits
        </Button>
        <Button
          variant={activeView === "overrides" ? "default" : "outline"}
          onClick={() => setActiveView("overrides")}
          className="flex items-center gap-2"
          data-testid="rate-limits-tab-overrides"
        >
          <Building className="h-4 w-4" />
          Organization Overrides
          {overrides.length > 0 && (
            <span className="ml-1 px-1.5 py-0.5 rounded-full bg-primary-foreground text-primary text-xs">
              {overrides.length}
            </span>
          )}
        </Button>
        <Button
          variant={activeView === "metrics" ? "default" : "outline"}
          onClick={() => setActiveView("metrics")}
          className="flex items-center gap-2"
          data-testid="rate-limits-tab-metrics"
        >
          <BarChart3 className="h-4 w-4" />
          Metrics
        </Button>
      </div>

      {/* Default Limits View */}
      {activeView === "limits" && (
        <div className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Configure default rate limits for each endpoint category. These apply to all
            organizations unless overridden.
          </p>

          <div className="space-y-3">
            {limits.map((limit) => (
              <div
                key={limit.id}
                className={`p-4 rounded-lg border ${
                  limit.enabled
                    ? "border-border"
                    : "border-amber-200 bg-amber-50/50"
                }`}
                data-testid={`rate-limit-item-${limit.endpoint_category}`}
              >
                {editingLimit?.id === limit.id ? (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <span className="font-medium">{limit.endpoint_category}</span>
                        <p className="text-sm text-muted-foreground">
                          {limit.description}
                        </p>
                      </div>
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setEditingLimit(null)}
                          data-testid={`rate-limit-cancel-${limit.endpoint_category}`}
                        >
                          <X className="h-4 w-4" />
                        </Button>
                        <Button
                          size="sm"
                          onClick={handleSaveLimit}
                          disabled={savingLimit}
                          data-testid={`rate-limit-save-${limit.endpoint_category}`}
                        >
                          {savingLimit ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                          ) : (
                            <Save className="h-4 w-4" />
                          )}
                        </Button>
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-4">
                      <div className="space-y-1">
                        <Label className="text-xs">Per Minute</Label>
                        <Input
                          type="number"
                          value={editedLimitData.requests_per_minute || ""}
                          onChange={(e) =>
                            setEditedLimitData((prev) => ({
                              ...prev,
                              requests_per_minute: parseInt(e.target.value, 10),
                            }))
                          }
                          data-testid={`rate-limit-per-minute-${limit.endpoint_category}`}
                        />
                      </div>
                      <div className="space-y-1">
                        <Label className="text-xs">Per Hour</Label>
                        <Input
                          type="number"
                          value={editedLimitData.requests_per_hour || ""}
                          onChange={(e) =>
                            setEditedLimitData((prev) => ({
                              ...prev,
                              requests_per_hour: parseInt(e.target.value, 10),
                            }))
                          }
                          data-testid={`rate-limit-per-hour-${limit.endpoint_category}`}
                        />
                      </div>
                      <div className="space-y-1">
                        <Label className="text-xs">Per Day</Label>
                        <Input
                          type="number"
                          value={editedLimitData.requests_per_day || ""}
                          onChange={(e) =>
                            setEditedLimitData((prev) => ({
                              ...prev,
                              requests_per_day: parseInt(e.target.value, 10),
                            }))
                          }
                          data-testid={`rate-limit-per-day-${limit.endpoint_category}`}
                        />
                      </div>
                    </div>

                    <div
                      onClick={() =>
                        setEditedLimitData((prev) => ({
                          ...prev,
                          enabled: !prev.enabled,
                        }))
                      }
                      className={`p-3 rounded-lg border cursor-pointer transition-colors ${
                        editedLimitData.enabled
                          ? "border-primary bg-primary/5"
                          : "border-border"
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium">
                          {editedLimitData.enabled ? "Enabled" : "Disabled"}
                        </span>
                        <div
                          className={`w-8 h-5 rounded-full transition-colors ${
                            editedLimitData.enabled ? "bg-primary" : "bg-muted"
                          }`}
                        >
                          <div
                            className={`w-4 h-4 rounded-full bg-white shadow transition-transform mt-0.5 ${
                              editedLimitData.enabled
                                ? "translate-x-3.5 ml-0.5"
                                : "translate-x-0.5"
                            }`}
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{limit.endpoint_category}</span>
                        {!limit.enabled && (
                          <span className="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700">
                            Disabled
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-muted-foreground">
                        {limit.description}
                      </p>
                      <div className="flex gap-4 mt-2 text-sm text-muted-foreground">
                        <span>{limit.requests_per_minute}/min</span>
                        <span>{limit.requests_per_hour}/hr</span>
                        <span>{limit.requests_per_day}/day</span>
                      </div>
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => startEditingLimit(limit)}
                      data-testid={`rate-limit-edit-${limit.endpoint_category}`}
                    >
                      <Edit className="h-4 w-4" />
                    </Button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Overrides View */}
      {activeView === "overrides" && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              Create custom rate limits for specific organizations.
            </p>
            <Button
              size="sm"
              onClick={() => {
                setEditingOverride(null);
                setOverrideFormData({
                  organization_id: "",
                  endpoint_category: "",
                  requests_per_minute: 60,
                  requests_per_hour: 1000,
                  requests_per_day: 10000,
                  reason: "",
                });
                setShowOverrideForm(true);
              }}
              data-testid="rate-limits-add-override"
            >
              <Plus className="h-4 w-4 mr-2" />
              Add Override
            </Button>
          </div>

          {overrides.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground border rounded-lg">
              <Building className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>No organization overrides configured</p>
              <p className="text-sm">
                All organizations use default rate limits
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {overrides.map((override) => (
                <div
                  key={override.id}
                  className="p-4 rounded-lg border"
                  data-testid={`rate-limit-override-${override.id}`}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <Building className="h-4 w-4 text-muted-foreground" />
                        <span className="font-medium">
                          {override.organization_name}
                        </span>
                        <span className="text-xs px-2 py-0.5 rounded bg-muted">
                          {override.endpoint_category}
                        </span>
                      </div>
                      <p className="text-sm text-muted-foreground mt-1">
                        {override.reason}
                      </p>
                      <div className="flex gap-4 mt-2 text-sm text-muted-foreground">
                        <span>{override.requests_per_minute}/min</span>
                        <span>{override.requests_per_hour}/hr</span>
                        <span>{override.requests_per_day}/day</span>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => startEditingOverride(override)}
                        data-testid={`rate-limit-override-edit-${override.id}`}
                      >
                        <Edit className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-red-600 hover:text-red-700 hover:bg-red-50"
                        onClick={() => setOverrideToDelete(override)}
                        data-testid={`rate-limit-override-delete-${override.id}`}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Metrics View */}
      {activeView === "metrics" && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              Current rate limit usage and metrics.
            </p>
            <Button
              variant="outline"
              size="sm"
              onClick={() => fetchMetrics(() => rateLimitsApi.getMetrics())}
              disabled={loadingMetrics}
              data-testid="rate-limits-refresh-metrics"
            >
              {loadingMetrics ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Activity className="h-4 w-4" />
              )}
              <span className="ml-2">Refresh</span>
            </Button>
          </div>

          {metrics.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground border rounded-lg">
              <BarChart3 className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>No rate limit metrics available</p>
            </div>
          ) : (
            <div className="space-y-3">
              {metrics.map((metric) => {
                const limit = limits.find(
                  (l) => l.endpoint_category === metric.endpoint_category
                );
                const usagePercent = limit
                  ? Math.min(
                      100,
                      (metric.peak_requests_per_minute / limit.requests_per_minute) * 100
                    )
                  : 0;

                return (
                  <div key={metric.endpoint_category} className="p-4 rounded-lg border" data-testid={`rate-limit-metric-${metric.endpoint_category}`}>
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-medium">{metric.endpoint_category}</span>
                      {metric.rate_limited_requests_today > 0 && (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-700">
                          {metric.rate_limited_requests_today} limited today
                        </span>
                      )}
                    </div>

                    <div className="grid grid-cols-3 gap-4 text-sm">
                      <div>
                        <p className="text-muted-foreground">Today&apos;s Requests</p>
                        <p className="text-lg font-semibold">
                          {metric.total_requests_today.toLocaleString()}
                        </p>
                      </div>
                      <div>
                        <p className="text-muted-foreground">Peak/min</p>
                        <p className="text-lg font-semibold">
                          {metric.peak_requests_per_minute}
                        </p>
                      </div>
                      <div>
                        <p className="text-muted-foreground">Rate Limited</p>
                        <p className="text-lg font-semibold">
                          {metric.rate_limited_requests_today}
                        </p>
                      </div>
                    </div>

                    {/* Usage bar */}
                    <div className="mt-3">
                      <div className="flex justify-between text-xs text-muted-foreground mb-1">
                        <span>Peak usage vs limit</span>
                        <span>{usagePercent.toFixed(0)}%</span>
                      </div>
                      <div className="h-2 bg-muted rounded-full overflow-hidden">
                        <div
                          className={`h-full transition-all duration-300 ${
                            usagePercent > 80
                              ? "bg-red-500"
                              : usagePercent > 50
                              ? "bg-amber-500"
                              : "bg-green-500"
                          }`}
                          style={{ width: `${usagePercent}%` }}
                        />
                      </div>
                    </div>

                    {metric.last_rate_limited_at && (
                      <p className="text-xs text-muted-foreground mt-2">
                        Last rate limited:{" "}
                        {new Date(metric.last_rate_limited_at).toLocaleString()}
                      </p>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Override Form Modal */}
      {showOverrideForm && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setShowOverrideForm(false)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-lg mx-4 p-6" data-testid="rate-limit-override-form-dialog">
            <h3 className="text-lg font-semibold mb-4">
              {editingOverride ? "Edit Override" : "Add Rate Limit Override"}
            </h3>

            <div className="space-y-4">
              <div className="space-y-2">
                <Label>Organization</Label>
                <select
                  value={overrideFormData.organization_id}
                  onChange={(e) =>
                    setOverrideFormData((prev) => ({
                      ...prev,
                      organization_id: e.target.value,
                    }))
                  }
                  className="w-full px-3 py-2 border rounded-md bg-background text-sm"
                  disabled={!!editingOverride}
                  data-testid="rate-limit-override-org-select"
                >
                  <option value="">Select organization...</option>
                  {organizations.map((org) => (
                    <option key={org.id} value={org.id}>
                      {org.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-2">
                <Label>Endpoint Category</Label>
                <select
                  value={overrideFormData.endpoint_category}
                  onChange={(e) =>
                    setOverrideFormData((prev) => ({
                      ...prev,
                      endpoint_category: e.target.value,
                    }))
                  }
                  className="w-full px-3 py-2 border rounded-md bg-background text-sm"
                  disabled={!!editingOverride}
                  data-testid="rate-limit-override-category-select"
                >
                  <option value="">Select category...</option>
                  {limits.map((limit) => (
                    <option key={limit.id} value={limit.endpoint_category}>
                      {limit.endpoint_category}
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div className="space-y-1">
                  <Label className="text-xs">Per Minute</Label>
                  <Input
                    type="number"
                    value={overrideFormData.requests_per_minute}
                    onChange={(e) =>
                      setOverrideFormData((prev) => ({
                        ...prev,
                        requests_per_minute: parseInt(e.target.value, 10),
                      }))
                    }
                    data-testid="rate-limit-override-per-minute"
                  />
                </div>
                <div className="space-y-1">
                  <Label className="text-xs">Per Hour</Label>
                  <Input
                    type="number"
                    value={overrideFormData.requests_per_hour}
                    onChange={(e) =>
                      setOverrideFormData((prev) => ({
                        ...prev,
                        requests_per_hour: parseInt(e.target.value, 10),
                      }))
                    }
                    data-testid="rate-limit-override-per-hour"
                  />
                </div>
                <div className="space-y-1">
                  <Label className="text-xs">Per Day</Label>
                  <Input
                    type="number"
                    value={overrideFormData.requests_per_day}
                    onChange={(e) =>
                      setOverrideFormData((prev) => ({
                        ...prev,
                        requests_per_day: parseInt(e.target.value, 10),
                      }))
                    }
                    data-testid="rate-limit-override-per-day"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label>Reason</Label>
                <Input
                  value={overrideFormData.reason}
                  onChange={(e) =>
                    setOverrideFormData((prev) => ({
                      ...prev,
                      reason: e.target.value,
                    }))
                  }
                  placeholder="Why does this org need custom limits?"
                  data-testid="rate-limit-override-reason"
                />
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-6">
              <Button
                variant="outline"
                onClick={() => setShowOverrideForm(false)}
                data-testid="rate-limit-override-cancel"
              >
                Cancel
              </Button>
              <Button
                onClick={handleSaveOverride}
                disabled={
                  savingOverride ||
                  !overrideFormData.organization_id ||
                  !overrideFormData.endpoint_category ||
                  !overrideFormData.reason
                }
                data-testid="rate-limit-override-save"
              >
                {savingOverride ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : (
                  <Save className="h-4 w-4 mr-2" />
                )}
                {editingOverride ? "Update" : "Create"}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {overrideToDelete && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setOverrideToDelete(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6" data-testid="rate-limit-delete-dialog">
            <div className="flex items-center gap-3 mb-4">
              <AlertCircle className="h-6 w-6 text-red-500" />
              <h3 className="text-lg font-semibold">Delete Override</h3>
            </div>
            <p className="text-muted-foreground mb-4">
              Are you sure you want to delete the rate limit override for{" "}
              <strong>{overrideToDelete.organization_name}</strong>?
              They will fall back to default rate limits.
            </p>
            <div className="flex justify-end gap-3">
              <Button
                variant="outline"
                onClick={() => setOverrideToDelete(null)}
                data-testid="rate-limit-delete-cancel"
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleDeleteOverride}
                disabled={deletingOverride}
                data-testid="rate-limit-delete-confirm"
              >
                {deletingOverride ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : (
                  <Trash2 className="h-4 w-4 mr-2" />
                )}
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
