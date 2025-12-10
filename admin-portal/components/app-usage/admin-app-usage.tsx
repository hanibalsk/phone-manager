"use client";

import { useState, useEffect, useCallback } from "react";
import type { AppUsageByCategory, TopApp, Organization } from "@/types";
import { AppCategoryBadge } from "./app-category-badge";
import { UsageChart } from "./usage-chart";
import { DeviceUsageBreakdown } from "./device-usage-breakdown";
import { appUsageApi, organizationsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  BarChart3,
  RefreshCw,
  TrendingUp,
  Clock,
  Smartphone,
  Award,
} from "lucide-react";

export function AdminAppUsage() {
  const [organizationId, setOrganizationId] = useState("");
  const [dateRange, setDateRange] = useState<"today" | "7days" | "30days">("7days");
  const [customFrom, setCustomFrom] = useState("");
  const [customTo, setCustomTo] = useState("");
  const [activeTab, setActiveTab] = useState<"overview" | "device">("overview");

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: categoryData, loading: loadingCategories, execute: fetchCategories } = useApi<{ items: AppUsageByCategory[] }>();
  const { data: topAppsData, loading: loadingTopApps, execute: fetchTopApps } = useApi<{ items: TopApp[] }>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
  }, [fetchOrgs]);

  const getDateRange = useCallback(() => {
    const now = new Date();
    let from: string;
    const to = now.toISOString().split("T")[0];

    switch (dateRange) {
      case "today":
        from = to;
        break;
      case "7days":
        const week = new Date(now);
        week.setDate(week.getDate() - 7);
        from = week.toISOString().split("T")[0];
        break;
      case "30days":
        const month = new Date(now);
        month.setDate(month.getDate() - 30);
        from = month.toISOString().split("T")[0];
        break;
      default:
        from = customFrom;
    }

    return { from, to: customTo || to };
  }, [dateRange, customFrom, customTo]);

  const loadData = useCallback(() => {
    const { from, to } = getDateRange();
    const params = {
      organization_id: organizationId || undefined,
      from,
      to,
    };

    fetchCategories(() => appUsageApi.getByCategory(params));
    fetchTopApps(() => appUsageApi.getTopApps({ ...params, limit: 10 }));
  }, [fetchCategories, fetchTopApps, getDateRange, organizationId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const formatTime = (minutes: number) => {
    if (minutes >= 60) {
      const hours = Math.floor(minutes / 60);
      const mins = minutes % 60;
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${minutes}m`;
  };

  const categories = categoryData?.items || [];
  const topApps = topAppsData?.items || [];
  const totalMinutes = categories.reduce((sum, c) => sum + c.total_minutes, 0);
  const totalApps = categories.reduce((sum, c) => sum + c.app_count, 0);
  const { from, to } = getDateRange();

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <BarChart3 className="h-6 w-6" />
              <div>
                <CardTitle>App Usage Statistics</CardTitle>
                <CardDescription>
                  View aggregated app usage across devices
                </CardDescription>
              </div>
            </div>
            <Button variant="outline" size="sm" onClick={loadData}>
              <RefreshCw className="mr-2 h-4 w-4" />
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {/* Filters */}
          <div className="flex flex-wrap gap-4">
            <div className="flex-1 min-w-[200px]">
              <label className="text-sm font-medium">Organization</label>
              <select
                className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={organizationId}
                onChange={(e) => setOrganizationId(e.target.value)}
              >
                <option value="">All Organizations</option>
                {orgsData?.items?.map((org) => (
                  <option key={org.id} value={org.id}>
                    {org.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-sm font-medium">Time Period</label>
              <div className="mt-1 flex gap-2">
                <Button
                  variant={dateRange === "today" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setDateRange("today")}
                >
                  Today
                </Button>
                <Button
                  variant={dateRange === "7days" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setDateRange("7days")}
                >
                  7 Days
                </Button>
                <Button
                  variant={dateRange === "30days" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setDateRange("30days")}
                >
                  30 Days
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Tab Switcher */}
      <div className="flex gap-2">
        <Button
          variant={activeTab === "overview" ? "default" : "outline"}
          onClick={() => setActiveTab("overview")}
        >
          <TrendingUp className="mr-2 h-4 w-4" />
          Overview
        </Button>
        <Button
          variant={activeTab === "device" ? "default" : "outline"}
          onClick={() => setActiveTab("device")}
        >
          <Smartphone className="mr-2 h-4 w-4" />
          By Device
        </Button>
      </div>

      {activeTab === "overview" ? (
        <>
          {/* Summary Cards */}
          <div className="grid gap-4 md:grid-cols-3">
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-4">
                  <div className="rounded-full bg-blue-100 p-3">
                    <Clock className="h-6 w-6 text-blue-600" />
                  </div>
                  <div>
                    <div className="text-sm text-muted-foreground">Total Usage</div>
                    <div className="text-2xl font-bold">{formatTime(totalMinutes)}</div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-4">
                  <div className="rounded-full bg-green-100 p-3">
                    <BarChart3 className="h-6 w-6 text-green-600" />
                  </div>
                  <div>
                    <div className="text-sm text-muted-foreground">Apps Tracked</div>
                    <div className="text-2xl font-bold">{totalApps}</div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-4">
                  <div className="rounded-full bg-purple-100 p-3">
                    <Award className="h-6 w-6 text-purple-600" />
                  </div>
                  <div>
                    <div className="text-sm text-muted-foreground">Categories</div>
                    <div className="text-2xl font-bold">{categories.length}</div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Charts */}
          <div className="grid gap-6 lg:grid-cols-2">
            {/* Usage by Category Chart */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <TrendingUp className="h-5 w-5" />
                  Usage by Category
                </CardTitle>
                <CardDescription>
                  App usage distribution across categories
                </CardDescription>
              </CardHeader>
              <CardContent>
                {loadingCategories ? (
                  <div className="flex justify-center py-8">
                    <div className="text-muted-foreground">Loading...</div>
                  </div>
                ) : categories.length > 0 ? (
                  <UsageChart data={categories} />
                ) : (
                  <div className="flex justify-center py-8">
                    <div className="text-muted-foreground">No usage data available</div>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Top Apps */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Award className="h-5 w-5" />
                  Top Apps by Usage
                </CardTitle>
                <CardDescription>
                  Most used apps across all devices
                </CardDescription>
              </CardHeader>
              <CardContent>
                {loadingTopApps ? (
                  <div className="flex justify-center py-8">
                    <div className="text-muted-foreground">Loading...</div>
                  </div>
                ) : topApps.length > 0 ? (
                  <div className="space-y-3">
                    {topApps.map((app, index) => {
                      const maxUsage = topApps[0]?.total_minutes || 1;
                      const percentage = (app.total_minutes / maxUsage) * 100;

                      return (
                        <div key={app.package_name} className="space-y-1">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              <span className="text-sm font-medium text-muted-foreground w-5">
                                {index + 1}.
                              </span>
                              <span className="font-medium">{app.app_name}</span>
                              <AppCategoryBadge category={app.category} compact showIcon={false} />
                            </div>
                            <div className="text-sm text-muted-foreground">
                              {formatTime(app.total_minutes)}
                            </div>
                          </div>
                          <div className="ml-7 h-2 w-full rounded-full bg-secondary overflow-hidden">
                            <div
                              className="h-full rounded-full bg-primary transition-all duration-300"
                              style={{ width: `${percentage}%` }}
                            />
                          </div>
                          <div className="ml-7 text-xs text-muted-foreground">
                            {app.device_count} device{app.device_count !== 1 ? "s" : ""}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="flex justify-center py-8">
                    <div className="text-muted-foreground">No apps data available</div>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </>
      ) : (
        <DeviceUsageBreakdown
          organizationId={organizationId || undefined}
          from={from}
          to={to}
        />
      )}
    </div>
  );
}
