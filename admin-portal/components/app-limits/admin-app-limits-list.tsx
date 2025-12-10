"use client";

import { useState, useEffect, useCallback } from "react";
import type { AppLimit, Organization, AdminDevice, AdminGroup } from "@/types";
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
import Link from "next/link";
import {
  Shield,
  RefreshCw,
  Search,
  Plus,
  ToggleLeft,
  ToggleRight,
  Trash2,
  Clock,
  Smartphone,
  Users,
  AlertTriangle,
} from "lucide-react";

const ITEMS_PER_PAGE = 20;

export function AdminAppLimitsList() {
  const [search, setSearch] = useState("");
  const [organizationId, setOrganizationId] = useState("");
  const [deviceId, setDeviceId] = useState("");
  const [groupId, setGroupId] = useState("");
  const [targetType, setTargetType] = useState<"" | "app" | "category">("");
  const [page, setPage] = useState(1);
  const [limitToDelete, setLimitToDelete] = useState<AppLimit | null>(null);
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();
  const { data: groupsData, execute: fetchGroups } = useApi<{ items: AdminGroup[] }>();
  const { data: limitsData, loading, error, execute: fetchLimits } = useApi<{ items: AppLimit[]; total: number }>();
  const { execute: toggleLimit } = useApi<AppLimit>();
  const { execute: deleteLimit } = useApi<void>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
    fetchDevices(() => adminDevicesApi.list({ limit: 100 }));
    fetchGroups(() => adminGroupsApi.list({ limit: 100 }));
  }, [fetchOrgs, fetchDevices, fetchGroups]);

  const loadLimits = useCallback(() => {
    fetchLimits(() =>
      appLimitsApi.list({
        organization_id: organizationId || undefined,
        device_id: deviceId || undefined,
        group_id: groupId || undefined,
        target_type: targetType || undefined,
        page,
        limit: ITEMS_PER_PAGE,
      })
    );
  }, [fetchLimits, organizationId, deviceId, groupId, targetType, page]);

  useEffect(() => {
    loadLimits();
  }, [loadLimits]);

  // Clear notification after 3 seconds
  useEffect(() => {
    if (notification) {
      const timer = setTimeout(() => setNotification(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [notification]);

  // Filter devices by organization
  const filteredDevices = organizationId
    ? devicesData?.items?.filter((d) => d.organization_id === organizationId)
    : devicesData?.items;

  // Filter groups by organization
  const filteredGroups = organizationId
    ? groupsData?.items?.filter((g) => g.organization_id === organizationId)
    : groupsData?.items;

  const handleToggle = async (limit: AppLimit) => {
    const result = await toggleLimit(() => appLimitsApi.toggle(limit.id));
    if (result) {
      setNotification({
        type: "success",
        message: `Limit ${result.enabled ? "enabled" : "disabled"} successfully`,
      });
      loadLimits();
    } else {
      setNotification({ type: "error", message: "Failed to toggle limit" });
    }
  };

  const handleDelete = async () => {
    if (!limitToDelete) return;
    const result = await deleteLimit(() => appLimitsApi.delete(limitToDelete.id));
    if (result !== undefined) {
      setNotification({ type: "success", message: "Limit deleted successfully" });
      setLimitToDelete(null);
      loadLimits();
    } else {
      setNotification({ type: "error", message: "Failed to delete limit" });
    }
  };

  const formatTime = (minutes: number | null) => {
    if (!minutes) return "-";
    if (minutes >= 60) {
      const hours = Math.floor(minutes / 60);
      const mins = minutes % 60;
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${minutes}m`;
  };

  const limits = limitsData?.items || [];
  const filteredLimits = search
    ? limits.filter(
        (l) =>
          l.name.toLowerCase().includes(search.toLowerCase()) ||
          l.target_display.toLowerCase().includes(search.toLowerCase())
      )
    : limits;
  const total = limitsData?.total || 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Shield className="h-6 w-6" />
            <div>
              <CardTitle>App Limits</CardTitle>
              <CardDescription>
                Configure time limits and app restrictions
              </CardDescription>
            </div>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={loadLimits}>
              <RefreshCw className="mr-2 h-4 w-4" />
              Refresh
            </Button>
            <Link href="/app-limits/new">
              <Button size="sm">
                <Plus className="mr-2 h-4 w-4" />
                Add Limit
              </Button>
            </Link>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {/* Notification */}
        {notification && (
          <div
            className={`mb-4 p-3 rounded-md ${
              notification.type === "success"
                ? "bg-green-100 text-green-800"
                : "bg-red-100 text-red-800"
            }`}
          >
            {notification.message}
          </div>
        )}

        {/* Filters */}
        <div className="flex flex-wrap gap-4 mb-6">
          <div className="flex-1 min-w-[200px]">
            <div className="relative">
              <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search by name or target..."
                className="pl-8"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={organizationId}
            onChange={(e) => {
              setOrganizationId(e.target.value);
              setDeviceId("");
              setGroupId("");
            }}
          >
            <option value="">All Organizations</option>
            {orgsData?.items?.map((org) => (
              <option key={org.id} value={org.id}>
                {org.name}
              </option>
            ))}
          </select>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={deviceId}
            onChange={(e) => setDeviceId(e.target.value)}
          >
            <option value="">All Devices</option>
            {filteredDevices?.map((device) => (
              <option key={device.id} value={device.id}>
                {device.display_name}
              </option>
            ))}
          </select>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={groupId}
            onChange={(e) => setGroupId(e.target.value)}
          >
            <option value="">All Groups</option>
            {filteredGroups?.map((group) => (
              <option key={group.id} value={group.id}>
                {group.name}
              </option>
            ))}
          </select>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={targetType}
            onChange={(e) => setTargetType(e.target.value as "" | "app" | "category")}
          >
            <option value="">All Types</option>
            <option value="app">App</option>
            <option value="category">Category</option>
          </select>
        </div>

        {/* Table */}
        {loading ? (
          <div className="flex justify-center py-8">
            <div className="text-muted-foreground">Loading limits...</div>
          </div>
        ) : error ? (
          <div className="flex justify-center py-8 text-red-500">
            Error loading limits
          </div>
        ) : filteredLimits.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
            <Shield className="h-12 w-12 mb-4 opacity-50" />
            <p>No app limits configured</p>
            <Link href="/app-limits/new">
              <Button className="mt-4" size="sm">
                <Plus className="mr-2 h-4 w-4" />
                Create First Limit
              </Button>
            </Link>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="py-3 px-4 text-left text-sm font-medium">Name</th>
                    <th className="py-3 px-4 text-left text-sm font-medium">Target</th>
                    <th className="py-3 px-4 text-left text-sm font-medium">Type</th>
                    <th className="py-3 px-4 text-left text-sm font-medium">Limits</th>
                    <th className="py-3 px-4 text-left text-sm font-medium">Applied To</th>
                    <th className="py-3 px-4 text-left text-sm font-medium">Status</th>
                    <th className="py-3 px-4 text-right text-sm font-medium">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredLimits.map((limit) => (
                    <tr key={limit.id} className="border-b hover:bg-secondary/50">
                      <td className="py-3 px-4">
                        <div className="font-medium">{limit.name}</div>
                        <div className="text-xs text-muted-foreground">
                          {limit.organization_name}
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2">
                          {limit.target_type === "category" ? (
                            <AppCategoryBadge
                              category={limit.target_value as "social" | "games" | "productivity" | "entertainment" | "education" | "communication" | "other"}
                            />
                          ) : (
                            <span className="text-sm">{limit.target_display}</span>
                          )}
                        </div>
                        <div className="text-xs text-muted-foreground">
                          {limit.target_type === "app" ? "App" : "Category"}
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <LimitTypeBadge type={limit.limit_type} />
                      </td>
                      <td className="py-3 px-4">
                        {limit.limit_type === "blocked" ? (
                          <span className="text-sm text-muted-foreground">-</span>
                        ) : (
                          <div className="text-sm">
                            {limit.daily_limit_minutes && (
                              <div className="flex items-center gap-1">
                                <Clock className="h-3 w-3" />
                                {formatTime(limit.daily_limit_minutes)}/day
                              </div>
                            )}
                            {limit.weekly_limit_minutes && (
                              <div className="flex items-center gap-1">
                                <Clock className="h-3 w-3" />
                                {formatTime(limit.weekly_limit_minutes)}/week
                              </div>
                            )}
                          </div>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        {limit.device_name ? (
                          <div className="flex items-center gap-1 text-sm">
                            <Smartphone className="h-3 w-3" />
                            {limit.device_name}
                          </div>
                        ) : limit.group_name ? (
                          <div className="flex items-center gap-1 text-sm">
                            <Users className="h-3 w-3" />
                            {limit.group_name}
                          </div>
                        ) : (
                          <span className="text-sm text-muted-foreground">All devices</span>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        <button
                          onClick={() => handleToggle(limit)}
                          className={`flex items-center gap-1 text-sm ${
                            limit.enabled ? "text-green-600" : "text-muted-foreground"
                          }`}
                        >
                          {limit.enabled ? (
                            <>
                              <ToggleRight className="h-5 w-5" />
                              Active
                            </>
                          ) : (
                            <>
                              <ToggleLeft className="h-5 w-5" />
                              Inactive
                            </>
                          )}
                        </button>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Link href={`/app-limits/${limit.id}/edit`}>
                            <Button variant="outline" size="sm">
                              Edit
                            </Button>
                          </Link>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setLimitToDelete(limit)}
                          >
                            <Trash2 className="h-4 w-4" />
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
                <div className="text-sm text-muted-foreground">
                  Showing {(page - 1) * ITEMS_PER_PAGE + 1} to{" "}
                  {Math.min(page * ITEMS_PER_PAGE, total)} of {total} limits
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 1}
                    onClick={() => setPage(page - 1)}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === totalPages}
                    onClick={() => setPage(page + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </CardContent>

      {/* Delete Confirmation Modal */}
      {limitToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center" role="dialog" aria-modal="true">
          <div className="absolute inset-0 bg-black/50" onClick={() => setLimitToDelete(null)} />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="rounded-full bg-red-100 p-2">
                <AlertTriangle className="h-5 w-5 text-red-600" />
              </div>
              <h3 className="text-lg font-semibold">Delete App Limit</h3>
            </div>
            <p className="text-muted-foreground mb-6">
              Are you sure you want to delete the limit &ldquo;{limitToDelete.name}&rdquo;?
              This action cannot be undone.
            </p>
            <div className="flex justify-end gap-3">
              <Button variant="outline" onClick={() => setLimitToDelete(null)}>
                Cancel
              </Button>
              <Button variant="destructive" onClick={handleDelete}>
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}
    </Card>
  );
}
