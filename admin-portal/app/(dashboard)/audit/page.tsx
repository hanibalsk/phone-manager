"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import {
  Search,
  Filter,
  Download,
  RefreshCw,
  Calendar,
  User,
  Building,
  ChevronLeft,
  ChevronRight,
  Eye,
  ArrowLeft,
  Clock,
  Globe,
  Monitor,
} from "lucide-react";
import Link from "next/link";
import { useApi } from "@/hooks/use-api";
import { auditApi } from "@/lib/api-client";
import type {
  AuditLogEntry,
  AuditLogFilter,
  AuditLogStats,
  AuditActionType,
  AuditResourceType,
} from "@/types";

const ACTION_COLORS: Record<AuditActionType, string> = {
  create: "bg-green-100 text-green-800",
  update: "bg-blue-100 text-blue-800",
  delete: "bg-red-100 text-red-800",
  login: "bg-purple-100 text-purple-800",
  logout: "bg-gray-100 text-gray-800",
  view: "bg-slate-100 text-slate-800",
  export: "bg-orange-100 text-orange-800",
  import: "bg-cyan-100 text-cyan-800",
  approve: "bg-emerald-100 text-emerald-800",
  reject: "bg-rose-100 text-rose-800",
  suspend: "bg-amber-100 text-amber-800",
  restore: "bg-teal-100 text-teal-800",
  archive: "bg-indigo-100 text-indigo-800",
};

const RESOURCE_ICONS: Record<AuditResourceType, string> = {
  user: "User",
  organization: "Building",
  device: "Smartphone",
  group: "Users",
  role: "Shield",
  permission: "Key",
  location: "MapPin",
  geofence: "Circle",
  webhook: "Webhook",
  trip: "Navigation",
  app_restriction: "Lock",
  unlock_request: "Unlock",
  config: "Settings",
  report: "FileText",
  audit_log: "History",
};

function StatCard({
  title,
  value,
  icon: Icon,
}: {
  title: string;
  value: string | number;
  icon: React.ComponentType<{ className?: string }>;
}) {
  return (
    <Card data-testid={`audit-stat-card-${title.toLowerCase().replace(/\s+/g, "-")}`}>
      <CardContent className="p-4">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-muted rounded-lg">
            <Icon className="h-5 w-5 text-muted-foreground" />
          </div>
          <div>
            <p className="text-sm text-muted-foreground">{title}</p>
            <p className="text-2xl font-semibold">{value}</p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function LogDetailSheet({
  log,
  open,
  onClose,
}: {
  log: AuditLogEntry | null;
  open: boolean;
  onClose: () => void;
}) {
  if (!log) return null;

  return (
    <Sheet open={open} onOpenChange={onClose}>
      <SheetContent className="w-[500px] sm:w-[600px] overflow-y-auto" data-testid="audit-log-detail-sheet">
        <SheetHeader>
          <SheetTitle>Audit Log Details</SheetTitle>
          <SheetDescription>
            Entry ID: {log.id}
          </SheetDescription>
        </SheetHeader>

        <div className="mt-6 space-y-6">
          {/* Basic Info */}
          <div className="space-y-4">
            <h3 className="font-semibold">Basic Information</h3>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-muted-foreground">Action</p>
                <Badge className={ACTION_COLORS[log.action]}>
                  {log.action.toUpperCase()}
                </Badge>
              </div>
              <div>
                <p className="text-muted-foreground">Resource Type</p>
                <p className="font-medium">{log.resource_type}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Resource ID</p>
                <p className="font-mono text-xs">{log.resource_id}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Resource Name</p>
                <p className="font-medium">{log.resource_name || "N/A"}</p>
              </div>
            </div>
          </div>

          {/* Actor Info */}
          <div className="space-y-4">
            <h3 className="font-semibold">Actor Information</h3>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-muted-foreground">Name</p>
                <p className="font-medium">{log.actor_name}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Email</p>
                <p className="font-medium">{log.actor_email}</p>
              </div>
              <div className="col-span-2">
                <p className="text-muted-foreground">User ID</p>
                <p className="font-mono text-xs">{log.actor_id}</p>
              </div>
            </div>
          </div>

          {/* Context */}
          <div className="space-y-4">
            <h3 className="font-semibold">Context</h3>
            <div className="grid grid-cols-1 gap-4 text-sm">
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-muted-foreground" />
                <span>{new Date(log.timestamp).toLocaleString()}</span>
              </div>
              <div className="flex items-center gap-2">
                <Globe className="h-4 w-4 text-muted-foreground" />
                <span>{log.ip_address}</span>
              </div>
              <div className="flex items-start gap-2">
                <Monitor className="h-4 w-4 text-muted-foreground mt-0.5" />
                <span className="text-xs break-all">{log.user_agent}</span>
              </div>
              {log.organization_name && (
                <div className="flex items-center gap-2">
                  <Building className="h-4 w-4 text-muted-foreground" />
                  <span>{log.organization_name}</span>
                </div>
              )}
            </div>
          </div>

          {/* State Changes */}
          {(log.before_state || log.after_state) && (
            <div className="space-y-4">
              <h3 className="font-semibold">State Changes</h3>
              <div className="grid grid-cols-1 gap-4">
                {log.before_state && (
                  <div>
                    <p className="text-sm text-muted-foreground mb-2">Before</p>
                    <pre className="p-3 bg-red-50 rounded-lg text-xs overflow-x-auto">
                      {JSON.stringify(log.before_state, null, 2)}
                    </pre>
                  </div>
                )}
                {log.after_state && (
                  <div>
                    <p className="text-sm text-muted-foreground mb-2">After</p>
                    <pre className="p-3 bg-green-50 rounded-lg text-xs overflow-x-auto">
                      {JSON.stringify(log.after_state, null, 2)}
                    </pre>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Metadata */}
          {log.metadata && Object.keys(log.metadata).length > 0 && (
            <div className="space-y-4">
              <h3 className="font-semibold">Additional Metadata</h3>
              <pre className="p-3 bg-muted rounded-lg text-xs overflow-x-auto">
                {JSON.stringify(log.metadata, null, 2)}
              </pre>
            </div>
          )}

          {/* Hash Chain */}
          {log.hash && (
            <div className="space-y-4">
              <h3 className="font-semibold">Integrity Information</h3>
              <div className="text-xs font-mono space-y-2">
                <div>
                  <p className="text-muted-foreground">Hash</p>
                  <p className="break-all">{log.hash}</p>
                </div>
                {log.previous_hash && (
                  <div>
                    <p className="text-muted-foreground">Previous Hash</p>
                    <p className="break-all">{log.previous_hash}</p>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}

export default function AuditLogsPage() {
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [stats, setStats] = useState<AuditLogStats | null>(null);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [selectedLog, setSelectedLog] = useState<AuditLogEntry | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);

  // Filters
  const [search, setSearch] = useState("");
  const [actionFilter, setActionFilter] = useState<AuditActionType | "">("");
  const [resourceFilter, setResourceFilter] = useState<AuditResourceType | "">("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");

  const { execute: fetchLogs, loading: logsLoading } = useApi<{
    data: AuditLogEntry[];
    total: number;
    page: number;
    limit: number;
  }>();
  const { execute: fetchStats, loading: statsLoading } = useApi<AuditLogStats>();
  const { execute: exportLogs, loading: exporting } = useApi<Blob>();

  const buildFilters = (): AuditLogFilter | undefined => {
    const filters: AuditLogFilter = {};
    if (search) filters.search = search;
    if (actionFilter) filters.action = actionFilter;
    if (resourceFilter) filters.resource_type = resourceFilter;
    if (dateFrom) filters.date_from = dateFrom;
    if (dateTo) filters.date_to = dateTo;
    return Object.keys(filters).length > 0 ? filters : undefined;
  };

  const loadData = async () => {
    const filters = buildFilters();
    const [logsResult, statsResult] = await Promise.all([
      fetchLogs(() => auditApi.getLogs(filters, page, 20)),
      fetchStats(() => auditApi.getStats(filters)),
    ]);
    if (logsResult) {
      setLogs(logsResult.data);
      setTotal(logsResult.total);
    }
    if (statsResult) {
      setStats(statsResult);
    }
  };

  useEffect(() => {
    loadData();
  }, [page]);

  const handleSearch = () => {
    setPage(1);
    loadData();
  };

  const handleExport = async (format: "csv" | "json") => {
    const filters = buildFilters();
    await exportLogs(() => auditApi.exportLogs(filters, format));
  };

  const handleViewDetails = (log: AuditLogEntry) => {
    setSelectedLog(log);
    setDetailOpen(true);
  };

  const totalPages = Math.ceil(total / 20);

  return (
    <div className="space-y-6" data-testid="audit-logs-page">
      {/* Header */}
      <div className="flex items-center justify-between" data-testid="audit-logs-header">
        <div>
          <div className="flex items-center gap-2 text-sm text-muted-foreground mb-2">
            <Link href="/" className="hover:text-foreground">
              Dashboard
            </Link>
            <span>/</span>
            <span>Audit Logs</span>
          </div>
          <h1 className="text-3xl font-bold">Audit Logs</h1>
          <p className="text-muted-foreground mt-1">
            Track all system changes and user actions
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => loadData()} disabled={logsLoading} data-testid="audit-logs-refresh-button">
            <RefreshCw className={`h-4 w-4 mr-2 ${logsLoading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
          <Button variant="outline" onClick={() => handleExport("csv")} disabled={exporting} data-testid="audit-logs-export-button">
            <Download className="h-4 w-4 mr-2" />
            Export CSV
          </Button>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4" data-testid="audit-logs-stats-section">
        <StatCard
          title="Total Entries"
          value={stats?.total_entries.toLocaleString() || 0}
          icon={Clock}
        />
        <StatCard
          title="Create Actions"
          value={stats?.entries_by_action.create || 0}
          icon={User}
        />
        <StatCard
          title="Update Actions"
          value={stats?.entries_by_action.update || 0}
          icon={Building}
        />
        <StatCard
          title="Delete Actions"
          value={stats?.entries_by_action.delete || 0}
          icon={Filter}
        />
      </div>

      {/* Quick Links */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4" data-testid="audit-logs-quick-links">
        <Link href="/audit/users" data-testid="audit-link-user-activity">
          <Card className="hover:border-primary transition-colors cursor-pointer">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <User className="h-5 w-5 text-primary" />
                <div>
                  <p className="font-medium">User Activity Reports</p>
                  <p className="text-sm text-muted-foreground">Review individual user actions</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </Link>
        <Link href="/audit/organizations" data-testid="audit-link-org-activity">
          <Card className="hover:border-primary transition-colors cursor-pointer">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <Building className="h-5 w-5 text-primary" />
                <div>
                  <p className="font-medium">Organization Reports</p>
                  <p className="text-sm text-muted-foreground">Monitor organization usage</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </Link>
        <Link href="/audit/gdpr" data-testid="audit-link-gdpr">
          <Card className="hover:border-primary transition-colors cursor-pointer">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <Download className="h-5 w-5 text-primary" />
                <div>
                  <p className="font-medium">GDPR Compliance</p>
                  <p className="text-sm text-muted-foreground">Data export & deletion</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </Link>
      </div>

      {/* Filters */}
      <Card data-testid="audit-logs-filter-card">
        <CardHeader className="pb-4">
          <CardTitle className="text-lg">Search & Filter</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
            <div className="md:col-span-2">
              <Label className="sr-only">Search</Label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search by actor, resource, or ID..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-10"
                  onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                  data-testid="audit-logs-search-input"
                />
              </div>
            </div>
            <div>
              <Label className="sr-only">Action</Label>
              <select
                className="w-full h-10 px-3 rounded-md border border-input bg-background"
                value={actionFilter}
                onChange={(e) => setActionFilter(e.target.value as AuditActionType | "")}
                data-testid="audit-logs-action-filter"
              >
                <option value="">All Actions</option>
                {Object.keys(ACTION_COLORS).map((action) => (
                  <option key={action} value={action}>
                    {action.charAt(0).toUpperCase() + action.slice(1)}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <Label className="sr-only">Resource</Label>
              <select
                className="w-full h-10 px-3 rounded-md border border-input bg-background"
                value={resourceFilter}
                onChange={(e) => setResourceFilter(e.target.value as AuditResourceType | "")}
                data-testid="audit-logs-resource-filter"
              >
                <option value="">All Resources</option>
                {Object.keys(RESOURCE_ICONS).map((resource) => (
                  <option key={resource} value={resource}>
                    {resource.replace(/_/g, " ").replace(/\b\w/g, (l) => l.toUpperCase())}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <Button onClick={handleSearch} className="w-full" data-testid="audit-logs-apply-filters-button">
                <Filter className="h-4 w-4 mr-2" />
                Apply Filters
              </Button>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mt-4">
            <div>
              <Label className="text-xs text-muted-foreground">Date From</Label>
              <Input
                type="date"
                value={dateFrom}
                onChange={(e) => setDateFrom(e.target.value)}
                data-testid="audit-logs-date-from"
              />
            </div>
            <div>
              <Label className="text-xs text-muted-foreground">Date To</Label>
              <Input
                type="date"
                value={dateTo}
                onChange={(e) => setDateTo(e.target.value)}
                data-testid="audit-logs-date-to"
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Logs Table */}
      <Card data-testid="audit-logs-table-card">
        <CardHeader className="pb-4">
          <CardTitle className="text-lg">
            Audit Log Entries
            <span className="text-sm font-normal text-muted-foreground ml-2" data-testid="audit-logs-total-count">
              ({total.toLocaleString()} total)
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Table data-testid="audit-logs-table">
            <TableHeader>
              <TableRow>
                <TableHead>Timestamp</TableHead>
                <TableHead>Actor</TableHead>
                <TableHead>Action</TableHead>
                <TableHead>Resource</TableHead>
                <TableHead>IP Address</TableHead>
                <TableHead className="text-right">Details</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {logsLoading ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8">
                    <RefreshCw className="h-6 w-6 animate-spin mx-auto text-muted-foreground" />
                  </TableCell>
                </TableRow>
              ) : logs.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                    No audit logs found
                  </TableCell>
                </TableRow>
              ) : (
                logs.map((log) => (
                  <TableRow key={log.id} data-testid={`audit-log-row-${log.id}`}>
                    <TableCell className="font-mono text-xs">
                      {new Date(log.timestamp).toLocaleString()}
                    </TableCell>
                    <TableCell>
                      <div>
                        <p className="font-medium text-sm">{log.actor_name}</p>
                        <p className="text-xs text-muted-foreground">{log.actor_email}</p>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge className={ACTION_COLORS[log.action]}>
                        {log.action.toUpperCase()}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div>
                        <p className="text-sm">{log.resource_type}</p>
                        <p className="text-xs text-muted-foreground truncate max-w-[150px]">
                          {log.resource_name || log.resource_id}
                        </p>
                      </div>
                    </TableCell>
                    <TableCell className="font-mono text-xs">
                      {log.ip_address}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleViewDetails(log)}
                        data-testid={`audit-log-view-details-${log.id}`}
                      >
                        <Eye className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between mt-4" data-testid="audit-logs-pagination">
              <p className="text-sm text-muted-foreground">
                Page {page} of {totalPages}
              </p>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.max(1, page - 1))}
                  disabled={page === 1}
                  data-testid="audit-logs-prev-page"
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.min(totalPages, page + 1))}
                  disabled={page === totalPages}
                  data-testid="audit-logs-next-page"
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Detail Sheet */}
      <LogDetailSheet
        log={selectedLog}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
      />
    </div>
  );
}
