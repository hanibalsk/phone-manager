"use client";

import { useEffect, useCallback } from "react";
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
    await unlockApi.approve(id, response);
    fetchRequests();
  };

  const handleDeny = async (id: string, response?: string) => {
    await unlockApi.deny(id, response);
    fetchRequests();
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
