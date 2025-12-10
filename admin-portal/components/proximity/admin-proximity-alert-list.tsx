"use client";

import { useState, useEffect, useCallback } from "react";
import type { ProximityAlert, Organization } from "@/types";
import { proximityAlertsApi, organizationsApi } from "@/lib/api-client";
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
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  RefreshCw,
  Plus,
  Search,
  Edit,
  Trash2,
  History,
  Users,
  Ruler,
  Clock,
} from "lucide-react";
import Link from "next/link";

const ITEMS_PER_PAGE = 20;

export function AdminProximityAlertList() {
  const [organizationId, setOrganizationId] = useState("");
  const [enabledFilter, setEnabledFilter] = useState<"" | "true" | "false">("");
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [page, setPage] = useState(1);
  const [alertToDelete, setAlertToDelete] = useState<ProximityAlert | null>(null);

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: alertsData, loading, error, execute: fetchAlerts } = useApi<{ items: ProximityAlert[]; total: number }>();
  const { execute: toggleAlert } = useApi<ProximityAlert>();
  const { loading: deleteLoading, execute: executeDelete } = useApi<void>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
  }, [fetchOrgs]);

  const loadAlerts = useCallback(() => {
    fetchAlerts(() =>
      proximityAlertsApi.list({
        organization_id: organizationId || undefined,
        enabled: enabledFilter ? enabledFilter === "true" : undefined,
        search: search || undefined,
        page,
        limit: ITEMS_PER_PAGE,
      })
    );
  }, [fetchAlerts, organizationId, enabledFilter, search, page]);

  useEffect(() => {
    loadAlerts();
  }, [loadAlerts]);

  const handleSearch = () => {
    setSearch(searchInput);
    setPage(1);
  };

  const handleToggle = async (alert: ProximityAlert) => {
    await toggleAlert(() => proximityAlertsApi.toggle(alert.id));
    loadAlerts();
  };

  const handleDelete = async () => {
    if (!alertToDelete) return;

    const result = await executeDelete(() => proximityAlertsApi.delete(alertToDelete.id));
    if (result !== undefined) {
      setAlertToDelete(null);
      loadAlerts();
    }
  };

  const formatDistance = (meters: number) => {
    if (meters >= 1000) {
      return `${(meters / 1000).toFixed(1)} km`;
    }
    return `${meters} m`;
  };

  const formatCooldown = (seconds: number) => {
    if (seconds < 60) return `${seconds}s`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m`;
    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  };

  const formatDateTime = (dateString: string | null) => {
    if (!dateString) return "Never";
    return new Date(dateString).toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const alerts = alertsData?.items || [];
  const total = alertsData?.total || 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  return (
    <>
      <Card data-testid="proximity-alert-list-card">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Proximity Alerts</CardTitle>
              <CardDescription>
                {total} alert{total !== 1 ? "s" : ""} configured
              </CardDescription>
            </div>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={loadAlerts} disabled={loading}>
                <RefreshCw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
                Refresh
              </Button>
              <Link href="/proximity-alerts/new">
                <Button size="sm">
                  <Plus className="h-4 w-4 mr-2" />
                  New Alert
                </Button>
              </Link>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {/* Filters */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div className="space-y-2">
              <Label htmlFor="org">Organization</Label>
              <select
                id="org"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={organizationId}
                onChange={(e) => {
                  setOrganizationId(e.target.value);
                  setPage(1);
                }}
                data-testid="proximity-org-filter"
              >
                <option value="">All Organizations</option>
                {orgsData?.items?.map((org) => (
                  <option key={org.id} value={org.id}>
                    {org.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="enabled">Status</Label>
              <select
                id="enabled"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={enabledFilter}
                onChange={(e) => {
                  setEnabledFilter(e.target.value as "" | "true" | "false");
                  setPage(1);
                }}
                data-testid="proximity-status-filter"
              >
                <option value="">All Statuses</option>
                <option value="true">Enabled</option>
                <option value="false">Disabled</option>
              </select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="search">Search</Label>
              <div className="flex gap-2">
                <Input
                  id="search"
                  placeholder="Search alerts..."
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                  data-testid="proximity-search-input"
                />
                <Button variant="outline" size="icon" onClick={handleSearch}>
                  <Search className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>

          {/* Error State */}
          {error && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <p className="text-destructive mb-4">{error}</p>
              <Button variant="outline" onClick={loadAlerts}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          )}

          {/* Loading State */}
          {loading && !alertsData && (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="flex items-center gap-4 py-3">
                  <div className="h-4 w-32 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-48 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-20 bg-muted animate-pulse rounded" />
                </div>
              ))}
            </div>
          )}

          {/* Empty State */}
          {!loading && !error && alerts.length === 0 && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <Users className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No proximity alerts found</p>
              <p className="text-sm text-muted-foreground mt-1">
                Create an alert to monitor distance between device pairs
              </p>
              <Link href="/proximity-alerts/new" className="mt-4">
                <Button>
                  <Plus className="h-4 w-4 mr-2" />
                  Create Alert
                </Button>
              </Link>
            </div>
          )}

          {/* Alerts Table */}
          {!error && alerts.length > 0 && (
            <>
              <div className="overflow-x-auto">
                <table className="w-full" data-testid="proximity-alert-table">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left py-3 px-4 font-medium">Alert Name</th>
                      <th className="text-left py-3 px-4 font-medium">Device Pair</th>
                      <th className="text-left py-3 px-4 font-medium">Distance</th>
                      <th className="text-left py-3 px-4 font-medium">Cooldown</th>
                      <th className="text-left py-3 px-4 font-medium">Triggers</th>
                      <th className="text-left py-3 px-4 font-medium">Status</th>
                      <th className="text-right py-3 px-4 font-medium">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {alerts.map((alert) => (
                      <tr key={alert.id} className="border-b hover:bg-muted/50" data-testid={`proximity-alert-row-${alert.id}`}>
                        <td className="py-3 px-4">
                          <div className="font-medium">{alert.name}</div>
                          <div className="text-xs text-muted-foreground">
                            {alert.organization_name}
                          </div>
                        </td>
                        <td className="py-3 px-4 text-sm">
                          <div className="flex items-center gap-2">
                            <Users className="h-4 w-4 text-muted-foreground" />
                            <div>
                              <div>{alert.device_a_name}</div>
                              <div className="text-muted-foreground">↔ {alert.device_b_name}</div>
                            </div>
                          </div>
                        </td>
                        <td className="py-3 px-4 text-sm">
                          <div className="flex items-center gap-1">
                            <Ruler className="h-4 w-4 text-muted-foreground" />
                            {formatDistance(alert.trigger_distance_meters)}
                          </div>
                        </td>
                        <td className="py-3 px-4 text-sm">
                          <div className="flex items-center gap-1">
                            <Clock className="h-4 w-4 text-muted-foreground" />
                            {formatCooldown(alert.cooldown_seconds)}
                          </div>
                        </td>
                        <td className="py-3 px-4 text-sm">
                          <div>{alert.trigger_count} triggers</div>
                          <div className="text-xs text-muted-foreground">
                            Last: {formatDateTime(alert.last_triggered_at)}
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <Switch
                            checked={alert.enabled}
                            onCheckedChange={() => handleToggle(alert)}
                          />
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex items-center justify-end gap-2">
                            <Link href={`/proximity-alerts/${alert.id}/history`}>
                              <Button variant="ghost" size="icon" title="View History">
                                <History className="h-4 w-4" />
                              </Button>
                            </Link>
                            <Link href={`/proximity-alerts/${alert.id}/edit`}>
                              <Button variant="ghost" size="icon" title="Edit">
                                <Edit className="h-4 w-4" />
                              </Button>
                            </Link>
                            <Button
                              variant="ghost"
                              size="icon"
                              title="Delete"
                              onClick={() => setAlertToDelete(alert)}
                            >
                              <Trash2 className="h-4 w-4 text-destructive" />
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {page} of {totalPages}
                  </p>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.max(1, p - 1))}
                      disabled={page === 1}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                      disabled={page === totalPages}
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      {/* Delete Confirmation */}
      {alertToDelete && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
          data-testid="proximity-delete-dialog"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setAlertToDelete(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <h2 className="text-lg font-semibold mb-2">Delete Proximity Alert</h2>
            <p className="text-sm text-muted-foreground mb-4">
              Are you sure you want to delete &quot;{alertToDelete.name}&quot;? This action
              cannot be undone and will also delete all trigger history.
            </p>
            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                onClick={() => setAlertToDelete(null)}
                disabled={deleteLoading}
                data-testid="proximity-delete-cancel"
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleDelete}
                disabled={deleteLoading}
                data-testid="proximity-delete-confirm"
              >
                {deleteLoading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
