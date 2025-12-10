"use client";

import type { DeviceLocation } from "@/types";
import { MapPin } from "lucide-react";

interface LocationHistoryListProps {
  locations: DeviceLocation[];
  loading?: boolean;
}

export function LocationHistoryList({ locations, loading }: LocationHistoryListProps) {
  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  if (loading) {
    return (
      <div className="space-y-3">
        {[...Array(10)].map((_, i) => (
          <div key={i} className="flex items-center gap-4 py-3">
            <div className="h-4 w-32 bg-muted animate-pulse rounded" />
            <div className="h-4 w-48 bg-muted animate-pulse rounded" />
            <div className="h-4 w-24 bg-muted animate-pulse rounded" />
            <div className="h-4 w-32 bg-muted animate-pulse rounded" />
          </div>
        ))}
      </div>
    );
  }

  if (locations.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <MapPin className="h-12 w-12 text-muted-foreground mb-4" />
        <p className="text-muted-foreground">No location history found</p>
        <p className="text-sm text-muted-foreground mt-1">
          Try adjusting your query parameters
        </p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr className="border-b">
            <th className="text-left py-3 px-4 font-medium">Timestamp</th>
            <th className="text-left py-3 px-4 font-medium">Device</th>
            <th className="text-left py-3 px-4 font-medium">Coordinates</th>
            <th className="text-left py-3 px-4 font-medium">Accuracy</th>
            <th className="text-left py-3 px-4 font-medium">Altitude</th>
            <th className="text-left py-3 px-4 font-medium">Speed</th>
            <th className="text-left py-3 px-4 font-medium">Battery</th>
          </tr>
        </thead>
        <tbody>
          {locations.map((loc, index) => (
            <tr
              key={`${loc.id}-${index}`}
              className="border-b hover:bg-muted/50"
            >
              <td className="py-3 px-4 text-sm">
                {formatDateTime(loc.timestamp)}
              </td>
              <td className="py-3 px-4">
                <div>
                  <p className="font-medium">{loc.device_name}</p>
                  <p className="text-xs text-muted-foreground">
                    {loc.organization_name}
                  </p>
                </div>
              </td>
              <td className="py-3 px-4 text-sm font-mono">
                {loc.latitude.toFixed(6)}, {loc.longitude.toFixed(6)}
              </td>
              <td className="py-3 px-4 text-sm">
                {Math.round(loc.accuracy)}m
              </td>
              <td className="py-3 px-4 text-sm">
                {loc.altitude !== null ? `${Math.round(loc.altitude)}m` : "-"}
              </td>
              <td className="py-3 px-4 text-sm">
                {loc.speed !== null ? `${loc.speed.toFixed(1)} m/s` : "-"}
              </td>
              <td className="py-3 px-4 text-sm">
                {loc.battery_level !== null
                  ? `${Math.round(loc.battery_level * 100)}%`
                  : "-"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
