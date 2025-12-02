"use client";

import { useEffect, useCallback } from "react";
import { useParams } from "next/navigation";
import { Header } from "@/components/layout";
import { UsageChart, UsageSummary } from "@/components/usage";
import { deviceApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { RefreshCw, ArrowLeft } from "lucide-react";
import Link from "next/link";
import type { AppUsage } from "@/types";
import { useState } from "react";

export default function DeviceUsagePage() {
  const params = useParams();
  const deviceId = params.id as string;
  const [date, setDate] = useState(new Date().toISOString().split("T")[0]);

  const { data: usage, loading, execute } = useApi<AppUsage[]>();

  const fetchUsage = useCallback(() => {
    execute(() => deviceApi.getUsage(deviceId, date));
  }, [execute, deviceId, date]);

  useEffect(() => {
    fetchUsage();
  }, [fetchUsage]);

  return (
    <div className="flex flex-col">
      <Header title="App Usage" />
      <div className="p-6 space-y-6">
        <div className="flex items-center justify-between">
          <Button variant="outline" asChild>
            <Link href="/devices">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Devices
            </Link>
          </Button>
          <div className="flex items-center gap-2">
            <Input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="w-auto"
            />
            <Button
              variant="outline"
              size="sm"
              onClick={fetchUsage}
              disabled={loading}
            >
              <RefreshCw
                className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
              />
              Refresh
            </Button>
          </div>
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-16">
            <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <>
            <UsageSummary usage={usage || []} />
            <UsageChart usage={usage || []} />
          </>
        )}
      </div>
    </div>
  );
}
