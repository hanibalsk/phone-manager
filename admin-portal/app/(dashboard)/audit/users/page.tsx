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
  Search,
  Download,
  RefreshCw,
  Calendar,
  User,
  ArrowLeft,
  Clock,
  FileText,
  Settings,
  Shield,
  Building,
  Smartphone,
} from "lucide-react";
import Link from "next/link";
import { useApi } from "@/hooks/use-api";
import { auditApi, usersApi } from "@/lib/api-client";
import type { UserActivityReport, AuditActionType, AuditResourceType, AdminUser } from "@/types";

const ACTION_COLORS: Record<AuditActionType, string> = {
  create: "bg-green-500",
  update: "bg-blue-500",
  delete: "bg-red-500",
  login: "bg-purple-500",
  logout: "bg-gray-500",
  view: "bg-slate-500",
  export: "bg-orange-500",
  import: "bg-cyan-500",
  approve: "bg-emerald-500",
  reject: "bg-rose-500",
  suspend: "bg-amber-500",
  restore: "bg-teal-500",
  archive: "bg-indigo-500",
};

const RESOURCE_ICONS: Record<AuditResourceType, React.ComponentType<{ className?: string }>> = {
  user: User,
  organization: Building,
  device: Smartphone,
  group: User,
  role: Shield,
  permission: Shield,
  location: Clock,
  geofence: Clock,
  webhook: Settings,
  trip: Clock,
  app_restriction: Settings,
  unlock_request: Shield,
  config: Settings,
  report: FileText,
  audit_log: Clock,
};

const DATE_PRESETS = [
  { label: "Last 7 days", days: 7 },
  { label: "Last 30 days", days: 30 },
  { label: "Last 90 days", days: 90 },
];

function ActionTypeBadge({ action }: { action: AuditActionType }) {
  return (
    <div className="flex items-center gap-2">
      <div className={`w-2 h-2 rounded-full ${ACTION_COLORS[action]}`} />
      <span className="text-sm capitalize">{action}</span>
    </div>
  );
}

function TimelineItem({
  item,
}: {
  item: {
    date: string;
    action: AuditActionType;
    resource_type: AuditResourceType;
    resource_name?: string;
    timestamp: string;
  };
}) {
  const Icon = RESOURCE_ICONS[item.resource_type] || Clock;

  return (
    <div className="flex gap-4">
      <div className="flex flex-col items-center">
        <div className={`w-3 h-3 rounded-full ${ACTION_COLORS[item.action]}`} />
        <div className="w-px h-full bg-border" />
      </div>
      <div className="pb-6 flex-1">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Icon className="h-4 w-4 text-muted-foreground" />
            <span className="font-medium text-sm capitalize">{item.action}</span>
            <span className="text-muted-foreground text-sm">
              {item.resource_type.replace(/_/g, " ")}
            </span>
          </div>
          <span className="text-xs text-muted-foreground">
            {new Date(item.timestamp).toLocaleTimeString()}
          </span>
        </div>
        {item.resource_name && (
          <p className="text-sm text-muted-foreground mt-1">{item.resource_name}</p>
        )}
      </div>
    </div>
  );
}

export default function UserActivityPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<string>("");
  const [userSearch, setUserSearch] = useState("");
  const [report, setReport] = useState<UserActivityReport | null>(null);
  const [dateFrom, setDateFrom] = useState(
    new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0]
  );
  const [dateTo, setDateTo] = useState(new Date().toISOString().split("T")[0]);

  const { execute: fetchUsers, loading: usersLoading } = useApi<{ items: AdminUser[]; total: number }>();
  const { execute: fetchReport, loading: reportLoading } = useApi<UserActivityReport>();
  const { execute: exportReport, loading: exporting } = useApi<Blob>();

  const loadUsers = async () => {
    const result = await fetchUsers(() => usersApi.list({ page: 1, limit: 100, search: userSearch }));
    if (result) {
      setUsers(result.items);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleUserSearch = () => {
    loadUsers();
  };

  const loadReport = async () => {
    if (!selectedUserId) return;
    const result = await fetchReport(() =>
      auditApi.getUserActivity(selectedUserId, dateFrom, dateTo)
    );
    if (result) {
      setReport(result);
    }
  };

  useEffect(() => {
    if (selectedUserId) {
      loadReport();
    }
  }, [selectedUserId, dateFrom, dateTo]);

  const handleExport = async (format: "pdf" | "csv") => {
    if (!selectedUserId) return;
    await exportReport(() => auditApi.exportUserActivity(selectedUserId, format));
  };

  const setDatePreset = (days: number) => {
    const end = new Date();
    const start = new Date(Date.now() - days * 24 * 60 * 60 * 1000);
    setDateFrom(start.toISOString().split("T")[0]);
    setDateTo(end.toISOString().split("T")[0]);
  };

  // Group timeline by date
  const timelineByDate = report?.timeline.reduce(
    (acc, item) => {
      const date = item.date;
      if (!acc[date]) acc[date] = [];
      acc[date].push(item);
      return acc;
    },
    {} as Record<string, typeof report.timeline>
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
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
            <span>User Activity</span>
          </div>
          <h1 className="text-3xl font-bold">User Activity Reports</h1>
          <p className="text-muted-foreground mt-1">
            Review individual user actions and activity timeline
          </p>
        </div>
        <Link href="/audit">
          <Button variant="outline">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Audit
          </Button>
        </Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* User Selection */}
        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle className="text-lg">Select User</CardTitle>
            <CardDescription>Choose a user to view their activity</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search users..."
                  value={userSearch}
                  onChange={(e) => setUserSearch(e.target.value)}
                  className="pl-10"
                  onKeyDown={(e) => e.key === "Enter" && handleUserSearch()}
                />
              </div>
              <Button variant="outline" onClick={handleUserSearch}>
                <Search className="h-4 w-4" />
              </Button>
            </div>

            <div className="space-y-2 max-h-[400px] overflow-y-auto">
              {usersLoading ? (
                <div className="text-center py-4">
                  <RefreshCw className="h-5 w-5 animate-spin mx-auto text-muted-foreground" />
                </div>
              ) : users.length === 0 ? (
                <p className="text-sm text-muted-foreground text-center py-4">
                  No users found
                </p>
              ) : (
                users.map((user) => (
                  <button
                    key={user.id}
                    onClick={() => setSelectedUserId(user.id)}
                    className={`w-full p-3 text-left rounded-lg border transition-colors ${
                      selectedUserId === user.id
                        ? "border-primary bg-primary/5"
                        : "border-border hover:border-primary/50"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-muted flex items-center justify-center">
                        <User className="h-4 w-4 text-muted-foreground" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-sm truncate">{user.display_name}</p>
                        <p className="text-xs text-muted-foreground truncate">
                          {user.email}
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
        <div className="lg:col-span-2 space-y-6">
          {/* Date Range */}
          <Card>
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
                  />
                </div>
                <div>
                  <Label className="text-xs text-muted-foreground">To</Label>
                  <Input
                    type="date"
                    value={dateTo}
                    onChange={(e) => setDateTo(e.target.value)}
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          {!selectedUserId ? (
            <Card>
              <CardContent className="py-12 text-center">
                <User className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
                <p className="text-muted-foreground">
                  Select a user to view their activity report
                </p>
              </CardContent>
            </Card>
          ) : reportLoading ? (
            <Card>
              <CardContent className="py-12 text-center">
                <RefreshCw className="h-8 w-8 animate-spin mx-auto text-muted-foreground" />
              </CardContent>
            </Card>
          ) : report ? (
            <>
              {/* Summary */}
              <Card>
                <CardHeader className="pb-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle className="text-lg">{report.user_name}</CardTitle>
                      <CardDescription>{report.user_email}</CardDescription>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleExport("csv")}
                        disabled={exporting}
                      >
                        <Download className="h-4 w-4 mr-2" />
                        CSV
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleExport("pdf")}
                        disabled={exporting}
                      >
                        <Download className="h-4 w-4 mr-2" />
                        PDF
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div className="text-center p-4 bg-muted rounded-lg">
                      <p className="text-3xl font-bold">{report.total_actions}</p>
                      <p className="text-sm text-muted-foreground">Total Actions</p>
                    </div>
                    <div className="text-center p-4 bg-muted rounded-lg">
                      <p className="text-3xl font-bold">
                        {Object.keys(report.actions_by_type).length}
                      </p>
                      <p className="text-sm text-muted-foreground">Action Types</p>
                    </div>
                    <div className="text-center p-4 bg-muted rounded-lg">
                      <p className="text-3xl font-bold">
                        {Object.keys(report.actions_by_resource).length}
                      </p>
                      <p className="text-sm text-muted-foreground">Resources</p>
                    </div>
                    <div className="text-center p-4 bg-muted rounded-lg">
                      <p className="text-3xl font-bold">{report.timeline.length}</p>
                      <p className="text-sm text-muted-foreground">Events</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Actions by Type */}
              <Card>
                <CardHeader className="pb-4">
                  <CardTitle className="text-lg">Actions by Type</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {Object.entries(report.actions_by_type).map(([action, count]) => (
                      <div key={action} className="flex items-center justify-between">
                        <ActionTypeBadge action={action as AuditActionType} />
                        <div className="flex items-center gap-2">
                          <div className="w-32 h-2 bg-muted rounded-full overflow-hidden">
                            <div
                              className={`h-full ${ACTION_COLORS[action as AuditActionType]}`}
                              style={{
                                width: `${(count / report.total_actions) * 100}%`,
                              }}
                            />
                          </div>
                          <span className="text-sm font-medium w-12 text-right">{count}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>

              {/* Timeline */}
              <Card>
                <CardHeader className="pb-4">
                  <CardTitle className="text-lg">Activity Timeline</CardTitle>
                </CardHeader>
                <CardContent>
                  {timelineByDate && Object.keys(timelineByDate).length > 0 ? (
                    <div className="space-y-6">
                      {Object.entries(timelineByDate).map(([date, items]) => (
                        <div key={date}>
                          <div className="flex items-center gap-2 mb-4">
                            <Calendar className="h-4 w-4 text-muted-foreground" />
                            <span className="font-medium">
                              {new Date(date).toLocaleDateString(undefined, {
                                weekday: "long",
                                year: "numeric",
                                month: "long",
                                day: "numeric",
                              })}
                            </span>
                            <Badge variant="secondary">{items.length} events</Badge>
                          </div>
                          <div className="ml-6">
                            {items.map((item, idx) => (
                              <TimelineItem key={idx} item={item} />
                            ))}
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-center text-muted-foreground py-8">
                      No activity in the selected date range
                    </p>
                  )}
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
