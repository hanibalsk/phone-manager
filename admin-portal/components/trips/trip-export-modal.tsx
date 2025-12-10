"use client";

import { useState, useEffect } from "react";
import type { Organization, AdminDevice, Trip, TripPoint, TripExportRequest } from "@/types";
import { organizationsApi, adminDevicesApi, tripsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { RefreshCw, Download, FileJson, FileSpreadsheet, X } from "lucide-react";

interface TripExportModalProps {
  /** Pre-selected filters from trip list */
  initialFilters?: {
    organization_id?: string;
    device_id?: string;
    from?: string;
    to?: string;
  };
  onClose: () => void;
}

export function TripExportModal({ initialFilters, onClose }: TripExportModalProps) {
  const [organizationId, setOrganizationId] = useState(initialFilters?.organization_id || "");
  const [deviceId, setDeviceId] = useState(initialFilters?.device_id || "");
  const [fromDate, setFromDate] = useState(initialFilters?.from || "");
  const [toDate, setToDate] = useState(initialFilters?.to || "");
  const [format, setFormat] = useState<"csv" | "json">("csv");
  const [includePath, setIncludePath] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [progress, setProgress] = useState<string | null>(null);

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
    fetchDevices(() => adminDevicesApi.list({ limit: 100 }));
  }, [fetchOrgs, fetchDevices]);

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
      return `${(meters / 1000).toFixed(2)} km`;
    }
    return `${meters} m`;
  };

  const downloadFile = (content: string, filename: string, mimeType: string) => {
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const exportToCSV = (trips: Trip[], pathData?: Record<string, TripPoint[]>) => {
    const headers = [
      "Trip ID",
      "Device ID",
      "Device Name",
      "Organization",
      "Status",
      "Start Time",
      "End Time",
      "Duration",
      "Distance (m)",
      "Start Latitude",
      "Start Longitude",
      "End Latitude",
      "End Longitude",
      "Point Count",
    ];

    if (includePath) {
      headers.push("Path (lat,lng pairs)");
    }

    const rows = trips.map((trip) => {
      const row = [
        trip.id,
        trip.device_id,
        trip.device_name,
        trip.organization_name,
        trip.status,
        trip.start_time,
        trip.end_time || "",
        formatDuration(trip.duration_seconds),
        trip.distance_meters.toString(),
        trip.start_latitude.toString(),
        trip.start_longitude.toString(),
        trip.end_latitude?.toString() || "",
        trip.end_longitude?.toString() || "",
        trip.point_count.toString(),
      ];

      if (includePath && pathData?.[trip.id]) {
        const pathStr = pathData[trip.id]
          .map((p) => `${p.latitude.toFixed(6)},${p.longitude.toFixed(6)}`)
          .join(";");
        row.push(pathStr);
      }

      return row;
    });

    const csvContent = [
      headers.join(","),
      ...rows.map((row) =>
        row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(",")
      ),
    ].join("\n");

    const timestamp = new Date().toISOString().slice(0, 10);
    downloadFile(csvContent, `trips_export_${timestamp}.csv`, "text/csv");
  };

  const exportToJSON = (trips: Trip[], pathData?: Record<string, TripPoint[]>) => {
    const exportData = trips.map((trip) => {
      const tripData: Record<string, unknown> = { ...trip };
      if (includePath && pathData?.[trip.id]) {
        tripData.path = pathData[trip.id];
      }
      return tripData;
    });

    const jsonContent = JSON.stringify(exportData, null, 2);
    const timestamp = new Date().toISOString().slice(0, 10);
    downloadFile(jsonContent, `trips_export_${timestamp}.json`, "application/json");
  };

  const handleExport = async () => {
    setExporting(true);
    setProgress("Fetching trips...");

    try {
      // Fetch all trips matching the filter
      const response = await tripsApi.list({
        organization_id: organizationId || undefined,
        device_id: deviceId || undefined,
        from: fromDate || undefined,
        to: toDate || undefined,
        limit: 1000, // Client-side export limit
      });

      if (!response.data) {
        throw new Error(response.error || "Failed to fetch trips");
      }

      const trips = response.data.items;
      setProgress(`Found ${trips.length} trips`);

      let pathData: Record<string, TripPoint[]> | undefined;

      // Fetch path data if requested
      if (includePath && trips.length > 0) {
        pathData = {};
        setProgress(`Fetching path data for ${trips.length} trips...`);

        // Fetch paths in batches to avoid overwhelming the server
        const batchSize = 10;
        for (let i = 0; i < trips.length; i += batchSize) {
          const batch = trips.slice(i, i + batchSize);
          const pathPromises = batch.map(async (trip: Trip) => {
            try {
              const pathResponse = await tripsApi.getPath(trip.id);
              return { id: trip.id, points: pathResponse.data?.points || [] };
            } catch {
              return { id: trip.id, points: [] as TripPoint[] };
            }
          });

          const results = await Promise.all(pathPromises);
          for (const result of results) {
            pathData[result.id] = result.points;
          }

          setProgress(`Fetched path data: ${Math.min(i + batchSize, trips.length)}/${trips.length}`);
        }
      }

      // Export based on format
      setProgress("Generating file...");
      if (format === "csv") {
        exportToCSV(trips, pathData);
      } else {
        exportToJSON(trips, pathData);
      }

      setProgress(null);
      onClose();
    } catch (error) {
      setProgress(`Error: ${error instanceof Error ? error.message : "Export failed"}`);
    } finally {
      setExporting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      role="dialog"
      aria-modal="true"
    >
      <div
        className="absolute inset-0 bg-black/50"
        onClick={onClose}
      />
      <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">Export Trips</h2>
          <Button variant="ghost" size="sm" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>

        <div className="space-y-4">
          {/* Organization Filter */}
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
              <option value="">All Organizations</option>
              {orgsData?.items?.map((org) => (
                <option key={org.id} value={org.id}>
                  {org.name}
                </option>
              ))}
            </select>
          </div>

          {/* Device Filter */}
          <div className="space-y-2">
            <Label htmlFor="device">Device</Label>
            <select
              id="device"
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
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
          </div>

          {/* Date Range */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="from">From Date</Label>
              <Input
                id="from"
                type="date"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="to">To Date</Label>
              <Input
                id="to"
                type="date"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
              />
            </div>
          </div>

          {/* Format Selection */}
          <div className="space-y-2">
            <Label>Export Format</Label>
            <div className="flex gap-4">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="format"
                  value="csv"
                  checked={format === "csv"}
                  onChange={() => setFormat("csv")}
                  className="w-4 h-4"
                />
                <FileSpreadsheet className="h-4 w-4" />
                <span className="text-sm">CSV</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="format"
                  value="json"
                  checked={format === "json"}
                  onChange={() => setFormat("json")}
                  className="w-4 h-4"
                />
                <FileJson className="h-4 w-4" />
                <span className="text-sm">JSON</span>
              </label>
            </div>
          </div>

          {/* Include Path Checkbox */}
          <div className="flex items-center gap-2">
            <Checkbox
              id="includePath"
              checked={includePath}
              onCheckedChange={(checked) => setIncludePath(checked === true)}
            />
            <Label htmlFor="includePath" className="cursor-pointer">
              Include path coordinates
              <span className="text-xs text-muted-foreground block">
                This will increase export time and file size
              </span>
            </Label>
          </div>

          {/* Progress */}
          {progress && (
            <div className="text-sm text-muted-foreground flex items-center gap-2">
              {exporting && <RefreshCw className="h-4 w-4 animate-spin" />}
              {progress}
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-2 pt-4">
            <Button variant="outline" onClick={onClose} disabled={exporting}>
              Cancel
            </Button>
            <Button onClick={handleExport} disabled={exporting}>
              {exporting ? (
                <>
                  <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                  Exporting...
                </>
              ) : (
                <>
                  <Download className="h-4 w-4 mr-2" />
                  Export
                </>
              )}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
