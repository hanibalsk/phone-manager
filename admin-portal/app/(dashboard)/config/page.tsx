"use client";

import { useEffect, useCallback } from "react";
import { toast } from "sonner";
import { ConfigDisplay } from "@/components/config";
import { configApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import type { PublicConfig } from "@/types";

export default function ConfigPage() {
  const { data: config, loading, execute } = useApi<PublicConfig>();

  const fetchConfig = useCallback(() => {
    execute(() => configApi.getPublic());
  }, [execute]);

  useEffect(() => {
    fetchConfig();
  }, [fetchConfig]);

  const handleRefresh = () => {
    fetchConfig();
    toast.info("Refreshing configuration...");
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">System Configuration</h1>
        <p className="text-muted-foreground">
          View current server feature flags and authentication settings
        </p>
      </div>
      <ConfigDisplay
        config={config}
        loading={loading}
        onRefresh={handleRefresh}
      />
    </div>
  );
}
