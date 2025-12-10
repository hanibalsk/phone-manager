"use client";

import { useState, useEffect, useCallback } from "react";
import type { Webhook, Organization, WebhookStatus, WebhookEventType } from "@/types";
import { WebhookStatusBadge } from "./webhook-status-badge";
import { WebhookEventBadge } from "./webhook-event-badge";
import { WebhookTestModal } from "./webhook-test-modal";
import { webhooksApi, organizationsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { useDebounce } from "@/hooks/use-debounce";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import Link from "next/link";
import {
  Webhook as WebhookIcon,
  RefreshCw,
  Plus,
  Search,
  Trash2,
  ToggleLeft,
  ToggleRight,
  ExternalLink,
  CheckCircle2,
  XCircle,
  History,
  PlayCircle,
} from "lucide-react";

const ITEMS_PER_PAGE = 20;

const EVENT_TYPES: WebhookEventType[] = [
  "location_update",
  "geofence_event",
  "proximity_alert",
  "trip_complete",
  "device_status",
];

export function AdminWebhookList() {
  const [search, setSearch] = useState("");
  const [organizationId, setOrganizationId] = useState("");
  const [statusFilter, setStatusFilter] = useState<WebhookStatus | "">("");
  const [eventTypeFilter, setEventTypeFilter] = useState<WebhookEventType | "">("");
  const [page, setPage] = useState(1);
  const [webhookToDelete, setWebhookToDelete] = useState<Webhook | null>(null);
  const [webhookToTest, setWebhookToTest] = useState<Webhook | null>(null);
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const debouncedSearch = useDebounce(search, 300);

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: webhooksData, loading, error, execute: fetchWebhooks } = useApi<{ items: Webhook[]; total: number }>();
  const { loading: toggleLoading, execute: executeToggle } = useApi<Webhook>();
  const { loading: deleteLoading, execute: executeDelete } = useApi<void>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
  }, [fetchOrgs]);

  const loadWebhooks = useCallback(() => {
    fetchWebhooks(() =>
      webhooksApi.list({
        search: debouncedSearch || undefined,
        organization_id: organizationId || undefined,
        status: statusFilter || undefined,
        event_type: eventTypeFilter || undefined,
        page,
        limit: ITEMS_PER_PAGE,
      })
    );
  }, [fetchWebhooks, debouncedSearch, organizationId, statusFilter, eventTypeFilter, page]);

  useEffect(() => {
    loadWebhooks();
  }, [loadWebhooks]);

  const showNotification = (type: "success" | "error", message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 3000);
  };

  const handleToggle = async (webhook: Webhook) => {
    const result = await executeToggle(() => webhooksApi.toggle(webhook.id));
    if (result) {
      const newStatus = result.status === "active" ? "activated" : "paused";
      showNotification("success", `Webhook ${newStatus}`);
      loadWebhooks();
    } else {
      showNotification("error", "Failed to toggle webhook");
    }
  };

  const handleDelete = async () => {
    if (!webhookToDelete) return;

    const result = await executeDelete(() => webhooksApi.delete(webhookToDelete.id));
    if (result !== null) {
      showNotification("success", "Webhook deleted");
      setWebhookToDelete(null);
      loadWebhooks();
    } else {
      showNotification("error", "Failed to delete webhook");
    }
  };

  const webhooks = webhooksData?.items || [];
  const total = webhooksData?.total || 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  const formatUrl = (url: string) => {
    try {
      const urlObj = new URL(url);
      return `${urlObj.hostname}${urlObj.pathname.length > 20 ? urlObj.pathname.slice(0, 20) + "..." : urlObj.pathname}`;
    } catch {
      return url.slice(0, 40) + (url.length > 40 ? "..." : "");
    }
  };

  return (
    <>
      <Card data-testid="webhook-list-card">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Webhooks</CardTitle>
              <CardDescription>
                {total} webhook{total !== 1 ? "s" : ""} total
              </CardDescription>
            </div>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={loadWebhooks} disabled={loading}>
                <RefreshCw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
                Refresh
              </Button>
              <Link href="/webhooks/new">
                <Button size="sm">
                  <Plus className="h-4 w-4 mr-2" />
                  New Webhook
                </Button>
              </Link>
            </div>
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
          <div className="flex flex-wrap gap-4 mb-4">
            <div className="flex-1 min-w-[200px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search by name or URL..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-9"
                  data-testid="webhook-search-input"
                />
              </div>
            </div>
            <select
              className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[180px]"
              value={organizationId}
              onChange={(e) => setOrganizationId(e.target.value)}
              data-testid="webhook-org-filter"
            >
              <option value="">All Organizations</option>
              {orgsData?.items?.map((org) => (
                <option key={org.id} value={org.id}>
                  {org.name}
                </option>
              ))}
            </select>
            <select
              className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[120px]"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as WebhookStatus | "")}
              data-testid="webhook-status-filter"
            >
              <option value="">All Status</option>
              <option value="active">Active</option>
              <option value="paused">Paused</option>
              <option value="failed">Failed</option>
            </select>
            <select
              className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[150px]"
              value={eventTypeFilter}
              onChange={(e) => setEventTypeFilter(e.target.value as WebhookEventType | "")}
              data-testid="webhook-event-filter"
            >
              <option value="">All Event Types</option>
              {EVENT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type.replace(/_/g, " ").replace(/\b\w/g, (l) => l.toUpperCase())}
                </option>
              ))}
            </select>
          </div>

          {/* Error State */}
          {error && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <p className="text-destructive mb-4">{error}</p>
              <Button variant="outline" onClick={loadWebhooks}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          )}

          {/* Loading State */}
          {loading && !webhooksData && (
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
          {!loading && !error && webhooks.length === 0 && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <WebhookIcon className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No webhooks found</p>
              <Link href="/webhooks/new" className="mt-4">
                <Button>
                  <Plus className="h-4 w-4 mr-2" />
                  Create First Webhook
                </Button>
              </Link>
            </div>
          )}

          {/* Webhook Table */}
          {!error && webhooks.length > 0 && (
            <>
              <div className="overflow-x-auto">
                <table className="w-full" data-testid="webhook-table">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left py-3 px-4 font-medium">Name</th>
                      <th className="text-left py-3 px-4 font-medium">URL</th>
                      <th className="text-left py-3 px-4 font-medium">Events</th>
                      <th className="text-left py-3 px-4 font-medium">Status</th>
                      <th className="text-left py-3 px-4 font-medium">Deliveries</th>
                      <th className="text-right py-3 px-4 font-medium">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {webhooks.map((webhook) => (
                      <tr key={webhook.id} className="border-b hover:bg-muted/50" data-testid={`webhook-row-${webhook.id}`}>
                        <td className="py-3 px-4">
                          <div>
                            <p className="font-medium">{webhook.name}</p>
                            <p className="text-xs text-muted-foreground">
                              {webhook.organization_name}
                            </p>
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-1">
                            <code className="text-xs bg-muted px-1.5 py-0.5 rounded">
                              {formatUrl(webhook.url)}
                            </code>
                            <a
                              href={webhook.url}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-muted-foreground hover:text-foreground"
                              title="Open URL"
                            >
                              <ExternalLink className="h-3 w-3" />
                            </a>
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex flex-wrap gap-1">
                            {webhook.event_types.map((type) => (
                              <WebhookEventBadge key={type} eventType={type} compact />
                            ))}
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <WebhookStatusBadge status={webhook.status} />
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-3 text-xs">
                            <span className="flex items-center gap-1 text-green-600 dark:text-green-400">
                              <CheckCircle2 className="h-3 w-3" />
                              {webhook.success_count}
                            </span>
                            <span className="flex items-center gap-1 text-red-600 dark:text-red-400">
                              <XCircle className="h-3 w-3" />
                              {webhook.failure_count}
                            </span>
                          </div>
                        </td>
                        <td className="py-3 px-4 text-right">
                          <div className="flex justify-end gap-1">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setWebhookToTest(webhook)}
                              title="Test Webhook"
                            >
                              <PlayCircle className="h-4 w-4" />
                            </Button>
                            <Link href={`/webhooks/${webhook.id}/deliveries`}>
                              <Button variant="ghost" size="sm" title="View Deliveries">
                                <History className="h-4 w-4" />
                              </Button>
                            </Link>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleToggle(webhook)}
                              disabled={toggleLoading}
                              title={webhook.status === "active" ? "Pause" : "Activate"}
                            >
                              {webhook.status === "active" ? (
                                <ToggleRight className="h-4 w-4" />
                              ) : (
                                <ToggleLeft className="h-4 w-4" />
                              )}
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setWebhookToDelete(webhook)}
                              className="text-destructive hover:text-destructive"
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
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

      {/* Delete Confirmation */}
      {webhookToDelete && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
          data-testid="webhook-delete-dialog"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setWebhookToDelete(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <h2 className="text-lg font-semibold mb-2">Delete Webhook</h2>
            <p className="text-sm text-muted-foreground mb-4">
              Are you sure you want to delete &quot;{webhookToDelete.name}&quot;? This will also
              delete all delivery history. This action cannot be undone.
            </p>
            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                onClick={() => setWebhookToDelete(null)}
                disabled={deleteLoading}
                data-testid="webhook-delete-cancel"
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleDelete}
                disabled={deleteLoading}
                data-testid="webhook-delete-confirm"
              >
                {deleteLoading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Test Modal */}
      {webhookToTest && (
        <WebhookTestModal
          webhook={webhookToTest}
          onClose={() => setWebhookToTest(null)}
        />
      )}
    </>
  );
}
