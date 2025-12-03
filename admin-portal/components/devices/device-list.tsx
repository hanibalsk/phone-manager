"use client";

import { useState } from "react";
import type { Device } from "@/types";
import { DeviceStatusBadge } from "./device-status-badge";
import { DeviceDetails } from "./device-details";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Smartphone, Search, RefreshCw } from "lucide-react";

interface DeviceListProps {
  devices: Device[];
  loading: boolean;
  onRefresh: () => void;
}

export function DeviceList({ devices, loading, onRefresh }: DeviceListProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [selectedDevice, setSelectedDevice] = useState<Device | null>(null);

  const filteredDevices = devices.filter((device) => {
    const matchesSearch = device.name
      .toLowerCase()
      .includes(searchQuery.toLowerCase());

    if (statusFilter === "all") return matchesSearch;

    return matchesSearch && device.status === statusFilter;
  });

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Devices</CardTitle>
              <CardDescription>
                Manage enrolled devices and their settings
              </CardDescription>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={onRefresh}
              disabled={loading}
            >
              <RefreshCw
                className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
              />
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex gap-4 mb-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search devices..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
              />
            </div>
            <select
              className="rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="all">All Status</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
              <option value="pending">Pending</option>
            </select>
          </div>

          {loading ? (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : filteredDevices.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <Smartphone className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">
                {devices.length === 0
                  ? "No devices enrolled yet"
                  : "No devices match your search"}
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="text-left py-3 px-4 font-medium">Device</th>
                    <th className="text-left py-3 px-4 font-medium">
                      Android ID
                    </th>
                    <th className="text-left py-3 px-4 font-medium">
                      Enrolled
                    </th>
                    <th className="text-left py-3 px-4 font-medium">
                      Last Seen
                    </th>
                    <th className="text-left py-3 px-4 font-medium">Status</th>
                    <th className="text-right py-3 px-4 font-medium">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {filteredDevices.map((device) => (
                    <tr
                      key={device.id}
                      className="border-b hover:bg-muted/50 cursor-pointer"
                      onClick={() => setSelectedDevice(device)}
                    >
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2">
                          <Smartphone className="h-4 w-4 text-muted-foreground" />
                          <span className="font-medium">{device.name}</span>
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <code className="text-xs bg-muted px-2 py-1 rounded">
                          {device.android_id.slice(0, 8)}...
                        </code>
                      </td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        {formatDate(device.enrolled_at)}
                      </td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        {formatDate(device.last_seen)}
                      </td>
                      <td className="py-3 px-4">
                        <DeviceStatusBadge lastSeen={device.last_seen} />
                      </td>
                      <td className="py-3 px-4 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedDevice(device);
                          }}
                        >
                          View
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {selectedDevice && (
        <DeviceDetails
          device={selectedDevice}
          onClose={() => setSelectedDevice(null)}
        />
      )}
    </>
  );
}
