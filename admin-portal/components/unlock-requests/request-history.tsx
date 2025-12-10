"use client";

import { useState, useEffect, useCallback } from "react";
import type { AdminUnlockRequest, AdminUnlockRequestStatus } from "@/types";
import { RequestStatusBadge } from "./request-status-badge";
import { unlockRequestsApi } from "@/lib/api-client";
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
  History,
  RefreshCw,
  Clock,
  User,
  MessageSquare,
  Zap,
} from "lucide-react";

const ITEMS_PER_PAGE = 10;

interface RequestHistoryProps {
  deviceId: string;
  deviceName: string;
}

export function RequestHistory({ deviceId, deviceName }: RequestHistoryProps) {
  const [statusFilter, setStatusFilter] = useState<AdminUnlockRequestStatus | "">("");
  const [page, setPage] = useState(1);

  const { data: requestsData, loading, error, execute: fetchRequests } = useApi<{ items: AdminUnlockRequest[]; total: number }>();

  const loadRequests = useCallback(() => {
    fetchRequests(() =>
      unlockRequestsApi.list({
        device_id: deviceId,
        status: statusFilter || undefined,
        page,
        limit: ITEMS_PER_PAGE,
      })
    );
  }, [fetchRequests, deviceId, statusFilter, page]);

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

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
  const total = requestsData?.total || 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  // Calculate statistics
  const stats = {
    total: requests.length,
    approved: requests.filter((r) => r.status === "approved").length,
    denied: requests.filter((r) => r.status === "denied").length,
    autoApproved: requests.filter((r) => r.auto_approved).length,
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <History className="h-5 w-5" />
            <div>
              <CardTitle className="text-lg">Request History</CardTitle>
              <CardDescription>
                Unlock request history for {deviceName}
              </CardDescription>
            </div>
          </div>
          <Button variant="outline" size="sm" onClick={loadRequests}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Refresh
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {/* Statistics */}
        <div className="grid grid-cols-4 gap-4 mb-6">
          <div className="bg-secondary/50 rounded-lg p-3 text-center">
            <div className="text-2xl font-bold">{stats.total}</div>
            <div className="text-xs text-muted-foreground">Total Requests</div>
          </div>
          <div className="bg-green-50 rounded-lg p-3 text-center">
            <div className="text-2xl font-bold text-green-600">{stats.approved}</div>
            <div className="text-xs text-muted-foreground">Approved</div>
          </div>
          <div className="bg-red-50 rounded-lg p-3 text-center">
            <div className="text-2xl font-bold text-red-600">{stats.denied}</div>
            <div className="text-xs text-muted-foreground">Denied</div>
          </div>
          <div className="bg-purple-50 rounded-lg p-3 text-center">
            <div className="text-2xl font-bold text-purple-600">{stats.autoApproved}</div>
            <div className="text-xs text-muted-foreground">Auto-Approved</div>
          </div>
        </div>

        {/* Filter */}
        <div className="mb-4">
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
            <div className="text-muted-foreground">Loading history...</div>
          </div>
        ) : error ? (
          <div className="flex justify-center py-8 text-red-500">
            Error loading history
          </div>
        ) : requests.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
            <History className="h-8 w-8 mb-2 opacity-50" />
            <p>No request history found</p>
          </div>
        ) : (
          <>
            <div className="space-y-3">
              {requests.map((request) => (
                <div
                  key={request.id}
                  className="border rounded-lg p-4"
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <RequestStatusBadge status={request.status} />
                      {request.auto_approved && (
                        <span className="inline-flex items-center gap-1 text-xs text-purple-600 bg-purple-100 px-2 py-0.5 rounded-full">
                          <Zap className="h-3 w-3" />
                          Auto
                        </span>
                      )}
                    </div>
                    <span className="text-sm text-muted-foreground">
                      {formatDate(request.created_at)}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
                    <div className="flex items-center gap-1">
                      <Clock className="h-4 w-4 text-muted-foreground" />
                      <span>Requested: {formatDuration(request.requested_duration_minutes)}</span>
                    </div>
                    {request.approved_duration_minutes && (
                      <div className="flex items-center gap-1 text-green-600">
                        <Clock className="h-4 w-4" />
                        <span>Approved: {formatDuration(request.approved_duration_minutes)}</span>
                      </div>
                    )}
                    <div className="flex items-center gap-1">
                      <User className="h-4 w-4 text-muted-foreground" />
                      <span>{request.user_name}</span>
                    </div>
                    {request.actioned_by_name && (
                      <div className="text-muted-foreground">
                        By: {request.actioned_by_name}
                      </div>
                    )}
                  </div>

                  {request.reason && (
                    <div className="mt-2 text-sm">
                      <div className="flex items-center gap-1 text-muted-foreground mb-1">
                        <MessageSquare className="h-3 w-3" />
                        Reason
                      </div>
                      <div className="bg-secondary/50 p-2 rounded text-sm">
                        {request.reason}
                      </div>
                    </div>
                  )}

                  {request.deny_note && (
                    <div className="mt-2 text-sm text-red-600">
                      Denial reason: {request.deny_note}
                    </div>
                  )}
                </div>
              ))}
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
    </Card>
  );
}
