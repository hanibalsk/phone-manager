"use client";

import { useEffect, useCallback } from "react";
import { toast } from "sonner";
import { Header } from "@/components/layout";
import { RequestList } from "@/components/unlock-requests";
import { unlockApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import type { UnlockRequest } from "@/types";

export default function UnlockRequestsPage() {
  const { data: requests, loading, execute } = useApi<UnlockRequest[]>();

  const fetchRequests = useCallback(() => {
    execute(() => unlockApi.list());
  }, [execute]);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const handleApprove = async (id: string, response?: string) => {
    try {
      const result = await unlockApi.approve(id, response);
      if (result.error) {
        toast.error("Failed to approve request", { description: result.error });
        return;
      }
      toast.success("Request approved successfully");
      fetchRequests();
    } catch (error) {
      toast.error("Failed to approve request", {
        description: error instanceof Error ? error.message : "An unexpected error occurred",
      });
    }
  };

  const handleDeny = async (id: string, response?: string) => {
    try {
      const result = await unlockApi.deny(id, response);
      if (result.error) {
        toast.error("Failed to deny request", { description: result.error });
        return;
      }
      toast.success("Request denied");
      fetchRequests();
    } catch (error) {
      toast.error("Failed to deny request", {
        description: error instanceof Error ? error.message : "An unexpected error occurred",
      });
    }
  };

  return (
    <div className="flex flex-col">
      <Header title="Unlock Requests" />
      <div className="p-6">
        <RequestList
          requests={requests || []}
          loading={loading}
          onRefresh={fetchRequests}
          onApprove={handleApprove}
          onDeny={handleDeny}
        />
      </div>
    </div>
  );
}
