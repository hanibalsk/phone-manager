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
  Activity,
  Clock,
  AlertTriangle,
  Users,
  TrendingUp,
  TrendingDown,
  Calendar,
  RefreshCw,
  ArrowLeft,
  BarChart3,
  Zap,
} from "lucide-react";
import Link from "next/link";
import { useApi } from "@/hooks/use-api";
import { apiAnalyticsApi } from "@/lib/api-client";
import type {
  ApiAnalytics,
  EndpointMetrics,
  ResponseTimeData,
  ErrorRateData,
  ApiConsumer,
} from "@/types";
import { AlertCircle } from "lucide-react";

type TimePeriod = "7d" | "30d" | "90d";

// Line chart for response times with multiple lines
function ResponseTimeChart({
  data,
  height = 250,
}: {
  data: ResponseTimeData[];
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

  const keys: ("p50" | "p90" | "p95" | "p99")[] = ["p50", "p90", "p95", "p99"];
  const colors = ["#22c55e", "#f59e0b", "#ef4444", "#7c3aed"];
  const allValues = data.flatMap((d) => keys.map((k) => d[k]));
  const maxValue = Math.max(...allValues, 1);
  const minValue = 0;
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
              const y = 100 - ((d[key] - minValue) / range) * 80 - 10;
              return `${x},${y}`;
            })
            .join(" ");

          return (
            <polyline
              key={key}
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
        <span>{maxValue.toFixed(0)}ms</span>
        <span>{Math.round(range / 2).toFixed(0)}ms</span>
        <span>0ms</span>
      </div>

      {/* Legend */}
      <div className="absolute top-0 right-0 flex gap-3 text-xs">
        {keys.map((key, i) => (
          <div key={key} className="flex items-center gap-1">
            <div
              className="w-2 h-2 rounded-full"
              style={{ backgroundColor: colors[i] }}
            />
            <span>{key.toUpperCase()}</span>
          </div>
        ))}
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
    </div>
  );
}

// Error rate chart
function ErrorRateChart({
  data,
  height = 200,
}: {
  data: ErrorRateData[];
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

  const maxRate = Math.max(...data.map((d) => d.error_rate), 1);

  return (
    <div className="relative" style={{ height }}>
      <svg className="w-full h-full" viewBox="0 0 100 100" preserveAspectRatio="none">
        {/* Grid lines */}
        <line x1="0" y1="10" x2="100" y2="10" stroke="#e5e7eb" strokeWidth="0.2" />
        <line x1="0" y1="50" x2="100" y2="50" stroke="#e5e7eb" strokeWidth="0.2" />
        <line x1="0" y1="90" x2="100" y2="90" stroke="#e5e7eb" strokeWidth="0.2" />

        {/* Error rate line */}
        <polyline
          fill="none"
          stroke="#ef4444"
          strokeWidth="0.5"
          points={data
            .map((d, i) => {
              const x = (i / (data.length - 1 || 1)) * 100;
              const y = 100 - (d.error_rate / maxRate) * 80 - 10;
              return `${x},${y}`;
            })
            .join(" ")}
        />

        {/* Area fill */}
        <polygon
          fill="#ef4444"
          fillOpacity="0.1"
          points={`0,90 ${data
            .map((d, i) => {
              const x = (i / (data.length - 1 || 1)) * 100;
              const y = 100 - (d.error_rate / maxRate) * 80 - 10;
              return `${x},${y}`;
            })
            .join(" ")} 100,90`}
        />
      </svg>

      {/* Y-axis labels */}
      <div className="absolute left-0 top-0 h-full flex flex-col justify-between text-xs text-muted-foreground py-2">
        <span>{maxRate.toFixed(1)}%</span>
        <span>{(maxRate / 2).toFixed(1)}%</span>
        <span>0%</span>
      </div>
    </div>
  );
}

// Endpoint metrics table
function EndpointTable({ data }: { data: EndpointMetrics[] }) {
  if (data.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        No endpoint data available
      </div>
    );
  }

  const getMethodColor = (method: string) => {
    switch (method.toUpperCase()) {
      case "GET":
        return "bg-green-500/10 text-green-700 border-green-500/30";
      case "POST":
        return "bg-blue-500/10 text-blue-700 border-blue-500/30";
      case "PUT":
        return "bg-yellow-500/10 text-yellow-700 border-yellow-500/30";
      case "DELETE":
        return "bg-red-500/10 text-red-700 border-red-500/30";
      default:
        return "bg-gray-500/10 text-gray-700 border-gray-500/30";
    }
  };

  const getResponseTimeColor = (time: number) => {
    if (time < 100) return "text-green-600";
    if (time < 300) return "text-yellow-600";
    return "text-red-600";
  };

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b">
            <th className="text-left py-3 px-4 font-medium">Endpoint</th>
            <th className="text-center py-3 px-4 font-medium">Method</th>
            <th className="text-right py-3 px-4 font-medium">Requests</th>
            <th className="text-right py-3 px-4 font-medium">Avg Response</th>
            <th className="text-right py-3 px-4 font-medium">Errors</th>
            <th className="text-right py-3 px-4 font-medium">Error Rate</th>
          </tr>
        </thead>
        <tbody>
          {data.map((endpoint, i) => (
            <tr key={i} className="border-b hover:bg-muted/50">
              <td className="py-3 px-4 font-mono text-xs">{endpoint.endpoint}</td>
              <td className="py-3 px-4 text-center">
                <Badge variant="outline" className={getMethodColor(endpoint.method)}>
                  {endpoint.method}
                </Badge>
              </td>
              <td className="py-3 px-4 text-right">
                {endpoint.request_count.toLocaleString()}
              </td>
              <td
                className={`py-3 px-4 text-right font-medium ${getResponseTimeColor(
                  endpoint.avg_response_time_ms
                )}`}
              >
                {endpoint.avg_response_time_ms.toFixed(0)}ms
              </td>
              <td className="py-3 px-4 text-right text-red-600">
                {endpoint.error_count.toLocaleString()}
              </td>
              <td className="py-3 px-4 text-right">
                <Badge
                  variant={endpoint.error_rate > 5 ? "destructive" : "secondary"}
                >
                  {endpoint.error_rate.toFixed(2)}%
                </Badge>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// Top consumers list
function ConsumersList({ data }: { data: ApiConsumer[] }) {
  if (data.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        No consumer data available
      </div>
    );
  }

  const maxRequests = Math.max(...data.map((c) => c.request_count), 1);

  const getTypeIcon = (type: string) => {
    switch (type) {
      case "api_key":
        return <Zap className="h-4 w-4 text-purple-500" />;
      case "user":
        return <Users className="h-4 w-4 text-blue-500" />;
      case "organization":
        return <Activity className="h-4 w-4 text-green-500" />;
      default:
        return <Activity className="h-4 w-4 text-gray-500" />;
    }
  };

  return (
    <div className="space-y-3">
      {data.map((consumer, i) => (
        <div key={consumer.id} className="space-y-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-muted-foreground font-mono text-sm">
                #{i + 1}
              </span>
              {getTypeIcon(consumer.type)}
              <span className="font-medium">{consumer.name}</span>
              <Badge variant="outline" className="text-xs">
                {consumer.type.replace("_", " ")}
              </Badge>
            </div>
            <span className="font-medium">
              {consumer.request_count.toLocaleString()} requests
            </span>
          </div>
          <div className="h-2 bg-muted rounded-full overflow-hidden">
            <div
              className="h-full bg-primary rounded-full transition-all"
              style={{
                width: `${(consumer.request_count / maxRequests) * 100}%`,
              }}
            />
          </div>
          <div className="text-xs text-muted-foreground">
            Last request:{" "}
            {new Date(consumer.last_request).toLocaleDateString("en-US", {
              month: "short",
              day: "numeric",
              hour: "numeric",
              minute: "2-digit",
            })}
          </div>
        </div>
      ))}
    </div>
  );
}

export default function ApiAnalyticsPage() {
  const [period, setPeriod] = useState<TimePeriod>("30d");
  const [analytics, setAnalytics] = useState<ApiAnalytics | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const { execute: fetchAnalytics, loading, error } = useApi<ApiAnalytics>();

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [period]);

  const loadData = async () => {
    const result = await fetchAnalytics(() =>
      apiAnalyticsApi.getAll({ period })
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
  const displayData: ApiAnalytics = analytics || {
    endpoints: [],
    response_times: [],
    error_rates: [],
    top_consumers: [],
    total_requests: 0,
    avg_response_time_ms: 0,
    overall_error_rate: 0,
  };

  // Calculate trends
  const errorRateTrend =
    displayData.error_rates.length >= 2
      ? displayData.error_rates[displayData.error_rates.length - 1].error_rate -
        displayData.error_rates[displayData.error_rates.length - 2].error_rate
      : 0;

  const avgLatest =
    displayData.response_times.length > 0
      ? displayData.response_times[displayData.response_times.length - 1].p50
      : 0;

  return (
    <div className="space-y-6" data-testid="api-analytics-container">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href="/">
            <Button variant="ghost" size="icon" data-testid="api-analytics-back-button">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <BarChart3 className="h-8 w-8 text-primary" />
          <div>
            <h1 className="text-3xl font-bold tracking-tight">API Analytics</h1>
            <p className="text-muted-foreground">
              Monitor API performance and usage
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex border rounded-md" data-testid="api-analytics-period-selector">
            {(["7d", "30d", "90d"] as TimePeriod[]).map((p) => (
              <Button
                key={p}
                variant={period === p ? "default" : "ghost"}
                size="sm"
                onClick={() => setPeriod(p)}
                className="rounded-none first:rounded-l-md last:rounded-r-md"
                data-testid={`api-analytics-period-${p}`}
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
            data-testid="api-analytics-refresh-button"
          >
            <RefreshCw
              className={`h-4 w-4 mr-2 ${isRefreshing || loading ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>
        </div>
      </div>

      {error && !analytics && (
        <div className="p-4 rounded-lg border border-red-200 bg-red-50 text-red-800 flex items-start gap-2" data-testid="api-analytics-error">
          <AlertCircle className="h-4 w-4 mt-0.5" />
          <div className="flex-1">
            <p className="font-medium">Failed to load API analytics</p>
            <p className="text-sm">{error}</p>
          </div>
          <Button variant="outline" size="sm" onClick={handleRefresh} disabled={loading || isRefreshing}>
            Retry
          </Button>
        </div>
      )}

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4" data-testid="api-analytics-summary-cards">
        <Card data-testid="api-analytics-total-requests-card">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Requests</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {displayData.total_requests.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground">in selected period</p>
          </CardContent>
        </Card>

        <Card data-testid="api-analytics-response-time-card">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Avg Response Time</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div
              className={`text-2xl font-bold ${
                displayData.avg_response_time_ms < 100
                  ? "text-green-600"
                  : displayData.avg_response_time_ms < 300
                  ? "text-yellow-600"
                  : "text-red-600"
              }`}
            >
              {displayData.avg_response_time_ms.toFixed(0)}ms
            </div>
            <p className="text-xs text-muted-foreground">
              {displayData.avg_response_time_ms < 100
                ? "Excellent performance"
                : displayData.avg_response_time_ms < 300
                ? "Good performance"
                : "Needs optimization"}
            </p>
          </CardContent>
        </Card>

        <Card data-testid="api-analytics-error-rate-card">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Error Rate</CardTitle>
            <AlertTriangle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div
              className={`text-2xl font-bold ${
                displayData.overall_error_rate < 1
                  ? "text-green-600"
                  : displayData.overall_error_rate < 5
                  ? "text-yellow-600"
                  : "text-red-600"
              }`}
            >
              {displayData.overall_error_rate.toFixed(2)}%
            </div>
            {errorRateTrend !== 0 && (
              <div className="flex items-center text-xs mt-1">
                {errorRateTrend < 0 ? (
                  <TrendingDown className="h-3 w-3 text-green-500 mr-1" />
                ) : (
                  <TrendingUp className="h-3 w-3 text-red-500 mr-1" />
                )}
                <span className={errorRateTrend < 0 ? "text-green-500" : "text-red-500"}>
                  {errorRateTrend > 0 ? "+" : ""}
                  {errorRateTrend.toFixed(2)}%
                </span>
              </div>
            )}
          </CardContent>
        </Card>

        <Card data-testid="api-analytics-p50-latency-card">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">P50 Latency</CardTitle>
            <Zap className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{avgLatest.toFixed(0)}ms</div>
            <p className="text-xs text-muted-foreground">median response time</p>
          </CardContent>
        </Card>
      </div>

      {/* Response Time Percentiles */}
      <Card data-testid="api-analytics-response-time-chart-card">
        <CardHeader>
          <CardTitle className="text-lg">Response Time Percentiles</CardTitle>
          <CardDescription>P50, P90, P95, P99 latency over time</CardDescription>
        </CardHeader>
        <CardContent>
          <ResponseTimeChart data={displayData.response_times} height={280} />
        </CardContent>
      </Card>

      {/* Error Rate and Top Consumers */}
      <div className="grid gap-6 md:grid-cols-2" data-testid="api-analytics-error-consumers-section">
        {/* Error Rate Chart */}
        <Card data-testid="api-analytics-error-trend-card">
          <CardHeader>
            <CardTitle className="text-lg">Error Rate Trend</CardTitle>
            <CardDescription>Error percentage over time</CardDescription>
          </CardHeader>
          <CardContent>
            <ErrorRateChart data={displayData.error_rates} height={200} />
          </CardContent>
        </Card>

        {/* Top Consumers */}
        <Card data-testid="api-analytics-top-consumers-card">
          <CardHeader>
            <CardTitle className="text-lg">Top API Consumers</CardTitle>
            <CardDescription>Highest volume consumers</CardDescription>
          </CardHeader>
          <CardContent>
            <ConsumersList data={displayData.top_consumers.slice(0, 5)} />
          </CardContent>
        </Card>
      </div>

      {/* Endpoint Metrics Table */}
      <Card data-testid="api-analytics-endpoints-card">
        <CardHeader>
          <CardTitle className="text-lg">Endpoint Metrics</CardTitle>
          <CardDescription>Request volume and performance by endpoint</CardDescription>
        </CardHeader>
        <CardContent>
          <EndpointTable data={displayData.endpoints} />
        </CardContent>
      </Card>

      {/* Quick Stats */}
      <Card data-testid="api-analytics-quick-stats-card">
        <CardHeader>
          <CardTitle className="text-lg">Quick Stats</CardTitle>
          <CardDescription>Key metrics at a glance</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <div className="flex items-center justify-between py-2 px-4 border rounded-lg">
              <span className="text-muted-foreground">Endpoints</span>
              <Badge variant="secondary">{displayData.endpoints.length}</Badge>
            </div>
            <div className="flex items-center justify-between py-2 px-4 border rounded-lg">
              <span className="text-muted-foreground">Total Errors</span>
              <Badge variant="destructive">
                {displayData.endpoints
                  .reduce((sum, e) => sum + e.error_count, 0)
                  .toLocaleString()}
              </Badge>
            </div>
            <div className="flex items-center justify-between py-2 px-4 border rounded-lg">
              <span className="text-muted-foreground">Consumers</span>
              <Badge variant="secondary">{displayData.top_consumers.length}</Badge>
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
