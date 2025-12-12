"use client";

import { useState, useEffect, useCallback } from "react";
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
  Search,
  Download,
  RefreshCw,
  Building,
  ArrowLeft,
  AlertTriangle,
  Users,
  Activity,
  TrendingUp,
  TrendingDown,
} from "lucide-react";
import Link from "next/link";
import { useApi } from "@/hooks/use-api";
import { auditApi, organizationsApi } from "@/lib/api-client";
import type { OrgActivityReport, Organization, AuditResourceType } from "@/types";

const DATE_PRESETS = [
  { label: "Last 7 days", days: 7 },
  { label: "Last 30 days", days: 30 },
  { label: "Last 90 days", days: 90 },
];

const SEVERITY_COLORS = {
  low: "bg-blue-100 text-blue-800",
  medium: "bg-yellow-100 text-yellow-800",
  high: "bg-red-100 text-red-800",
};

function StatCard({
  title,
  value,
  icon: Icon,
  trend,
}: {
  title: string;
  value: string | number;
  icon: React.ComponentType<{ className?: string }>;
  trend?: "up" | "down" | null;
}) {
  return (
    <Card data-testid={`org-activity-stat-card-${title.toLowerCase().replace(/\s+/g, "-")}`}>
      <CardContent className="p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-muted rounded-lg">
              <Icon className="h-5 w-5 text-muted-foreground" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">{title}</p>
              <p className="text-2xl font-semibold">{value}</p>
            </div>
          </div>
          {trend && (
            <div
              className={`p-1 rounded ${trend === "up" ? "bg-green-100" : "bg-red-100"}`}
            >
              {trend === "up" ? (
                <TrendingUp className="h-4 w-4 text-green-600" />
              ) : (
                <TrendingDown className="h-4 w-4 text-red-600" />
              )}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

function AnomalyCard({
  anomaly,
}: {
  anomaly: {
    type: string;
    description: string;
    severity: "low" | "medium" | "high";
    timestamp: string;
  };
}) {
  return (
    <div className="flex items-start gap-3 p-3 border rounded-lg">
      <AlertTriangle
        className={`h-5 w-5 mt-0.5 ${
          anomaly.severity === "high"
            ? "text-red-500"
            : anomaly.severity === "medium"
              ? "text-yellow-500"
              : "text-blue-500"
        }`}
      />
      <div className="flex-1">
        <div className="flex items-center gap-2 mb-1">
          <span className="font-medium text-sm">{anomaly.type}</span>
          <Badge className={SEVERITY_COLORS[anomaly.severity]}>
            {anomaly.severity}
          </Badge>
        </div>
        <p className="text-sm text-muted-foreground">{anomaly.description}</p>
        <p className="text-xs text-muted-foreground mt-1">
          {new Date(anomaly.timestamp).toLocaleString()}
        </p>
      </div>
    </div>
  );
}

export default function OrganizationActivityPage() {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [selectedOrgId, setSelectedOrgId] = useState<string>("");
  const [orgSearch, setOrgSearch] = useState("");
  const [report, setReport] = useState<OrgActivityReport | null>(null);
  const [dateFrom, setDateFrom] = useState(
    new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0]
  );
  const [dateTo, setDateTo] = useState(new Date().toISOString().split("T")[0]);

  const { execute: fetchOrgs, loading: orgsLoading } = useApi<{
    items: Organization[];
    total: number;
  }>();
  const { execute: fetchReport, loading: reportLoading } =
    useApi<OrgActivityReport>();

  const loadOrganizations = useCallback(async () => {
    const result = await fetchOrgs(() =>
      organizationsApi.list({ page: 1, limit: 100, search: orgSearch })
    );
    if (result) {
      setOrganizations(result.items);
    }
  }, [fetchOrgs, orgSearch]);

  useEffect(() => {
    loadOrganizations();
  }, [loadOrganizations]);

  const handleOrgSearch = () => {
    loadOrganizations();
  };

  const loadReport = useCallback(async () => {
    if (!selectedOrgId) return;
    const result = await fetchReport(() =>
      auditApi.getOrgActivity(selectedOrgId, dateFrom, dateTo)
    );
    if (result) {
      setReport(result);
    }
  }, [fetchReport, selectedOrgId, dateFrom, dateTo]);

  useEffect(() => {
    if (selectedOrgId) {
      loadReport();
    }
  }, [selectedOrgId, loadReport]);

  const setDatePreset = (days: number) => {
    const end = new Date();
    const start = new Date(Date.now() - days * 24 * 60 * 60 * 1000);
    setDateFrom(start.toISOString().split("T")[0]);
    setDateTo(end.toISOString().split("T")[0]);
  };

  return (
    <div className="space-y-6" data-testid="org-activity-page">
      {/* Header */}
      <div className="flex items-center justify-between" data-testid="org-activity-header">
        <div>
          <div className="flex items-center gap-2 text-sm text-muted-foreground mb-2">
            <Link href="/" className="hover:text-foreground">
              Dashboard
            </Link>
            <span>/</span>
            <Link href="/audit" className="hover:text-foreground">
              Audit
            </Link>
            <span>/</span>
            <span>Organization Activity</span>
          </div>
          <h1 className="text-3xl font-bold">Organization Activity Reports</h1>
          <p className="text-muted-foreground mt-1">
            Monitor organization usage and detect anomalies
          </p>
        </div>
        <Link href="/audit">
          <Button variant="outline" data-testid="org-activity-back-button">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Audit
          </Button>
        </Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Organization Selection */}
        <Card className="lg:col-span-1" data-testid="org-activity-selection-card">
          <CardHeader>
            <CardTitle className="text-lg">Select Organization</CardTitle>
            <CardDescription>
              Choose an organization to view activity
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search organizations..."
                  value={orgSearch}
                  onChange={(e) => setOrgSearch(e.target.value)}
                  className="pl-10"
                  onKeyDown={(e) => e.key === "Enter" && handleOrgSearch()}
                  data-testid="org-activity-search-input"
                />
              </div>
              <Button variant="outline" onClick={handleOrgSearch} data-testid="org-activity-search-button">
                <Search className="h-4 w-4" />
              </Button>
            </div>

            <div className="space-y-2 max-h-[400px] overflow-y-auto" data-testid="org-activity-org-list">
              {orgsLoading ? (
                <div className="text-center py-4" data-testid="org-activity-loading">
                  <RefreshCw className="h-5 w-5 animate-spin mx-auto text-muted-foreground" />
                </div>
              ) : organizations.length === 0 ? (
                <p className="text-sm text-muted-foreground text-center py-4" data-testid="org-activity-empty">
                  No organizations found
                </p>
              ) : (
                organizations.map((org) => (
                  <button
                    key={org.id}
                    onClick={() => setSelectedOrgId(org.id)}
                    className={`w-full p-3 text-left rounded-lg border transition-colors ${
                      selectedOrgId === org.id
                        ? "border-primary bg-primary/5"
                        : "border-border hover:border-primary/50"
                    }`}
                    data-testid={`org-activity-org-item-${org.id}`}
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-muted flex items-center justify-center">
                        <Building className="h-4 w-4 text-muted-foreground" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-sm truncate">{org.name}</p>
                        <p className="text-xs text-muted-foreground truncate">
                          {org.type}
                        </p>
                      </div>
                    </div>
                  </button>
                ))
              )}
            </div>
          </CardContent>
        </Card>

        {/* Activity Report */}
        <div className="lg:col-span-2 space-y-6" data-testid="org-activity-report-section">
          {/* Date Range */}
          <Card data-testid="org-activity-date-range-card">
            <CardHeader className="pb-4">
              <CardTitle className="text-lg">Date Range</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-2 mb-4">
                {DATE_PRESETS.map((preset) => (
                  <Button
                    key={preset.days}
                    variant="outline"
                    size="sm"
                    onClick={() => setDatePreset(preset.days)}
                    data-testid={`org-activity-preset-${preset.days}`}
                  >
                    {preset.label}
                  </Button>
                ))}
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label className="text-xs text-muted-foreground">From</Label>
                  <Input
                    type="date"
                    value={dateFrom}
                    onChange={(e) => setDateFrom(e.target.value)}
                    data-testid="org-activity-date-from"
                  />
                </div>
                <div>
                  <Label className="text-xs text-muted-foreground">To</Label>
                  <Input
                    type="date"
                    value={dateTo}
                    onChange={(e) => setDateTo(e.target.value)}
                    data-testid="org-activity-date-to"
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          {!selectedOrgId ? (
            <Card data-testid="org-activity-no-selection">
              <CardContent className="py-12 text-center">
                <Building className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
                <p className="text-muted-foreground">
                  Select an organization to view activity report
                </p>
              </CardContent>
            </Card>
          ) : reportLoading ? (
            <Card data-testid="org-activity-report-loading">
              <CardContent className="py-12 text-center">
                <RefreshCw className="h-8 w-8 animate-spin mx-auto text-muted-foreground" />
              </CardContent>
            </Card>
          ) : report ? (
            <>
              {/* Summary Stats */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4" data-testid="org-activity-stats">
                <StatCard
                  title="Total Actions"
                  value={report.total_actions.toLocaleString()}
                  icon={Activity}
                />
                <StatCard
                  title="Active Users"
                  value={report.user_action_counts.length}
                  icon={Users}
                />
                <StatCard
                  title="Resources Changed"
                  value={report.resource_changes.reduce(
                    (sum, r) => sum + r.created + r.updated + r.deleted,
                    0
                  )}
                  icon={Building}
                />
                <StatCard
                  title="Anomalies"
                  value={report.anomalies.length}
                  icon={AlertTriangle}
                  trend={report.anomalies.length > 0 ? "up" : null}
                />
              </div>

              {/* Anomalies */}
              {report.anomalies.length > 0 && (
                <Card className="border-yellow-200 bg-yellow-50/50" data-testid="org-activity-anomalies-card">
                  <CardHeader className="pb-4">
                    <CardTitle className="text-lg flex items-center gap-2">
                      <AlertTriangle className="h-5 w-5 text-yellow-600" />
                      Detected Anomalies
                    </CardTitle>
                    <CardDescription>
                      Unusual activity patterns that may require attention
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-3" data-testid="org-activity-anomalies-list">
                      {report.anomalies.map((anomaly, idx) => (
                        <AnomalyCard key={idx} anomaly={anomaly} />
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* User Action Counts */}
              <Card data-testid="org-activity-users-card">
                <CardHeader className="pb-4">
                  <CardTitle className="text-lg">User Activity</CardTitle>
                  <CardDescription>
                    Action counts by user in this organization
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <Table data-testid="org-activity-users-table">
                    <TableHeader>
                      <TableRow>
                        <TableHead>User</TableHead>
                        <TableHead className="text-right">Actions</TableHead>
                        <TableHead className="w-[200px]">Distribution</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {report.user_action_counts.length === 0 ? (
                        <TableRow>
                          <TableCell
                            colSpan={3}
                            className="text-center text-muted-foreground"
                          >
                            No user activity
                          </TableCell>
                        </TableRow>
                      ) : (
                        report.user_action_counts.map((user) => (
                          <TableRow key={user.user_id}>
                            <TableCell>
                              <div className="flex items-center gap-2">
                                <div className="w-6 h-6 rounded-full bg-muted flex items-center justify-center">
                                  <Users className="h-3 w-3 text-muted-foreground" />
                                </div>
                                <span className="font-medium">{user.user_name}</span>
                              </div>
                            </TableCell>
                            <TableCell className="text-right font-mono">
                              {user.action_count}
                            </TableCell>
                            <TableCell>
                              <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                                <div
                                  className="h-full bg-primary"
                                  style={{
                                    width: `${(user.action_count / report.total_actions) * 100}%`,
                                  }}
                                />
                              </div>
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </CardContent>
              </Card>

              {/* Resource Changes */}
              <Card data-testid="org-activity-resources-card">
                <CardHeader className="pb-4">
                  <CardTitle className="text-lg">Resource Changes</CardTitle>
                  <CardDescription>
                    Summary of changes by resource type
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <Table data-testid="org-activity-resources-table">
                    <TableHeader>
                      <TableRow>
                        <TableHead>Resource Type</TableHead>
                        <TableHead className="text-right text-green-600">
                          Created
                        </TableHead>
                        <TableHead className="text-right text-blue-600">
                          Updated
                        </TableHead>
                        <TableHead className="text-right text-red-600">
                          Deleted
                        </TableHead>
                        <TableHead className="text-right">Total</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {report.resource_changes.length === 0 ? (
                        <TableRow>
                          <TableCell
                            colSpan={5}
                            className="text-center text-muted-foreground"
                          >
                            No resource changes
                          </TableCell>
                        </TableRow>
                      ) : (
                        report.resource_changes.map((resource) => (
                          <TableRow key={resource.resource_type}>
                            <TableCell className="font-medium capitalize">
                              {resource.resource_type.replace(/_/g, " ")}
                            </TableCell>
                            <TableCell className="text-right font-mono text-green-600">
                              +{resource.created}
                            </TableCell>
                            <TableCell className="text-right font-mono text-blue-600">
                              ~{resource.updated}
                            </TableCell>
                            <TableCell className="text-right font-mono text-red-600">
                              -{resource.deleted}
                            </TableCell>
                            <TableCell className="text-right font-mono font-semibold">
                              {resource.created + resource.updated + resource.deleted}
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </CardContent>
              </Card>
            </>
          ) : (
            <Card>
              <CardContent className="py-12 text-center">
                <p className="text-muted-foreground">No activity data available</p>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
