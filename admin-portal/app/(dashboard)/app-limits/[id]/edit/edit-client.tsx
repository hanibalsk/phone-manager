"use client";

import { useEffect, useState } from "react";
import type { AppLimit } from "@/types";
import { AppLimitForm } from "@/components/app-limits";
import { appLimitsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";

interface Props {
  limitId: string;
}

export function EditAppLimitClient({ limitId }: Props) {
  const [limit, setLimit] = useState<AppLimit | null>(null);

  const { loading, execute: fetchLimit } = useApi<AppLimit>();

  useEffect(() => {
    if (limitId) {
      fetchLimit(async () => {
        const response = await appLimitsApi.get(limitId);
        if (response.data) {
          setLimit(response.data);
        }
        return response;
      });
    }
  }, [limitId, fetchLimit]);

  if (loading) {
    return (
      <div className="container mx-auto py-6">
        <div className="flex justify-center py-8">
          <div className="text-muted-foreground">Loading limit...</div>
        </div>
      </div>
    );
  }

  if (!limit) {
    return (
      <div className="container mx-auto py-6">
        <div className="flex justify-center py-8">
          <div className="text-muted-foreground">Limit not found</div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6">
      <AppLimitForm limit={limit} />
    </div>
  );
}
