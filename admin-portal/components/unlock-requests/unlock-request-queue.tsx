"use client";

import { useState, useEffect, useCallback } from "react";
import type { AdminUnlockRequest, Organization, AdminDevice, AdminUnlockRequestStatus } from "@/types";
import { RequestStatusBadge } from "./request-status-badge";
import { RequestActionModal } from "./request-action-modal";
import { unlockRequestsApi, organizationsApi, adminDevicesApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import {
  Unlock,
  RefreshCw,
  Smartphone,
  User,
  MessageSquare,
  ChevronDown,
  ChevronUp,
  Clock,
  Settings,
  Zap,
} from "lucide-react";

const ITEMS_PER_PAGE = 20;

export function UnlockRequestQueue() {
  const [organizationId, setOrganizationId] = useState("");
  const [deviceId, setDeviceId] = useState("");
  const [statusFilter, setStatusFilter] = useState<AdminUnlockRequestStatus | "">("");
  const [page, setPage] = useState(1);
  const [expandedRequests, setExpandedRequests] = useState<Set<string>>(new Set());
  const [requestToAction, setRequestToAction] = useState<{ request: AdminUnlockRequest; action: "approve" | "deny" } | null>(null);
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();
  const { data: requestsData, loading, error, execute: fetchRequests } = useApi<{ items: AdminUnlockRequest[]; total: number }>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
    fetchDevices(() => adminDevicesApi.list({ limit: 100 }));
  }, [fetchOrgs, fetchDevices]);

  const loadRequests = useCallback(() => {
    fetchRequests(() =>
      unlockRequestsApi.list({
        organization_id: organizationId || undefined,
        device_id: deviceId || undefined,
        status: statusFilter || undefined,
        page,
        limit: ITEMS_PER_PAGE,
      })
    );
  }, [fetchRequests, organizationId, deviceId, statusFilter, page]);

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

  // Clear notification after 3 seconds
  useEffect(() => {
    if (notification) {
      const timer = setTimeout(() => setNotification(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [notification]);

  // Filter devices by organization
  const filteredDevices = organizationId
    ? devicesData?.items?.filter((d) => d.organization_id === organizationId)
    : devicesData?.items;

  const toggleExpand = (requestId: string) => {
    const newExpanded = new Set(expandedRequests);
    if (newExpanded.has(requestId)) {
      newExpanded.delete(requestId);
    } else {
      newExpanded.add(requestId);
    }
    setExpandedRequests(newExpanded);
  };

  const handleActionComplete = (success: boolean, message: string) => {
    setNotification({ type: success ? "success" : "error", message });
    setRequestToAction(null);
    if (success) {
      loadRequests();
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString();
  };

  const formatDuration = (minutes: number) => {
    if (minutes >= 60) {
      const hours = Math.floor(minutes / 60);
      const mins = minutes % 60;
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${minutes}m`;
  };

  const requests = requestsData?.items || [];
  const pendingCount = requests.filter((r) => r.status === "pending").length;
  const total = requestsData?.total || 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Unlock className="h-6 w-6" />
            <div>
              <CardTitle>Unlock Requests</CardTitle>
              <CardDescription>
                Manage device unlock requests
                {pendingCount > 0 && (
                  <span className="ml-2 text-yellow-600 font-medium">
                    ({pendingCount} pending)
                  </span>
                )}
              </CardDescription>
            </div>
          </div>
          <div className="flex gap-2">
            <Link href="/unlock-requests/rules">
              <Button variant="outline" size="sm">
                <Settings className="mr-2 h-4 w-4" />
                Auto-Approval Rules
              </Button>
            </Link>
            <Button variant="outline" size="sm" onClick={loadRequests}>
              <RefreshCw className="mr-2 h-4 w-4" />
              Refresh
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {/* Notification */}
        {notification && (
          <div
            className={`mb-4 p-3 rounded-md ${
              notification.type === "success"
                ? "bg-green-100 text-green-800"
                : "bg-red-100 text-red-800"
            }`}
          >
            {notification.message}
          </div>
        )}

        {/* Filters */}
        <div className="flex flex-wrap gap-4 mb-6">
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={organizationId}
            onChange={(e) => {
              setOrganizationId(e.target.value);
              setDeviceId("");
            }}
          >
            <option value="">All Organizations</option>
            {orgsData?.items?.map((org) => (
              <option key={org.id} value={org.id}>
                {org.name}
              </option>
            ))}
          </select>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={deviceId}
            onChange={(e) => setDeviceId(e.target.value)}
          >
            <option value="">All Devices</option>
            {filteredDevices?.map((device) => (
              <option key={device.id} value={device.id}>
                {device.display_name}
              </option>
            ))}
          </select>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as AdminUnlockRequestStatus | "")}
          >
            <option value="">All Status</option>
            <option value="pending">Pending</option>
            <option value="approved">Approved</option>
            <option value="denied">Denied</option>
            <option value="expired">Expired</option>
            <option value="cancelled">Cancelled</option>
          </select>
        </div>

        {/* Request List */}
        {loading ? (
          <div className="flex justify-center py-8">
            <div className="text-muted-foreground">Loading requests...</div>
          </div>
        ) : error ? (
          <div className="flex justify-center py-8 text-red-500">
            Error loading requests
          </div>
        ) : requests.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
            <Unlock className="h-12 w-12 mb-4 opacity-50" />
            <p>No unlock requests found</p>
          </div>
        ) : (
          <>
            <div className="space-y-3">
              {requests.map((request) => {
                const isExpanded = expandedRequests.has(request.id);
                const isPending = request.status === "pending";

                return (
                  <div
                    key={request.id}
                    className={`border rounded-lg overflow-hidden ${
                      isPending ? "border-yellow-200 bg-yellow-50/50" : ""
                    }`}
                  >
                    <div
                      className="p-4 flex items-center justify-between cursor-pointer hover:bg-secondary/30"
                      onClick={() => toggleExpand(request.id)}
                    >
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-1">
                          <RequestStatusBadge status={request.status} />
                          {request.auto_approved && (
                            <span className="inline-flex items-center gap-1 text-xs text-purple-600 bg-purple-100 px-2 py-0.5 rounded-full">
                              <Zap className="h-3 w-3" />
                              Auto
                            </span>
                          )}
                          <span className="text-sm text-muted-foreground">
                            {formatDate(request.created_at)}
                          </span>
                        </div>
                        <div className="flex items-center gap-4 text-sm">
                          <span className="flex items-center gap-1">
                            <Smartphone className="h-4 w-4 text-muted-foreground" />
                            {request.device_name}
                          </span>
                          <span className="flex items-center gap-1">
                            <User className="h-4 w-4 text-muted-foreground" />
                            {request.user_name}
                          </span>
                          <span className="flex items-center gap-1">
                            <Clock className="h-4 w-4 text-muted-foreground" />
                            {formatDuration(request.requested_duration_minutes)}
                          </span>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {isPending && (
                          <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
                            <Button
                              size="sm"
                              onClick={() => setRequestToAction({ request, action: "approve" })}
                            >
                              Approve
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => setRequestToAction({ request, action: "deny" })}
                            >
                              Deny
                            </Button>
                          </div>
                        )}
                        {isExpanded ? (
                          <ChevronUp className="h-5 w-5 text-muted-foreground" />
                        ) : (
                          <ChevronDown className="h-5 w-5 text-muted-foreground" />
                        )}
                      </div>
                    </div>

                    {isExpanded && (
                      <div className="border-t bg-secondary/20 p-4 space-y-3">
                        {/* Reason */}
                        <div>
                          <div className="flex items-center gap-1 text-sm font-medium mb-1">
                            <MessageSquare className="h-4 w-4" />
                            Reason
                          </div>
                          <div className="text-sm text-muted-foreground bg-background p-3 rounded">
                            {request.reason || "No reason provided"}
                          </div>
                        </div>

                        {/* Details */}
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                          <div>
                            <div className="text-muted-foreground">Organization</div>
                            <div className="font-medium">{request.organization_name}</div>
                          </div>
                          <div>
                            <div className="text-muted-foreground">User Email</div>
                            <div className="font-medium">{request.user_email}</div>
                          </div>
                          <div>
                            <div className="text-muted-foreground">Requested Duration</div>
                            <div className="font-medium">{formatDuration(request.requested_duration_minutes)}</div>
                          </div>
                          {request.approved_duration_minutes && (
                            <div>
                              <div className="text-muted-foreground">Approved Duration</div>
                              <div className="font-medium text-green-600">
                                {formatDuration(request.approved_duration_minutes)}
                              </div>
                            </div>
                          )}
                        </div>

                        {/* Action info */}
                        {request.actioned_at && (
                          <div className="text-sm">
                            <span className="text-muted-foreground">
                              {request.status === "approved" ? "Approved" : "Denied"} by{" "}
                            </span>
                            <span className="font-medium">{request.actioned_by_name || "System"}</span>
                            <span className="text-muted-foreground"> at {formatDate(request.actioned_at)}</span>
                          </div>
                        )}

                        {/* Deny note */}
                        {request.deny_note && (
                          <div className="text-sm">
                            <span className="text-muted-foreground">Deny reason: </span>
                            <span>{request.deny_note}</span>
                          </div>
                        )}

                        {/* Expiration */}
                        {request.expires_at && (
                          <div className="text-sm">
                            <span className="text-muted-foreground">Expires: </span>
                            <span>{formatDate(request.expires_at)}</span>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-4">
                <div className="text-sm text-muted-foreground">
                  Showing {(page - 1) * ITEMS_PER_PAGE + 1} to{" "}
                  {Math.min(page * ITEMS_PER_PAGE, total)} of {total} requests
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 1}
                    onClick={() => setPage(page - 1)}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === totalPages}
                    onClick={() => setPage(page + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </CardContent>

      {/* Action Modal */}
      {requestToAction && (
        <RequestActionModal
          request={requestToAction.request}
          action={requestToAction.action}
          onClose={() => setRequestToAction(null)}
          onComplete={handleActionComplete}
        />
      )}
    </Card>
  );
}
