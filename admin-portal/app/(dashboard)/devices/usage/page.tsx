"use client";

import { Suspense, useEffect, useCallback, useState } from "react";
import { useSearchParams } from "next/navigation";
import { UsageChart, UsageSummary } from "@/components/usage";
import { deviceApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { RefreshCw, ArrowLeft } from "lucide-react";
import Link from "next/link";
import type { AppUsage } from "@/types";

function DeviceUsageContent() {
  const searchParams = useSearchParams();
  const deviceId = searchParams.get("id");
  const [date, setDate] = useState(new Date().toISOString().split("T")[0]);

  const { data: usage, loading, execute } = useApi<AppUsage[]>();

  const fetchUsage = useCallback(() => {
    if (deviceId) {
      execute(() => deviceApi.getUsage(deviceId, date));
    }
  }, [execute, deviceId, date]);

  useEffect(() => {
    fetchUsage();
  }, [fetchUsage]);

  if (!deviceId) {
    return (
      <div className="p-6">
        <div className="text-center py-16">
          <p className="text-muted-foreground">No device ID provided</p>
          <Button variant="outline" asChild className="mt-4">
            <Link href="/devices/">Back to Devices</Link>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <Button variant="outline" asChild>
          <Link href="/devices/">
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
  );
}

function DeviceUsageFallback() {
  return (
    <div className="p-6">
      <div className="flex items-center justify-center py-16">
        <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    </div>
  );
}

export default function DeviceUsagePage() {
  return (
    <Suspense fallback={<DeviceUsageFallback />}>
      <DeviceUsageContent />
    </Suspense>
  );
}
