"use client";

import { useState, useEffect, useCallback } from "react";
import type { Trip, Organization, AdminDevice, TripStatus } from "@/types";
import { TripStatusBadge } from "./trip-status-badge";
import { TripExportModal } from "./trip-export-modal";
import { tripsApi, organizationsApi, adminDevicesApi } from "@/lib/api-client";
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
  Route,
  RefreshCw,
  Search,
  MapPin,
  Clock,
  Gauge,
  Download,
} from "lucide-react";

const ITEMS_PER_PAGE = 20;

export function AdminTripList() {
  const [search, setSearch] = useState("");
  const [organizationId, setOrganizationId] = useState("");
  const [deviceId, setDeviceId] = useState("");
  const [statusFilter, setStatusFilter] = useState<TripStatus | "">("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [page, setPage] = useState(1);
  const [showExportModal, setShowExportModal] = useState(false);

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();
  const { data: tripsData, loading, error, execute: fetchTrips } = useApi<{ items: Trip[]; total: number }>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
    fetchDevices(() => adminDevicesApi.list({ limit: 100 }));
  }, [fetchOrgs, fetchDevices]);

  const loadTrips = useCallback(() => {
    fetchTrips(() =>
      tripsApi.list({
        organization_id: organizationId || undefined,
        device_id: deviceId || undefined,
        status: statusFilter || undefined,
        from: fromDate || undefined,
        to: toDate || undefined,
        page,
        limit: ITEMS_PER_PAGE,
      })
    );
  }, [fetchTrips, organizationId, deviceId, statusFilter, fromDate, toDate, page]);

  useEffect(() => {
    loadTrips();
  }, [loadTrips]);

  // Filter devices by organization
  const filteredDevices = organizationId
    ? devicesData?.items?.filter((d) => d.organization_id === organizationId)
    : devicesData?.items;

  const formatDuration = (seconds: number) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
  };

  const formatDistance = (meters: number) => {
    if (meters >= 1000) {
      return `${(meters / 1000).toFixed(1)} km`;
    }
    return `${meters} m`;
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString();
  };

  const trips = tripsData?.items || [];
  const total = tripsData?.total || 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Trips</CardTitle>
            <CardDescription>
              {total} trip{total !== 1 ? "s" : ""} total
            </CardDescription>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => setShowExportModal(true)}>
              <Download className="h-4 w-4 mr-2" />
              Export
            </Button>
            <Button variant="outline" size="sm" onClick={loadTrips} disabled={loading}>
              <RefreshCw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
              Refresh
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {/* Filters */}
        <div className="flex flex-wrap gap-4 mb-4">
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[180px]"
            value={organizationId}
            onChange={(e) => {
              setOrganizationId(e.target.value);
              setDeviceId(""); // Reset device when org changes
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
            className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[180px]"
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
            className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[130px]"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as TripStatus | "")}
          >
            <option value="">All Status</option>
            <option value="in_progress">In Progress</option>
            <option value="completed">Completed</option>
            <option value="paused">Paused</option>
          </select>
          <Input
            type="date"
            placeholder="From"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
            className="w-[150px]"
          />
          <Input
            type="date"
            placeholder="To"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
            className="w-[150px]"
          />
        </div>

        {/* Error State */}
        {error && (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <p className="text-destructive mb-4">{error}</p>
            <Button variant="outline" onClick={loadTrips}>
              <RefreshCw className="h-4 w-4 mr-2" />
              Retry
            </Button>
          </div>
        )}

        {/* Loading State */}
        {loading && !tripsData && (
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
        {!loading && !error && trips.length === 0 && (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <Route className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">No trips found</p>
          </div>
        )}

        {/* Trip Table */}
        {!error && trips.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="text-left py-3 px-4 font-medium">Device</th>
                    <th className="text-left py-3 px-4 font-medium">Status</th>
                    <th className="text-left py-3 px-4 font-medium">Start Time</th>
                    <th className="text-left py-3 px-4 font-medium">Duration</th>
                    <th className="text-left py-3 px-4 font-medium">Distance</th>
                    <th className="text-left py-3 px-4 font-medium">Points</th>
                    <th className="text-right py-3 px-4 font-medium">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {trips.map((trip) => (
                    <tr key={trip.id} className="border-b hover:bg-muted/50">
                      <td className="py-3 px-4">
                        <div>
                          <p className="font-medium">{trip.device_name}</p>
                          <p className="text-xs text-muted-foreground">
                            {trip.organization_name}
                          </p>
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <TripStatusBadge status={trip.status} />
                      </td>
                      <td className="py-3 px-4 text-sm">
                        {formatDate(trip.start_time)}
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-1 text-sm">
                          <Clock className="h-3 w-3 text-muted-foreground" />
                          {formatDuration(trip.duration_seconds)}
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-1 text-sm">
                          <Gauge className="h-3 w-3 text-muted-foreground" />
                          {formatDistance(trip.distance_meters)}
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-1 text-sm text-muted-foreground">
                          <MapPin className="h-3 w-3" />
                          {trip.point_count}
                        </div>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <Link href={`/trips/${trip.id}`}>
                          <Button variant="outline" size="sm">
                            View Details
                          </Button>
                        </Link>
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

      {/* Export Modal */}
      {showExportModal && (
        <TripExportModal
          initialFilters={{
            organization_id: organizationId || undefined,
            device_id: deviceId || undefined,
            from: fromDate || undefined,
            to: toDate || undefined,
          }}
          onClose={() => setShowExportModal(false)}
        />
      )}
    </Card>
  );
}
