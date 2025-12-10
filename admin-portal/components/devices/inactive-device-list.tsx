"use client";

import { useState, useEffect, useCallback } from "react";
import type {
  InactiveDevice,
  PaginatedResponse,
  NotifyOwnersResult,
} from "@/types";
import { AdminDeviceStatusBadge } from "./admin-device-status-badge";
import { DevicePlatformBadge } from "./device-platform-badge";
import { adminDevicesApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import {
  Smartphone,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  Mail,
  Clock,
  CheckCircle,
  XCircle,
} from "lucide-react";
import Link from "next/link";

const ITEMS_PER_PAGE = 50;

const INACTIVITY_THRESHOLDS = [
  { value: 7, label: "7 days" },
  { value: 14, label: "14 days" },
  { value: 30, label: "30 days" },
  { value: 60, label: "60 days" },
  { value: 90, label: "90 days" },
];

export function InactiveDeviceList() {
  // Filter state
  const [daysInactive, setDaysInactive] = useState(30);

  // Selection state
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  // Notification state
  const [showNotifyDialog, setShowNotifyDialog] = useState(false);
  const [messageTemplate, setMessageTemplate] = useState(
    "Your device {device_name} has been inactive for {days} days. Please connect to resume service."
  );
  const [notifyResult, setNotifyResult] = useState<NotifyOwnersResult | null>(null);
  const [showPreview, setShowPreview] = useState(false);

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);

  // API state
  const { data, loading, error, execute } = useApi<PaginatedResponse<InactiveDevice>>();
  const { loading: notifyLoading, execute: executeNotify } = useApi<NotifyOwnersResult>();

  // Fetch inactive devices
  const fetchDevices = useCallback(() => {
    execute(() =>
      adminDevicesApi.getInactive({
        days: daysInactive,
        page: currentPage,
        limit: ITEMS_PER_PAGE,
      })
    );
  }, [execute, daysInactive, currentPage]);

  useEffect(() => {
    fetchDevices();
  }, [fetchDevices]);

  // Reset page when threshold changes
  useEffect(() => {
    setCurrentPage(1);
    setSelectedIds(new Set());
  }, [daysInactive]);

  const devices = data?.items || [];
  const totalDevices = data?.total || 0;
  const totalPages = Math.ceil(totalDevices / ITEMS_PER_PAGE);

  // Selection helpers
  const selectedDevices = devices.filter((d) => selectedIds.has(d.id));
  const allSelected = devices.length > 0 && devices.every((d) => selectedIds.has(d.id));

  const toggleSelectAll = () => {
    if (allSelected) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(devices.map((d) => d.id)));
    }
  };

  const toggleSelect = (id: string) => {
    const newSelected = new Set(selectedIds);
    if (newSelected.has(id)) {
      newSelected.delete(id);
    } else {
      newSelected.add(id);
    }
    setSelectedIds(newSelected);
  };

  const handleNotifyOwners = async () => {
    const result = await executeNotify(() =>
      adminDevicesApi.notifyOwners(Array.from(selectedIds), messageTemplate)
    );
    if (result) {
      setNotifyResult(result);
    }
  };

  const closeNotifyDialog = () => {
    setShowNotifyDialog(false);
    setNotifyResult(null);
    setShowPreview(false);
    setSelectedIds(new Set());
  };

  // Generate preview message for a specific device
  const getPreviewMessage = (device: InactiveDevice) => {
    return messageTemplate
      .replace("{device_name}", device.display_name)
      .replace("{days}", String(device.days_inactive));
  };

  const formatDate = (dateString: string | null) => {
    if (!dateString) return "Never";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const getActivityTypeLabel = (type: InactiveDevice["last_activity_type"]) => {
    switch (type) {
      case "location":
        return "Location Update";
      case "sync":
        return "Data Sync";
      case "login":
        return "App Login";
      default:
        return type;
    }
  };

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <AlertCircle className="h-5 w-5 text-yellow-500" />
                Inactive Devices
              </CardTitle>
              <CardDescription>
                Devices that have not been active for the selected period
                {totalDevices > 0 && ` • ${totalDevices} inactive devices`}
              </CardDescription>
            </div>
            <div className="flex items-center gap-2">
              {selectedIds.size > 0 && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowNotifyDialog(true)}
                >
                  <Mail className="h-4 w-4 mr-2" />
                  Notify Owners ({selectedIds.size})
                </Button>
              )}
              <select
                className="rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={daysInactive}
                onChange={(e) => setDaysInactive(parseInt(e.target.value, 10))}
              >
                {INACTIVITY_THRESHOLDS.map((threshold) => (
                  <option key={threshold.value} value={threshold.value}>
                    Inactive {threshold.label}+
                  </option>
                ))}
              </select>
              <Button
                variant="outline"
                size="sm"
                onClick={fetchDevices}
                disabled={loading}
              >
                <RefreshCw
                  className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
                />
                Refresh
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {/* Error State */}
          {error && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <p className="text-destructive mb-4">{error}</p>
              <Button variant="outline" onClick={fetchDevices}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          )}

          {/* Loading State */}
          {loading && !data && (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="flex items-center gap-4 py-3">
                  <div className="h-4 w-6 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-48 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-24 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-20 bg-muted animate-pulse rounded" />
                </div>
              ))}
            </div>
          )}

          {/* Empty State */}
          {!loading && !error && devices.length === 0 && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <CheckCircle className="h-12 w-12 text-green-500 mb-4" />
              <p className="text-muted-foreground">
                No inactive devices found for the selected period
              </p>
            </div>
          )}

          {/* Device Table */}
          {!error && devices.length > 0 && (
            <>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b">
                      <th className="w-10 py-3 px-4">
                        <Checkbox
                          checked={allSelected}
                          onCheckedChange={toggleSelectAll}
                          aria-label="Select all devices"
                        />
                      </th>
                      <th className="text-left py-3 px-4 font-medium">Device</th>
                      <th className="text-left py-3 px-4 font-medium">Platform</th>
                      <th className="text-left py-3 px-4 font-medium">Owner</th>
                      <th className="text-left py-3 px-4 font-medium">Organization</th>
                      <th className="text-left py-3 px-4 font-medium">Days Inactive</th>
                      <th className="text-left py-3 px-4 font-medium">Last Activity</th>
                      <th className="text-left py-3 px-4 font-medium">Status</th>
                      <th className="text-right py-3 px-4 font-medium">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {devices.map((device) => (
                      <tr
                        key={device.id}
                        className={`border-b hover:bg-muted/50 ${
                          selectedIds.has(device.id) ? "bg-muted/30" : ""
                        }`}
                      >
                        <td className="py-3 px-4">
                          <Checkbox
                            checked={selectedIds.has(device.id)}
                            onCheckedChange={() => toggleSelect(device.id)}
                            aria-label={`Select ${device.display_name}`}
                          />
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-2">
                            <Smartphone className="h-4 w-4 text-muted-foreground" />
                            <span className="font-medium">{device.display_name}</span>
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <DevicePlatformBadge platform={device.platform} />
                        </td>
                        <td className="py-3 px-4 text-sm">{device.owner_email}</td>
                        <td className="py-3 px-4 text-sm text-muted-foreground">
                          {device.organization_name}
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-2">
                            <Clock className="h-4 w-4 text-yellow-500" />
                            <span className="font-medium text-yellow-600 dark:text-yellow-400">
                              {device.days_inactive} days
                            </span>
                          </div>
                        </td>
                        <td className="py-3 px-4 text-sm text-muted-foreground">
                          <div>
                            <p>{formatDate(device.last_seen)}</p>
                            <p className="text-xs">{getActivityTypeLabel(device.last_activity_type)}</p>
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <AdminDeviceStatusBadge status={device.status} />
                        </td>
                        <td className="py-3 px-4 text-right">
                          <Link href={`/devices/fleet/${device.id}`}>
                            <Button variant="ghost" size="sm">
                              View
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
                    Showing {(currentPage - 1) * ITEMS_PER_PAGE + 1} to{" "}
                    {Math.min(currentPage * ITEMS_PER_PAGE, totalDevices)} of{" "}
                    {totalDevices} devices
                  </p>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                      disabled={currentPage === 1 || loading}
                    >
                      <ChevronLeft className="h-4 w-4" />
                      Previous
                    </Button>
                    <span className="text-sm text-muted-foreground">
                      Page {currentPage} of {totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                      disabled={currentPage === totalPages || loading}
                    >
                      Next
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      {/* Notify Owners Dialog */}
      {showNotifyDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div
            className="absolute inset-0 bg-black/50"
            onClick={closeNotifyDialog}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            {!notifyResult ? (
              <>
                <div className="flex items-start gap-3 mb-4">
                  <Mail className="h-5 w-5 text-primary mt-0.5" />
                  <div>
                    <h2 className="text-lg font-semibold">Notify Device Owners</h2>
                    <p className="text-sm text-muted-foreground">
                      Send a notification to owners of {selectedDevices.length} inactive device
                      {selectedDevices.length !== 1 ? "s" : ""}.
                    </p>
                  </div>
                </div>

                <div className="mb-4">
                  <label className="text-sm font-medium">Message Template</label>
                  <textarea
                    className="w-full mt-1 rounded-md border border-input bg-background px-3 py-2 text-sm min-h-[100px]"
                    value={messageTemplate}
                    onChange={(e) => setMessageTemplate(e.target.value)}
                    placeholder="Enter notification message..."
                  />
                  <p className="text-xs text-muted-foreground mt-1">
                    Available variables: {"{device_name}"}, {"{days}"}
                  </p>
                </div>

                {/* Preview Section */}
                {showPreview && selectedDevices.length > 0 && (
                  <div className="mb-4 p-3 rounded-lg border bg-muted/30">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-medium">Message Preview</span>
                      <span className="text-xs text-muted-foreground">
                        Showing {Math.min(3, selectedDevices.length)} of {selectedDevices.length} recipient{selectedDevices.length !== 1 ? "s" : ""}
                      </span>
                    </div>
                    <div className="space-y-2 max-h-40 overflow-auto">
                      {selectedDevices.slice(0, 3).map((device) => (
                        <div key={device.id} className="p-2 rounded bg-background text-sm">
                          <p className="text-xs text-muted-foreground mb-1">
                            To: {device.owner_email} ({device.display_name})
                          </p>
                          <p className="text-foreground">{getPreviewMessage(device)}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div className="flex justify-end gap-2">
                  <Button variant="outline" onClick={closeNotifyDialog} disabled={notifyLoading}>
                    Cancel
                  </Button>
                  {!showPreview ? (
                    <Button
                      variant="secondary"
                      onClick={() => setShowPreview(true)}
                      disabled={notifyLoading || !messageTemplate.trim()}
                    >
                      Preview Message
                    </Button>
                  ) : (
                    <Button onClick={handleNotifyOwners} disabled={notifyLoading || !messageTemplate.trim()}>
                      {notifyLoading ? (
                        <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                      ) : (
                        <Mail className="h-4 w-4 mr-2" />
                      )}
                      Send Notifications
                    </Button>
                  )}
                </div>
              </>
            ) : (
              <>
                <h2 className="text-lg font-semibold mb-4">Notifications Sent</h2>

                <div className="space-y-3 mb-4">
                  <div className="flex items-center justify-between p-3 rounded-lg bg-green-50 dark:bg-green-900/20">
                    <div className="flex items-center gap-2">
                      <CheckCircle className="h-5 w-5 text-green-600 dark:text-green-400" />
                      <span className="font-medium">Sent Successfully</span>
                    </div>
                    <span className="font-bold text-green-600 dark:text-green-400">
                      {notifyResult.sent}
                    </span>
                  </div>

                  {notifyResult.failed > 0 && (
                    <div className="flex items-center justify-between p-3 rounded-lg bg-red-50 dark:bg-red-900/20">
                      <div className="flex items-center gap-2">
                        <XCircle className="h-5 w-5 text-red-600 dark:text-red-400" />
                        <span className="font-medium">Failed</span>
                      </div>
                      <span className="font-bold text-red-600 dark:text-red-400">
                        {notifyResult.failed}
                      </span>
                    </div>
                  )}
                </div>

                <Button className="w-full" onClick={closeNotifyDialog}>
                  Close
                </Button>
              </>
            )}
          </div>
        </div>
      )}
    </>
  );
}
