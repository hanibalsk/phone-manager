"use client";

import { useState, useEffect, useCallback } from "react";
import type { Organization, OrganizationType, OrganizationStatus, OrganizationListParams, PaginatedResponse } from "@/types";
import { OrganizationStatusBadge } from "./organization-status-badge";
import { OrganizationTypeBadge } from "./organization-type-badge";
import { useDebounce } from "@/hooks/use-debounce";
import { organizationsApi } from "@/lib/api-client";
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
  Building2,
  Search,
  RefreshCw,
  ChevronUp,
  ChevronDown,
  ChevronsUpDown,
  ChevronLeft,
  ChevronRight,
  X,
  Plus,
} from "lucide-react";
import { OrganizationCreateDialog } from "./organization-create-dialog";
import { OrganizationActionsMenu } from "./organization-actions-menu";

type SortField = "name" | "created_at" | "updated_at";
type SortOrder = "asc" | "desc";

const ITEMS_PER_PAGE = 100;

const STATUS_OPTIONS: { value: OrganizationStatus | "all"; label: string }[] = [
  { value: "all", label: "All Status" },
  { value: "active", label: "Active" },
  { value: "suspended", label: "Suspended" },
  { value: "pending", label: "Pending" },
  { value: "archived", label: "Archived" },
];

const TYPE_OPTIONS: { value: OrganizationType | "all"; label: string }[] = [
  { value: "all", label: "All Types" },
  { value: "enterprise", label: "Enterprise" },
  { value: "smb", label: "SMB" },
  { value: "startup", label: "Startup" },
  { value: "personal", label: "Personal" },
];

export function OrganizationList() {
  // Dialog state
  const [showCreateDialog, setShowCreateDialog] = useState(false);

  // Search and filter state
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<OrganizationStatus | "all">("all");
  const [typeFilter, setTypeFilter] = useState<OrganizationType | "all">("all");

  // Sorting state
  const [sortBy, setSortBy] = useState<SortField>("created_at");
  const [sortOrder, setSortOrder] = useState<SortOrder>("desc");

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);

  // Debounced search
  const debouncedSearch = useDebounce(searchQuery, 300);

  // API state
  const { data, loading, error, execute } = useApi<PaginatedResponse<Organization>>();

  // Fetch organizations
  const fetchOrganizations = useCallback(() => {
    const params: OrganizationListParams = {
      page: currentPage,
      limit: ITEMS_PER_PAGE,
    };

    if (debouncedSearch) {
      params.search = debouncedSearch;
    }

    if (statusFilter !== "all") {
      params.status = statusFilter;
    }

    if (typeFilter !== "all") {
      params.type = typeFilter;
    }

    execute(() => organizationsApi.list(params));
  }, [execute, currentPage, debouncedSearch, statusFilter, typeFilter]);

  // Fetch on mount and when params change
  useEffect(() => {
    fetchOrganizations();
  }, [fetchOrganizations]);

  // Reset to page 1 when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [debouncedSearch, statusFilter, typeFilter]);

  const organizations = data?.items || [];
  const totalOrganizations = data?.total || 0;
  const totalPages = Math.ceil(totalOrganizations / ITEMS_PER_PAGE);

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
    setTypeFilter("all");
    setCurrentPage(1);
  };

  const hasActiveFilters = searchQuery || statusFilter !== "all" || typeFilter !== "all";

  const formatDate = (dateString: string) => {
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
            <CardTitle>Organizations</CardTitle>
            <CardDescription>
              Manage organizations and their settings
              {totalOrganizations > 0 && ` • ${totalOrganizations} total organizations`}
            </CardDescription>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={fetchOrganizations}
              disabled={loading}
            >
              <RefreshCw
                className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
              />
              Refresh
            </Button>
            <Button size="sm" onClick={() => setShowCreateDialog(true)}>
              <Plus className="h-4 w-4 mr-2" />
              Add Organization
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
              placeholder="Search by name, slug, or email..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9"
            />
          </div>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[150px]"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as OrganizationStatus | "all")}
          >
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[150px]"
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value as OrganizationType | "all")}
          >
            {TYPE_OPTIONS.map((option) => (
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
            <Button variant="outline" onClick={fetchOrganizations}>
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
        {!loading && !error && organizations.length === 0 && (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <Building2 className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">
              {hasActiveFilters
                ? "No organizations match your filters"
                : "No organizations found"}
            </p>
          </div>
        )}

        {/* Organization Table */}
        {!error && organizations.length > 0 && (
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
                        Name
                        {getSortIcon("name")}
                      </div>
                    </th>
                    <th className="text-left py-3 px-4 font-medium">Slug</th>
                    <th className="text-left py-3 px-4 font-medium">Type</th>
                    <th className="text-left py-3 px-4 font-medium">Status</th>
                    <th className="text-left py-3 px-4 font-medium">Contact</th>
                    <th className="text-left py-3 px-4 font-medium">Limits</th>
                    <th
                      className="text-left py-3 px-4 font-medium cursor-pointer hover:bg-muted/50"
                      onClick={() => handleSort("created_at")}
                    >
                      <div className="flex items-center">
                        Created
                        {getSortIcon("created_at")}
                      </div>
                    </th>
                    <th className="text-right py-3 px-4 font-medium">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {organizations.map((org) => (
                    <tr
                      key={org.id}
                      className="border-b hover:bg-muted/50"
                    >
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2">
                          <Building2 className="h-4 w-4 text-muted-foreground" />
                          <span className="font-medium">{org.name}</span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-muted-foreground font-mono text-sm">
                        {org.slug}
                      </td>
                      <td className="py-3 px-4">
                        <OrganizationTypeBadge type={org.type} />
                      </td>
                      <td className="py-3 px-4">
                        <OrganizationStatusBadge status={org.status} />
                      </td>
                      <td className="py-3 px-4 text-sm">
                        {org.contact_email}
                      </td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        <div className="flex flex-col gap-0.5">
                          <span>{org.max_users} users</span>
                          <span>{org.max_devices} devices</span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        {formatDate(org.created_at)}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <OrganizationActionsMenu organization={org} onActionComplete={fetchOrganizations} />
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
                  {Math.min(currentPage * ITEMS_PER_PAGE, totalOrganizations)} of{" "}
                  {totalOrganizations} organizations
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

      {/* Create Organization Dialog */}
      {showCreateDialog && (
        <OrganizationCreateDialog
          onSuccess={() => {
            setShowCreateDialog(false);
            fetchOrganizations();
          }}
          onCancel={() => setShowCreateDialog(false)}
        />
      )}
    </Card>
  );
}
