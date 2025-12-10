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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  RefreshCw,
  ArrowLeft,
  Shield,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Clock,
  Database,
  Link as LinkIcon,
  HardDrive,
  Calendar,
  Play,
  Check,
} from "lucide-react";
import Link from "next/link";
import { useApi } from "@/hooks/use-api";
import { auditApi } from "@/lib/api-client";
import type { AuditIntegrityStatus } from "@/types";

const STATUS_CONFIG = {
  healthy: {
    label: "Healthy",
    color: "bg-green-100 text-green-800 border-green-200",
    icon: CheckCircle,
    bgColor: "bg-green-50",
    borderColor: "border-green-200",
  },
  warning: {
    label: "Warning",
    color: "bg-yellow-100 text-yellow-800 border-yellow-200",
    icon: AlertTriangle,
    bgColor: "bg-yellow-50",
    borderColor: "border-yellow-200",
  },
  error: {
    label: "Error",
    color: "bg-red-100 text-red-800 border-red-200",
    icon: XCircle,
    bgColor: "bg-red-50",
    borderColor: "border-red-200",
  },
};

const SEVERITY_COLORS = {
  low: "bg-blue-100 text-blue-800",
  medium: "bg-yellow-100 text-yellow-800",
  high: "bg-orange-100 text-orange-800",
  critical: "bg-red-100 text-red-800",
};

function formatBytes(bytes: number): string {
  if (bytes === 0) return "0 Bytes";
  const k = 1024;
  const sizes = ["Bytes", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
}

function StatCard({
  title,
  value,
  icon: Icon,
  description,
  status,
}: {
  title: string;
  value: string | number;
  icon: React.ComponentType<{ className?: string }>;
  description?: string;
  status?: "good" | "warning" | "error";
}) {
  const statusColors = {
    good: "text-green-600",
    warning: "text-yellow-600",
    error: "text-red-600",
  };

  return (
    <Card data-testid={`integrity-stat-card-${title.toLowerCase().replace(/\s+/g, "-")}`}>
      <CardContent className="p-4">
        <div className="flex items-start gap-3">
          <div
            className={`p-2 rounded-lg ${
              status ? `bg-${status === "good" ? "green" : status === "warning" ? "yellow" : "red"}-50` : "bg-muted"
            }`}
          >
            <Icon
              className={`h-5 w-5 ${status ? statusColors[status] : "text-muted-foreground"}`}
            />
          </div>
          <div className="flex-1">
            <p className="text-sm text-muted-foreground">{title}</p>
            <p className="text-2xl font-semibold">{value}</p>
            {description && (
              <p className="text-xs text-muted-foreground mt-1">{description}</p>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export default function IntegrityPage() {
  const [status, setStatus] = useState<AuditIntegrityStatus | null>(null);
  const [verifying, setVerifying] = useState(false);
  const [verificationResult, setVerificationResult] = useState<{
    success: boolean;
    issues: string[];
  } | null>(null);

  const { execute: fetchStatus, loading: statusLoading } =
    useApi<AuditIntegrityStatus>();
  const { execute: verifyIntegrity, loading: verifyLoading } = useApi<{
    success: boolean;
    issues: string[];
  }>();
  const { execute: resolveAlert, loading: resolvingAlert } = useApi<void>();

  const loadStatus = async () => {
    const result = await fetchStatus(() => auditApi.getIntegrityStatus());
    if (result) {
      setStatus(result);
    }
  };

  useEffect(() => {
    loadStatus();
  }, []);

  const handleVerify = async () => {
    setVerifying(true);
    const result = await verifyIntegrity(() => auditApi.verifyIntegrity());
    if (result) {
      setVerificationResult(result);
      loadStatus(); // Refresh status after verification
    }
    setVerifying(false);
  };

  const handleResolveAlert = async (alertId: string) => {
    await resolveAlert(() => auditApi.resolveAlert(alertId));
    loadStatus();
  };

  const statusConfig = status ? STATUS_CONFIG[status.status] : STATUS_CONFIG.healthy;
  const StatusIcon = statusConfig.icon;

  return (
    <div className="space-y-6" data-testid="integrity-page">
      {/* Header */}
      <div className="flex items-center justify-between" data-testid="integrity-header">
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
            <span>Integrity</span>
          </div>
          <h1 className="text-3xl font-bold">Audit Log Integrity</h1>
          <p className="text-muted-foreground mt-1">
            Monitor tamper-evident storage and verify log integrity
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link href="/audit">
            <Button variant="outline" data-testid="integrity-back-button">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Audit
            </Button>
          </Link>
          <Button onClick={loadStatus} disabled={statusLoading} data-testid="integrity-refresh-button">
            <RefreshCw
              className={`h-4 w-4 mr-2 ${statusLoading ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>
        </div>
      </div>

      {statusLoading && !status ? (
        <Card data-testid="integrity-loading">
          <CardContent className="py-12 text-center">
            <RefreshCw className="h-8 w-8 animate-spin mx-auto text-muted-foreground" />
          </CardContent>
        </Card>
      ) : status ? (
        <>
          {/* Status Banner */}
          <Card className={`${statusConfig.bgColor} ${statusConfig.borderColor}`} data-testid="integrity-status-banner">
            <CardContent className="p-6">
              <div className="flex items-center justify-between" data-testid="integrity-status-content">
                <div className="flex items-center gap-4">
                  <div
                    className={`p-3 rounded-full ${
                      status.status === "healthy"
                        ? "bg-green-100"
                        : status.status === "warning"
                          ? "bg-yellow-100"
                          : "bg-red-100"
                    }`}
                  >
                    <StatusIcon
                      className={`h-8 w-8 ${
                        status.status === "healthy"
                          ? "text-green-600"
                          : status.status === "warning"
                            ? "text-yellow-600"
                            : "text-red-600"
                      }`}
                    />
                  </div>
                  <div>
                    <h2 className="text-xl font-semibold">
                      System Status:{" "}
                      <span className={statusConfig.color.replace("bg-", "text-").split(" ")[0]}>
                        {statusConfig.label}
                      </span>
                    </h2>
                    <p className="text-muted-foreground mt-1">
                      Last verified: {new Date(status.last_verified).toLocaleString()}
                    </p>
                  </div>
                </div>
                <Button
                  onClick={handleVerify}
                  disabled={verifying || verifyLoading}
                  size="lg"
                  data-testid="integrity-verify-button"
                >
                  {(verifying || verifyLoading) && (
                    <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                  )}
                  <Play className="h-4 w-4 mr-2" />
                  Run Verification
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Verification Result */}
          {verificationResult && (
            <Card
              className={
                verificationResult.success
                  ? "bg-green-50 border-green-200"
                  : "bg-red-50 border-red-200"
              }
              data-testid="integrity-verification-result"
            >
              <CardContent className="p-4">
                <div className="flex items-start gap-3" data-testid={`integrity-verification-${verificationResult.success ? "success" : "failure"}`}>
                  {verificationResult.success ? (
                    <CheckCircle className="h-5 w-5 text-green-600 mt-0.5" />
                  ) : (
                    <XCircle className="h-5 w-5 text-red-600 mt-0.5" />
                  )}
                  <div>
                    <p className="font-medium">
                      {verificationResult.success
                        ? "Verification Passed"
                        : "Verification Found Issues"}
                    </p>
                    {verificationResult.issues.length > 0 && (
                      <ul className="text-sm mt-2 space-y-1" data-testid="integrity-verification-issues">
                        {verificationResult.issues.map((issue, idx) => (
                          <li key={idx} className="text-red-700" data-testid={`integrity-verification-issue-${idx}`}>
                            {issue}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4" data-testid="integrity-stats-grid">
            <StatCard
              title="Chain Length"
              value={status.chain_length.toLocaleString()}
              icon={LinkIcon}
              description="Total audit entries"
              status="good"
            />
            <StatCard
              title="Broken Links"
              value={status.broken_links}
              icon={AlertTriangle}
              status={status.broken_links === 0 ? "good" : "error"}
            />
            <StatCard
              title="Storage Used"
              value={formatBytes(status.storage_usage.size_bytes)}
              icon={HardDrive}
              description={`${status.storage_usage.total_entries.toLocaleString()} entries`}
            />
            <StatCard
              title="Retention Period"
              value={`${status.retention_policy.days} days`}
              icon={Calendar}
              description={status.retention_policy.auto_archive ? "Auto-archive enabled" : "Manual archive"}
            />
          </div>

          {/* Alerts */}
          <Card data-testid="integrity-alerts-card">
            <CardHeader className="pb-4">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-lg flex items-center gap-2">
                    <AlertTriangle className="h-5 w-5 text-yellow-600" />
                    Integrity Alerts
                  </CardTitle>
                  <CardDescription>
                    Issues detected in audit log integrity
                  </CardDescription>
                </div>
                <Badge variant="secondary">
                  {status.alerts.filter((a) => !a.resolved).length} active
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              {status.alerts.length === 0 ? (
                <div className="text-center py-8" data-testid="integrity-alerts-empty">
                  <CheckCircle className="h-12 w-12 mx-auto text-green-500 mb-3" />
                  <p className="text-muted-foreground">No integrity alerts</p>
                </div>
              ) : (
                <Table data-testid="integrity-alerts-table">
                  <TableHeader>
                    <TableRow>
                      <TableHead>Type</TableHead>
                      <TableHead>Description</TableHead>
                      <TableHead>Severity</TableHead>
                      <TableHead>Detected</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="text-right">Action</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {status.alerts.map((alert) => (
                      <TableRow key={alert.id} data-testid={`integrity-alert-row-${alert.id}`}>
                        <TableCell>
                          <Badge variant="outline" className="capitalize">
                            {alert.type}
                          </Badge>
                        </TableCell>
                        <TableCell className="max-w-[300px]">
                          <p className="truncate">{alert.description}</p>
                        </TableCell>
                        <TableCell>
                          <Badge className={SEVERITY_COLORS[alert.severity]}>
                            {alert.severity}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {new Date(alert.detected_at).toLocaleDateString()}
                        </TableCell>
                        <TableCell>
                          {alert.resolved ? (
                            <Badge className="bg-green-100 text-green-800">
                              Resolved
                            </Badge>
                          ) : (
                            <Badge className="bg-yellow-100 text-yellow-800">
                              Active
                            </Badge>
                          )}
                        </TableCell>
                        <TableCell className="text-right">
                          {!alert.resolved && (
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => handleResolveAlert(alert.id)}
                              disabled={resolvingAlert}
                              data-testid={`integrity-alert-resolve-${alert.id}`}
                            >
                              <Check className="h-4 w-4 mr-1" />
                              Resolve
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>

          {/* Retention Policy */}
          <Card data-testid="integrity-storage-card">
            <CardHeader className="pb-4">
              <CardTitle className="text-lg flex items-center gap-2">
                <Database className="h-5 w-5 text-muted-foreground" />
                Storage & Retention
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6" data-testid="integrity-storage-grid">
                <div className="space-y-2">
                  <p className="text-sm text-muted-foreground">Retention Policy</p>
                  <p className="text-lg font-semibold">
                    {status.retention_policy.days} days
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Logs older than {status.retention_policy.days} days are{" "}
                    {status.retention_policy.auto_archive ? "archived" : "deleted"}
                  </p>
                </div>
                <div className="space-y-2">
                  <p className="text-sm text-muted-foreground">Auto-Archive</p>
                  <p className="text-lg font-semibold">
                    {status.retention_policy.auto_archive ? "Enabled" : "Disabled"}
                  </p>
                  {status.retention_policy.archive_location && (
                    <p className="text-xs text-muted-foreground">
                      Location: {status.retention_policy.archive_location}
                    </p>
                  )}
                </div>
                <div className="space-y-2">
                  <p className="text-sm text-muted-foreground">Oldest Entry</p>
                  <p className="text-lg font-semibold">
                    {new Date(status.storage_usage.oldest_entry).toLocaleDateString()}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {Math.ceil(
                      (Date.now() - new Date(status.storage_usage.oldest_entry).getTime()) /
                        (1000 * 60 * 60 * 24)
                    )}{" "}
                    days ago
                  </p>
                </div>
              </div>

              {/* Storage Usage Bar */}
              <div className="mt-6" data-testid="integrity-storage-usage">
                <div className="flex items-center justify-between text-sm mb-2">
                  <span className="text-muted-foreground">Storage Usage</span>
                  <span className="font-medium">
                    {formatBytes(status.storage_usage.size_bytes)}
                  </span>
                </div>
                <div className="w-full h-3 bg-muted rounded-full overflow-hidden">
                  <div
                    className="h-full bg-primary rounded-full"
                    data-testid="integrity-storage-bar"
                    style={{
                      width: `${Math.min(
                        (status.storage_usage.size_bytes / (1024 * 1024 * 1024)) * 100,
                        100
                      )}%`,
                    }}
                  />
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  {status.storage_usage.total_entries.toLocaleString()} audit entries stored
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Hash Chain Info */}
          <Card data-testid="integrity-hashchain-card">
            <CardHeader className="pb-4">
              <CardTitle className="text-lg flex items-center gap-2">
                <Shield className="h-5 w-5 text-muted-foreground" />
                Hash Chain Integrity
              </CardTitle>
              <CardDescription>
                Cryptographic verification of audit log immutability
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="p-4 bg-muted rounded-lg" data-testid="integrity-hashchain-info">
                  <p className="text-sm text-muted-foreground mb-2">How it works</p>
                  <p className="text-sm">
                    Each audit log entry is cryptographically linked to the previous entry
                    using SHA-256 hashing. This creates an append-only chain where any
                    tampering would break the chain and be immediately detectable.
                  </p>
                </div>
                <div className="grid grid-cols-2 gap-4" data-testid="integrity-hashchain-grid">
                  <div className="p-4 border rounded-lg" data-testid="integrity-chain-status">
                    <p className="text-sm text-muted-foreground">Chain Status</p>
                    <div className="flex items-center gap-2 mt-2">
                      {status.broken_links === 0 ? (
                        <>
                          <CheckCircle className="h-5 w-5 text-green-500" />
                          <span className="font-medium text-green-700">
                            Chain Intact
                          </span>
                        </>
                      ) : (
                        <>
                          <XCircle className="h-5 w-5 text-red-500" />
                          <span className="font-medium text-red-700">
                            {status.broken_links} broken link(s)
                          </span>
                        </>
                      )}
                    </div>
                  </div>
                  <div className="p-4 border rounded-lg" data-testid="integrity-last-verification">
                    <p className="text-sm text-muted-foreground">Last Verification</p>
                    <p className="font-medium mt-2">
                      {new Date(status.last_verified).toLocaleString()}
                    </p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </>
      ) : (
        <Card data-testid="integrity-error">
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">Unable to load integrity status</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
