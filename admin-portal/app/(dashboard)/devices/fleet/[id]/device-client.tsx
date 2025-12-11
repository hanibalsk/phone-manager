"use client";

import { useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import type { DeviceDetails } from "@/types";
import { adminDevicesApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { AdminDeviceInfo } from "@/components/devices/admin-device-info";
import { AdminDeviceStatus } from "@/components/devices/admin-device-status";
import { AdminDeviceMetrics } from "@/components/devices/admin-device-metrics";
import { AdminDeviceActions } from "@/components/devices/admin-device-actions";
import { Button } from "@/components/ui/button";
import { ChevronLeft, RefreshCw, Smartphone } from "lucide-react";
import Link from "next/link";

interface Props {
  deviceId: string;
}

export function DeviceDetailsClient({ deviceId }: Props) {
  const router = useRouter();

  const { data: device, loading, error, execute } = useApi<DeviceDetails>();

  const fetchDevice = useCallback(() => {
    execute(() => adminDevicesApi.get(deviceId));
  }, [execute, deviceId]);

  useEffect(() => {
    fetchDevice();
  }, [fetchDevice]);

  const handleActionComplete = () => {
    fetchDevice();
  };

  const handleDeleteComplete = () => {
    router.push("/devices/fleet");
  };

  if (loading && !device) {
    return (
      <div className="p-6">
        {/* Breadcrumb skeleton */}
        <div className="flex items-center gap-2 mb-6">
          <div className="h-4 w-20 bg-muted animate-pulse rounded" />
          <div className="h-4 w-4 bg-muted animate-pulse rounded" />
          <div className="h-4 w-32 bg-muted animate-pulse rounded" />
        </div>

        {/* Header skeleton */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 bg-muted animate-pulse rounded" />
            <div>
              <div className="h-6 w-48 bg-muted animate-pulse rounded mb-2" />
              <div className="h-4 w-32 bg-muted animate-pulse rounded" />
            </div>
          </div>
          <div className="h-9 w-24 bg-muted animate-pulse rounded" />
        </div>

        {/* Content skeleton */}
        <div className="grid gap-6 md:grid-cols-2">
          <div className="h-64 bg-muted animate-pulse rounded-lg" />
          <div className="h-64 bg-muted animate-pulse rounded-lg" />
          <div className="h-48 bg-muted animate-pulse rounded-lg" />
          <div className="h-48 bg-muted animate-pulse rounded-lg" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <Smartphone className="h-12 w-12 text-muted-foreground mb-4" />
          <h2 className="text-lg font-medium mb-2">Device Not Found</h2>
          <p className="text-sm text-muted-foreground mb-4">{error}</p>
          <div className="flex gap-2">
            <Button variant="outline" onClick={fetchDevice}>
              <RefreshCw className="h-4 w-4 mr-2" />
              Retry
            </Button>
            <Link href="/devices/fleet">
              <Button>
                <ChevronLeft className="h-4 w-4 mr-2" />
                Back to Devices
              </Button>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (!device) {
    return null;
  }

  return (
    <div className="p-6">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm text-muted-foreground mb-6">
        <Link href="/devices/fleet" className="hover:text-foreground">
          Devices
        </Link>
        <span>/</span>
        <span className="text-foreground">{device.display_name}</span>
      </nav>

      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-muted rounded-lg">
            <Smartphone className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">{device.display_name}</h1>
            <p className="text-sm text-muted-foreground">
              {device.platform === "android" ? "Android" : "iOS"} Device
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={fetchDevice} disabled={loading}>
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
          <Link href="/devices/fleet">
            <Button variant="outline" size="sm">
              <ChevronLeft className="h-4 w-4 mr-2" />
              Back
            </Button>
          </Link>
        </div>
      </div>

      {/* Content Grid */}
      <div className="grid gap-6 md:grid-cols-2">
        <AdminDeviceInfo device={device} />
        <AdminDeviceStatus device={device} />
        <AdminDeviceMetrics device={device} />
        <AdminDeviceActions
          device={device}
          onActionComplete={handleActionComplete}
          onDelete={handleDeleteComplete}
        />
      </div>
    </div>
  );
}
