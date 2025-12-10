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
import { Badge } from "@/components/ui/badge";
import {
  Users,
  TrendingUp,
  TrendingDown,
  UserPlus,
  UserCheck,
  UserMinus,
  Calendar,
  RefreshCw,
  ArrowLeft,
} from "lucide-react";
import Link from "next/link";
import { useApi } from "@/hooks/use-api";
import { userAnalyticsApi } from "@/lib/api-client";
import type {
  UserAnalytics,
  UserGrowthData,
  UserRetentionData,
  UserSegmentData,
} from "@/types";

type TimePeriod = "7d" | "30d" | "90d" | "custom";

// Simple line chart component
function SimpleLineChart({
  data,
  dataKey,
  color = "#3b82f6",
  height = 200,
}: {
  data: UserGrowthData[];
  dataKey: keyof UserGrowthData;
  color?: string;
  height?: number;
}) {
  if (data.length === 0) {
    return (
      <div
        className="flex items-center justify-center text-muted-foreground"
        style={{ height }}
      >
        No data available
      </div>
    );
  }

  const values = data.map((d) => Number(d[dataKey]) || 0);
  const maxValue = Math.max(...values, 1);
  const minValue = Math.min(...values, 0);
  const range = maxValue - minValue || 1;

  const points = data
    .map((d, i) => {
      const x = (i / (data.length - 1 || 1)) * 100;
      const y = 100 - ((Number(d[dataKey]) - minValue) / range) * 80 - 10;
      return `${x},${y}`;
    })
    .join(" ");

  return (
    <div className="relative" style={{ height }}>
      <svg className="w-full h-full" viewBox="0 0 100 100" preserveAspectRatio="none">
        {/* Grid lines */}
        <line x1="0" y1="10" x2="100" y2="10" stroke="#e5e7eb" strokeWidth="0.2" />
        <line x1="0" y1="50" x2="100" y2="50" stroke="#e5e7eb" strokeWidth="0.2" />
        <line x1="0" y1="90" x2="100" y2="90" stroke="#e5e7eb" strokeWidth="0.2" />

        {/* Line */}
        <polyline
          fill="none"
          stroke={color}
          strokeWidth="0.5"
          points={points}
        />

        {/* Area fill */}
        <polygon
          fill={color}
          fillOpacity="0.1"
          points={`0,90 ${points} 100,90`}
        />
      </svg>

      {/* Y-axis labels */}
      <div className="absolute left-0 top-0 h-full flex flex-col justify-between text-xs text-muted-foreground py-2">
        <span>{maxValue.toLocaleString()}</span>
        <span>{Math.round(minValue + range / 2).toLocaleString()}</span>
        <span>{minValue.toLocaleString()}</span>
      </div>

      {/* X-axis labels */}
      <div className="absolute bottom-0 left-10 right-0 flex justify-between text-xs text-muted-foreground">
        {data.length > 0 && (
          <>
            <span>{new Date(data[0].date).toLocaleDateString("en-US", { month: "short", day: "numeric" })}</span>
            <span>{new Date(data[data.length - 1].date).toLocaleDateString("en-US", { month: "short", day: "numeric" })}</span>
          </>
        )}
      </div>
    </div>
  );
}

// Bar chart for segments
function SegmentBars({ data }: { data: UserSegmentData[] }) {
  const colors: Record<string, string> = {
    new: "#22c55e",
    returning: "#3b82f6",
    inactive: "#ef4444",
  };

  const total = data.reduce((sum, d) => sum + d.count, 0);

  return (
    <div className="space-y-4">
      {data.map((segment) => (
        <div key={segment.segment} className="space-y-2">
          <div className="flex items-center justify-between text-sm">
            <div className="flex items-center gap-2">
              <div
                className="w-3 h-3 rounded-full"
                style={{ backgroundColor: colors[segment.segment] || "#6b7280" }}
              />
              <span className="capitalize">{segment.segment}</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="font-medium">{segment.count.toLocaleString()}</span>
              <span className="text-muted-foreground">({segment.percentage.toFixed(1)}%)</span>
            </div>
          </div>
          <div className="h-2 bg-muted rounded-full overflow-hidden">
            <div
              className="h-full rounded-full transition-all"
              style={{
                width: `${(segment.count / (total || 1)) * 100}%`,
                backgroundColor: colors[segment.segment] || "#6b7280",
              }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

// Retention cohort table
function RetentionTable({ data }: { data: UserRetentionData[] }) {
  if (data.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        No retention data available
      </div>
    );
  }

  const getCellColor = (percentage: number) => {
    if (percentage >= 70) return "bg-green-500/20 text-green-700";
    if (percentage >= 50) return "bg-yellow-500/20 text-yellow-700";
    if (percentage >= 30) return "bg-orange-500/20 text-orange-700";
    return "bg-red-500/20 text-red-700";
  };

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b">
            <th className="text-left py-2 px-3 font-medium">Cohort</th>
            <th className="text-center py-2 px-3 font-medium">Users</th>
            <th className="text-center py-2 px-3 font-medium">Day 1</th>
            <th className="text-center py-2 px-3 font-medium">Day 7</th>
            <th className="text-center py-2 px-3 font-medium">Day 14</th>
            <th className="text-center py-2 px-3 font-medium">Day 30</th>
          </tr>
        </thead>
        <tbody>
          {data.map((row) => (
            <tr key={row.cohort_date} className="border-b">
              <td className="py-2 px-3">
                {new Date(row.cohort_date).toLocaleDateString("en-US", {
                  month: "short",
                  day: "numeric",
                })}
              </td>
              <td className="text-center py-2 px-3">{row.users}</td>
              <td className="py-2 px-3">
                <div className={`text-center rounded px-2 py-1 ${getCellColor((row.day_1 / row.users) * 100)}`}>
                  {((row.day_1 / row.users) * 100).toFixed(0)}%
                </div>
              </td>
              <td className="py-2 px-3">
                <div className={`text-center rounded px-2 py-1 ${getCellColor((row.day_7 / row.users) * 100)}`}>
                  {((row.day_7 / row.users) * 100).toFixed(0)}%
                </div>
              </td>
              <td className="py-2 px-3">
                <div className={`text-center rounded px-2 py-1 ${getCellColor((row.day_14 / row.users) * 100)}`}>
                  {((row.day_14 / row.users) * 100).toFixed(0)}%
                </div>
              </td>
              <td className="py-2 px-3">
                <div className={`text-center rounded px-2 py-1 ${getCellColor((row.day_30 / row.users) * 100)}`}>
                  {((row.day_30 / row.users) * 100).toFixed(0)}%
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function UserAnalyticsPage() {
  const [period, setPeriod] = useState<TimePeriod>("30d");
  const [analytics, setAnalytics] = useState<UserAnalytics | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const { execute: fetchAnalytics, loading } = useApi<UserAnalytics>();

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [period]);

  const loadData = async () => {
    const result = await fetchAnalytics(() =>
      userAnalyticsApi.getAll({ period })
    );
    if (result) {
      setAnalytics(result);
    }
  };

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await loadData();
    setIsRefreshing(false);
  };

  // Placeholder data
  const displayData: UserAnalytics = analytics || {
    growth: [],
    retention: [],
    segments: [
      { segment: "new", count: 0, percentage: 0 },
      { segment: "returning", count: 0, percentage: 0 },
      { segment: "inactive", count: 0, percentage: 0 },
    ],
    total_users: 0,
    active_users: 0,
    churn_rate: 0,
  };

  // Calculate trends
  const calculateTrend = (data: UserGrowthData[], key: keyof UserGrowthData) => {
    if (data.length < 2) return 0;
    const recent = Number(data[data.length - 1][key]) || 0;
    const previous = Number(data[data.length - 2][key]) || 1;
    return ((recent - previous) / previous) * 100;
  };

  const userGrowthTrend = calculateTrend(displayData.growth, "total_users");
  const activeUsersTrend = calculateTrend(displayData.growth, "active_users");

  return (
    <div className="space-y-6" data-testid="user-analytics-container">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href="/">
            <Button variant="ghost" size="icon" data-testid="user-analytics-back-button">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <Users className="h-8 w-8 text-primary" />
          <div>
            <h1 className="text-3xl font-bold tracking-tight">User Analytics</h1>
            <p className="text-muted-foreground">
              Track user growth, retention, and engagement
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex border rounded-md" data-testid="user-analytics-period-selector">
            {(["7d", "30d", "90d"] as TimePeriod[]).map((p) => (
              <Button
                key={p}
                variant={period === p ? "default" : "ghost"}
                size="sm"
                onClick={() => setPeriod(p)}
                className="rounded-none first:rounded-l-md last:rounded-r-md"
                data-testid={`user-analytics-period-${p}`}
              >
                {p === "7d" ? "7 Days" : p === "30d" ? "30 Days" : "90 Days"}
              </Button>
            ))}
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={handleRefresh}
            disabled={isRefreshing || loading}
            data-testid="user-analytics-refresh-button"
          >
            <RefreshCw
              className={`h-4 w-4 mr-2 ${isRefreshing || loading ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4" data-testid="user-analytics-summary-cards">
        <Card data-testid="user-analytics-total-users-card">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Users</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {displayData.total_users.toLocaleString()}
            </div>
            {userGrowthTrend !== 0 && (
              <div className="flex items-center text-xs mt-1">
                {userGrowthTrend > 0 ? (
                  <TrendingUp className="h-3 w-3 text-green-500 mr-1" />
                ) : (
                  <TrendingDown className="h-3 w-3 text-red-500 mr-1" />
                )}
                <span className={userGrowthTrend > 0 ? "text-green-500" : "text-red-500"}>
                  {userGrowthTrend > 0 ? "+" : ""}
                  {userGrowthTrend.toFixed(1)}%
                </span>
                <span className="text-muted-foreground ml-1">vs previous period</span>
              </div>
            )}
          </CardContent>
        </Card>

        <Card data-testid="user-analytics-active-users-card">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Active Users</CardTitle>
            <UserCheck className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {displayData.active_users.toLocaleString()}
            </div>
            {activeUsersTrend !== 0 && (
              <div className="flex items-center text-xs mt-1">
                {activeUsersTrend > 0 ? (
                  <TrendingUp className="h-3 w-3 text-green-500 mr-1" />
                ) : (
                  <TrendingDown className="h-3 w-3 text-red-500 mr-1" />
                )}
                <span className={activeUsersTrend > 0 ? "text-green-500" : "text-red-500"}>
                  {activeUsersTrend > 0 ? "+" : ""}
                  {activeUsersTrend.toFixed(1)}%
                </span>
                <span className="text-muted-foreground ml-1">vs previous period</span>
              </div>
            )}
          </CardContent>
        </Card>

        <Card data-testid="user-analytics-new-users-card">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">New Users</CardTitle>
            <UserPlus className="h-4 w-4 text-blue-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {displayData.growth.length > 0
                ? displayData.growth.reduce((sum, d) => sum + d.new_users, 0).toLocaleString()
                : "0"}
            </div>
            <p className="text-xs text-muted-foreground">in selected period</p>
          </CardContent>
        </Card>

        <Card data-testid="user-analytics-churn-rate-card">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Churn Rate</CardTitle>
            <UserMinus className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {displayData.churn_rate.toFixed(1)}%
            </div>
            <p className="text-xs text-muted-foreground">
              {displayData.churn_rate < 5
                ? "Excellent retention"
                : displayData.churn_rate < 10
                ? "Good retention"
                : "Needs improvement"}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Charts Row */}
      <div className="grid gap-6 md:grid-cols-2" data-testid="user-analytics-charts-section">
        {/* User Growth Chart */}
        <Card data-testid="user-analytics-growth-chart-card">
          <CardHeader>
            <CardTitle className="text-lg">User Growth</CardTitle>
            <CardDescription>Total users over time</CardDescription>
          </CardHeader>
          <CardContent>
            <SimpleLineChart
              data={displayData.growth}
              dataKey="total_users"
              color="#3b82f6"
              height={250}
            />
          </CardContent>
        </Card>

        {/* Active Users Chart */}
        <Card data-testid="user-analytics-active-chart-card">
          <CardHeader>
            <CardTitle className="text-lg">Active Users</CardTitle>
            <CardDescription>Daily active users over time</CardDescription>
          </CardHeader>
          <CardContent>
            <SimpleLineChart
              data={displayData.growth}
              dataKey="active_users"
              color="#22c55e"
              height={250}
            />
          </CardContent>
        </Card>
      </div>

      {/* Segments and Retention */}
      <div className="grid gap-6 md:grid-cols-2" data-testid="user-analytics-segments-section">
        {/* User Segments */}
        <Card data-testid="user-analytics-segments-card">
          <CardHeader>
            <CardTitle className="text-lg">User Segments</CardTitle>
            <CardDescription>New vs returning vs inactive users</CardDescription>
          </CardHeader>
          <CardContent>
            <SegmentBars data={displayData.segments} />
          </CardContent>
        </Card>

        {/* Quick Stats */}
        <Card data-testid="user-analytics-quick-stats-card">
          <CardHeader>
            <CardTitle className="text-lg">Quick Stats</CardTitle>
            <CardDescription>Key metrics at a glance</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between py-2 border-b">
              <span className="text-muted-foreground">Activation Rate</span>
              <Badge variant="secondary">
                {displayData.total_users > 0
                  ? ((displayData.active_users / displayData.total_users) * 100).toFixed(1)
                  : 0}
                %
              </Badge>
            </div>
            <div className="flex items-center justify-between py-2 border-b">
              <span className="text-muted-foreground">Avg Daily Growth</span>
              <Badge variant="secondary">
                {displayData.growth.length > 0
                  ? (displayData.growth.reduce((sum, d) => sum + d.new_users, 0) / displayData.growth.length).toFixed(1)
                  : 0}
              </Badge>
            </div>
            <div className="flex items-center justify-between py-2 border-b">
              <span className="text-muted-foreground">Peak Active Users</span>
              <Badge variant="secondary">
                {displayData.growth.length > 0
                  ? Math.max(...displayData.growth.map((d) => d.active_users)).toLocaleString()
                  : 0}
              </Badge>
            </div>
            <div className="flex items-center justify-between py-2">
              <span className="text-muted-foreground">Data Period</span>
              <Badge variant="outline">
                <Calendar className="h-3 w-3 mr-1" />
                {period === "7d" ? "7 Days" : period === "30d" ? "30 Days" : "90 Days"}
              </Badge>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Retention Cohorts */}
      <Card data-testid="user-analytics-retention-card">
        <CardHeader>
          <CardTitle className="text-lg">Retention Cohorts</CardTitle>
          <CardDescription>
            User retention by signup cohort - percentage of users still active
          </CardDescription>
        </CardHeader>
        <CardContent>
          <RetentionTable data={displayData.retention} />
        </CardContent>
      </Card>
    </div>
  );
}
