"use client";

import { useState, useEffect, useId, useCallback } from "react";
import type { Organization } from "@/types";
import { organizationsApi, type OrganizationStats } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { X, BarChart3, Users, Smartphone, FolderTree, HardDrive, RefreshCw, AlertCircle, Download } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface OrganizationStatsDialogProps {
  organization: Organization;
  onClose: () => void;
}

export function OrganizationStatsDialog({ organization, onClose }: OrganizationStatsDialogProps) {
  const [stats, setStats] = useState<OrganizationStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onClose });
  const titleId = useId();
  const descriptionId = useId();

  const fetchStats = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const result = await organizationsApi.getStats(organization.id);

      if (result.error) {
        setError(result.error);
        return;
      }

      if (result.data) {
        setStats(result.data);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load statistics");
    } finally {
      setLoading(false);
    }
  }, [organization.id]);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  const formatStorageSize = (mb: number): string => {
    if (mb >= 1024) {
      return `${(mb / 1024).toFixed(1)} GB`;
    }
    return `${mb} MB`;
  };

  const exportToCSV = useCallback(() => {
    if (!stats) return;

    const rows: string[] = [];

    // Header section
    rows.push(`Organization Statistics: ${organization.name}`);
    rows.push(`Export Date: ${new Date().toISOString()}`);
    rows.push("");

    // Summary section
    rows.push("Summary Statistics");
    rows.push("Metric,Current,Limit");
    rows.push(`Users,${stats.users_count},${organization.max_users}`);
    rows.push(`Devices,${stats.devices_count},${organization.max_devices}`);
    rows.push(`Groups,${stats.groups_count},${organization.max_groups || "Unlimited"}`);
    rows.push(`Storage Used (MB),${stats.storage_used_mb},`);
    rows.push("");

    // Usage trends section
    if (stats.usage_trends && stats.usage_trends.length > 0) {
      rows.push("Usage Trends");
      rows.push("Period,Users,Devices");
      stats.usage_trends.forEach(trend => {
        rows.push(`${trend.period},${trend.users},${trend.devices}`);
      });
    }

    const csvContent = rows.join("\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `${organization.name.toLowerCase().replace(/\s+/g, "-")}-stats-${new Date().toISOString().split("T")[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }, [stats, organization.name, organization.max_users, organization.max_devices, organization.max_groups]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
    >
      <Card ref={dialogRef} className="w-full max-w-lg mx-4">
        <CardHeader className="relative">
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="absolute right-4 top-4"
            onClick={onClose}
            aria-label="Close dialog"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </Button>
          <CardTitle id={titleId} className="flex items-center gap-2">
            <BarChart3 className="h-5 w-5" aria-hidden="true" />
            Organization Statistics
          </CardTitle>
          <CardDescription id={descriptionId}>
            Usage statistics for {organization.name}
          </CardDescription>
        </CardHeader>

        <CardContent>
          {loading && (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          )}

          {error && (
            <div className="flex flex-col items-center gap-4 py-8">
              <div className="flex items-center gap-2 text-destructive">
                <AlertCircle className="h-5 w-5" />
                <span>{error}</span>
              </div>
              <Button variant="outline" size="sm" onClick={fetchStats}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          )}

          {!loading && !error && stats && (
            <div className="space-y-6">
              {/* Summary Stats */}
              <div className="grid grid-cols-2 gap-4">
                <div className="flex items-center gap-3 p-4 border rounded-lg">
                  <div className="p-2 bg-primary/10 rounded-lg">
                    <Users className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">{stats.users_count}</p>
                    <p className="text-sm text-muted-foreground">
                      of {organization.max_users} users
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3 p-4 border rounded-lg">
                  <div className="p-2 bg-primary/10 rounded-lg">
                    <Smartphone className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">{stats.devices_count}</p>
                    <p className="text-sm text-muted-foreground">
                      of {organization.max_devices} devices
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3 p-4 border rounded-lg">
                  <div className="p-2 bg-primary/10 rounded-lg">
                    <FolderTree className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">{stats.groups_count}</p>
                    <p className="text-sm text-muted-foreground">
                      of {organization.max_groups || "∞"} groups
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3 p-4 border rounded-lg">
                  <div className="p-2 bg-primary/10 rounded-lg">
                    <HardDrive className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">{formatStorageSize(stats.storage_used_mb)}</p>
                    <p className="text-sm text-muted-foreground">storage used</p>
                  </div>
                </div>
              </div>

              {/* Usage Trends */}
              {stats.usage_trends && stats.usage_trends.length > 0 && (
                <div className="space-y-3">
                  <h4 className="text-sm font-medium">Usage Trends</h4>
                  <div className="border rounded-lg overflow-hidden">
                    <table className="w-full text-sm">
                      <thead className="bg-muted/50">
                        <tr>
                          <th className="text-left py-2 px-3 font-medium">Period</th>
                          <th className="text-right py-2 px-3 font-medium">Users</th>
                          <th className="text-right py-2 px-3 font-medium">Devices</th>
                        </tr>
                      </thead>
                      <tbody>
                        {stats.usage_trends.map((trend, index) => (
                          <tr key={index} className="border-t">
                            <td className="py-2 px-3">{trend.period}</td>
                            <td className="text-right py-2 px-3">{trend.users}</td>
                            <td className="text-right py-2 px-3">{trend.devices}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex justify-end gap-2">
                <Button variant="outline" size="sm" onClick={exportToCSV}>
                  <Download className="h-4 w-4 mr-2" />
                  Export CSV
                </Button>
                <Button variant="outline" size="sm" onClick={fetchStats}>
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Refresh
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
