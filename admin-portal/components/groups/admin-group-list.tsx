"use client";

import { useState, useEffect, useCallback } from "react";
import type {
  AdminGroup,
  GroupStatus,
  GroupListParams,
  PaginatedResponse,
  Organization,
} from "@/types";
import { GroupStatusBadge } from "./group-status-badge";
import { GroupActionsMenu } from "./group-actions-menu";
import { useDebounce } from "@/hooks/use-debounce";
import { adminGroupsApi, organizationsApi } from "@/lib/api-client";
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
import {
  Users,
  Search,
  RefreshCw,
  ChevronUp,
  ChevronDown,
  ChevronsUpDown,
  ChevronLeft,
  ChevronRight,
  X,
  Smartphone,
} from "lucide-react";
import Link from "next/link";

type SortField = "name" | "member_count" | "device_count" | "created_at";
type SortOrder = "asc" | "desc";

const ITEMS_PER_PAGE = 50;

const STATUS_OPTIONS: { value: GroupStatus | "all"; label: string }[] = [
  { value: "all", label: "All Status" },
  { value: "active", label: "Active" },
  { value: "suspended", label: "Suspended" },
  { value: "archived", label: "Archived" },
];

export function AdminGroupList() {
  // Search and filter state
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<GroupStatus | "all">("all");
  const [organizationFilter, setOrganizationFilter] = useState<string>("all");

  // Sorting state
  const [sortBy, setSortBy] = useState<SortField>("created_at");
  const [sortOrder, setSortOrder] = useState<SortOrder>("desc");

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);

  // Debounced search
  const debouncedSearch = useDebounce(searchQuery, 300);

  // API state for groups
  const { data, loading, error, execute } = useApi<PaginatedResponse<AdminGroup>>();

  // API state for organizations (for filter dropdown)
  const { data: orgData, execute: executeOrgs } = useApi<PaginatedResponse<Organization>>();

  // Fetch organizations for filter dropdown
  useEffect(() => {
    executeOrgs(() => organizationsApi.list({ limit: 100 }));
  }, [executeOrgs]);

  // Fetch groups
  const fetchGroups = useCallback(() => {
    const params: GroupListParams = {
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

    if (organizationFilter !== "all") {
      params.organization_id = organizationFilter;
    }

    execute(() => adminGroupsApi.list(params));
  }, [execute, currentPage, sortBy, sortOrder, debouncedSearch, statusFilter, organizationFilter]);

  // Fetch on mount and when params change
  useEffect(() => {
    fetchGroups();
  }, [fetchGroups]);

  // Reset to page 1 when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [debouncedSearch, statusFilter, organizationFilter]);

  const groups = data?.items || [];
  const totalGroups = data?.total || 0;
  const totalPages = Math.ceil(totalGroups / ITEMS_PER_PAGE);
  const organizations = orgData?.items || [];

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
    setOrganizationFilter("all");
    setCurrentPage(1);
  };

  const hasActiveFilters =
    searchQuery ||
    statusFilter !== "all" ||
    organizationFilter !== "all";

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Groups</CardTitle>
            <CardDescription>
              Manage all groups across organizations
              {totalGroups > 0 && ` - ${totalGroups} total groups`}
            </CardDescription>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={fetchGroups}
            disabled={loading}
          >
            <RefreshCw
              className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {/* Search and Filters */}
        <div className="flex flex-wrap gap-4 mb-4">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search by group name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9"
            />
          </div>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[150px]"
            value={organizationFilter}
            onChange={(e) => setOrganizationFilter(e.target.value)}
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
            onChange={(e) => setStatusFilter(e.target.value as GroupStatus | "all")}
          >
            {STATUS_OPTIONS.map((option) => (
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
            <Button variant="outline" onClick={fetchGroups}>
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
        {!loading && !error && groups.length === 0 && (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <Users className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">
              {hasActiveFilters
                ? "No groups match your filters"
                : "No groups found"}
            </p>
          </div>
        )}

        {/* Groups Table */}
        {!error && groups.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th
                      className="text-left py-3 px-4 font-medium cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("name")}
                    >
                      <div className="flex items-center">
                        Group
                        {getSortIcon("name")}
                      </div>
                    </th>
                    <th className="text-left py-3 px-4 font-medium">Owner</th>
                    <th
                      className="text-left py-3 px-4 font-medium cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("member_count")}
                    >
                      <div className="flex items-center">
                        Members
                        {getSortIcon("member_count")}
                      </div>
                    </th>
                    <th
                      className="text-left py-3 px-4 font-medium cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("device_count")}
                    >
                      <div className="flex items-center">
                        Devices
                        {getSortIcon("device_count")}
                      </div>
                    </th>
                    <th className="text-left py-3 px-4 font-medium">Organization</th>
                    <th className="text-left py-3 px-4 font-medium">Status</th>
                    <th
                      className="text-left py-3 px-4 font-medium cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("created_at")}
                    >
                      <div className="flex items-center">
                        Created
                        {getSortIcon("created_at")}
                      </div>
                    </th>
                    <th className="text-right py-3 px-4 font-medium">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {groups.map((group) => (
                    <tr
                      key={group.id}
                      className="border-b hover:bg-muted/50"
                    >
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2">
                          <Users className="h-4 w-4 text-muted-foreground" />
                          <div>
                            <Link
                              href={`/groups/${group.id}`}
                              className="font-medium hover:underline"
                            >
                              {group.name}
                            </Link>
                            {group.description && (
                              <p className="text-xs text-muted-foreground truncate max-w-[200px]">
                                {group.description}
                              </p>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-sm">
                        <div>
                          <p>{group.owner_name}</p>
                          <p className="text-xs text-muted-foreground">{group.owner_email}</p>
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-1">
                          <Users className="h-4 w-4 text-muted-foreground" />
                          <span className="font-medium">{group.member_count}</span>
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-1">
                          <Smartphone className="h-4 w-4 text-muted-foreground" />
                          <span className="font-medium">{group.device_count}</span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        {group.organization_name}
                      </td>
                      <td className="py-3 px-4">
                        <GroupStatusBadge status={group.status} />
                      </td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        {formatDateTime(group.created_at)}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <GroupActionsMenu
                          group={group}
                          onActionComplete={fetchGroups}
                        />
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
                  {Math.min(currentPage * ITEMS_PER_PAGE, totalGroups)} of{" "}
                  {totalGroups} groups
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
