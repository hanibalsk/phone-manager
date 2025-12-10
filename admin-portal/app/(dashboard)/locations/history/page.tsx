"use client";

import { useState, useEffect, useCallback } from "react";
import type { DeviceLocation, Organization, AdminDevice } from "@/types";
import { locationsApi, organizationsApi, adminDevicesApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import {
  LocationHistoryList,
  LocationHistoryMap,
  ExportDropdown,
} from "@/components/locations";
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
import Link from "next/link";
import {
  ArrowLeft,
  RefreshCw,
  Search,
  AlertTriangle,
  List,
  Map,
} from "lucide-react";

const MAX_RESULTS = 10000;

type ViewMode = "list" | "map";

export default function LocationHistoryPage() {
  const [deviceId, setDeviceId] = useState<string>("");
  const [organizationId, setOrganizationId] = useState<string>("");
  const [fromDate, setFromDate] = useState<string>("");
  const [toDate, setToDate] = useState<string>("");
  const [viewMode, setViewMode] = useState<ViewMode>("list");
  const [hasQueried, setHasQueried] = useState(false);

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();
  const {
    data: queryResult,
    loading,
    error,
    execute: executeQuery,
  } = useApi<{ locations: DeviceLocation[]; total: number; truncated: boolean }>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
    fetchDevices(() => adminDevicesApi.list({ limit: 100 }));
  }, [fetchOrgs, fetchDevices]);

  const handleQuery = useCallback(() => {
    if (!deviceId && !organizationId) {
      return;
    }

    setHasQueried(true);
    executeQuery(() =>
      locationsApi.query({
        device_id: deviceId || undefined,
        organization_id: organizationId || undefined,
        from: fromDate || undefined,
        to: toDate || undefined,
        limit: MAX_RESULTS,
      })
    );
  }, [executeQuery, deviceId, organizationId, fromDate, toDate]);

  // Filter devices by selected organization
  const filteredDevices =
    devicesData?.items?.filter(
      (d) => !organizationId || d.organization_id === organizationId
    ) || [];

  const locations = queryResult?.locations || [];
  const isTruncated = queryResult?.truncated || false;
  const totalCount = queryResult?.total || 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Link href="/locations">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Map
          </Button>
        </Link>
      </div>

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Location History</h1>
          <p className="text-muted-foreground">
            Query and export historical location data
          </p>
        </div>
      </div>

      {/* Query Form */}
      <Card data-testid="location-history-query-card">
        <CardHeader>
          <CardTitle>Query Parameters</CardTitle>
          <CardDescription>
            Select a device or organization and date range to query location history
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
            <div className="space-y-2">
              <Label htmlFor="organization">Organization</Label>
              <select
                id="organization"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={organizationId}
                onChange={(e) => {
                  setOrganizationId(e.target.value);
                  setDeviceId(""); // Reset device when org changes
                }}
              >
                <option value="">Select Organization</option>
                {orgsData?.items?.map((org) => (
                  <option key={org.id} value={org.id}>
                    {org.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="device">Device (Optional)</Label>
              <select
                id="device"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={deviceId}
                onChange={(e) => setDeviceId(e.target.value)}
              >
                <option value="">All Devices</option>
                {filteredDevices.map((device) => (
                  <option key={device.id} value={device.id}>
                    {device.display_name}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="from">From Date</Label>
              <Input
                id="from"
                type="datetime-local"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="to">To Date</Label>
              <Input
                id="to"
                type="datetime-local"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
              />
            </div>
          </div>

          <div className="flex justify-between items-center">
            <p className="text-sm text-muted-foreground">
              Maximum {MAX_RESULTS.toLocaleString()} results per query
            </p>
            <Button
              onClick={handleQuery}
              disabled={loading || (!deviceId && !organizationId)}
            >
              {loading ? (
                <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <Search className="h-4 w-4 mr-2" />
              )}
              Query History
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Results */}
      {error && (
        <Card>
          <CardContent className="py-8">
            <div className="flex flex-col items-center justify-center text-center">
              <p className="text-destructive mb-4">{error}</p>
              <Button variant="outline" onClick={handleQuery}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {hasQueried && !error && (
        <Card data-testid="location-history-results-card">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle>Query Results</CardTitle>
                <CardDescription>
                  {loading
                    ? "Loading..."
                    : `${locations.length.toLocaleString()} of ${totalCount.toLocaleString()} results`}
                </CardDescription>
              </div>
              <div className="flex items-center gap-2">
                {/* View Toggle */}
                <div className="flex border rounded-md">
                  <Button
                    variant={viewMode === "list" ? "secondary" : "ghost"}
                    size="sm"
                    onClick={() => setViewMode("list")}
                    className="rounded-r-none"
                  >
                    <List className="h-4 w-4" />
                  </Button>
                  <Button
                    variant={viewMode === "map" ? "secondary" : "ghost"}
                    size="sm"
                    onClick={() => setViewMode("map")}
                    className="rounded-l-none"
                  >
                    <Map className="h-4 w-4" />
                  </Button>
                </div>

                <ExportDropdown locations={locations} disabled={loading} />
              </div>
            </div>
          </CardHeader>
          <CardContent>
            {/* Truncation Warning */}
            {isTruncated && (
              <div className="mb-4 p-3 bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-400 rounded-md flex items-center gap-2">
                <AlertTriangle className="h-4 w-4" />
                <span>
                  Results were limited to {MAX_RESULTS.toLocaleString()}. Narrow
                  your date range or filter by device for complete results.
                </span>
              </div>
            )}

            {viewMode === "list" ? (
              <LocationHistoryList locations={locations} loading={loading} />
            ) : (
              <LocationHistoryMap locations={locations} loading={loading} />
            )}
          </CardContent>
        </Card>
      )}

      {!hasQueried && !error && (
        <Card>
          <CardContent className="py-12">
            <div className="flex flex-col items-center justify-center text-center">
              <Search className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">
                Select query parameters and click &quot;Query History&quot; to view results
              </p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
