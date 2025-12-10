"use client";

import { useState, useEffect, useCallback } from "react";
import type { Geofence, Organization } from "@/types";
import { GeofenceShapeBadge } from "./geofence-shape-badge";
import { geofencesApi, organizationsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { useDebounce } from "@/hooks/use-debounce";
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
  MapPin,
  RefreshCw,
  Plus,
  Search,
  Edit,
  Trash2,
  ToggleLeft,
  ToggleRight,
} from "lucide-react";

const ITEMS_PER_PAGE = 20;

export function AdminGeofenceList() {
  const [search, setSearch] = useState("");
  const [organizationId, setOrganizationId] = useState("");
  const [enabledFilter, setEnabledFilter] = useState<string>("");
  const [page, setPage] = useState(1);
  const [geofenceToDelete, setGeofenceToDelete] = useState<Geofence | null>(null);
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const debouncedSearch = useDebounce(search, 300);

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: geofencesData, loading, error, execute: fetchGeofences } = useApi<{ items: Geofence[]; total: number }>();
  const { loading: toggleLoading, execute: executeToggle } = useApi<Geofence>();
  const { loading: deleteLoading, execute: executeDelete } = useApi<void>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
  }, [fetchOrgs]);

  const loadGeofences = useCallback(() => {
    fetchGeofences(() =>
      geofencesApi.list({
        search: debouncedSearch || undefined,
        organization_id: organizationId || undefined,
        enabled: enabledFilter === "" ? undefined : enabledFilter === "true",
        page,
        limit: ITEMS_PER_PAGE,
      })
    );
  }, [fetchGeofences, debouncedSearch, organizationId, enabledFilter, page]);

  useEffect(() => {
    loadGeofences();
  }, [loadGeofences]);

  const showNotification = (type: "success" | "error", message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 3000);
  };

  const handleToggle = async (geofence: Geofence) => {
    const result = await executeToggle(() => geofencesApi.toggle(geofence.id));
    if (result) {
      showNotification("success", `Geofence ${result.enabled ? "enabled" : "disabled"}`);
      loadGeofences();
    } else {
      showNotification("error", "Failed to toggle geofence");
    }
  };

  const handleDelete = async () => {
    if (!geofenceToDelete) return;

    const result = await executeDelete(() => geofencesApi.delete(geofenceToDelete.id));
    if (result !== null) {
      showNotification("success", "Geofence deleted");
      setGeofenceToDelete(null);
      loadGeofences();
    } else {
      showNotification("error", "Failed to delete geofence");
    }
  };

  const geofences = geofencesData?.items || [];
  const total = geofencesData?.total || 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Geofences</CardTitle>
              <CardDescription>
                {total} geofence{total !== 1 ? "s" : ""} total
              </CardDescription>
            </div>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={loadGeofences} disabled={loading}>
                <RefreshCw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
                Refresh
              </Button>
              <Link href="/geofences/new">
                <Button size="sm">
                  <Plus className="h-4 w-4 mr-2" />
                  New Geofence
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
                  ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                  : "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400"
              }`}
            >
              {notification.message}
            </div>
          )}

          {/* Filters */}
          <div className="flex flex-wrap gap-4 mb-4">
            <div className="flex-1 min-w-[200px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search geofences..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-9"
                />
              </div>
            </div>
            <select
              className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[180px]"
              value={organizationId}
              onChange={(e) => setOrganizationId(e.target.value)}
            >
              <option value="">All Organizations</option>
              {orgsData?.items?.map((org) => (
                <option key={org.id} value={org.id}>
                  {org.name}
                </option>
              ))}
            </select>
            <select
              className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[120px]"
              value={enabledFilter}
              onChange={(e) => setEnabledFilter(e.target.value)}
            >
              <option value="">All Status</option>
              <option value="true">Enabled</option>
              <option value="false">Disabled</option>
            </select>
          </div>

          {/* Error State */}
          {error && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <p className="text-destructive mb-4">{error}</p>
              <Button variant="outline" onClick={loadGeofences}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          )}

          {/* Loading State */}
          {loading && !geofencesData && (
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
          {!loading && !error && geofences.length === 0 && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <MapPin className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No geofences found</p>
              <Link href="/geofences/new" className="mt-4">
                <Button>
                  <Plus className="h-4 w-4 mr-2" />
                  Create First Geofence
                </Button>
              </Link>
            </div>
          )}

          {/* Geofence Table */}
          {!error && geofences.length > 0 && (
            <>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left py-3 px-4 font-medium">Name</th>
                      <th className="text-left py-3 px-4 font-medium">Device</th>
                      <th className="text-left py-3 px-4 font-medium">Shape</th>
                      <th className="text-left py-3 px-4 font-medium">Triggers</th>
                      <th className="text-left py-3 px-4 font-medium">Status</th>
                      <th className="text-right py-3 px-4 font-medium">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {geofences.map((geofence) => (
                      <tr key={geofence.id} className="border-b hover:bg-muted/50">
                        <td className="py-3 px-4">
                          <div>
                            <p className="font-medium">{geofence.name}</p>
                            <p className="text-xs text-muted-foreground">
                              {geofence.organization_name}
                            </p>
                          </div>
                        </td>
                        <td className="py-3 px-4 text-sm">{geofence.device_name}</td>
                        <td className="py-3 px-4">
                          <GeofenceShapeBadge shape={geofence.shape} />
                          {geofence.shape === "circle" && geofence.radius_meters && (
                            <span className="ml-2 text-xs text-muted-foreground">
                              {geofence.radius_meters}m
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-4 text-sm">
                          <div className="flex gap-1 text-xs">
                            {geofence.trigger_on_enter && (
                              <span className="bg-green-100 dark:bg-green-900/30 px-1.5 py-0.5 rounded">
                                Enter
                              </span>
                            )}
                            {geofence.trigger_on_exit && (
                              <span className="bg-red-100 dark:bg-red-900/30 px-1.5 py-0.5 rounded">
                                Exit
                              </span>
                            )}
                            {geofence.trigger_on_dwell && (
                              <span className="bg-blue-100 dark:bg-blue-900/30 px-1.5 py-0.5 rounded">
                                Dwell
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <span
                            className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                              geofence.enabled
                                ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                                : "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400"
                            }`}
                          >
                            {geofence.enabled ? "Enabled" : "Disabled"}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-right">
                          <div className="flex justify-end gap-1">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleToggle(geofence)}
                              disabled={toggleLoading}
                              title={geofence.enabled ? "Disable" : "Enable"}
                            >
                              {geofence.enabled ? (
                                <ToggleRight className="h-4 w-4" />
                              ) : (
                                <ToggleLeft className="h-4 w-4" />
                              )}
                            </Button>
                            <Link href={`/geofences/${geofence.id}/edit`}>
                              <Button variant="ghost" size="sm">
                                <Edit className="h-4 w-4" />
                              </Button>
                            </Link>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setGeofenceToDelete(geofence)}
                              className="text-destructive hover:text-destructive"
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
      {geofenceToDelete && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setGeofenceToDelete(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <h2 className="text-lg font-semibold mb-2">Delete Geofence</h2>
            <p className="text-sm text-muted-foreground mb-4">
              Are you sure you want to delete &quot;{geofenceToDelete.name}&quot;? This action
              cannot be undone.
            </p>
            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                onClick={() => setGeofenceToDelete(null)}
                disabled={deleteLoading}
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleDelete}
                disabled={deleteLoading}
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
