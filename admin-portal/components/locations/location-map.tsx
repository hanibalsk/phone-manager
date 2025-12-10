"use client";

import { useState, useMemo } from "react";
import type { LatestDeviceLocation, AdminDeviceStatus } from "@/types";
import { DeviceMarkerPopup } from "./device-marker-popup";

interface LocationMapProps {
  locations: LatestDeviceLocation[];
  loading?: boolean;
}

const statusColors: Record<AdminDeviceStatus, string> = {
  active: "#22c55e",
  suspended: "#ef4444",
  offline: "#9ca3af",
  pending: "#eab308",
};

interface Cluster {
  lat: number;
  lng: number;
  devices: LatestDeviceLocation[];
}

function clusterLocations(
  locations: LatestDeviceLocation[],
  clusterDistance: number = 0.01
): Cluster[] {
  const clusters: Cluster[] = [];

  for (const location of locations) {
    let added = false;
    for (const cluster of clusters) {
      const distance = Math.sqrt(
        Math.pow(location.latitude - cluster.lat, 2) +
        Math.pow(location.longitude - cluster.lng, 2)
      );
      if (distance < clusterDistance) {
        cluster.devices.push(location);
        // Recalculate cluster center
        cluster.lat = cluster.devices.reduce((sum, d) => sum + d.latitude, 0) / cluster.devices.length;
        cluster.lng = cluster.devices.reduce((sum, d) => sum + d.longitude, 0) / cluster.devices.length;
        added = true;
        break;
      }
    }
    if (!added) {
      clusters.push({
        lat: location.latitude,
        lng: location.longitude,
        devices: [location],
      });
    }
  }

  return clusters;
}

export function LocationMap({ locations, loading }: LocationMapProps) {
  const [selectedDevice, setSelectedDevice] = useState<LatestDeviceLocation | null>(null);
  const [expandedCluster, setExpandedCluster] = useState<Cluster | null>(null);

  // Calculate bounds for the map
  const bounds = useMemo(() => {
    if (locations.length === 0) {
      return { minLat: 0, maxLat: 0, minLng: 0, maxLng: 0, centerLat: 40, centerLng: -100 };
    }

    const lats = locations.map((l) => l.latitude);
    const lngs = locations.map((l) => l.longitude);

    return {
      minLat: Math.min(...lats),
      maxLat: Math.max(...lats),
      minLng: Math.min(...lngs),
      maxLng: Math.max(...lngs),
      centerLat: (Math.min(...lats) + Math.max(...lats)) / 2,
      centerLng: (Math.min(...lngs) + Math.max(...lngs)) / 2,
    };
  }, [locations]);

  // Cluster nearby locations
  const clusters = useMemo(() => clusterLocations(locations), [locations]);

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

    return { x: Math.max(5, Math.min(95, x)), y: Math.max(5, Math.min(95, y)) };
  };

  if (loading) {
    return (
      <div className="relative w-full h-[500px] bg-muted rounded-lg flex items-center justify-center">
        <div className="text-muted-foreground">Loading map data...</div>
      </div>
    );
  }

  if (locations.length === 0) {
    return (
      <div className="relative w-full h-[500px] bg-muted rounded-lg flex items-center justify-center">
        <div className="text-center">
          <p className="text-muted-foreground">No location data available</p>
          <p className="text-sm text-muted-foreground mt-1">
            Try adjusting your filters or check back later
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="relative w-full h-[500px] bg-slate-100 dark:bg-slate-800 rounded-lg overflow-hidden">
      {/* Map grid background */}
      <div
        className="absolute inset-0 opacity-20"
        style={{
          backgroundImage: `
            linear-gradient(to right, currentColor 1px, transparent 1px),
            linear-gradient(to bottom, currentColor 1px, transparent 1px)
          `,
          backgroundSize: "50px 50px",
        }}
      />

      {/* Map title overlay */}
      <div className="absolute top-2 left-2 bg-background/80 px-2 py-1 rounded text-xs text-muted-foreground">
        {locations.length} device{locations.length !== 1 ? "s" : ""} shown
      </div>

      {/* Markers */}
      {clusters.map((cluster, index) => {
        const pos = getPosition(cluster.lat, cluster.lng);
        const isCluster = cluster.devices.length > 1;

        return (
          <div
            key={index}
            className="absolute transform -translate-x-1/2 -translate-y-1/2 cursor-pointer transition-transform hover:scale-110"
            style={{
              left: `${pos.x}%`,
              top: `${pos.y}%`,
              zIndex: isCluster ? 10 : 5,
            }}
            onClick={() => {
              if (isCluster) {
                setExpandedCluster(expandedCluster === cluster ? null : cluster);
                setSelectedDevice(null);
              } else {
                setSelectedDevice(cluster.devices[0]);
                setExpandedCluster(null);
              }
            }}
          >
            {isCluster ? (
              <div
                className="w-10 h-10 rounded-full flex items-center justify-center text-white text-sm font-bold shadow-lg border-2 border-white"
                style={{ backgroundColor: "#3b82f6" }}
              >
                {cluster.devices.length}
              </div>
            ) : (
              <div
                className="w-4 h-4 rounded-full shadow-lg border-2 border-white"
                style={{ backgroundColor: statusColors[cluster.devices[0].status] }}
                title={cluster.devices[0].device_name}
              />
            )}
          </div>
        );
      })}

      {/* Expanded cluster popup */}
      {expandedCluster && (
        <div
          className="absolute z-20 bg-background border rounded-lg shadow-lg p-3 max-w-[200px]"
          style={{
            left: `${getPosition(expandedCluster.lat, expandedCluster.lng).x}%`,
            top: `${getPosition(expandedCluster.lat, expandedCluster.lng).y}%`,
            transform: "translate(-50%, 20px)",
          }}
        >
          <div className="text-sm font-medium mb-2">
            {expandedCluster.devices.length} Devices
          </div>
          <div className="space-y-1 max-h-[200px] overflow-y-auto">
            {expandedCluster.devices.map((device) => (
              <button
                key={device.device_id}
                className="w-full text-left px-2 py-1 text-sm hover:bg-muted rounded flex items-center gap-2"
                onClick={() => {
                  setSelectedDevice(device);
                  setExpandedCluster(null);
                }}
              >
                <div
                  className="w-2 h-2 rounded-full"
                  style={{ backgroundColor: statusColors[device.status] }}
                />
                {device.device_name}
              </button>
            ))}
          </div>
          <button
            className="mt-2 text-xs text-muted-foreground hover:text-foreground"
            onClick={() => setExpandedCluster(null)}
          >
            Close
          </button>
        </div>
      )}

      {/* Device popup */}
      {selectedDevice && (
        <div
          className="absolute z-30"
          style={{
            left: `${getPosition(selectedDevice.latitude, selectedDevice.longitude).x}%`,
            top: `${getPosition(selectedDevice.latitude, selectedDevice.longitude).y}%`,
            transform: "translate(-50%, 20px)",
          }}
        >
          <DeviceMarkerPopup
            device={selectedDevice}
            onClose={() => setSelectedDevice(null)}
          />
        </div>
      )}

      {/* Legend */}
      <div className="absolute bottom-2 right-2 bg-background/90 px-3 py-2 rounded text-xs">
        <div className="font-medium mb-1">Status</div>
        <div className="space-y-1">
          {Object.entries(statusColors).map(([status, color]) => (
            <div key={status} className="flex items-center gap-2">
              <div
                className="w-2 h-2 rounded-full"
                style={{ backgroundColor: color }}
              />
              <span className="capitalize">{status}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
