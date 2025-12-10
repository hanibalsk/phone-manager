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
  Smartphone,
  Wifi,
  WifiOff,
  MapPin,
  Calendar,
  RefreshCw,
  ArrowLeft,
  Monitor,
  Tablet,
} from "lucide-react";
import Link from "next/link";
import { useApi } from "@/hooks/use-api";
import { deviceAnalyticsApi } from "@/lib/api-client";
import type {
  DeviceAnalytics,
  DeviceDistribution,
  DeviceConnectivityData,
  LocationVolumeData,
  DeviceActivityHeatmap,
} from "@/types";

type TimePeriod = "7d" | "30d" | "90d";

// Pie chart component
function PieChart({
  data,
  height = 200,
}: {
  data: DeviceDistribution[];
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

  const colors = ["#3b82f6", "#22c55e", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899"];
  const total = data.reduce((sum, d) => sum + d.count, 0);
  let currentAngle = 0;

  const slices = data.map((item, i) => {
    const angle = (item.count / total) * 360;
    const startAngle = currentAngle;
    currentAngle += angle;

    const x1 = 50 + 40 * Math.cos(((startAngle - 90) * Math.PI) / 180);
    const y1 = 50 + 40 * Math.sin(((startAngle - 90) * Math.PI) / 180);
    const x2 = 50 + 40 * Math.cos(((startAngle + angle - 90) * Math.PI) / 180);
    const y2 = 50 + 40 * Math.sin(((startAngle + angle - 90) * Math.PI) / 180);
    const largeArc = angle > 180 ? 1 : 0;

    return {
      path: `M 50 50 L ${x1} ${y1} A 40 40 0 ${largeArc} 1 ${x2} ${y2} Z`,
      color: colors[i % colors.length],
      item,
    };
  });

  return (
    <div className="flex items-center gap-6" style={{ minHeight: height }}>
      <svg viewBox="0 0 100 100" className="w-40 h-40">
        {slices.map((slice, i) => (
          <path
            key={i}
            d={slice.path}
            fill={slice.color}
            stroke="white"
            strokeWidth="0.5"
          />
        ))}
      </svg>
      <div className="space-y-2">
        {data.map((item, i) => (
          <div key={item.platform} className="flex items-center gap-2 text-sm">
            <div
              className="w-3 h-3 rounded-full"
              style={{ backgroundColor: colors[i % colors.length] }}
            />
            <span className="font-medium">{item.platform}</span>
            <span className="text-muted-foreground">
              {item.count} ({item.percentage.toFixed(1)}%)
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

// Line chart component
function LineChart<T extends { date: string }>({
  data,
  keys,
  colors,
  height = 200,
}: {
  data: T[];
  keys: (keyof T)[];
  colors: string[];
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

  const allValues = data.flatMap((d) =>
    keys.map((k) => Number(d[k]) || 0)
  );
  const maxValue = Math.max(...allValues, 1);
  const minValue = Math.min(...allValues, 0);
  const range = maxValue - minValue || 1;

  return (
    <div className="relative" style={{ height }}>
      <svg className="w-full h-full" viewBox="0 0 100 100" preserveAspectRatio="none">
        {/* Grid lines */}
        <line x1="0" y1="10" x2="100" y2="10" stroke="#e5e7eb" strokeWidth="0.2" />
        <line x1="0" y1="50" x2="100" y2="50" stroke="#e5e7eb" strokeWidth="0.2" />
        <line x1="0" y1="90" x2="100" y2="90" stroke="#e5e7eb" strokeWidth="0.2" />

        {keys.map((key, keyIndex) => {
          const points = data
            .map((d, i) => {
              const x = (i / (data.length - 1 || 1)) * 100;
              const y =
                100 -
                ((Number(d[key]) - minValue) / range) * 80 -
                10;
              return `${x},${y}`;
            })
            .join(" ");

          return (
            <polyline
              key={String(key)}
              fill="none"
              stroke={colors[keyIndex]}
              strokeWidth="0.5"
              points={points}
            />
          );
        })}
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
            <span>
              {new Date(data[0].date).toLocaleDateString("en-US", {
                month: "short",
                day: "numeric",
              })}
            </span>
            <span>
              {new Date(data[data.length - 1].date).toLocaleDateString("en-US", {
                month: "short",
                day: "numeric",
              })}
            </span>
          </>
        )}
      </div>

      {/* Legend */}
      <div className="absolute top-0 right-0 flex gap-4 text-xs">
        {keys.map((key, i) => (
          <div key={String(key)} className="flex items-center gap-1">
            <div
              className="w-2 h-2 rounded-full"
              style={{ backgroundColor: colors[i] }}
            />
            <span className="capitalize">{String(key).replace("_", " ")}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

// Activity heatmap component
function ActivityHeatmap({ data }: { data: DeviceActivityHeatmap[] }) {
  if (data.length === 0) {
    return (
      <div className="flex items-center justify-center text-muted-foreground h-48">
        No data available
      </div>
    );
  }

  const days = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
  const hours = Array.from({ length: 24 }, (_, i) => i);
  const maxActivity = Math.max(...data.map((d) => d.activity), 1);

  const getActivity = (day: number, hour: number) => {
    const item = data.find((d) => d.day === day && d.hour === hour);
    return item?.activity || 0;
  };

  const getColor = (activity: number) => {
    const intensity = activity / maxActivity;
    if (intensity === 0) return "bg-muted";
    if (intensity < 0.25) return "bg-green-200";
    if (intensity < 0.5) return "bg-green-400";
    if (intensity < 0.75) return "bg-green-600";
    return "bg-green-800";
  };

  return (
    <div className="overflow-x-auto">
      <div className="min-w-[600px]">
        {/* Hour headers */}
        <div className="flex gap-1 mb-1 ml-12">
          {hours.map((h) => (
            <div
              key={h}
              className="w-5 text-center text-xs text-muted-foreground"
            >
              {h % 3 === 0 ? h : ""}
            </div>
          ))}
        </div>

        {/* Heatmap grid */}
        {days.map((day, dayIndex) => (
          <div key={day} className="flex gap-1 mb-1">
            <div className="w-10 text-xs text-muted-foreground text-right pr-2 py-1">
              {day}
            </div>
            {hours.map((hour) => (
              <div
                key={hour}
                className={`w-5 h-5 rounded-sm ${getColor(
                  getActivity(dayIndex, hour)
                )}`}
                title={`${day} ${hour}:00 - ${getActivity(dayIndex, hour)} activities`}
              />
            ))}
          </div>
        ))}

        {/* Legend */}
        <div className="flex items-center gap-2 mt-4 ml-12 text-xs text-muted-foreground">
          <span>Less</span>
          <div className="w-4 h-4 rounded-sm bg-muted" />
          <div className="w-4 h-4 rounded-sm bg-green-200" />
          <div className="w-4 h-4 rounded-sm bg-green-400" />
          <div className="w-4 h-4 rounded-sm bg-green-600" />
          <div className="w-4 h-4 rounded-sm bg-green-800" />
          <span>More</span>
        </div>
      </div>
    </div>
  );
}

export default function DeviceAnalyticsPage() {
  const [period, setPeriod] = useState<TimePeriod>("30d");
  const [analytics, setAnalytics] = useState<DeviceAnalytics | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const { execute: fetchAnalytics, loading } = useApi<DeviceAnalytics>();

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [period]);

  const loadData = async () => {
    const result = await fetchAnalytics(() =>
      deviceAnalyticsApi.getAll({ period })
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
  const displayData: DeviceAnalytics = analytics || {
    platform_distribution: [],
    status_distribution: [],
    connectivity: [],
    location_volume: [],
    heatmap: [],
    total_devices: 0,
    online_devices: 0,
  };

  const onlinePercentage =
    displayData.total_devices > 0
      ? ((displayData.online_devices / displayData.total_devices) * 100).toFixed(1)
      : "0";

  const offlineDevices = displayData.total_devices - displayData.online_devices;

  // Calculate location upload totals
  const totalUploads = displayData.location_volume.reduce(
    (sum, d) => sum + d.uploads,
    0
  );
  const totalDataPoints = displayData.location_volume.reduce(
    (sum, d) => sum + d.data_points,
    0
  );

  return (
    <div className="space-y-6" data-testid="device-analytics-container">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href="/">
            <Button variant="ghost" size="icon" data-testid="device-analytics-back-button">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <Smartphone className="h-8 w-8 text-primary" />
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Device Analytics</h1>
            <p className="text-muted-foreground">
              Monitor fleet health and device activity
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex border rounded-md" data-testid="device-analytics-period-selector">
            {(["7d", "30d", "90d"] as TimePeriod[]).map((p) => (
              <Button
                key={p}
                variant={period === p ? "default" : "ghost"}
                size="sm"
                onClick={() => setPeriod(p)}
                className="rounded-none first:rounded-l-md last:rounded-r-md"
                data-testid={`device-analytics-period-${p}`}
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
            data-testid="device-analytics-refresh-button"
          >
            <RefreshCw
              className={`h-4 w-4 mr-2 ${isRefreshing || loading ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4" data-testid="device-analytics-summary-cards">
        <Card data-testid="device-analytics-total-devices-card">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Devices</CardTitle>
            <Smartphone className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {displayData.total_devices.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground">enrolled in fleet</p>
          </CardContent>
        </Card>

        <Card data-testid="device-analytics-online-card">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Online</CardTitle>
            <Wifi className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              {displayData.online_devices.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground">{onlinePercentage}% of fleet</p>
          </CardContent>
        </Card>

        <Card data-testid="device-analytics-offline-card">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Offline</CardTitle>
            <WifiOff className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">
              {offlineDevices.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground">
              {displayData.total_devices > 0
                ? ((offlineDevices / displayData.total_devices) * 100).toFixed(1)
                : "0"}
              % of fleet
            </p>
          </CardContent>
        </Card>

        <Card data-testid="device-analytics-uploads-card">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Location Uploads</CardTitle>
            <MapPin className="h-4 w-4 text-blue-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalUploads.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground">
              {totalDataPoints.toLocaleString()} data points
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Distribution Charts */}
      <div className="grid gap-6 md:grid-cols-2" data-testid="device-analytics-distribution-section">
        {/* Platform Distribution */}
        <Card data-testid="device-analytics-platform-card">
          <CardHeader>
            <CardTitle className="text-lg">Platform Distribution</CardTitle>
            <CardDescription>Devices by operating system</CardDescription>
          </CardHeader>
          <CardContent>
            <PieChart data={displayData.platform_distribution} height={200} />
          </CardContent>
        </Card>

        {/* Status Distribution */}
        <Card data-testid="device-analytics-status-card">
          <CardHeader>
            <CardTitle className="text-lg">Status Distribution</CardTitle>
            <CardDescription>Devices by current status</CardDescription>
          </CardHeader>
          <CardContent>
            {displayData.status_distribution.length > 0 ? (
              <div className="space-y-4">
                {displayData.status_distribution.map((status) => (
                  <div key={status.status} className="space-y-2">
                    <div className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2">
                        {status.status === "online" && (
                          <Wifi className="h-4 w-4 text-green-500" />
                        )}
                        {status.status === "offline" && (
                          <WifiOff className="h-4 w-4 text-red-500" />
                        )}
                        {status.status === "inactive" && (
                          <Smartphone className="h-4 w-4 text-gray-500" />
                        )}
                        <span className="capitalize">{status.status}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{status.count.toLocaleString()}</span>
                        <span className="text-muted-foreground">
                          ({status.percentage.toFixed(1)}%)
                        </span>
                      </div>
                    </div>
                    <div className="h-2 bg-muted rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full ${
                          status.status === "online"
                            ? "bg-green-500"
                            : status.status === "offline"
                            ? "bg-red-500"
                            : "bg-gray-500"
                        }`}
                        style={{ width: `${status.percentage}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="flex items-center justify-center text-muted-foreground h-48">
                No data available
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Connectivity Over Time */}
      <Card data-testid="device-analytics-connectivity-card">
        <CardHeader>
          <CardTitle className="text-lg">Connectivity Over Time</CardTitle>
          <CardDescription>Online vs offline devices over time</CardDescription>
        </CardHeader>
        <CardContent>
          <LineChart
            data={displayData.connectivity}
            keys={["online", "offline"]}
            colors={["#22c55e", "#ef4444"]}
            height={250}
          />
        </CardContent>
      </Card>

      {/* Location Upload Volume */}
      <Card data-testid="device-analytics-volume-card">
        <CardHeader>
          <CardTitle className="text-lg">Location Upload Volume</CardTitle>
          <CardDescription>Upload count and data points over time</CardDescription>
        </CardHeader>
        <CardContent>
          <LineChart
            data={displayData.location_volume}
            keys={["uploads", "data_points"]}
            colors={["#3b82f6", "#8b5cf6"]}
            height={250}
          />
        </CardContent>
      </Card>

      {/* Activity Heatmap */}
      <Card data-testid="device-analytics-heatmap-card">
        <CardHeader>
          <CardTitle className="text-lg">Device Activity Heatmap</CardTitle>
          <CardDescription>Activity patterns by hour and day of week</CardDescription>
        </CardHeader>
        <CardContent>
          <ActivityHeatmap data={displayData.heatmap} />
        </CardContent>
      </Card>

      {/* Quick Stats */}
      <Card data-testid="device-analytics-quick-stats-card">
        <CardHeader>
          <CardTitle className="text-lg">Quick Stats</CardTitle>
          <CardDescription>Key metrics at a glance</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <div className="flex items-center justify-between py-2 px-4 border rounded-lg">
              <span className="text-muted-foreground">Avg Uploads/Day</span>
              <Badge variant="secondary">
                {displayData.location_volume.length > 0
                  ? (totalUploads / displayData.location_volume.length).toFixed(0)
                  : 0}
              </Badge>
            </div>
            <div className="flex items-center justify-between py-2 px-4 border rounded-lg">
              <span className="text-muted-foreground">Peak Online</span>
              <Badge variant="secondary">
                {displayData.connectivity.length > 0
                  ? Math.max(...displayData.connectivity.map((d) => d.online)).toLocaleString()
                  : 0}
              </Badge>
            </div>
            <div className="flex items-center justify-between py-2 px-4 border rounded-lg">
              <span className="text-muted-foreground">Platforms</span>
              <Badge variant="secondary">
                {displayData.platform_distribution.length}
              </Badge>
            </div>
            <div className="flex items-center justify-between py-2 px-4 border rounded-lg">
              <span className="text-muted-foreground">Data Period</span>
              <Badge variant="outline">
                <Calendar className="h-3 w-3 mr-1" />
                {period === "7d" ? "7 Days" : period === "30d" ? "30 Days" : "90 Days"}
              </Badge>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
