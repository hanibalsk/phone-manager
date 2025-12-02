"use client";

import { useEffect, useCallback, useState } from "react";
import { Header } from "@/components/layout";
import { LimitList } from "@/components/limits";
import { limitsApi, deviceApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import type { DailyLimit, Device } from "@/types";

export default function LimitsPage() {
  const [selectedDeviceId, setSelectedDeviceId] = useState<string>("");
  const { data: devices, execute: fetchDevices } = useApi<Device[]>();
  const { data: limits, loading, execute: fetchLimits } = useApi<DailyLimit[]>();

  useEffect(() => {
    fetchDevices(() => deviceApi.list());
  }, [fetchDevices]);

  useEffect(() => {
    if (devices && devices.length > 0 && !selectedDeviceId) {
      setSelectedDeviceId(devices[0].id);
    }
  }, [devices, selectedDeviceId]);

  const refreshLimits = useCallback(() => {
    if (selectedDeviceId) {
      fetchLimits(() => limitsApi.list(selectedDeviceId));
    }
  }, [fetchLimits, selectedDeviceId]);

  useEffect(() => {
    refreshLimits();
  }, [refreshLimits]);

  const handleCreate = async (data: Omit<DailyLimit, "id">) => {
    await limitsApi.set(selectedDeviceId, data);
    refreshLimits();
  };

  const handleUpdate = async (id: string, data: Partial<DailyLimit>) => {
    await limitsApi.update(selectedDeviceId, id, data);
    refreshLimits();
  };

  const handleDelete = async (id: string) => {
    await limitsApi.delete(selectedDeviceId, id);
    refreshLimits();
  };

  return (
    <div className="flex flex-col">
      <Header title="App Limits" />
      <div className="p-6 space-y-6">
        {devices && devices.length > 0 && (
          <div className="flex items-center gap-4">
            <label className="text-sm font-medium">Device:</label>
            <select
              className="rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={selectedDeviceId}
              onChange={(e) => setSelectedDeviceId(e.target.value)}
            >
              {devices.map((device) => (
                <option key={device.id} value={device.id}>
                  {device.name}
                </option>
              ))}
            </select>
          </div>
        )}

        <LimitList
          limits={limits || []}
          loading={loading}
          onRefresh={refreshLimits}
          onCreate={handleCreate}
          onUpdate={handleUpdate}
          onDelete={handleDelete}
        />
      </div>
    </div>
  );
}
