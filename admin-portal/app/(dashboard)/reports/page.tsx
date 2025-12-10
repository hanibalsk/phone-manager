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
  FileText,
  Plus,
  Download,
  Play,
  Trash2,
  Calendar,
  Filter,
  BarChart3,
  RefreshCw,
  ArrowLeft,
  Check,
  X,
  Clock,
  Save,
} from "lucide-react";
import Link from "next/link";
import { useApi } from "@/hooks/use-api";
import { reportsApi } from "@/lib/api-client";
import type {
  SavedReport,
  ReportConfig,
  ReportResult,
  ReportMetricType,
  ReportMetric,
  ReportFilter,
} from "@/types";

// Available metrics for report builder
const AVAILABLE_METRICS: { type: ReportMetricType; label: string; icon: string }[] = [
  { type: "users", label: "Users", icon: "users" },
  { type: "devices", label: "Devices", icon: "smartphone" },
  { type: "organizations", label: "Organizations", icon: "building" },
  { type: "locations", label: "Locations", icon: "map-pin" },
  { type: "api_calls", label: "API Calls", icon: "zap" },
  { type: "errors", label: "Errors", icon: "alert-triangle" },
  { type: "retention", label: "Retention", icon: "trending-up" },
];

// Aggregation types
const AGGREGATIONS: { value: "count" | "sum" | "average" | "min" | "max"; label: string }[] = [
  { value: "count", label: "Count" },
  { value: "sum", label: "Sum" },
  { value: "average", label: "Average" },
  { value: "min", label: "Minimum" },
  { value: "max", label: "Maximum" },
];

// Filter operators
const OPERATORS: { value: ReportFilter["operator"]; label: string }[] = [
  { value: "eq", label: "Equals" },
  { value: "ne", label: "Not equals" },
  { value: "gt", label: "Greater than" },
  { value: "lt", label: "Less than" },
  { value: "gte", label: "Greater or equal" },
  { value: "lte", label: "Less or equal" },
  { value: "contains", label: "Contains" },
];

// Quick date range presets
const DATE_PRESETS = [
  { label: "Last 7 days", days: 7 },
  { label: "Last 30 days", days: 30 },
  { label: "Last 90 days", days: 90 },
  { label: "Last year", days: 365 },
];

function SavedReportCard({
  report,
  onRun,
  onDelete,
  onExport,
  isRunning,
}: {
  report: SavedReport;
  onRun: () => void;
  onDelete: () => void;
  onExport: (format: "pdf" | "csv") => void;
  isRunning: boolean;
}) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <div>
            <CardTitle className="text-base">{report.name}</CardTitle>
            {report.description && (
              <CardDescription className="mt-1">{report.description}</CardDescription>
            )}
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={onDelete}
            className="text-red-500 hover:text-red-700 hover:bg-red-50"
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {/* Metrics */}
          <div className="flex flex-wrap gap-1">
            {report.config.metrics.map((m, i) => (
              <Badge key={i} variant="secondary" className="text-xs">
                {m.label}
              </Badge>
            ))}
          </div>

          {/* Dates */}
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Calendar className="h-3 w-3" />
            <span>
              {new Date(report.config.date_range.start).toLocaleDateString()} -{" "}
              {new Date(report.config.date_range.end).toLocaleDateString()}
            </span>
          </div>

          {/* Last run */}
          {report.last_run && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Clock className="h-3 w-3" />
              <span>
                Last run:{" "}
                {new Date(report.last_run).toLocaleDateString("en-US", {
                  month: "short",
                  day: "numeric",
                  hour: "numeric",
                  minute: "2-digit",
                })}
              </span>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-2 pt-2">
            <Button
              size="sm"
              onClick={onRun}
              disabled={isRunning}
              className="flex-1"
            >
              {isRunning ? (
                <RefreshCw className="h-4 w-4 mr-1 animate-spin" />
              ) : (
                <Play className="h-4 w-4 mr-1" />
              )}
              Run
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => onExport("pdf")}
            >
              <Download className="h-4 w-4 mr-1" />
              PDF
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => onExport("csv")}
            >
              <Download className="h-4 w-4 mr-1" />
              CSV
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function ReportBuilder({
  onSave,
  onCancel,
  isSaving,
}: {
  onSave: (config: Omit<SavedReport, "id" | "created_at" | "updated_at" | "last_run">) => void;
  onCancel: () => void;
  isSaving: boolean;
}) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [selectedMetrics, setSelectedMetrics] = useState<ReportMetric[]>([]);
  const [filters, setFilters] = useState<ReportFilter[]>([]);
  const [dateRange, setDateRange] = useState({
    start: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0],
    end: new Date().toISOString().split("T")[0],
  });
  const [groupBy, setGroupBy] = useState<string>("");

  const handleAddMetric = (type: ReportMetricType) => {
    const metric = AVAILABLE_METRICS.find((m) => m.type === type);
    if (metric && !selectedMetrics.find((m) => m.type === type)) {
      setSelectedMetrics([
        ...selectedMetrics,
        { type, aggregation: "count", label: metric.label },
      ]);
    }
  };

  const handleRemoveMetric = (type: ReportMetricType) => {
    setSelectedMetrics(selectedMetrics.filter((m) => m.type !== type));
  };

  const handleAddFilter = () => {
    setFilters([...filters, { field: "", operator: "eq", value: "" }]);
  };

  const handleUpdateFilter = (index: number, field: keyof ReportFilter, value: string) => {
    const updated = [...filters];
    updated[index] = { ...updated[index], [field]: value };
    setFilters(updated);
  };

  const handleRemoveFilter = (index: number) => {
    setFilters(filters.filter((_, i) => i !== index));
  };

  const handleSetDatePreset = (days: number) => {
    const end = new Date();
    const start = new Date(Date.now() - days * 24 * 60 * 60 * 1000);
    setDateRange({
      start: start.toISOString().split("T")[0],
      end: end.toISOString().split("T")[0],
    });
  };

  const handleSave = () => {
    if (!name || selectedMetrics.length === 0) return;

    onSave({
      name,
      description: description || undefined,
      config: {
        name,
        description: description || undefined,
        metrics: selectedMetrics,
        filters: filters.filter((f) => f.field && f.value),
        date_range: dateRange,
        group_by: groupBy || undefined,
      },
    });
  };

  const canSave = name.length > 0 && selectedMetrics.length > 0;

  return (
    <div className="space-y-6">
      {/* Basic Info */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Report Details</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">Report Name *</Label>
            <Input
              id="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g., Monthly User Growth"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Input
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Optional description"
            />
          </div>
        </CardContent>
      </Card>

      {/* Metrics Selection */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Select Metrics *</CardTitle>
          <CardDescription>Choose the metrics to include in your report</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Available metrics */}
          <div className="flex flex-wrap gap-2">
            {AVAILABLE_METRICS.map((metric) => {
              const isSelected = selectedMetrics.some((m) => m.type === metric.type);
              return (
                <Button
                  key={metric.type}
                  variant={isSelected ? "default" : "outline"}
                  size="sm"
                  onClick={() =>
                    isSelected
                      ? handleRemoveMetric(metric.type)
                      : handleAddMetric(metric.type)
                  }
                >
                  {isSelected && <Check className="h-3 w-3 mr-1" />}
                  {metric.label}
                </Button>
              );
            })}
          </div>

          {/* Selected metrics with aggregation */}
          {selectedMetrics.length > 0 && (
            <div className="space-y-2 pt-4 border-t">
              <Label>Selected Metrics</Label>
              {selectedMetrics.map((metric, i) => (
                <div key={metric.type} className="flex items-center gap-2">
                  <Badge variant="secondary">{metric.label}</Badge>
                  <select
                    className="text-sm border rounded px-2 py-1"
                    value={metric.aggregation}
                    onChange={(e) => {
                      const updated = [...selectedMetrics];
                      updated[i] = {
                        ...updated[i],
                        aggregation: e.target.value as ReportMetric["aggregation"],
                      };
                      setSelectedMetrics(updated);
                    }}
                  >
                    {AGGREGATIONS.map((agg) => (
                      <option key={agg.value} value={agg.value}>
                        {agg.label}
                      </option>
                    ))}
                  </select>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-6 w-6"
                    onClick={() => handleRemoveMetric(metric.type)}
                  >
                    <X className="h-3 w-3" />
                  </Button>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Date Range */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Date Range</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Quick presets */}
          <div className="flex flex-wrap gap-2">
            {DATE_PRESETS.map((preset) => (
              <Button
                key={preset.days}
                variant="outline"
                size="sm"
                onClick={() => handleSetDatePreset(preset.days)}
              >
                {preset.label}
              </Button>
            ))}
          </div>

          {/* Custom range */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Start Date</Label>
              <Input
                type="date"
                value={dateRange.start}
                onChange={(e) =>
                  setDateRange({ ...dateRange, start: e.target.value })
                }
              />
            </div>
            <div className="space-y-2">
              <Label>End Date</Label>
              <Input
                type="date"
                value={dateRange.end}
                onChange={(e) =>
                  setDateRange({ ...dateRange, end: e.target.value })
                }
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Filters */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-lg">Filters</CardTitle>
              <CardDescription>Add optional filters to narrow down data</CardDescription>
            </div>
            <Button variant="outline" size="sm" onClick={handleAddFilter}>
              <Plus className="h-4 w-4 mr-1" />
              Add Filter
            </Button>
          </div>
        </CardHeader>
        {filters.length > 0 && (
          <CardContent className="space-y-3">
            {filters.map((filter, i) => (
              <div key={i} className="flex items-center gap-2">
                <Input
                  placeholder="Field"
                  value={filter.field}
                  onChange={(e) => handleUpdateFilter(i, "field", e.target.value)}
                  className="flex-1"
                />
                <select
                  className="text-sm border rounded px-2 py-2 h-10"
                  value={filter.operator}
                  onChange={(e) =>
                    handleUpdateFilter(i, "operator", e.target.value)
                  }
                >
                  {OPERATORS.map((op) => (
                    <option key={op.value} value={op.value}>
                      {op.label}
                    </option>
                  ))}
                </select>
                <Input
                  placeholder="Value"
                  value={String(filter.value)}
                  onChange={(e) => handleUpdateFilter(i, "value", e.target.value)}
                  className="flex-1"
                />
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => handleRemoveFilter(i)}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
            ))}
          </CardContent>
        )}
      </Card>

      {/* Group By */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Group By</CardTitle>
          <CardDescription>Optional field to group results by</CardDescription>
        </CardHeader>
        <CardContent>
          <select
            className="w-full border rounded px-3 py-2"
            value={groupBy}
            onChange={(e) => setGroupBy(e.target.value)}
          >
            <option value="">No grouping</option>
            <option value="day">Day</option>
            <option value="week">Week</option>
            <option value="month">Month</option>
            <option value="organization">Organization</option>
            <option value="platform">Platform</option>
          </select>
        </CardContent>
      </Card>

      {/* Actions */}
      <div className="flex gap-4">
        <Button onClick={handleSave} disabled={!canSave || isSaving}>
          {isSaving ? (
            <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
          ) : (
            <Save className="h-4 w-4 mr-2" />
          )}
          Save Report
        </Button>
        <Button variant="outline" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </div>
  );
}

export default function ReportsPage() {
  const [savedReports, setSavedReports] = useState<SavedReport[]>([]);
  const [showBuilder, setShowBuilder] = useState(false);
  const [runningReportId, setRunningReportId] = useState<string | null>(null);
  const [notification, setNotification] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  const { execute: fetchSaved, loading } = useApi<SavedReport[]>();
  const { execute: createSaved, loading: creating } = useApi<SavedReport>();
  const { execute: deleteSaved } = useApi<void>();
  const { execute: runReport } = useApi<ReportResult>();

  useEffect(() => {
    loadReports();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadReports = async () => {
    const result = await fetchSaved(() => reportsApi.getSaved());
    if (result) {
      setSavedReports(result);
    }
  };

  const handleSaveReport = async (
    data: Omit<SavedReport, "id" | "created_at" | "updated_at" | "last_run">
  ) => {
    const result = await createSaved(() => reportsApi.createSaved(data));
    if (result) {
      setSavedReports([result, ...savedReports]);
      setShowBuilder(false);
      setNotification({ type: "success", message: "Report saved successfully" });
      setTimeout(() => setNotification(null), 3000);
    }
  };

  const handleDeleteReport = async (id: string) => {
    await deleteSaved(() => reportsApi.deleteSaved(id));
    setSavedReports(savedReports.filter((r) => r.id !== id));
    setNotification({ type: "success", message: "Report deleted" });
    setTimeout(() => setNotification(null), 3000);
  };

  const handleRunReport = async (id: string) => {
    setRunningReportId(id);
    const result = await runReport(() => reportsApi.runSaved(id));
    setRunningReportId(null);
    if (result) {
      // Update last_run in local state
      setSavedReports(
        savedReports.map((r) =>
          r.id === id ? { ...r, last_run: new Date().toISOString() } : r
        )
      );
      setNotification({
        type: "success",
        message: `Report generated: ${result.row_count} rows`,
      });
      setTimeout(() => setNotification(null), 3000);
    }
  };

  const handleExportReport = async (id: string, format: "pdf" | "csv") => {
    try {
      // In a real implementation, this would trigger a download
      setNotification({
        type: "success",
        message: `Exporting report as ${format.toUpperCase()}...`,
      });
      setTimeout(() => setNotification(null), 3000);
    } catch {
      setNotification({ type: "error", message: "Export failed" });
      setTimeout(() => setNotification(null), 3000);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href="/">
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <FileText className="h-8 w-8 text-primary" />
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Custom Reports</h1>
            <p className="text-muted-foreground">
              Build and save custom report configurations
            </p>
          </div>
        </div>
        {!showBuilder && (
          <Button onClick={() => setShowBuilder(true)}>
            <Plus className="h-4 w-4 mr-2" />
            New Report
          </Button>
        )}
      </div>

      {/* Notification */}
      {notification && (
        <div
          className={`p-4 rounded-md ${
            notification.type === "success"
              ? "bg-green-50 text-green-800 border border-green-200"
              : "bg-red-50 text-red-800 border border-red-200"
          }`}
        >
          {notification.message}
        </div>
      )}

      {/* Report Builder */}
      {showBuilder && (
        <ReportBuilder
          onSave={handleSaveReport}
          onCancel={() => setShowBuilder(false)}
          isSaving={creating}
        />
      )}

      {/* Saved Reports */}
      {!showBuilder && (
        <>
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : savedReports.length === 0 ? (
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-12">
                <FileText className="h-12 w-12 text-muted-foreground mb-4" />
                <h3 className="text-lg font-medium mb-2">No saved reports</h3>
                <p className="text-muted-foreground mb-4">
                  Create your first custom report to get started
                </p>
                <Button onClick={() => setShowBuilder(true)}>
                  <Plus className="h-4 w-4 mr-2" />
                  Create Report
                </Button>
              </CardContent>
            </Card>
          ) : (
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {savedReports.map((report) => (
                <SavedReportCard
                  key={report.id}
                  report={report}
                  onRun={() => handleRunReport(report.id)}
                  onDelete={() => handleDeleteReport(report.id)}
                  onExport={(format) => handleExportReport(report.id, format)}
                  isRunning={runningReportId === report.id}
                />
              ))}
            </div>
          )}
        </>
      )}

      {/* Quick Tips */}
      {!showBuilder && savedReports.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Tips</CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground">
              <li>Reports can be exported as PDF or CSV for sharing</li>
              <li>Use filters to narrow down data to specific organizations or time periods</li>
              <li>Group by day, week, or month to see trends over time</li>
              <li>Saved reports remember your configuration for easy re-running</li>
            </ul>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
