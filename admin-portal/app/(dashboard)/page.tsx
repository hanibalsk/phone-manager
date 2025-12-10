"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Users,
  Smartphone,
  Building2,
  FolderOpen,
  Bell,
  AlertTriangle,
  Webhook,
  Key,
  RefreshCw,
  ArrowUpRight,
  TrendingUp,
  TrendingDown,
  Plus,
  Settings,
  BarChart3,
  MapPin,
  Clock,
  Shield,
  Activity,
  LayoutDashboard,
} from "lucide-react";
import { useApi } from "@/hooks/use-api";
import { dashboardApi } from "@/lib/api-client";
import type { DashboardMetrics, AlertIndicators } from "@/types";

interface MetricCardProps {
  title: string;
  value: number;
  icon: React.ElementType;
  subtitle?: string;
  trend?: {
    value: number;
    label: string;
    isPositive?: boolean;
  };
  href?: string;
}

function MetricCard({
  title,
  value,
  icon: Icon,
  subtitle,
  trend,
  href,
}: MetricCardProps) {
  const content = (
    <Card className={href ? "hover:bg-muted/50 transition-colors cursor-pointer" : ""}>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium">{title}</CardTitle>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value.toLocaleString()}</div>
        {subtitle && (
          <p className="text-xs text-muted-foreground">{subtitle}</p>
        )}
        {trend && (
          <div className="flex items-center text-xs mt-1">
            {trend.isPositive !== false ? (
              <TrendingUp className="h-3 w-3 text-green-500 mr-1" />
            ) : (
              <TrendingDown className="h-3 w-3 text-red-500 mr-1" />
            )}
            <span
              className={
                trend.isPositive !== false ? "text-green-500" : "text-red-500"
              }
            >
              {trend.value > 0 ? "+" : ""}
              {trend.value}
            </span>
            <span className="text-muted-foreground ml-1">{trend.label}</span>
          </div>
        )}
      </CardContent>
    </Card>
  );

  if (href) {
    return <Link href={href}>{content}</Link>;
  }
  return content;
}

interface AlertCardProps {
  title: string;
  count: number;
  icon: React.ElementType;
  variant: "warning" | "danger" | "info";
  href: string;
}

function AlertCard({ title, count, icon: Icon, variant, href }: AlertCardProps) {
  if (count === 0) return null;

  const variantStyles = {
    warning: "border-yellow-500/50 bg-yellow-500/10",
    danger: "border-red-500/50 bg-red-500/10",
    info: "border-blue-500/50 bg-blue-500/10",
  };

  const badgeVariant = variant === "danger" ? "destructive" : "secondary";

  return (
    <Link href={href}>
      <Card
        className={`${variantStyles[variant]} hover:opacity-80 transition-opacity cursor-pointer`}
      >
        <CardContent className="flex items-center justify-between p-4">
          <div className="flex items-center gap-3">
            <Icon className="h-5 w-5" />
            <span className="font-medium">{title}</span>
          </div>
          <Badge variant={badgeVariant}>{count}</Badge>
        </CardContent>
      </Card>
    </Link>
  );
}

interface QuickActionProps {
  label: string;
  icon: React.ElementType;
  href: string;
  badge?: number;
}

function QuickAction({ label, icon: Icon, href, badge }: QuickActionProps) {
  return (
    <Link href={href}>
      <Button variant="outline" className="w-full justify-start gap-2 relative">
        <Icon className="h-4 w-4" />
        {label}
        {badge !== undefined && badge > 0 && (
          <Badge variant="secondary" className="ml-auto">
            {badge}
          </Badge>
        )}
        <ArrowUpRight className="h-3 w-3 ml-auto" />
      </Button>
    </Link>
  );
}

export default function DashboardPage() {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [alerts, setAlerts] = useState<AlertIndicators | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const { execute: fetchMetrics, loading: metricsLoading } = useApi<DashboardMetrics>();
  const { execute: fetchAlerts, loading: alertsLoading } = useApi<AlertIndicators>();
  const { execute: refreshMetricsApi } = useApi<DashboardMetrics>();

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadData = async () => {
    const [metricsResult, alertsResult] = await Promise.all([
      fetchMetrics(() => dashboardApi.getMetrics()),
      fetchAlerts(() => dashboardApi.getAlerts()),
    ]);
    if (metricsResult) {
      setMetrics(metricsResult);
    }
    if (alertsResult) {
      setAlerts(alertsResult);
    }
  };

  const handleRefresh = async () => {
    setIsRefreshing(true);
    const result = await refreshMetricsApi(() => dashboardApi.refreshMetrics());
    if (result) {
      setMetrics(result);
    }
    // Also refresh alerts
    const alertsResult = await fetchAlerts(() => dashboardApi.getAlerts());
    if (alertsResult) {
      setAlerts(alertsResult);
    }
    setIsRefreshing(false);
  };

  const loading = metricsLoading || alertsLoading;

  // Placeholder data while loading
  const displayMetrics: DashboardMetrics = metrics || {
    users: { total: 0, active: 0, new_today: 0, active_today: 0 },
    devices: { total: 0, online: 0, offline: 0, new_today: 0 },
    organizations: { total: 0, active: 0, new_today: 0 },
    groups: { total: 0, active: 0, new_today: 0 },
  };

  const displayAlerts: AlertIndicators = alerts || {
    pending_unlock_requests: 0,
    pending_registrations: 0,
    failed_webhooks: 0,
    system_alerts: 0,
    expiring_api_keys: 0,
  };

  const totalAlerts =
    displayAlerts.pending_unlock_requests +
    displayAlerts.pending_registrations +
    displayAlerts.failed_webhooks +
    displayAlerts.system_alerts +
    displayAlerts.expiring_api_keys;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <LayoutDashboard className="h-8 w-8 text-primary" />
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
            <p className="text-muted-foreground">
              Platform overview and key metrics
            </p>
          </div>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={handleRefresh}
          disabled={isRefreshing}
        >
          <RefreshCw
            className={`h-4 w-4 mr-2 ${isRefreshing ? "animate-spin" : ""}`}
          />
          Refresh
        </Button>
      </div>

      {/* Alert Indicators */}
      {totalAlerts > 0 && (
        <div className="space-y-2">
          <h2 className="text-lg font-semibold flex items-center gap-2">
            <Bell className="h-5 w-5" />
            Attention Required
            <Badge variant="destructive">{totalAlerts}</Badge>
          </h2>
          <div className="grid gap-2 md:grid-cols-2 lg:grid-cols-3">
            <AlertCard
              title="Pending Unlock Requests"
              count={displayAlerts.pending_unlock_requests}
              icon={Clock}
              variant="warning"
              href="/unlock-requests?status=pending"
            />
            <AlertCard
              title="Pending Registrations"
              count={displayAlerts.pending_registrations}
              icon={Users}
              variant="info"
              href="/users?status=pending"
            />
            <AlertCard
              title="Failed Webhooks"
              count={displayAlerts.failed_webhooks}
              icon={Webhook}
              variant="danger"
              href="/webhooks?status=failed"
            />
            <AlertCard
              title="System Alerts"
              count={displayAlerts.system_alerts}
              icon={AlertTriangle}
              variant="danger"
              href="/system-config"
            />
            <AlertCard
              title="Expiring API Keys"
              count={displayAlerts.expiring_api_keys}
              icon={Key}
              variant="warning"
              href="/system-config?tab=api-keys"
            />
          </div>
        </div>
      )}

      {/* Key Metrics */}
      <div className="space-y-4">
        <h2 className="text-lg font-semibold">Key Metrics</h2>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <MetricCard
            title="Total Users"
            value={displayMetrics.users.total}
            icon={Users}
            subtitle={`${displayMetrics.users.active} active`}
            trend={{
              value: displayMetrics.users.new_today,
              label: "new today",
            }}
            href="/users"
          />
          <MetricCard
            title="Total Devices"
            value={displayMetrics.devices.total}
            icon={Smartphone}
            subtitle={`${displayMetrics.devices.online} online`}
            trend={{
              value: displayMetrics.devices.new_today,
              label: "new today",
            }}
            href="/devices/fleet"
          />
          <MetricCard
            title="Organizations"
            value={displayMetrics.organizations.total}
            icon={Building2}
            subtitle={`${displayMetrics.organizations.active} active`}
            trend={{
              value: displayMetrics.organizations.new_today,
              label: "new today",
            }}
            href="/organizations"
          />
          <MetricCard
            title="Groups"
            value={displayMetrics.groups.total}
            icon={FolderOpen}
            subtitle={`${displayMetrics.groups.active} active`}
            trend={{
              value: displayMetrics.groups.new_today,
              label: "new today",
            }}
            href="/groups"
          />
        </div>
      </div>

      {/* Activity Summary */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Active Today</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {displayMetrics.users.active_today}
            </div>
            <p className="text-xs text-muted-foreground">users active today</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Online Devices</CardTitle>
            <Smartphone className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {displayMetrics.devices.online}
            </div>
            <p className="text-xs text-muted-foreground">
              {displayMetrics.devices.total > 0
                ? `${Math.round(
                    (displayMetrics.devices.online / displayMetrics.devices.total) *
                      100
                  )}% of fleet online`
                : "No devices enrolled"}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Offline Devices</CardTitle>
            <Smartphone className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {displayMetrics.devices.offline}
            </div>
            <p className="text-xs text-muted-foreground">
              {displayMetrics.devices.total > 0
                ? `${Math.round(
                    (displayMetrics.devices.offline / displayMetrics.devices.total) *
                      100
                  )}% of fleet offline`
                : "No devices enrolled"}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">New Today</CardTitle>
            <TrendingUp className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {displayMetrics.users.new_today + displayMetrics.devices.new_today}
            </div>
            <p className="text-xs text-muted-foreground">
              {displayMetrics.users.new_today} users, {displayMetrics.devices.new_today} devices
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Quick Actions */}
      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Quick Actions</CardTitle>
            <CardDescription>Common administrative tasks</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-2">
            <QuickAction
              label="Create New User"
              icon={Plus}
              href="/users?action=create"
            />
            <QuickAction
              label="Enroll Device"
              icon={Smartphone}
              href="/devices/enrollment"
            />
            <QuickAction
              label="Create Organization"
              icon={Building2}
              href="/organizations?action=create"
            />
            <QuickAction
              label="View Unlock Requests"
              icon={Clock}
              href="/unlock-requests"
              badge={displayAlerts.pending_unlock_requests}
            />
            <QuickAction
              label="System Configuration"
              icon={Settings}
              href="/system-config"
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Analytics & Reports</CardTitle>
            <CardDescription>View detailed analytics and generate reports</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-2">
            <QuickAction
              label="User Analytics"
              icon={Users}
              href="/analytics/users"
            />
            <QuickAction
              label="Device Analytics"
              icon={Smartphone}
              href="/analytics/devices"
            />
            <QuickAction
              label="API Analytics"
              icon={BarChart3}
              href="/analytics/api"
            />
            <QuickAction
              label="Location History"
              icon={MapPin}
              href="/locations/history"
            />
            <QuickAction
              label="Custom Reports"
              icon={Shield}
              href="/reports"
            />
          </CardContent>
        </Card>
      </div>

      {/* Welcome Card for new users */}
      {displayMetrics.users.total === 0 && displayMetrics.devices.total === 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Welcome to Phone Manager Admin</CardTitle>
            <CardDescription>
              Get started by enrolling a device or configuring your settings.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex gap-4">
              <Link href="/devices/enrollment">
                <Button>
                  <Smartphone className="h-4 w-4 mr-2" />
                  Enroll First Device
                </Button>
              </Link>
              <Link href="/system-config">
                <Button variant="outline">
                  <Settings className="h-4 w-4 mr-2" />
                  Configure Settings
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
