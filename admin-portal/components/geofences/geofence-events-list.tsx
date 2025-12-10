"use client";

import { useState, useEffect, useCallback } from "react";
import type { GeofenceEvent, GeofenceEventType, Geofence, AdminDevice } from "@/types";
import { EventTypeBadge } from "./event-type-badge";
import { geofencesApi, adminDevicesApi } from "@/lib/api-client";
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
import {
  RefreshCw,
  Download,
  MapPin,
} from "lucide-react";
import { exportToCSV, exportToJSON } from "@/lib/export-utils";

const ITEMS_PER_PAGE = 25;

export function GeofenceEventsList() {
  const [geofenceId, setGeofenceId] = useState("");
  const [deviceId, setDeviceId] = useState("");
  const [eventType, setEventType] = useState<GeofenceEventType | "">("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [page, setPage] = useState(1);

  const { data: geofencesData, execute: fetchGeofences } = useApi<{ items: Geofence[] }>();
  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();
  const { data: eventsData, loading, error, execute: fetchEvents } = useApi<{ items: GeofenceEvent[]; total: number }>();

  useEffect(() => {
    fetchGeofences(() => geofencesApi.list({ limit: 100 }));
    fetchDevices(() => adminDevicesApi.list({ limit: 100 }));
  }, [fetchGeofences, fetchDevices]);

  const loadEvents = useCallback(() => {
    fetchEvents(() =>
      geofencesApi.getEvents({
        geofence_id: geofenceId || undefined,
        device_id: deviceId || undefined,
        event_type: eventType || undefined,
        from: fromDate || undefined,
        to: toDate || undefined,
        page,
        limit: ITEMS_PER_PAGE,
      })
    );
  }, [fetchEvents, geofenceId, deviceId, eventType, fromDate, toDate, page]);

  useEffect(() => {
    loadEvents();
  }, [loadEvents]);

  const handleExport = (format: "csv" | "json") => {
    if (!eventsData?.items?.length) return;

    // Convert events to exportable format
    const exportData = eventsData.items.map((event) => ({
      id: event.id,
      device_id: event.device_id,
      device_name: event.device_name,
      geofence_id: event.geofence_id,
      geofence_name: event.geofence_name,
      event_type: event.event_type,
      latitude: event.latitude,
      longitude: event.longitude,
      triggered_at: event.triggered_at,
      dwell_time_seconds: event.dwell_time_seconds,
      organization_id: "",
      organization_name: "",
      accuracy: 0,
      altitude: null,
      speed: null,
      bearing: null,
      battery_level: null,
      timestamp: event.triggered_at,
    }));

    const filename = `geofence-events-${new Date().toISOString().split("T")[0]}`;

    if (format === "csv") {
      // Custom CSV for events
      const headers = [
        "Event ID",
        "Device",
        "Geofence",
        "Event Type",
        "Latitude",
        "Longitude",
        "Triggered At",
        "Dwell Time (s)",
      ];
      const rows = eventsData.items.map((e) => [
        e.id,
        e.device_name,
        e.geofence_name,
        e.event_type,
        e.latitude.toFixed(6),
        e.longitude.toFixed(6),
        e.triggered_at,
        e.dwell_time_seconds?.toString() || "",
      ]);
      const csvContent = [
        headers.join(","),
        ...rows.map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(",")),
      ].join("\n");
      const blob = new Blob([csvContent], { type: "text/csv" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `${filename}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } else {
      exportToJSON(exportData, filename);
    }
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  const formatDwellTime = (seconds: number | null) => {
    if (seconds === null) return "-";
    if (seconds < 60) return `${seconds}s`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    return `${hours}h ${mins}m`;
  };

  const events = eventsData?.items || [];
  const total = eventsData?.total || 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Geofence Events</CardTitle>
            <CardDescription>
              {total} event{total !== 1 ? "s" : ""} total
            </CardDescription>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => handleExport("csv")}
              disabled={!events.length}
            >
              <Download className="h-4 w-4 mr-2" />
              CSV
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => handleExport("json")}
              disabled={!events.length}
            >
              <Download className="h-4 w-4 mr-2" />
              JSON
            </Button>
            <Button variant="outline" size="sm" onClick={loadEvents} disabled={loading}>
              <RefreshCw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
              Refresh
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {/* Filters */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
          <div className="space-y-2">
            <Label htmlFor="geofence">Geofence</Label>
            <select
              id="geofence"
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={geofenceId}
              onChange={(e) => setGeofenceId(e.target.value)}
            >
              <option value="">All Geofences</option>
              {geofencesData?.items?.map((g) => (
                <option key={g.id} value={g.id}>
                  {g.name}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="device">Device</Label>
            <select
              id="device"
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={deviceId}
              onChange={(e) => setDeviceId(e.target.value)}
            >
              <option value="">All Devices</option>
              {devicesData?.items?.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.display_name}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="eventType">Event Type</Label>
            <select
              id="eventType"
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={eventType}
              onChange={(e) => setEventType(e.target.value as GeofenceEventType | "")}
            >
              <option value="">All Types</option>
              <option value="ENTER">Enter</option>
              <option value="EXIT">Exit</option>
              <option value="DWELL">Dwell</option>
            </select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="from">From</Label>
            <Input
              id="from"
              type="datetime-local"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="to">To</Label>
            <Input
              id="to"
              type="datetime-local"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
            />
          </div>
        </div>

        {/* Error State */}
        {error && (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <p className="text-destructive mb-4">{error}</p>
            <Button variant="outline" onClick={loadEvents}>
              <RefreshCw className="h-4 w-4 mr-2" />
              Retry
            </Button>
          </div>
        )}

        {/* Loading State */}
        {loading && !eventsData && (
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="flex items-center gap-4 py-3">
                <div className="h-4 w-24 bg-muted animate-pulse rounded" />
                <div className="h-4 w-32 bg-muted animate-pulse rounded" />
                <div className="h-4 w-20 bg-muted animate-pulse rounded" />
                <div className="h-4 w-40 bg-muted animate-pulse rounded" />
              </div>
            ))}
          </div>
        )}

        {/* Empty State */}
        {!loading && !error && events.length === 0 && (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <MapPin className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">No geofence events found</p>
            <p className="text-sm text-muted-foreground mt-1">
              Events will appear here when devices enter or exit geofences
            </p>
          </div>
        )}

        {/* Events Table */}
        {!error && events.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="text-left py-3 px-4 font-medium">Time</th>
                    <th className="text-left py-3 px-4 font-medium">Device</th>
                    <th className="text-left py-3 px-4 font-medium">Geofence</th>
                    <th className="text-left py-3 px-4 font-medium">Event</th>
                    <th className="text-left py-3 px-4 font-medium">Location</th>
                    <th className="text-left py-3 px-4 font-medium">Dwell Time</th>
                  </tr>
                </thead>
                <tbody>
                  {events.map((event) => (
                    <tr key={event.id} className="border-b hover:bg-muted/50">
                      <td className="py-3 px-4 text-sm">
                        {formatDateTime(event.triggered_at)}
                      </td>
                      <td className="py-3 px-4 text-sm font-medium">
                        {event.device_name}
                      </td>
                      <td className="py-3 px-4 text-sm">
                        {event.geofence_name}
                      </td>
                      <td className="py-3 px-4">
                        <EventTypeBadge type={event.event_type} />
                      </td>
                      <td className="py-3 px-4 text-sm font-mono">
                        {event.latitude.toFixed(6)}, {event.longitude.toFixed(6)}
                      </td>
                      <td className="py-3 px-4 text-sm">
                        {formatDwellTime(event.dwell_time_seconds)}
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
  );
}
