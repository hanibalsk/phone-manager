"use client";

import { useMemo } from "react";
import type { DeviceLocation } from "@/types";

interface LocationHistoryMapProps {
  locations: DeviceLocation[];
  loading?: boolean;
}

export function LocationHistoryMap({ locations, loading }: LocationHistoryMapProps) {
  // Calculate bounds for the map
  const bounds = useMemo(() => {
    if (locations.length === 0) {
      return { minLat: 0, maxLat: 0, minLng: 0, maxLng: 0 };
    }

    const lats = locations.map((l) => l.latitude);
    const lngs = locations.map((l) => l.longitude);

    return {
      minLat: Math.min(...lats),
      maxLat: Math.max(...lats),
      minLng: Math.min(...lngs),
      maxLng: Math.max(...lngs),
    };
  }, [locations]);

  // Group locations by device for path drawing
  const devicePaths = useMemo(() => {
    const groups = new Map<string, DeviceLocation[]>();
    for (const loc of locations) {
      const existing = groups.get(loc.device_id) || [];
      existing.push(loc);
      groups.set(loc.device_id, existing);
    }

    // Sort each group by timestamp
    Array.from(groups.entries()).forEach(([, locs]) => {
      locs.sort(
        (a: DeviceLocation, b: DeviceLocation) =>
          new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
      );
    });

    return groups;
  }, [locations]);

  // Convert lat/lng to percentage position on the map
  const getPosition = (lat: number, lng: number) => {
    const latRange = bounds.maxLat - bounds.minLat || 1;
    const lngRange = bounds.maxLng - bounds.minLng || 1;

    // Add padding
    const padding = 0.1;
    const latPadded = latRange * (1 + padding * 2);
    const lngPadded = lngRange * (1 + padding * 2);

    const x = ((lng - bounds.minLng + lngRange * padding) / lngPadded) * 100;
    const y = ((bounds.maxLat + latRange * padding - lat) / latPadded) * 100;

    return { x: Math.max(2, Math.min(98, x)), y: Math.max(2, Math.min(98, y)) };
  };

  // Generate colors for each device
  const deviceColors = useMemo(() => {
    const colors = [
      "#3b82f6", // blue
      "#22c55e", // green
      "#ef4444", // red
      "#f59e0b", // amber
      "#8b5cf6", // violet
      "#ec4899", // pink
      "#06b6d4", // cyan
      "#84cc16", // lime
    ];
    const colorMap = new Map<string, string>();
    let i = 0;
    Array.from(devicePaths.keys()).forEach((deviceId) => {
      colorMap.set(deviceId, colors[i % colors.length]);
      i++;
    });
    return colorMap;
  }, [devicePaths]);

  if (loading) {
    return (
      <div className="relative w-full h-[400px] bg-muted rounded-lg flex items-center justify-center">
        <div className="text-muted-foreground">Loading map data...</div>
      </div>
    );
  }

  if (locations.length === 0) {
    return (
      <div className="relative w-full h-[400px] bg-muted rounded-lg flex items-center justify-center">
        <div className="text-center">
          <p className="text-muted-foreground">No location data to display</p>
        </div>
      </div>
    );
  }

  return (
    <div className="relative w-full h-[400px] bg-slate-100 dark:bg-slate-800 rounded-lg overflow-hidden">
      {/* Map grid background */}
      <div
        className="absolute inset-0 opacity-20"
        style={{
          backgroundImage: `
            linear-gradient(to right, currentColor 1px, transparent 1px),
            linear-gradient(to bottom, currentColor 1px, transparent 1px)
          `,
          backgroundSize: "40px 40px",
        }}
      />

      {/* SVG for paths */}
      <svg className="absolute inset-0 w-full h-full">
        {Array.from(devicePaths.entries()).map(([deviceId, locs]) => {
          const color = deviceColors.get(deviceId) || "#3b82f6";
          const points = locs.map((loc) => {
            const pos = getPosition(loc.latitude, loc.longitude);
            return `${pos.x}%,${pos.y}%`;
          });

          if (points.length < 2) return null;

          // Create path data
          const pathData = points
            .map((point, i) => {
              const [x, y] = point.split(",");
              return i === 0 ? `M ${x} ${y}` : `L ${x} ${y}`;
            })
            .join(" ");

          return (
            <path
              key={deviceId}
              d={pathData}
              fill="none"
              stroke={color}
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              opacity="0.7"
            />
          );
        })}
      </svg>

      {/* Points */}
      {Array.from(devicePaths.entries()).map(([deviceId, locs]) => {
        const color = deviceColors.get(deviceId) || "#3b82f6";
        return locs.map((loc, index) => {
          const pos = getPosition(loc.latitude, loc.longitude);
          const isFirst = index === 0;
          const isLast = index === locs.length - 1;

          return (
            <div
              key={`${loc.id}-${index}`}
              className="absolute transform -translate-x-1/2 -translate-y-1/2"
              style={{
                left: `${pos.x}%`,
                top: `${pos.y}%`,
              }}
              title={`${loc.device_name} - ${new Date(loc.timestamp).toLocaleString()}`}
            >
              <div
                className={`rounded-full border-2 border-white shadow ${
                  isFirst || isLast ? "w-4 h-4" : "w-2 h-2"
                }`}
                style={{ backgroundColor: color }}
              />
            </div>
          );
        });
      })}

      {/* Legend */}
      <div className="absolute bottom-2 right-2 bg-background/90 px-3 py-2 rounded text-xs">
        <div className="font-medium mb-1">Devices</div>
        <div className="space-y-1 max-h-[150px] overflow-y-auto">
          {Array.from(devicePaths.entries()).map(([deviceId, locs]) => (
            <div key={deviceId} className="flex items-center gap-2">
              <div
                className="w-3 h-3 rounded-full"
                style={{ backgroundColor: deviceColors.get(deviceId) }}
              />
              <span className="truncate max-w-[120px]">
                {locs[0]?.device_name || deviceId}
              </span>
              <span className="text-muted-foreground">({locs.length})</span>
            </div>
          ))}
        </div>
      </div>

      {/* Info overlay */}
      <div className="absolute top-2 left-2 bg-background/80 px-2 py-1 rounded text-xs text-muted-foreground">
        {locations.length} point{locations.length !== 1 ? "s" : ""}
      </div>
    </div>
  );
}
