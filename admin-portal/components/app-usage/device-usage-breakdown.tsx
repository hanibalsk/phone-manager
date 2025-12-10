"use client";

import { useState, useEffect } from "react";
import type { DeviceAppUsage, AdminDevice } from "@/types";
import { AppCategoryBadge } from "./app-category-badge";
import { appUsageApi, adminDevicesApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Smartphone, Clock, ChevronDown, ChevronUp } from "lucide-react";

interface DeviceUsageBreakdownProps {
  organizationId?: string;
  from?: string;
  to?: string;
}

export function DeviceUsageBreakdown({ organizationId, from, to }: DeviceUsageBreakdownProps) {
  const [selectedDeviceId, setSelectedDeviceId] = useState("");
  const [expandedApps, setExpandedApps] = useState<Set<string>>(new Set());

  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();
  const { data: usageData, loading, execute: fetchUsage } = useApi<DeviceAppUsage>();

  useEffect(() => {
    fetchDevices(() =>
      adminDevicesApi.list({
        organization_id: organizationId || undefined,
        limit: 100,
      })
    );
  }, [fetchDevices, organizationId]);

  useEffect(() => {
    if (selectedDeviceId) {
      fetchUsage(() =>
        appUsageApi.getDeviceUsage(selectedDeviceId, {
          from: from || undefined,
          to: to || undefined,
        })
      );
    }
  }, [fetchUsage, selectedDeviceId, from, to]);

  const formatTime = (minutes: number) => {
    if (minutes >= 60) {
      const hours = Math.floor(minutes / 60);
      const mins = minutes % 60;
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${minutes}m`;
  };

  const toggleApp = (packageName: string) => {
    const newExpanded = new Set(expandedApps);
    if (newExpanded.has(packageName)) {
      newExpanded.delete(packageName);
    } else {
      newExpanded.add(packageName);
    }
    setExpandedApps(newExpanded);
  };

  const devices = devicesData?.items || [];
  const sortedApps = usageData?.apps
    ? [...usageData.apps].sort((a, b) => b.usage_minutes - a.usage_minutes)
    : [];

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Smartphone className="h-5 w-5" />
          Device Usage Breakdown
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Device selector */}
        <div>
          <label className="text-sm font-medium">Select Device</label>
          <select
            className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={selectedDeviceId}
            onChange={(e) => setSelectedDeviceId(e.target.value)}
          >
            <option value="">Choose a device...</option>
            {devices.map((device) => (
              <option key={device.id} value={device.id}>
                {device.display_name}
              </option>
            ))}
          </select>
        </div>

        {/* Usage data */}
        {selectedDeviceId && (
          <>
            {loading ? (
              <div className="text-center py-8 text-muted-foreground">
                Loading usage data...
              </div>
            ) : usageData ? (
              <>
                {/* Summary */}
                <div className="flex items-center justify-between p-4 rounded-lg bg-secondary">
                  <div>
                    <div className="text-sm text-muted-foreground">Total Usage</div>
                    <div className="text-2xl font-bold">
                      {formatTime(usageData.total_minutes)}
                    </div>
                  </div>
                  <div>
                    <div className="text-sm text-muted-foreground">Apps Used</div>
                    <div className="text-2xl font-bold">{usageData.apps.length}</div>
                  </div>
                </div>

                {/* App list */}
                <div className="space-y-2">
                  <div className="text-sm font-medium">Apps by Usage</div>
                  {sortedApps.length === 0 ? (
                    <div className="text-center py-4 text-muted-foreground">
                      No usage data for this period
                    </div>
                  ) : (
                    <div className="divide-y divide-border rounded-lg border">
                      {sortedApps.map((app) => {
                        const percentage =
                          usageData.total_minutes > 0
                            ? (app.usage_minutes / usageData.total_minutes) * 100
                            : 0;
                        const isExpanded = expandedApps.has(app.package_name);

                        return (
                          <div key={app.package_name}>
                            <button
                              className="w-full p-3 flex items-center justify-between hover:bg-secondary/50 transition-colors"
                              onClick={() => toggleApp(app.package_name)}
                            >
                              <div className="flex items-center gap-3">
                                <AppCategoryBadge category={app.category} compact />
                                <div className="text-left">
                                  <div className="font-medium">{app.app_name}</div>
                                  <div className="text-xs text-muted-foreground">
                                    {app.package_name}
                                  </div>
                                </div>
                              </div>
                              <div className="flex items-center gap-3">
                                <div className="text-right">
                                  <div className="font-medium flex items-center gap-1">
                                    <Clock className="h-3 w-3" />
                                    {formatTime(app.usage_minutes)}
                                  </div>
                                  <div className="text-xs text-muted-foreground">
                                    {percentage.toFixed(1)}%
                                  </div>
                                </div>
                                {isExpanded ? (
                                  <ChevronUp className="h-4 w-4 text-muted-foreground" />
                                ) : (
                                  <ChevronDown className="h-4 w-4 text-muted-foreground" />
                                )}
                              </div>
                            </button>
                            {isExpanded && (
                              <div className="px-3 pb-3 pt-0 bg-secondary/30">
                                <div className="h-2 w-full rounded-full bg-secondary overflow-hidden">
                                  <div
                                    className="h-full rounded-full bg-primary transition-all duration-300"
                                    style={{ width: `${percentage}%` }}
                                  />
                                </div>
                                <div className="mt-2 grid grid-cols-2 gap-2 text-sm">
                                  <div>
                                    <span className="text-muted-foreground">Category: </span>
                                    <AppCategoryBadge category={app.category} />
                                  </div>
                                </div>
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="text-center py-8 text-muted-foreground">
                No usage data available
              </div>
            )}
          </>
        )}

        {!selectedDeviceId && (
          <div className="text-center py-8 text-muted-foreground">
            Select a device to view usage breakdown
          </div>
        )}
      </CardContent>
    </Card>
  );
}
