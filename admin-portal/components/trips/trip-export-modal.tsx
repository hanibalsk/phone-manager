"use client";

import { useState, useEffect, useCallback } from "react";
import type { Organization, AdminDevice, Trip, TripPoint, ExportJob } from "@/types";
import { organizationsApi, adminDevicesApi, tripsApi, exportJobsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { RefreshCw, Download, FileJson, FileSpreadsheet, X, Clock, CheckCircle2, AlertCircle, Loader2 } from "lucide-react";

const SYNC_EXPORT_LIMIT = 1000;

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

  // Async export states
  const [tripCount, setTripCount] = useState<number | null>(null);
  const [countLoading, setCountLoading] = useState(false);
  const [pendingJobs, setPendingJobs] = useState<ExportJob[]>([]);
  const [showPendingJobs, setShowPendingJobs] = useState(false);
  const [asyncJobCreated, setAsyncJobCreated] = useState(false);

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();

  // Fetch trip count when filters change
  const fetchTripCount = useCallback(async () => {
    setCountLoading(true);
    try {
      const response = await tripsApi.count({
        device_id: deviceId || undefined,
        organization_id: organizationId || undefined,
        from: fromDate || undefined,
        to: toDate || undefined,
      });
      setTripCount(response.data?.count ?? null);
    } catch {
      setTripCount(null);
    } finally {
      setCountLoading(false);
    }
  }, [deviceId, organizationId, fromDate, toDate]);

  // Fetch pending export jobs
  const fetchPendingJobs = useCallback(async () => {
    try {
      const response = await exportJobsApi.list({ type: "trips", limit: 10 });
      if (response.data) {
        setPendingJobs(response.data.items.filter(j => j.status === "pending" || j.status === "processing" || j.status === "completed"));
      }
    } catch {
      // Ignore errors for pending jobs
    }
  }, []);

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
    fetchDevices(() => adminDevicesApi.list({ limit: 100 }));
    fetchPendingJobs();
  }, [fetchOrgs, fetchDevices, fetchPendingJobs]);

  // Debounce trip count fetch
  useEffect(() => {
    const timer = setTimeout(() => {
      fetchTripCount();
    }, 500);
    return () => clearTimeout(timer);
  }, [fetchTripCount]);

  const requiresAsyncExport = tripCount !== null && tripCount > SYNC_EXPORT_LIMIT;

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
    // Use async export for large datasets
    if (requiresAsyncExport) {
      await handleAsyncExport();
      return;
    }

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

  const handleAsyncExport = async () => {
    setExporting(true);
    setProgress("Creating export job...");

    try {
      const response = await exportJobsApi.create({
        type: "trips",
        format,
        filters: {
          device_id: deviceId || undefined,
          organization_id: organizationId || undefined,
          from: fromDate || undefined,
          to: toDate || undefined,
          include_path: includePath,
        },
      });

      if (!response.data) {
        throw new Error(response.error || "Failed to create export job");
      }

      setAsyncJobCreated(true);
      setProgress(null);
      fetchPendingJobs();
    } catch (error) {
      setProgress(`Error: ${error instanceof Error ? error.message : "Failed to create export job"}`);
    } finally {
      setExporting(false);
    }
  };

  const getJobStatusIcon = (status: ExportJob["status"]) => {
    switch (status) {
      case "pending":
        return <Clock className="h-4 w-4 text-muted-foreground" />;
      case "processing":
        return <Loader2 className="h-4 w-4 text-blue-500 animate-spin" />;
      case "completed":
        return <CheckCircle2 className="h-4 w-4 text-green-500" />;
      case "failed":
        return <AlertCircle className="h-4 w-4 text-destructive" />;
    }
  };

  const formatJobDate = (dateString: string) => {
    return new Date(dateString).toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      role="dialog"
      aria-modal="true"
      data-testid="trip-export-dialog"
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
              data-testid="export-org-filter"
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
              data-testid="export-device-filter"
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

          {/* Trip Count & Async Export Notice */}
          {tripCount !== null && (
            <div className={`text-sm p-3 rounded-md ${requiresAsyncExport ? "bg-amber-50 border border-amber-200 dark:bg-amber-950/20 dark:border-amber-800" : "bg-muted"}`}>
              <div className="flex items-center gap-2">
                {countLoading ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : requiresAsyncExport ? (
                  <Clock className="h-4 w-4 text-amber-600" />
                ) : (
                  <CheckCircle2 className="h-4 w-4 text-green-600" />
                )}
                <span className="font-medium">
                  {tripCount.toLocaleString()} trips match your filters
                </span>
              </div>
              {requiresAsyncExport && (
                <p className="text-xs text-muted-foreground mt-1 ml-6">
                  Large exports are processed in the background. You&apos;ll be able to download the file when ready.
                </p>
              )}
            </div>
          )}

          {/* Async Job Created Success */}
          {asyncJobCreated && (
            <div className="text-sm p-3 rounded-md bg-green-50 border border-green-200 dark:bg-green-950/20 dark:border-green-800">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="h-4 w-4 text-green-600" />
                <span className="font-medium text-green-800 dark:text-green-200">
                  Export job created successfully!
                </span>
              </div>
              <p className="text-xs text-muted-foreground mt-1 ml-6">
                Your export is being processed. Check the pending jobs below for status.
              </p>
            </div>
          )}

          {/* Pending Export Jobs */}
          {pendingJobs.length > 0 && (
            <div className="border rounded-md">
              <button
                type="button"
                className="w-full flex items-center justify-between p-3 text-sm font-medium hover:bg-muted/50"
                onClick={() => setShowPendingJobs(!showPendingJobs)}
              >
                <span className="flex items-center gap-2">
                  <Clock className="h-4 w-4" />
                  Pending Export Jobs ({pendingJobs.length})
                </span>
                <RefreshCw
                  className={`h-4 w-4 transition-transform ${showPendingJobs ? "rotate-180" : ""}`}
                />
              </button>
              {showPendingJobs && (
                <div className="border-t divide-y">
                  {pendingJobs.map((job) => (
                    <div key={job.id} className="p-3 text-sm">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          {getJobStatusIcon(job.status)}
                          <span className="capitalize">{job.status}</span>
                          <span className="text-muted-foreground">•</span>
                          <span className="text-muted-foreground">{job.format.toUpperCase()}</span>
                        </div>
                        <span className="text-xs text-muted-foreground">
                          {formatJobDate(job.created_at)}
                        </span>
                      </div>
                      {job.status === "processing" && job.total_records > 0 && (
                        <div className="mt-2">
                          <div className="h-1.5 bg-muted rounded-full overflow-hidden">
                            <div
                              className="h-full bg-blue-500 transition-all"
                              style={{ width: `${(job.processed_records / job.total_records) * 100}%` }}
                            />
                          </div>
                          <span className="text-xs text-muted-foreground">
                            {job.processed_records.toLocaleString()} / {job.total_records.toLocaleString()} records
                          </span>
                        </div>
                      )}
                      {job.status === "completed" && job.download_url && (
                        <div className="mt-2">
                          <a
                            href={job.download_url}
                            className="inline-flex items-center gap-1 text-xs text-blue-600 hover:underline"
                            download
                          >
                            <Download className="h-3 w-3" />
                            Download ({job.total_records.toLocaleString()} records)
                          </a>
                          {job.expires_at && (
                            <span className="text-xs text-muted-foreground ml-2">
                              Expires {formatJobDate(job.expires_at)}
                            </span>
                          )}
                        </div>
                      )}
                      {job.status === "failed" && job.error_message && (
                        <p className="mt-1 text-xs text-destructive">{job.error_message}</p>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Progress */}
          {progress && (
            <div className="text-sm text-muted-foreground flex items-center gap-2">
              {exporting && <RefreshCw className="h-4 w-4 animate-spin" />}
              {progress}
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-2 pt-4">
            <Button variant="outline" onClick={onClose} disabled={exporting} data-testid="export-cancel">
              Cancel
            </Button>
            <Button
              onClick={handleExport}
              disabled={exporting || asyncJobCreated}
              data-testid="export-button"
            >
              {exporting ? (
                <>
                  <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                  {requiresAsyncExport ? "Creating Job..." : "Exporting..."}
                </>
              ) : asyncJobCreated ? (
                <>
                  <CheckCircle2 className="h-4 w-4 mr-2" />
                  Job Created
                </>
              ) : requiresAsyncExport ? (
                <>
                  <Clock className="h-4 w-4 mr-2" />
                  Start Background Export
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
