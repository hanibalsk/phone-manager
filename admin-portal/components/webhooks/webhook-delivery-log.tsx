"use client";

import { useState, useEffect, useCallback } from "react";
import type { WebhookDelivery, DeliveryStatus } from "@/types";
import { WebhookEventBadge } from "./webhook-event-badge";
import { webhooksApi } from "@/lib/api-client";
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
  RefreshCw,
  CheckCircle2,
  XCircle,
  Clock,
  Send,
  ChevronDown,
  ChevronUp,
  ArrowLeft,
} from "lucide-react";
import Link from "next/link";

const ITEMS_PER_PAGE = 20;

interface WebhookDeliveryLogProps {
  webhookId: string;
  webhookName: string;
}

interface DeliveryStatusBadgeProps {
  status: DeliveryStatus;
}

function DeliveryStatusBadge({ status }: DeliveryStatusBadgeProps) {
  const config = {
    success: {
      label: "Success",
      icon: CheckCircle2,
      className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
    },
    failed: {
      label: "Failed",
      icon: XCircle,
      className: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
    },
    pending: {
      label: "Pending",
      icon: Clock,
      className: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
    },
  };

  const statusConfig = config[status];
  const Icon = statusConfig.icon;

  return (
    <span
      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${statusConfig.className}`}
    >
      <Icon className="h-3 w-3" />
      {statusConfig.label}
    </span>
  );
}

export function WebhookDeliveryLog({ webhookId, webhookName }: WebhookDeliveryLogProps) {
  const [statusFilter, setStatusFilter] = useState<DeliveryStatus | "">("");
  const [page, setPage] = useState(1);
  const [expandedDelivery, setExpandedDelivery] = useState<string | null>(null);
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const { data: deliveriesData, loading, error, execute: fetchDeliveries } = useApi<{ items: WebhookDelivery[]; total: number }>();
  const { loading: resendLoading, execute: executeResend } = useApi<WebhookDelivery>();

  const loadDeliveries = useCallback(() => {
    fetchDeliveries(() =>
      webhooksApi.getDeliveries(webhookId, {
        status: statusFilter || undefined,
        page,
        limit: ITEMS_PER_PAGE,
      })
    );
  }, [fetchDeliveries, webhookId, statusFilter, page]);

  useEffect(() => {
    loadDeliveries();
  }, [loadDeliveries]);

  const showNotification = (type: "success" | "error", message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 3000);
  };

  const handleResend = async (delivery: WebhookDelivery) => {
    const result = await executeResend(() => webhooksApi.resendDelivery(webhookId, delivery.id));
    if (result) {
      showNotification("success", "Delivery resent successfully");
      loadDeliveries();
    } else {
      showNotification("error", "Failed to resend delivery");
    }
  };

  const toggleExpanded = (deliveryId: string) => {
    setExpandedDelivery((prev) => (prev === deliveryId ? null : deliveryId));
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString();
  };

  const formatDuration = (ms: number | null) => {
    if (ms === null) return "N/A";
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  };

  const formatJson = (jsonString: string | null) => {
    if (!jsonString) return null;
    try {
      return JSON.stringify(JSON.parse(jsonString), null, 2);
    } catch {
      return jsonString;
    }
  };

  const deliveries = deliveriesData?.items || [];
  const total = deliveriesData?.total || 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  return (
    <>
      <div className="mb-4">
        <Link href="/webhooks">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Webhooks
          </Button>
        </Link>
      </div>

      <Card data-testid="delivery-log-card">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Delivery Log</CardTitle>
              <CardDescription>
                {webhookName} - {total} deliver{total !== 1 ? "ies" : "y"} total
              </CardDescription>
            </div>
            <Button variant="outline" size="sm" onClick={loadDeliveries} disabled={loading}>
              <RefreshCw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {/* Notification */}
          {notification && (
            <div
              className={`mb-4 p-3 rounded-md ${
                notification.type === "success"
                  ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                  : "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400"
              }`}
            >
              {notification.message}
            </div>
          )}

          {/* Filters */}
          <div className="flex gap-4 mb-4">
            <select
              className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[150px]"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as DeliveryStatus | "")}
              data-testid="delivery-status-filter"
            >
              <option value="">All Status</option>
              <option value="success">Success</option>
              <option value="failed">Failed</option>
              <option value="pending">Pending</option>
            </select>
          </div>

          {/* Error State */}
          {error && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <p className="text-destructive mb-4">{error}</p>
              <Button variant="outline" onClick={loadDeliveries}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          )}

          {/* Loading State */}
          {loading && !deliveriesData && (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="flex items-center gap-4 py-3">
                  <div className="h-4 w-32 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-48 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-20 bg-muted animate-pulse rounded" />
                </div>
              ))}
            </div>
          )}

          {/* Empty State */}
          {!loading && !error && deliveries.length === 0 && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <Send className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No deliveries found</p>
            </div>
          )}

          {/* Delivery Table */}
          {!error && deliveries.length > 0 && (
            <>
              <div className="space-y-2">
                {deliveries.map((delivery) => (
                  <div key={delivery.id} className="border rounded-lg" data-testid={`delivery-row-${delivery.id}`}>
                    <div
                      className="flex items-center justify-between p-4 cursor-pointer hover:bg-muted/50"
                      onClick={() => toggleExpanded(delivery.id)}
                    >
                      <div className="flex items-center gap-4">
                        <DeliveryStatusBadge status={delivery.status} />
                        <WebhookEventBadge eventType={delivery.event_type} />
                        <span className="text-sm text-muted-foreground">
                          {formatTimestamp(delivery.created_at)}
                        </span>
                      </div>
                      <div className="flex items-center gap-4">
                        {delivery.response_status && (
                          <span className="text-sm">
                            HTTP {delivery.response_status}
                          </span>
                        )}
                        <span className="text-sm text-muted-foreground">
                          {formatDuration(delivery.duration_ms)}
                        </span>
                        {delivery.retry_count > 0 && (
                          <span className="text-xs bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-400 px-2 py-0.5 rounded">
                            {delivery.retry_count} retries
                          </span>
                        )}
                        {expandedDelivery === delivery.id ? (
                          <ChevronUp className="h-4 w-4" />
                        ) : (
                          <ChevronDown className="h-4 w-4" />
                        )}
                      </div>
                    </div>

                    {/* Expanded Details */}
                    {expandedDelivery === delivery.id && (
                      <div className="border-t p-4 bg-muted/30">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          {/* Request Payload */}
                          <div>
                            <h4 className="text-sm font-medium mb-2">Request Payload</h4>
                            <pre className="text-xs bg-muted p-3 rounded-md overflow-auto max-h-[200px]">
                              {formatJson(delivery.request_payload) || "No payload"}
                            </pre>
                          </div>

                          {/* Response */}
                          <div>
                            <h4 className="text-sm font-medium mb-2">Response</h4>
                            {delivery.error_message ? (
                              <div className="text-xs bg-red-50 dark:bg-red-900/20 text-red-800 dark:text-red-400 p-3 rounded-md">
                                {delivery.error_message}
                              </div>
                            ) : (
                              <pre className="text-xs bg-muted p-3 rounded-md overflow-auto max-h-[200px]">
                                {formatJson(delivery.response_body) || "No response body"}
                              </pre>
                            )}
                          </div>
                        </div>

                        {/* Response Headers */}
                        {delivery.response_headers && Object.keys(delivery.response_headers).length > 0 && (
                          <div className="mt-4">
                            <h4 className="text-sm font-medium mb-2">Response Headers</h4>
                            <div className="text-xs bg-muted p-3 rounded-md">
                              {Object.entries(delivery.response_headers).map(([key, value]) => (
                                <div key={key}>
                                  <span className="font-medium">{key}:</span> {value}
                                </div>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* Actions */}
                        {delivery.status === "failed" && (
                          <div className="mt-4 flex justify-end">
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => handleResend(delivery)}
                              disabled={resendLoading}
                              data-testid={`delivery-resend-${delivery.id}`}
                            >
                              {resendLoading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                              <Send className="h-4 w-4 mr-2" />
                              Resend
                            </Button>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {page} of {totalPages}
                  </p>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.max(1, p - 1))}
                      disabled={page === 1}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                      disabled={page === totalPages}
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </>
  );
}
