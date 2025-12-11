"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { useApi } from "@/hooks/use-api";
import { systemRetentionApi } from "@/lib/api-client";
import type {
  RetentionConfig,
  UpdateRetentionConfigRequest,
  RetentionStats,
  RetentionPeriod,
} from "@/types";
import {
  Loader2,
  Database,
  AlertCircle,
  Check,
  Save,
  Trash2,
  Calendar,
  HardDrive,
  Clock,
  RefreshCw,
  AlertTriangle,
  Info,
} from "lucide-react";

const retentionPeriodLabels: Record<RetentionPeriod, string> = {
  "30d": "30 Days",
  "60d": "60 Days",
  "90d": "90 Days",
  "180d": "6 Months",
  "365d": "1 Year",
  unlimited: "Unlimited",
};

const dataTypeLabels: Record<string, { label: string; description: string; icon: React.ElementType }> = {
  locations: {
    label: "Location Data",
    description: "Device location history and tracking data",
    icon: Database,
  },
  audit_logs: {
    label: "Audit Logs",
    description: "System and user activity logs",
    icon: Clock,
  },
  trips: {
    label: "Trip Data",
    description: "Recorded trips and routes",
    icon: Database,
  },
  alerts: {
    label: "Alerts",
    description: "Geofence and proximity alert history",
    icon: AlertCircle,
  },
  device_events: {
    label: "Device Events",
    description: "Device status and connection events",
    icon: HardDrive,
  },
};

export function RetentionSettings() {
  const [notification, setNotification] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // State
  const [config, setConfig] = useState<RetentionConfig | null>(null);
  const [stats, setStats] = useState<RetentionStats[]>([]);
  const [formData, setFormData] = useState<UpdateRetentionConfigRequest>({});
  const [hasChanges, setHasChanges] = useState(false);

  // View state
  const [activeView, setActiveView] = useState<"settings" | "stats">("settings");
  const [previewStats, setPreviewStats] = useState<RetentionStats[] | null>(null);
  const [showPreview, setShowPreview] = useState(false);

  // API hooks
  const { loading: loadingConfig, execute: fetchConfig } = useApi<RetentionConfig>();
  const { loading: savingConfig, execute: saveConfig } = useApi<RetentionConfig>();
  const { loading: loadingStats, execute: fetchStats } = useApi<RetentionStats[]>();
  const { loading: loadingPreview, execute: fetchPreview } = useApi<RetentionStats[]>();

  // Load data
  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadData = async () => {
    const [configResult, statsResult] = await Promise.all([
      fetchConfig(() => systemRetentionApi.get()),
      fetchStats(() => systemRetentionApi.getStats()),
    ]);

    if (configResult) {
      setConfig(configResult);
      setFormData({
        default_retention_period: configResult.default_retention_period,
        data_types: configResult.data_types.map((dt) => ({
          data_type: dt.data_type,
          retention_period: dt.retention_period,
        })),
        inactive_device_threshold_days: configResult.inactive_device_threshold_days,
        auto_cleanup_enabled: configResult.auto_cleanup_enabled,
      });
      setHasChanges(false);
    }

    if (statsResult) {
      setStats(statsResult);
    }
  };

  const handleFormChange = (
    field: keyof UpdateRetentionConfigRequest,
    value: unknown
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setHasChanges(true);
  };

  const handleDataTypeChange = (dataType: string, period: RetentionPeriod) => {
    const currentTypes = formData.data_types || [];
    const updated = currentTypes.map((dt) =>
      dt.data_type === dataType ? { ...dt, retention_period: period } : dt
    );
    handleFormChange("data_types", updated);
  };

  const handleSave = async () => {
    const result = await saveConfig(() => systemRetentionApi.update(formData));
    if (result) {
      setConfig(result);
      setHasChanges(false);
      setNotification({ type: "success", message: "Retention settings saved" });
      setTimeout(() => setNotification(null), 3000);
    } else {
      setNotification({ type: "error", message: "Failed to save settings" });
      setTimeout(() => setNotification(null), 3000);
    }
  };

  const handlePreview = async () => {
    const result = await fetchPreview(() => systemRetentionApi.preview());
    if (result) {
      setPreviewStats(result);
      setShowPreview(true);
    }
  };

  const getDataTypePeriod = (dataType: string): RetentionPeriod => {
    const dt = formData.data_types?.find((d) => d.data_type === dataType);
    return dt?.retention_period || formData.default_retention_period || "90d";
  };

  const formatStorage = (mb: number): string => {
    if (mb >= 1024) {
      return `${(mb / 1024).toFixed(2)} GB`;
    }
    return `${mb.toFixed(2)} MB`;
  };

  if (loadingConfig && !config) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6" data-testid="retention-settings-container">
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

      {/* Info Banner */}
      <div className="p-4 rounded-lg bg-blue-50 border border-blue-200 flex items-start gap-3">
        <Info className="h-5 w-5 text-blue-600 mt-0.5" />
        <div>
          <p className="text-blue-800 font-medium">Data Retention Policies</p>
          <p className="text-blue-700 text-sm">
            Configure how long data is retained before automatic cleanup. Shorter
            retention periods reduce storage costs but remove historical data.
          </p>
        </div>
      </div>

      {/* View Tabs */}
      <div className="flex gap-2">
        <Button
          variant={activeView === "settings" ? "default" : "outline"}
          onClick={() => setActiveView("settings")}
          className="flex items-center gap-2"
          data-testid="retention-tab-settings"
        >
          <Database className="h-4 w-4" />
          Retention Settings
        </Button>
        <Button
          variant={activeView === "stats" ? "default" : "outline"}
          onClick={() => setActiveView("stats")}
          className="flex items-center gap-2"
          data-testid="retention-tab-stats"
        >
          <HardDrive className="h-4 w-4" />
          Storage Statistics
        </Button>
      </div>

      {/* Settings View */}
      {activeView === "settings" && (
        <div className="space-y-6">
          {/* Default Retention Period */}
          <div className="space-y-3">
            <Label className="text-base font-semibold">Default Retention Period</Label>
            <p className="text-sm text-muted-foreground">
              Applied to data types without specific settings
            </p>
            <div className="grid grid-cols-3 md:grid-cols-6 gap-2">
              {(Object.keys(retentionPeriodLabels) as RetentionPeriod[]).map(
                (period) => (
                  <Button
                    key={period}
                    variant={
                      formData.default_retention_period === period
                        ? "default"
                        : "outline"
                    }
                    size="sm"
                    onClick={() =>
                      handleFormChange("default_retention_period", period)
                    }
                    data-testid={`retention-default-period-${period}`}
                  >
                    {retentionPeriodLabels[period]}
                  </Button>
                )
              )}
            </div>
          </div>

          {/* Per-Data-Type Settings */}
          <div className="space-y-3">
            <Label className="text-base font-semibold">Per-Data-Type Retention</Label>
            <p className="text-sm text-muted-foreground">
              Override the default retention period for specific data types
            </p>

            <div className="space-y-3">
              {config?.data_types.map((dt) => {
                const typeInfo = dataTypeLabels[dt.data_type] || {
                  label: dt.data_type,
                  description: dt.description,
                  icon: Database,
                };
                const Icon = typeInfo.icon;

                return (
                  <div
                    key={dt.data_type}
                    className="p-4 rounded-lg border flex flex-col md:flex-row md:items-center justify-between gap-4"
                    data-testid={`retention-data-type-${dt.data_type}`}
                  >
                    <div className="flex items-start gap-3">
                      <Icon className="h-5 w-5 text-muted-foreground mt-0.5" />
                      <div>
                        <div className="font-medium">{typeInfo.label}</div>
                        <p className="text-sm text-muted-foreground">
                          {typeInfo.description}
                        </p>
                      </div>
                    </div>
                    <select
                      value={getDataTypePeriod(dt.data_type)}
                      onChange={(e) =>
                        handleDataTypeChange(
                          dt.data_type,
                          e.target.value as RetentionPeriod
                        )
                      }
                      className="px-3 py-2 border rounded-md bg-background text-sm min-w-[140px]"
                      data-testid={`retention-data-type-select-${dt.data_type}`}
                    >
                      {(
                        Object.keys(retentionPeriodLabels) as RetentionPeriod[]
                      ).map((period) => (
                        <option key={period} value={period}>
                          {retentionPeriodLabels[period]}
                        </option>
                      ))}
                    </select>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Inactive Device Cleanup */}
          <div className="space-y-3">
            <Label className="text-base font-semibold">Inactive Device Cleanup</Label>
            <p className="text-sm text-muted-foreground">
              Automatically archive devices that haven&apos;t been seen for the specified
              period
            </p>

            <div className="flex items-center gap-4">
              <div className="flex-1 max-w-xs">
                <div className="flex items-center gap-2">
                  <Input
                    type="number"
                    min={30}
                    max={365}
                    value={formData.inactive_device_threshold_days || ""}
                    onChange={(e) =>
                      handleFormChange(
                        "inactive_device_threshold_days",
                        parseInt(e.target.value, 10)
                      )
                    }
                    className="w-24"
                    data-testid="retention-inactive-device-threshold"
                  />
                  <span className="text-sm text-muted-foreground">days</span>
                </div>
              </div>

              {/* Quick presets */}
              <div className="flex gap-1">
                {[30, 60, 90, 180].map((days) => (
                  <Button
                    key={days}
                    variant="outline"
                    size="sm"
                    className="h-8 text-xs"
                    onClick={() =>
                      handleFormChange("inactive_device_threshold_days", days)
                    }
                    data-testid={`retention-inactive-preset-${days}d`}
                  >
                    {days}d
                  </Button>
                ))}
              </div>
            </div>
          </div>

          {/* Auto Cleanup Toggle */}
          <div
            onClick={() =>
              handleFormChange(
                "auto_cleanup_enabled",
                !formData.auto_cleanup_enabled
              )
            }
            className={`p-4 rounded-lg border cursor-pointer transition-colors flex items-center justify-between ${
              formData.auto_cleanup_enabled
                ? "border-primary bg-primary/5"
                : "border-border"
            }`}
            data-testid="retention-auto-cleanup-toggle"
          >
            <div>
              <div className="font-medium">Automatic Cleanup</div>
              <p className="text-sm text-muted-foreground">
                Automatically delete data older than the retention period
              </p>
              {config?.next_cleanup_at && (
                <p className="text-xs text-muted-foreground mt-1">
                  Next cleanup: {new Date(config.next_cleanup_at).toLocaleString()}
                </p>
              )}
            </div>
            <div
              className={`w-10 h-6 rounded-full transition-colors ${
                formData.auto_cleanup_enabled ? "bg-primary" : "bg-muted"
              }`}
            >
              <div
                className={`w-5 h-5 rounded-full bg-white shadow transition-transform mt-0.5 ${
                  formData.auto_cleanup_enabled
                    ? "translate-x-4.5 ml-0.5"
                    : "translate-x-0.5"
                }`}
              />
            </div>
          </div>

          {/* Last Cleanup Info */}
          {config?.last_cleanup_at && (
            <div className="text-sm text-muted-foreground flex items-center gap-2">
              <Calendar className="h-4 w-4" />
              Last cleanup: {new Date(config.last_cleanup_at).toLocaleString()}
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center justify-between pt-4 border-t">
            <Button
              variant="outline"
              onClick={handlePreview}
              disabled={loadingPreview}
              data-testid="retention-preview-cleanup"
            >
              {loadingPreview ? (
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
              ) : (
                <Trash2 className="h-4 w-4 mr-2" />
              )}
              Preview Cleanup
            </Button>

            <Button onClick={handleSave} disabled={!hasChanges || savingConfig} data-testid="retention-save-button">
              {savingConfig ? (
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
              ) : (
                <Save className="h-4 w-4 mr-2" />
              )}
              Save Changes
            </Button>
          </div>
        </div>
      )}

      {/* Stats View */}
      {activeView === "stats" && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              Current storage usage by data type
            </p>
            <Button
              variant="outline"
              size="sm"
              onClick={() => fetchStats(() => systemRetentionApi.getStats())}
              disabled={loadingStats}
              data-testid="retention-refresh-stats"
            >
              {loadingStats ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <RefreshCw className="h-4 w-4" />
              )}
              <span className="ml-2">Refresh</span>
            </Button>
          </div>

          {/* Total Storage Summary */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <div className="p-4 rounded-lg bg-blue-50 border border-blue-200" data-testid="retention-total-storage">
              <div className="text-2xl font-bold text-blue-700">
                {formatStorage(
                  stats.reduce((sum, s) => sum + s.storage_used_mb, 0)
                )}
              </div>
              <div className="text-sm text-blue-600">Total Storage</div>
            </div>
            <div className="p-4 rounded-lg bg-green-50 border border-green-200" data-testid="retention-total-records">
              <div className="text-2xl font-bold text-green-700">
                {stats.reduce((sum, s) => sum + s.total_records, 0).toLocaleString()}
              </div>
              <div className="text-sm text-green-600">Total Records</div>
            </div>
            <div className="p-4 rounded-lg bg-amber-50 border border-amber-200" data-testid="retention-pending-deletion">
              <div className="text-2xl font-bold text-amber-700">
                {stats
                  .reduce((sum, s) => sum + s.records_to_delete, 0)
                  .toLocaleString()}
              </div>
              <div className="text-sm text-amber-600">Pending Deletion</div>
            </div>
            <div className="p-4 rounded-lg bg-gray-50 border border-gray-200" data-testid="retention-data-types-count">
              <div className="text-2xl font-bold text-gray-700">
                {stats.length}
              </div>
              <div className="text-sm text-gray-600">Data Types</div>
            </div>
          </div>

          {/* Per-Type Stats */}
          <div className="space-y-3">
            {stats.map((stat) => {
              const typeInfo = dataTypeLabels[stat.data_type] || {
                label: stat.data_type,
                description: "",
                icon: Database,
              };
              const Icon = typeInfo.icon;
              const deletePercent =
                stat.total_records > 0
                  ? (stat.records_to_delete / stat.total_records) * 100
                  : 0;

              return (
                <div key={stat.data_type} className="p-4 rounded-lg border" data-testid={`retention-stat-${stat.data_type}`}>
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <Icon className="h-4 w-4 text-muted-foreground" />
                      <span className="font-medium">{typeInfo.label}</span>
                    </div>
                    <span className="text-sm font-semibold">
                      {formatStorage(stat.storage_used_mb)}
                    </span>
                  </div>

                  <div className="grid grid-cols-3 gap-4 text-sm mb-3">
                    <div>
                      <p className="text-muted-foreground">Total Records</p>
                      <p className="font-semibold">
                        {stat.total_records.toLocaleString()}
                      </p>
                    </div>
                    <div>
                      <p className="text-muted-foreground">To Delete</p>
                      <p className="font-semibold text-amber-600">
                        {stat.records_to_delete.toLocaleString()}
                      </p>
                    </div>
                    <div>
                      <p className="text-muted-foreground">Oldest Record</p>
                      <p className="font-semibold">
                        {stat.oldest_record_date
                          ? new Date(stat.oldest_record_date).toLocaleDateString()
                          : "N/A"}
                      </p>
                    </div>
                  </div>

                  {/* Deletion progress bar */}
                  {stat.records_to_delete > 0 && (
                    <div>
                      <div className="flex justify-between text-xs text-muted-foreground mb-1">
                        <span>Records to delete</span>
                        <span>{deletePercent.toFixed(1)}%</span>
                      </div>
                      <div className="h-2 bg-muted rounded-full overflow-hidden">
                        <div
                          className="h-full bg-amber-500 transition-all duration-300"
                          style={{ width: `${deletePercent}%` }}
                        />
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Preview Modal */}
      {showPreview && previewStats && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setShowPreview(false)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-lg mx-4 p-6" data-testid="retention-preview-dialog">
            <div className="flex items-center gap-3 mb-4">
              <AlertTriangle className="h-6 w-6 text-amber-500" />
              <h3 className="text-lg font-semibold">Cleanup Preview</h3>
            </div>

            <p className="text-muted-foreground mb-4">
              The following data will be removed when cleanup runs:
            </p>

            <div className="space-y-2 mb-4">
              {previewStats.map((stat) => {
                const typeInfo = dataTypeLabels[stat.data_type] || {
                  label: stat.data_type,
                  description: "",
                  icon: Database,
                };

                return (
                  <div
                    key={stat.data_type}
                    className="flex items-center justify-between p-3 rounded-lg bg-muted/50"
                  >
                    <span>{typeInfo.label}</span>
                    <div className="text-right">
                      <div className="font-semibold text-amber-600">
                        {stat.records_to_delete.toLocaleString()} records
                      </div>
                      <div className="text-xs text-muted-foreground">
                        {formatStorage(
                          stat.total_records > 0
                            ? (stat.records_to_delete / stat.total_records) *
                                stat.storage_used_mb
                            : 0
                        )}{" "}
                        to free
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="p-3 rounded-lg bg-amber-50 border border-amber-200 mb-4">
              <p className="text-amber-700 text-sm">
                <strong>Total to delete:</strong>{" "}
                {previewStats
                  .reduce((sum, s) => sum + s.records_to_delete, 0)
                  .toLocaleString()}{" "}
                records
              </p>
            </div>

            <div className="flex justify-end">
              <Button onClick={() => setShowPreview(false)} data-testid="retention-preview-close">Close</Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
