"use client";

import { useState, useEffect } from "react";
import type { Geofence, AdminDevice, CreateGeofenceRequest, GeofenceShape } from "@/types";
import { adminDevicesApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RefreshCw, Circle, Pentagon } from "lucide-react";

interface GeofenceFormProps {
  geofence?: Geofence;
  onSubmit: (data: CreateGeofenceRequest) => Promise<void>;
  loading?: boolean;
}

export function GeofenceForm({ geofence, onSubmit, loading }: GeofenceFormProps) {
  const [name, setName] = useState(geofence?.name || "");
  const [deviceId, setDeviceId] = useState(geofence?.device_id || "");
  const [shape, setShape] = useState<GeofenceShape>(geofence?.shape || "circle");
  const [centerLat, setCenterLat] = useState(geofence?.center_latitude?.toString() || "");
  const [centerLng, setCenterLng] = useState(geofence?.center_longitude?.toString() || "");
  const [radius, setRadius] = useState(geofence?.radius_meters?.toString() || "100");
  const [triggerEnter, setTriggerEnter] = useState(geofence?.trigger_on_enter ?? true);
  const [triggerExit, setTriggerExit] = useState(geofence?.trigger_on_exit ?? true);
  const [triggerDwell, setTriggerDwell] = useState(geofence?.trigger_on_dwell ?? false);
  const [dwellTime, setDwellTime] = useState(geofence?.dwell_time_seconds?.toString() || "300");
  const [errors, setErrors] = useState<Record<string, string>>({});

  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();

  useEffect(() => {
    fetchDevices(() => adminDevicesApi.list({ limit: 100 }));
  }, [fetchDevices]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!name.trim()) {
      newErrors.name = "Name is required";
    }

    if (!deviceId) {
      newErrors.deviceId = "Device is required";
    }

    if (shape === "circle") {
      if (!centerLat || isNaN(parseFloat(centerLat))) {
        newErrors.centerLat = "Valid latitude is required";
      } else {
        const lat = parseFloat(centerLat);
        if (lat < -90 || lat > 90) {
          newErrors.centerLat = "Latitude must be between -90 and 90";
        }
      }

      if (!centerLng || isNaN(parseFloat(centerLng))) {
        newErrors.centerLng = "Valid longitude is required";
      } else {
        const lng = parseFloat(centerLng);
        if (lng < -180 || lng > 180) {
          newErrors.centerLng = "Longitude must be between -180 and 180";
        }
      }

      if (!radius || isNaN(parseFloat(radius)) || parseFloat(radius) <= 0) {
        newErrors.radius = "Valid radius is required (> 0)";
      }
    }

    if (triggerDwell) {
      if (!dwellTime || isNaN(parseInt(dwellTime)) || parseInt(dwellTime) <= 0) {
        newErrors.dwellTime = "Valid dwell time is required (> 0)";
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    const data: CreateGeofenceRequest = {
      name: name.trim(),
      device_id: deviceId,
      shape,
      trigger_on_enter: triggerEnter,
      trigger_on_exit: triggerExit,
      trigger_on_dwell: triggerDwell,
      dwell_time_seconds: triggerDwell ? parseInt(dwellTime) : undefined,
    };

    if (shape === "circle") {
      data.center_latitude = parseFloat(centerLat);
      data.center_longitude = parseFloat(centerLng);
      data.radius_meters = parseFloat(radius);
    }

    await onSubmit(data);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Basic Info */}
      <div className="space-y-4">
        <h3 className="text-lg font-medium">Basic Information</h3>

        <div className="space-y-2">
          <Label htmlFor="name">Geofence Name</Label>
          <Input
            id="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g., Home, Office, School"
          />
          {errors.name && <p className="text-sm text-destructive">{errors.name}</p>}
        </div>

        <div className="space-y-2">
          <Label htmlFor="device">Device</Label>
          <select
            id="device"
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={deviceId}
            onChange={(e) => setDeviceId(e.target.value)}
            disabled={!!geofence} // Can't change device on edit
          >
            <option value="">Select a device</option>
            {devicesData?.items?.map((device) => (
              <option key={device.id} value={device.id}>
                {device.display_name} ({device.organization_name})
              </option>
            ))}
          </select>
          {errors.deviceId && <p className="text-sm text-destructive">{errors.deviceId}</p>}
        </div>
      </div>

      {/* Shape Selection */}
      <div className="space-y-4">
        <h3 className="text-lg font-medium">Shape</h3>

        <div className="grid grid-cols-2 gap-4">
          <button
            type="button"
            className={`p-4 border rounded-lg flex flex-col items-center gap-2 transition-colors ${
              shape === "circle"
                ? "border-primary bg-primary/10"
                : "border-input hover:border-primary/50"
            }`}
            onClick={() => setShape("circle")}
          >
            <Circle className="h-8 w-8" />
            <span className="font-medium">Circle</span>
            <span className="text-xs text-muted-foreground">Center point + radius</span>
          </button>

          <button
            type="button"
            className={`p-4 border rounded-lg flex flex-col items-center gap-2 transition-colors ${
              shape === "polygon"
                ? "border-primary bg-primary/10"
                : "border-input hover:border-primary/50"
            }`}
            onClick={() => setShape("polygon")}
            disabled // Polygon editing not implemented yet
          >
            <Pentagon className="h-8 w-8" />
            <span className="font-medium">Polygon</span>
            <span className="text-xs text-muted-foreground">Coming soon</span>
          </button>
        </div>
      </div>

      {/* Circle Configuration */}
      {shape === "circle" && (
        <div className="space-y-4">
          <h3 className="text-lg font-medium">Circle Configuration</h3>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="centerLat">Center Latitude</Label>
              <Input
                id="centerLat"
                type="number"
                step="any"
                value={centerLat}
                onChange={(e) => setCenterLat(e.target.value)}
                placeholder="e.g., 37.7749"
              />
              {errors.centerLat && (
                <p className="text-sm text-destructive">{errors.centerLat}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="centerLng">Center Longitude</Label>
              <Input
                id="centerLng"
                type="number"
                step="any"
                value={centerLng}
                onChange={(e) => setCenterLng(e.target.value)}
                placeholder="e.g., -122.4194"
              />
              {errors.centerLng && (
                <p className="text-sm text-destructive">{errors.centerLng}</p>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="radius">Radius (meters)</Label>
            <Input
              id="radius"
              type="number"
              min="1"
              value={radius}
              onChange={(e) => setRadius(e.target.value)}
              placeholder="e.g., 100"
            />
            {errors.radius && <p className="text-sm text-destructive">{errors.radius}</p>}
          </div>
        </div>
      )}

      {/* Triggers */}
      <div className="space-y-4">
        <h3 className="text-lg font-medium">Triggers</h3>

        <div className="space-y-3">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={triggerEnter}
              onChange={(e) => setTriggerEnter(e.target.checked)}
              className="rounded border-input"
            />
            <div>
              <p className="font-medium">Trigger on Enter</p>
              <p className="text-sm text-muted-foreground">
                Fire event when device enters the geofence
              </p>
            </div>
          </label>

          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={triggerExit}
              onChange={(e) => setTriggerExit(e.target.checked)}
              className="rounded border-input"
            />
            <div>
              <p className="font-medium">Trigger on Exit</p>
              <p className="text-sm text-muted-foreground">
                Fire event when device exits the geofence
              </p>
            </div>
          </label>

          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={triggerDwell}
              onChange={(e) => setTriggerDwell(e.target.checked)}
              className="rounded border-input"
            />
            <div>
              <p className="font-medium">Trigger on Dwell</p>
              <p className="text-sm text-muted-foreground">
                Fire event when device stays inside for a duration
              </p>
            </div>
          </label>

          {triggerDwell && (
            <div className="ml-7 space-y-2">
              <Label htmlFor="dwellTime">Dwell Time (seconds)</Label>
              <Input
                id="dwellTime"
                type="number"
                min="1"
                value={dwellTime}
                onChange={(e) => setDwellTime(e.target.value)}
                placeholder="e.g., 300"
              />
              {errors.dwellTime && (
                <p className="text-sm text-destructive">{errors.dwellTime}</p>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Submit */}
      <div className="flex justify-end gap-2">
        <Button type="submit" disabled={loading}>
          {loading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
          {geofence ? "Update Geofence" : "Create Geofence"}
        </Button>
      </div>
    </form>
  );
}
