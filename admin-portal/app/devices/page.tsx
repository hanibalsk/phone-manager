"use client";

import { useEffect, useCallback } from "react";
import { Header } from "@/components/layout";
import { DeviceList } from "@/components/devices";
import { deviceApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import type { Device } from "@/types";

export default function DevicesPage() {
  const { data: devices, loading, execute } = useApi<Device[]>();

  const fetchDevices = useCallback(() => {
    execute(() => deviceApi.list());
  }, [execute]);

  useEffect(() => {
    fetchDevices();
  }, [fetchDevices]);

  return (
    <div className="flex flex-col">
      <Header title="Devices" />
      <div className="p-6">
        <DeviceList
          devices={devices || []}
          loading={loading}
          onRefresh={fetchDevices}
        />
      </div>
    </div>
  );
}
