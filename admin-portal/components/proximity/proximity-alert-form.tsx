"use client";

import { useState, useEffect } from "react";
import type { ProximityAlert, CreateProximityAlertRequest, AdminDevice, Organization } from "@/types";
import { adminDevicesApi, organizationsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2 } from "lucide-react";

interface ProximityAlertFormProps {
  alert?: ProximityAlert;
  onSubmit: (data: CreateProximityAlertRequest) => Promise<void>;
  loading?: boolean;
}

export function ProximityAlertForm({ alert, onSubmit, loading }: ProximityAlertFormProps) {
  const [name, setName] = useState(alert?.name || "");
  const [deviceAId, setDeviceAId] = useState(alert?.device_a_id || "");
  const [deviceBId, setDeviceBId] = useState(alert?.device_b_id || "");
  const [triggerDistance, setTriggerDistance] = useState(
    alert?.trigger_distance_meters?.toString() || "100"
  );
  const [cooldownSeconds, setCooldownSeconds] = useState(
    alert?.cooldown_seconds?.toString() || "300"
  );
  const [selectedOrg, setSelectedOrg] = useState("");

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
  }, [fetchOrgs]);

  useEffect(() => {
    if (selectedOrg) {
      fetchDevices(() => adminDevicesApi.list({ organization_id: selectedOrg, limit: 100 }));
    }
  }, [fetchDevices, selectedOrg]);

  // Set organization from existing alert
  useEffect(() => {
    if (alert?.organization_id && orgsData?.items) {
      setSelectedOrg(alert.organization_id);
    }
  }, [alert, orgsData]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    await onSubmit({
      name,
      device_a_id: deviceAId,
      device_b_id: deviceBId,
      trigger_distance_meters: parseInt(triggerDistance, 10),
      cooldown_seconds: parseInt(cooldownSeconds, 10),
    });
  };

  const isValid =
    name.trim() &&
    deviceAId &&
    deviceBId &&
    deviceAId !== deviceBId &&
    parseInt(triggerDistance, 10) > 0 &&
    parseInt(cooldownSeconds, 10) >= 0;

  // Filter out already selected device for the other dropdown
  const availableDevicesA = devicesData?.items || [];
  const availableDevicesB = devicesData?.items?.filter((d) => d.id !== deviceAId) || [];

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="name">Alert Name *</Label>
        <Input
          id="name"
          placeholder="e.g., Parent-Child Proximity"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <p className="text-xs text-muted-foreground">
          A descriptive name for this proximity alert
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="organization">Organization *</Label>
        <select
          id="organization"
          className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
          value={selectedOrg}
          onChange={(e) => {
            setSelectedOrg(e.target.value);
            setDeviceAId("");
            setDeviceBId("");
          }}
          required
        >
          <option value="">Select organization...</option>
          {orgsData?.items?.map((org) => (
            <option key={org.id} value={org.id}>
              {org.name}
            </option>
          ))}
        </select>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="deviceA">Device A *</Label>
          <select
            id="deviceA"
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={deviceAId}
            onChange={(e) => setDeviceAId(e.target.value)}
            required
            disabled={!selectedOrg}
          >
            <option value="">Select device...</option>
            {availableDevicesA.map((device) => (
              <option key={device.id} value={device.id}>
                {device.display_name}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-2">
          <Label htmlFor="deviceB">Device B *</Label>
          <select
            id="deviceB"
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={deviceBId}
            onChange={(e) => setDeviceBId(e.target.value)}
            required
            disabled={!selectedOrg || !deviceAId}
          >
            <option value="">Select device...</option>
            {availableDevicesB.map((device) => (
              <option key={device.id} value={device.id}>
                {device.display_name}
              </option>
            ))}
          </select>
          {deviceAId && deviceBId && deviceAId === deviceBId && (
            <p className="text-xs text-destructive">
              Device B must be different from Device A
            </p>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="distance">Trigger Distance (meters) *</Label>
          <Input
            id="distance"
            type="number"
            min="1"
            max="50000"
            value={triggerDistance}
            onChange={(e) => setTriggerDistance(e.target.value)}
            required
          />
          <p className="text-xs text-muted-foreground">
            Alert triggers when devices are within this distance (1-50,000m)
          </p>
        </div>

        <div className="space-y-2">
          <Label htmlFor="cooldown">Cooldown Period (seconds) *</Label>
          <Input
            id="cooldown"
            type="number"
            min="0"
            max="86400"
            value={cooldownSeconds}
            onChange={(e) => setCooldownSeconds(e.target.value)}
            required
          />
          <p className="text-xs text-muted-foreground">
            Minimum time between triggers (0 = no cooldown, max 24h)
          </p>
        </div>
      </div>

      {/* Common presets */}
      <div className="space-y-2">
        <Label>Quick Presets</Label>
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => {
              setTriggerDistance("50");
              setCooldownSeconds("300");
            }}
          >
            Close Range (50m, 5min cooldown)
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => {
              setTriggerDistance("100");
              setCooldownSeconds("600");
            }}
          >
            Medium Range (100m, 10min cooldown)
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => {
              setTriggerDistance("500");
              setCooldownSeconds("1800");
            }}
          >
            Wide Range (500m, 30min cooldown)
          </Button>
        </div>
      </div>

      <div className="flex justify-end gap-3">
        <Button type="button" variant="outline" onClick={() => window.history.back()}>
          Cancel
        </Button>
        <Button type="submit" disabled={!isValid || loading}>
          {loading ? (
            <>
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              {alert ? "Updating..." : "Creating..."}
            </>
          ) : (
            <>{alert ? "Update Alert" : "Create Alert"}</>
          )}
        </Button>
      </div>
    </form>
  );
}
