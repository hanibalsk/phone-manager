"use client";

import { useState } from "react";
import type { UnlockRequest } from "@/types";
import { RequestActionDialog } from "./request-action-dialog";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Bell, RefreshCw, CheckCircle, XCircle, Clock } from "lucide-react";

interface RequestListProps {
  requests: UnlockRequest[];
  loading: boolean;
  onRefresh: () => void;
  onApprove: (id: string, response?: string) => Promise<void>;
  onDeny: (id: string, response?: string) => Promise<void>;
}

export function RequestList({
  requests,
  loading,
  onRefresh,
  onApprove,
  onDeny,
}: RequestListProps) {
  const [statusFilter, setStatusFilter] = useState<string>("pending");
  const [actionRequest, setActionRequest] = useState<{
    request: UnlockRequest;
    action: "approve" | "deny";
  } | null>(null);

  const filteredRequests = requests.filter((request) => {
    if (statusFilter === "all") return true;
    return request.status === statusFilter;
  });

  const pendingCount = requests.filter((r) => r.status === "pending").length;

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatDuration = (minutes: number) => {
    if (minutes < 60) return `${minutes} min`;
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  };

  const statusConfig: Record<
    string,
    { label: string; variant: "default" | "success" | "destructive" | "warning" }
  > = {
    pending: { label: "Pending", variant: "warning" },
    approved: { label: "Approved", variant: "success" },
    denied: { label: "Denied", variant: "destructive" },
  };

  const handleAction = async (response?: string) => {
    if (!actionRequest) return;

    if (actionRequest.action === "approve") {
      await onApprove(actionRequest.request.id, response);
    } else {
      await onDeny(actionRequest.request.id, response);
    }
    setActionRequest(null);
  };

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                Unlock Requests
                {pendingCount > 0 && (
                  <Badge variant="warning">{pendingCount} pending</Badge>
                )}
              </CardTitle>
              <CardDescription>
                Review and respond to unlock requests from devices
              </CardDescription>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={onRefresh}
              disabled={loading}
            >
              <RefreshCw
                className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
              />
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex gap-2 mb-4">
            {["pending", "approved", "denied", "all"].map((status) => (
              <Button
                key={status}
                variant={statusFilter === status ? "default" : "outline"}
                size="sm"
                onClick={() => setStatusFilter(status)}
              >
                {status.charAt(0).toUpperCase() + status.slice(1)}
              </Button>
            ))}
          </div>

          {loading ? (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : filteredRequests.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <Bell className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">
                {requests.length === 0
                  ? "No unlock requests yet"
                  : `No ${statusFilter} requests`}
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {filteredRequests.map((request) => (
                <div
                  key={request.id}
                  className="flex items-center justify-between p-4 border rounded-lg"
                >
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-medium">{request.device_name}</span>
                      <Badge variant={statusConfig[request.status].variant}>
                        {statusConfig[request.status].label}
                      </Badge>
                    </div>
                    <p className="text-sm text-muted-foreground mb-1">
                      {request.reason}
                    </p>
                    <div className="flex items-center gap-4 text-xs text-muted-foreground">
                      <span className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {formatDuration(request.requested_duration)}
                      </span>
                      <span>{formatDate(request.requested_at)}</span>
                    </div>
                    {request.admin_response && (
                      <p className="text-sm mt-2 italic text-muted-foreground">
                        Response: {request.admin_response}
                      </p>
                    )}
                  </div>
                  {request.status === "pending" && (
                    <div className="flex gap-2 ml-4">
                      <Button
                        variant="outline"
                        size="sm"
                        className="text-green-600 hover:text-green-700"
                        onClick={() =>
                          setActionRequest({ request, action: "approve" })
                        }
                      >
                        <CheckCircle className="h-4 w-4 mr-1" />
                        Approve
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="text-red-600 hover:text-red-700"
                        onClick={() =>
                          setActionRequest({ request, action: "deny" })
                        }
                      >
                        <XCircle className="h-4 w-4 mr-1" />
                        Deny
                      </Button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {actionRequest && (
        <RequestActionDialog
          request={actionRequest.request}
          action={actionRequest.action}
          onConfirm={handleAction}
          onCancel={() => setActionRequest(null)}
        />
      )}
    </>
  );
}
