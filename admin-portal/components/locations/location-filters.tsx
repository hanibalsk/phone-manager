"use client";

import { useState, useEffect, useCallback } from "react";
import type { Organization, AdminDevice } from "@/types";
import { organizationsApi, adminDevicesApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { X, Filter } from "lucide-react";

interface LocationFiltersProps {
  onFilterChange: (filters: {
    organization_id?: string;
    device_id?: string;
    from?: string;
    to?: string;
  }) => void;
}

export function LocationFilters({ onFilterChange }: LocationFiltersProps) {
  const [organizationId, setOrganizationId] = useState<string>("");
  const [deviceId, setDeviceId] = useState<string>("");
  const [fromDate, setFromDate] = useState<string>("");
  const [toDate, setToDate] = useState<string>("");

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: devicesData, execute: fetchDevices } = useApi<{ items: AdminDevice[] }>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
    fetchDevices(() => adminDevicesApi.list({ limit: 100 }));
  }, [fetchOrgs, fetchDevices]);

  const applyFilters = useCallback(() => {
    onFilterChange({
      organization_id: organizationId || undefined,
      device_id: deviceId || undefined,
      from: fromDate || undefined,
      to: toDate || undefined,
    });
  }, [onFilterChange, organizationId, deviceId, fromDate, toDate]);

  const clearFilters = () => {
    setOrganizationId("");
    setDeviceId("");
    setFromDate("");
    setToDate("");
    onFilterChange({});
  };

  const hasFilters = organizationId || deviceId || fromDate || toDate;

  // Filter devices by selected organization
  const filteredDevices = devicesData?.items?.filter(
    (d) => !organizationId || d.organization_id === organizationId
  ) || [];

  return (
    <div className="bg-muted/30 rounded-lg p-4 space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4 text-muted-foreground" />
          <span className="text-sm font-medium">Filters</span>
        </div>
        {hasFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters}>
            <X className="h-4 w-4 mr-1" />
            Clear
          </Button>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="space-y-2">
          <Label htmlFor="organization">Organization</Label>
          <select
            id="organization"
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={organizationId}
            onChange={(e) => {
              setOrganizationId(e.target.value);
              setDeviceId(""); // Reset device when org changes
            }}
          >
            <option value="">All Organizations</option>
            {orgsData?.items?.map((org) => (
              <option key={org.id} value={org.id}>
                {org.name}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-2">
          <Label htmlFor="device">Device</Label>
          <select
            id="device"
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={deviceId}
            onChange={(e) => setDeviceId(e.target.value)}
          >
            <option value="">All Devices</option>
            {filteredDevices.map((device) => (
              <option key={device.id} value={device.id}>
                {device.display_name}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-2">
          <Label htmlFor="from">From Date</Label>
          <Input
            id="from"
            type="datetime-local"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="to">To Date</Label>
          <Input
            id="to"
            type="datetime-local"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
          />
        </div>
      </div>

      <div className="flex justify-end">
        <Button onClick={applyFilters}>
          Apply Filters
        </Button>
      </div>
    </div>
  );
}
