"use client";

import { useState, useEffect, useCallback } from "react";
import type { LatestDeviceLocation } from "@/types";
import { locationsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { LocationMap, LocationFilters } from "@/components/locations";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  MapPin,
  RefreshCw,
} from "lucide-react";

interface Filters {
  organization_id?: string;
  device_id?: string;
  from?: string;
  to?: string;
}

export default function LocationsPage() {
  const [filters, setFilters] = useState<Filters>({});

  const { data: locations, loading, error, execute } = useApi<LatestDeviceLocation[]>();

  const fetchLocations = useCallback(() => {
    execute(() => locationsApi.getLatest({ organization_id: filters.organization_id }));
  }, [execute, filters.organization_id]);

  useEffect(() => {
    fetchLocations();
  }, [fetchLocations]);

  const handleFilterChange = (newFilters: Filters) => {
    setFilters(newFilters);
  };

  // Filter locations by device if specified
  const filteredLocations = locations?.filter((loc) => {
    if (filters.device_id && loc.device_id !== filters.device_id) {
      return false;
    }
    if (filters.from) {
      const fromDate = new Date(filters.from);
      const locDate = new Date(loc.timestamp);
      if (locDate < fromDate) return false;
    }
    if (filters.to) {
      const toDate = new Date(filters.to);
      const locDate = new Date(loc.timestamp);
      if (locDate > toDate) return false;
    }
    return true;
  }) || [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold flex items-center gap-2">
            <MapPin className="h-8 w-8" />
            Device Locations
          </h1>
          <p className="text-muted-foreground">
            View and monitor device locations across your fleet
          </p>
        </div>
        <Button
          variant="outline"
          onClick={fetchLocations}
          disabled={loading}
        >
          <RefreshCw
            className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
          />
          Refresh
        </Button>
      </div>

      {/* Filters */}
      <LocationFilters onFilterChange={handleFilterChange} />

      {/* Map */}
      <Card>
        <CardHeader>
          <CardTitle>Location Map</CardTitle>
          <CardDescription>
            {loading
              ? "Loading locations..."
              : `${filteredLocations.length} device${filteredLocations.length !== 1 ? "s" : ""} with location data`}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {error ? (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <p className="text-destructive mb-4">{error}</p>
              <Button variant="outline" onClick={fetchLocations}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          ) : (
            <LocationMap locations={filteredLocations} loading={loading} />
          )}
        </CardContent>
      </Card>

      {/* Location List */}
      <Card>
        <CardHeader>
          <CardTitle>Device List</CardTitle>
          <CardDescription>
            All devices with their latest location
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading && !locations ? (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="flex items-center gap-4 py-3">
                  <div className="h-4 w-32 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-48 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-24 bg-muted animate-pulse rounded" />
                </div>
              ))}
            </div>
          ) : filteredLocations.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <MapPin className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No devices with location data</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="text-left py-3 px-4 font-medium">Device</th>
                    <th className="text-left py-3 px-4 font-medium">Organization</th>
                    <th className="text-left py-3 px-4 font-medium">Coordinates</th>
                    <th className="text-left py-3 px-4 font-medium">Accuracy</th>
                    <th className="text-left py-3 px-4 font-medium">Battery</th>
                    <th className="text-left py-3 px-4 font-medium">Last Update</th>
                    <th className="text-left py-3 px-4 font-medium">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredLocations.map((loc) => (
                    <tr key={loc.device_id} className="border-b hover:bg-muted/50">
                      <td className="py-3 px-4 font-medium">{loc.device_name}</td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        {loc.organization_name}
                      </td>
                      <td className="py-3 px-4 text-sm font-mono">
                        {loc.latitude.toFixed(6)}, {loc.longitude.toFixed(6)}
                      </td>
                      <td className="py-3 px-4 text-sm">
                        {Math.round(loc.accuracy)}m
                      </td>
                      <td className="py-3 px-4 text-sm">
                        {loc.battery_level !== null
                          ? `${Math.round(loc.battery_level * 100)}%`
                          : "-"}
                      </td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        {new Date(loc.timestamp).toLocaleString()}
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            loc.status === "active"
                              ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                              : loc.status === "offline"
                              ? "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400"
                              : loc.status === "suspended"
                              ? "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400"
                              : "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400"
                          }`}
                        >
                          {loc.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
