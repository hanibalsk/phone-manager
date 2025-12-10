"use client";

import { useState, useEffect, useCallback } from "react";
import type {
  AdminDevice,
  AdminDeviceStatus,
  DevicePlatform,
  DeviceListParams,
  PaginatedResponse,
  Organization,
} from "@/types";
import { AdminDeviceStatusBadge } from "./admin-device-status-badge";
import { DevicePlatformBadge } from "./device-platform-badge";
import { BulkActionsMenu } from "./bulk-actions-menu";
import { useDebounce } from "@/hooks/use-debounce";
import { adminDevicesApi, organizationsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Smartphone,
  Search,
  RefreshCw,
  ChevronUp,
  ChevronDown,
  ChevronsUpDown,
  ChevronLeft,
  ChevronRight,
  X,
} from "lucide-react";
import Link from "next/link";

type SortField = "display_name" | "last_seen" | "location_count" | "created_at";
type SortOrder = "asc" | "desc";

const ITEMS_PER_PAGE = 50;

const STATUS_OPTIONS: { value: AdminDeviceStatus | "all"; label: string }[] = [
  { value: "all", label: "All Status" },
  { value: "active", label: "Active" },
  { value: "suspended", label: "Suspended" },
  { value: "offline", label: "Offline" },
  { value: "pending", label: "Pending" },
];

const PLATFORM_OPTIONS: { value: DevicePlatform | "all"; label: string }[] = [
  { value: "all", label: "All Platforms" },
  { value: "android", label: "Android" },
  { value: "ios", label: "iOS" },
];

export function AdminDeviceList() {
  // Search and filter state
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<AdminDeviceStatus | "all">("all");
  const [platformFilter, setPlatformFilter] = useState<DevicePlatform | "all">("all");
  const [organizationFilter, setOrganizationFilter] = useState<string>("all");

  // Selection state for bulk operations
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  // Sorting state
  const [sortBy, setSortBy] = useState<SortField>("created_at");
  const [sortOrder, setSortOrder] = useState<SortOrder>("desc");

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);

  // Debounced search
  const debouncedSearch = useDebounce(searchQuery, 300);

  // API state for devices
  const { data, loading, error, execute } = useApi<PaginatedResponse<AdminDevice>>();

  // API state for organizations (for filter dropdown)
  const { data: orgData, execute: executeOrgs } = useApi<PaginatedResponse<Organization>>();

  // Fetch organizations for filter dropdown
  useEffect(() => {
    executeOrgs(() => organizationsApi.list({ limit: 100 }));
  }, [executeOrgs]);

  // Fetch devices
  const fetchDevices = useCallback(() => {
    const params: DeviceListParams = {
      page: currentPage,
      limit: ITEMS_PER_PAGE,
      sort_by: sortBy,
      sort_order: sortOrder,
    };

    if (debouncedSearch) {
      params.search = debouncedSearch;
    }

    if (statusFilter !== "all") {
      params.status = statusFilter;
    }

    if (platformFilter !== "all") {
      params.platform = platformFilter;
    }

    if (organizationFilter !== "all") {
      params.organization_id = organizationFilter;
    }

    execute(() => adminDevicesApi.list(params));
  }, [execute, currentPage, sortBy, sortOrder, debouncedSearch, statusFilter, platformFilter, organizationFilter]);

  // Fetch on mount and when params change
  useEffect(() => {
    fetchDevices();
  }, [fetchDevices]);

  // Reset to page 1 when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [debouncedSearch, statusFilter, platformFilter, organizationFilter]);

  const devices = data?.items || [];
  const totalDevices = data?.total || 0;
  const totalPages = Math.ceil(totalDevices / ITEMS_PER_PAGE);
  const organizations = orgData?.items || [];

  // Selection helpers
  const selectedDevices = devices.filter((d) => selectedIds.has(d.id));
  const allSelected = devices.length > 0 && devices.every((d) => selectedIds.has(d.id));
  const someSelected = selectedIds.size > 0;

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

  const clearSelection = () => {
    setSelectedIds(new Set());
  };

  const handleSort = (field: SortField) => {
    if (sortBy === field) {
      setSortOrder(sortOrder === "asc" ? "desc" : "asc");
    } else {
      setSortBy(field);
      setSortOrder("asc");
    }
  };

  const getSortIcon = (field: SortField) => {
    if (sortBy !== field) {
      return <ChevronsUpDown className="h-4 w-4 ml-1" />;
    }
    return sortOrder === "asc" ? (
      <ChevronUp className="h-4 w-4 ml-1" />
    ) : (
      <ChevronDown className="h-4 w-4 ml-1" />
    );
  };

  const clearFilters = () => {
    setSearchQuery("");
    setStatusFilter("all");
    setPlatformFilter("all");
    setOrganizationFilter("all");
    setCurrentPage(1);
  };

  const hasActiveFilters =
    searchQuery ||
    statusFilter !== "all" ||
    platformFilter !== "all" ||
    organizationFilter !== "all";

  const formatDateTime = (dateString: string | null) => {
    if (!dateString) return "Never";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <Card data-testid="device-list-card">
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Device Fleet</CardTitle>
            <CardDescription>
              Manage all devices across organizations
              {totalDevices > 0 && ` • ${totalDevices} total devices`}
            </CardDescription>
          </div>
          <div className="flex items-center gap-2">
            {someSelected && (
              <BulkActionsMenu
                selectedDevices={selectedDevices}
                onActionComplete={() => {
                  fetchDevices();
                  clearSelection();
                }}
                onClearSelection={clearSelection}
              />
            )}
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
        {/* Search and Filters */}
        <div className="flex flex-wrap gap-4 mb-4">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search by name, UUID, or owner email..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9"
              data-testid="device-search-input"
            />
          </div>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[150px]"
            value={organizationFilter}
            onChange={(e) => setOrganizationFilter(e.target.value)}
            data-testid="device-org-filter"
          >
            <option value="all">All Organizations</option>
            {organizations.map((org) => (
              <option key={org.id} value={org.id}>
                {org.name}
              </option>
            ))}
          </select>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[130px]"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as AdminDeviceStatus | "all")}
            data-testid="device-status-filter"
          >
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[130px]"
            value={platformFilter}
            onChange={(e) => setPlatformFilter(e.target.value as DevicePlatform | "all")}
            data-testid="device-platform-filter"
          >
            {PLATFORM_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          {hasActiveFilters && (
            <Button variant="ghost" size="sm" onClick={clearFilters}>
              <X className="h-4 w-4 mr-1" />
              Clear
            </Button>
          )}
        </div>

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
                <div className="h-4 w-48 bg-muted animate-pulse rounded" />
                <div className="h-4 w-32 bg-muted animate-pulse rounded" />
                <div className="h-4 w-24 bg-muted animate-pulse rounded" />
                <div className="h-4 w-20 bg-muted animate-pulse rounded" />
                <div className="h-4 w-24 bg-muted animate-pulse rounded" />
              </div>
            ))}
          </div>
        )}

        {/* Empty State */}
        {!loading && !error && devices.length === 0 && (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <Smartphone className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">
              {hasActiveFilters
                ? "No devices match your filters"
                : "No devices found"}
            </p>
          </div>
        )}

        {/* Device Table */}
        {!error && devices.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className="w-full" data-testid="device-table">
                <thead>
                  <tr className="border-b">
                    <th className="w-10 py-3 px-4">
                      <Checkbox
                        checked={allSelected}
                        onCheckedChange={toggleSelectAll}
                        aria-label="Select all devices"
                      />
                    </th>
                    <th
                      className="text-left py-3 px-4 font-medium cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("display_name")}
                    >
                      <div className="flex items-center">
                        Device
                        {getSortIcon("display_name")}
                      </div>
                    </th>
                    <th className="text-left py-3 px-4 font-medium">UUID</th>
                    <th className="text-left py-3 px-4 font-medium">Platform</th>
                    <th className="text-left py-3 px-4 font-medium">Owner</th>
                    <th className="text-left py-3 px-4 font-medium">Organization</th>
                    <th className="text-left py-3 px-4 font-medium">Group</th>
                    <th className="text-left py-3 px-4 font-medium">Status</th>
                    <th
                      className="text-left py-3 px-4 font-medium cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("last_seen")}
                    >
                      <div className="flex items-center">
                        Last Seen
                        {getSortIcon("last_seen")}
                      </div>
                    </th>
                    <th className="text-right py-3 px-4 font-medium">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {devices.map((device) => (
                    <tr
                      key={device.id}
                      className={`border-b hover:bg-muted/50 ${selectedIds.has(device.id) ? "bg-muted/30" : ""}`}
                      data-testid={`device-row-${device.id}`}
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
                        <code className="text-xs bg-muted px-2 py-1 rounded">
                          {device.device_id.slice(0, 8)}...
                        </code>
                      </td>
                      <td className="py-3 px-4">
                        <DevicePlatformBadge platform={device.platform} />
                      </td>
                      <td className="py-3 px-4 text-sm">
                        {device.owner_email}
                      </td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        {device.organization_name}
                      </td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        {device.group_name || "—"}
                      </td>
                      <td className="py-3 px-4">
                        <AdminDeviceStatusBadge status={device.status} />
                      </td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        {formatDateTime(device.last_seen)}
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
  );
}
